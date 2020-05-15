package org.labkey.targetedms.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChromatogramCrawlerJob extends PipelineJob
{
    private CrawlType _crawlType = CrawlType.verifyOnly;

    /** For JSON serialization/deserialzation round-tripping
     * @noinspection unused*/
    protected ChromatogramCrawlerJob()
    {

    }

    public ChromatogramCrawlerJob(ViewBackgroundInfo info, @NotNull PipeRoot root)
    {
        super(TargetedMSPipelineProvider.name, info, root);
        setLogFile(new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("ChromatogramCrawl", "log")));

        getLogger().info("Description of key statuses:");
        for (Chromatogram.SourceStatus value : Chromatogram.SourceStatus.values())
        {
            getLogger().info("\t" + value.toString() + ": " + value.getDescription());
        }
        getLogger().info("\n");
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
        },
        /** Not supported yet, but coming soon */
        populateIndices
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

        getLogger().info("Starting to process chromatogram rows over " + containers.size() + " containers");

        CrawlSummary crawlSummary = new CrawlSummary();

        int containerCount = 0;
        // Loop through all containers
        for (Container container : containers)
        {
            containerCount++;

            // See how many rows we have to process in this container
            var schema = new TargetedMSSchema(getUser(), container);
            var table = schema.getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO, ContainerFilter.current(container));
            var selector = new TableSelector(table, PageFlowUtil.set("Id", "SampleFileID"), _crawlType.getFilter(), new Sort(FieldKey.fromParts("SampleFileId")));
            long containerPCICount = selector.getRowCount();

            if (containerPCICount == 0)
            {
                getLogger().info("Skipping container " + container.getPath() + " - no chromatogram rows to process");
            }
            else
            {
                getLogger().info("Starting processing of container " + container.getPath() + " (" + containerCount + " of " + containers.size() + ") with " + containerPCICount + " chromatogram rows to process");

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
                }
            }
        }
        getLogger().info("All done!");
        getLogger().info(crawlSummary.toString());
        setStatus(TaskStatus.complete);
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Crawling chromatograms: " + _crawlType;
    }

    private static class CrawlSummary
    {
        private int _dbOnlyCount;
        private int _diskOnlyCount;
        private int _noSkydResolvedCount;
        private int _skydNotPresentCount;
        private int _mismatchCount;
        private int _matchCount;
        private int _noChromatogramCount;

        public void add(RunSummary runSummary)
        {
            if (runSummary._dbOnlyCount > 0)
            {
                _dbOnlyCount++;
            }
            if (runSummary._diskOnlyCount > 0)
            {
                _diskOnlyCount++;
            }
            if (runSummary._noSkydResolvedCount > 0)
            {
                _noSkydResolvedCount++;
            }
            if (runSummary._skydNotPresentCount > 0)
            {
                _skydNotPresentCount++;
            }
            if (runSummary._mismatchCount > 0)
            {
                _mismatchCount++;
            }
            if (runSummary._matchCount > 0)
            {
                _matchCount++;
            }
            if (runSummary._noChromatogramCount > 0)
            {
                _noChromatogramCount++;
            }
        }

        @Override
        public String toString()
        {
            return String.format("Crawl summary by Skyline document counts. Match: %3d,   No SKYD resolved: %3d, DB only: %3d,   Disk only: %3d,   SKYD not present: %3d,   Mismatch: %3d",
                    _matchCount,
                    _noSkydResolvedCount,
                    _dbOnlyCount,
                    _diskOnlyCount,
                    _skydNotPresentCount,
                    _mismatchCount,
                    _noChromatogramCount);
        }
    }

    /**  */
    private static class RunSummary
    {
        private final TargetedMSRun _run;

        private int _dbOnlyCount;
        private int _diskOnlyCount;
        private int _noSkydResolvedCount;
        private int _skydNotPresentCount;
        private int _mismatchCount;
        private int _matchCount;
        private int _noChromatogramCount;
        private int _skippedCount;

        public RunSummary(TargetedMSRun run)
        {
            _run = run;
        }

        @Override
        public String toString()
        {
            // Count the number of different states we found for chromatograms, excluding the "skipped" one
            int states = (_matchCount > 0 ? 1 : 0) +
                    (_noSkydResolvedCount > 0 ? 1 : 0) +
                    (_dbOnlyCount > 0 ? 1 : 0) +
                    (_diskOnlyCount > 0 ? 1 : 0) +
                    (_skydNotPresentCount > 0 ? 1 : 0) +
                    (_mismatchCount > 0 ? 1 : 0) +
                    (_noChromatogramCount > 0 ? 1 : 0);

            return String.format("Skyline document %7d.  Match: %3d,   No SKYD resolved: %3d, DB only: %3d,   Disk only: %3d,   SKYD not present: %3d,   Mismatch: %3d,   No chromatogram: %7d,   Skipped: %7d" + (states > 1 ? "  - MIXED STATES!!!" : ""),
                    _run.getId(),
                    _matchCount,
                    _noSkydResolvedCount,
                    _dbOnlyCount,
                    _diskOnlyCount,
                    _skydNotPresentCount,
                    _mismatchCount,
                    _noChromatogramCount,
                    _skippedCount);
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

        public void exec(int id, int sampleFileId)
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

            if (_rowsForCurrentRun++ < 5)
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
            }
        }
    }
}
