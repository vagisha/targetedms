package org.labkey.targetedms.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.PrecursorManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FixAreaRatiosJob extends PipelineJob
{
    private CrawlType _crawlType = CrawlType.verifyOnly;

    /** For JSON serialization/deserialzation round-tripping
     * @noinspection unused*/
    protected FixAreaRatiosJob()
    {

    }

    public FixAreaRatiosJob(ViewBackgroundInfo info, @NotNull PipeRoot root)
    {
        super(TargetedMSPipelineProvider.name, info, root);
        setLogFile(new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("FixAreaRatiosJob", "log")));

//        getLogger().info("Description of key statuses:");
//        for (Chromatogram.SourceStatus value : Chromatogram.SourceStatus.values())
//        {
//            getLogger().info("\t" + value.toString() + ": " + value.getDescription());
//        }
//        getLogger().info("\n");
    }

    public enum CrawlType
    {
        verifyOnly
        {
            @Override
            public SimpleFilter getFilter()
            {
                return new SimpleFilter();
            }
        };

        public abstract SimpleFilter getFilter();
    }

    @Override
    public void run()
    {
        setStatus(TaskStatus.running);
        var containers = ContainerManager.getAllChildren(getContainer(), getUser());

        getLogger().info("Starting to correct area ratios over " + containers.size() + " containers");

        Summary summary = new Summary();

        int containerCount = 0;
        int runCount = 0;
        // Loop through all containers
        for (Container container : containers)
        {
            containerCount++;

            // Get the number of runs in the container
            TargetedMSRun[] runs = TargetedMSManager.getRunsInContainer(container);
            runCount += runs.length;

            var schema = new TargetedMSSchema(getUser(), container);
            for(TargetedMSRun run: runs)
            {
                int pepAreaRatiosCount = getPeptideAreaRatiosCount(run, schema);
                if(pepAreaRatiosCount > 0)
                {
                    RunSummary runSummary = fixRatiosForRun(run, schema);
                    summary.add(runSummary);
                    getLogger().info(runSummary.toString());
                }
            }

        }
        getLogger().info("All done!");
        getLogger().info("Containers: " + containerCount + ", Run Count: " + runCount);
        getLogger().info(summary.toString());
        setStatus(TaskStatus.complete);
    }

    private RunSummary fixRatiosForRun(TargetedMSRun run, TargetedMSSchema schema)
    {
        /*getLogger().info("Starting processing of container " + container.getPath() + " (" + containerCount + " of " + containers.size() + ") with " + containerPCICount + " chromatogram rows to process");

        var handler = new PrecursorChromInfoHandler(container);
        try
        {
            // Use an uncached result set so that we can stream however many rows we have in the DB without
            // blowing out the heap
            try (ResultSet rs = selector.getResultSet(false, false))
            {
                while (rs.next())
                {
                    handler.exec(rs.getInt("Id"), rs.getInt("SampleFileId"));
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        for (RunSummary summary : handler._summaries)
        {
            getLogger().info(summary);
            crawlSummary.add(summary);
        }*/
        RunSummary summary = new RunSummary(run);
        summary._peptideRatioCount = getPeptideAreaRatiosCount(run, schema);
        summary._precursorRatioCount = getPrecursorAreaRatiosCount(run, schema);
        summary._transitionRatioCount = getTransitionAreaRatiosCount(run, schema);
        return summary;
    }

    private int getPeptideAreaRatiosCount(TargetedMSRun run, TargetedMSSchema schema)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ").append(TargetedMSManager.getTableInfoPeptideAreaRatio(), "pa")
                .append(" INNER JOIN ").append(TargetedMSManager.getTableInfoIsotopeLabel(), "iso").append(" ON iso.id = pa.isotopeLabelId")
                .append(" WHERE iso.runId=?").add(run.getId());
        return new SqlSelector(schema.getDbSchema(), sql).getObject(Integer.class);
    }

    private int getPrecursorAreaRatiosCount(TargetedMSRun run, TargetedMSSchema schema)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ").append(TargetedMSManager.getTableInfoPrecursorAreaRatio(), "pa")
                .append(" INNER JOIN ").append(TargetedMSManager.getTableInfoIsotopeLabel(), "iso").append(" ON iso.id = pa.isotopeLabelId")
                .append(" WHERE iso.runId=?").add(run.getId());
        return new SqlSelector(schema.getDbSchema(), sql).getObject(Integer.class);
    }

    private int getTransitionAreaRatiosCount(TargetedMSRun run, TargetedMSSchema schema)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ").append(TargetedMSManager.getTableInfoTransitionAreaRatio(), "ta")
                .append(" INNER JOIN ").append(TargetedMSManager.getTableInfoIsotopeLabel(), "iso").append(" ON iso.id = ta.isotopeLabelId")
                .append(" WHERE iso.runId=?").add(run.getId());
        return new SqlSelector(schema.getDbSchema(), sql).getObject(Integer.class);
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Fixing area ratios: " + _crawlType;
    }

    private static class Summary
    {
        private int _runsWithRatios;
        private int _runsUpdated;
        private int _transitionRatioCount;
        private int _precursorRatioCount;
        private int _peptideRatioCount;
        private int _wrongTransitionRatios;
        private int _wrongPrecursorRatios;
        private int _wrongPeptideRatios;

        public void add(RunSummary runSummary)
        {
            _runsWithRatios++;
            if(runSummary.needsUpdate())
            {
                _runsUpdated++;
            }

            _transitionRatioCount += runSummary._transitionRatioCount;
            _wrongTransitionRatios += runSummary._wrongTransitionRatios;
            _precursorRatioCount += runSummary._precursorRatioCount;
            _wrongPrecursorRatios += runSummary._wrongPrecursorRatios;
            _peptideRatioCount += runSummary._peptideRatioCount;
            _wrongPeptideRatios += runSummary._wrongPeptideRatios;
        }

        @Override
        public String toString()
        {
            return String.format("Summary: Runs: %d,   Runs updated: %d,  Peptide Ratios: %d (Wrong: %d), Precursor Ratios: %d (Wrong: %d), Transition Ratios: %d (Wrong: %d)",
                    _runsWithRatios,
                    _runsUpdated,
                    _peptideRatioCount,
                    _wrongPeptideRatios,
                    _precursorRatioCount,
                    _wrongPrecursorRatios,
                    _transitionRatioCount,
                    _wrongTransitionRatios);
        }
    }

    /**  */
    private static class RunSummary
    {
        private final TargetedMSRun _run;

        private int _transitionRatioCount;
        private int _precursorRatioCount;
        private int _peptideRatioCount;
        private int _wrongTransitionRatios;
        private int _wrongPrecursorRatios;
        private int _wrongPeptideRatios;

        public RunSummary(TargetedMSRun run)
        {
            _run = run;
        }

        @Override
        public String toString()
        {
            return String.format("Skyline document %7d.  Peptide Ratios: %d (Wrong: %d), Precursor Ratios: %d (Wrong: %d),   Transition Ratios: %d (Wrong: %d)",
                    _run.getId(),
                    _peptideRatioCount,
                    _wrongPeptideRatios,
                    _precursorRatioCount,
                    _wrongPrecursorRatios,
                    _transitionRatioCount,
                    _wrongTransitionRatios);
        }

        public boolean needsUpdate()
        {
            return _wrongPeptideRatios > 0 || _wrongPrecursorRatios > 0 || _wrongTransitionRatios > 0;
        }
    }

    private class PrecursorChromInfoHandler
    {
        private final Container _container;
        private SampleFile _sampleFile;
        private RunSummary _currentSummary;
        private TargetedMSRun _run;

        private int _rowsForCurrentRun;

        private final List<RunSummary> _summaries = new ArrayList<>();

        public PrecursorChromInfoHandler(Container container)
        {
            _container = container;
        }

        public void exec(int id, long sampleFileId)
        {
            // We're sorted by sample file, so keep
            if (_sampleFile == null || _sampleFile.getId() != sampleFileId)
            {
                _sampleFile = TargetedMSManager.getSampleFile(sampleFileId, _container);
                if (_sampleFile == null)
                {
                    throw new IllegalStateException("Could not find sample file " + sampleFileId);
                }
                var replicate = TargetedMSManager.getReplicate(_sampleFile.getReplicateId(), _container);
                if (replicate == null)
                {
                    throw new IllegalStateException("Could not find replicate " + _sampleFile.getReplicateId());
                }
                if (_run == null || replicate.getRunId() != _run.getId())
                {
                    _run = TargetedMSManager.getRun(replicate.getRunId());
                    if (_run == null)
                    {
                        throw new IllegalStateException("Could not find run " + replicate.getRunId());
                    }
                    _rowsForCurrentRun = 0;

                    _currentSummary = new RunSummary(_run);
                    _summaries.add(_currentSummary);
                }
            }

            /*if (_rowsForCurrentRun++ < 5)
            {
                PrecursorChromInfo pci = PrecursorManager.getPrecursorChromInfo(_container, id);
                Chromatogram chromatogram = pci.createChromatogram(_run, true);
                if (chromatogram == null)
                {
                    _currentSummary._noChromatogramCount++;
                }
                else
                {
                    switch (chromatogram.getStatus())
                    {
                        case dbOnly:
                            _currentSummary._dbOnlyCount++;
                            break;
                        case diskOnly:
                            _currentSummary._diskOnlyCount++;
                            break;
                        case noSkydResolved:
                            _currentSummary._noSkydResolvedCount++;
                            break;
                        case skydMissing:
                            _currentSummary._skydNotPresentCount++;
                            break;
                        case mismatch:
                            _currentSummary._mismatchCount++;
                            break;
                        case match:
                            _currentSummary._matchCount++;
                            break;
                        default:
                            throw new IllegalArgumentException("Unhandled status" + chromatogram.getStatus());
                    }
                }
            }
            else
            {
                _currentSummary._skippedCount++;
            }*/
        }
    }
}
