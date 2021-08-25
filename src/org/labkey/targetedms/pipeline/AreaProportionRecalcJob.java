package org.labkey.targetedms.pipeline;

import org.apache.commons.lang3.mutable.MutableLong;
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

public class AreaProportionRecalcJob extends PipelineJob
{
    @SuppressWarnings("unused")  // for serialization
    protected AreaProportionRecalcJob()
    {

    }

    public AreaProportionRecalcJob(ViewBackgroundInfo info, @NotNull PipeRoot root)
    {
        super(TargetedMSPipelineProvider.name, info, root);
        setLogFile(new File(root.getRootPath(), FileUtil.makeFileNameWithTimestamp("AreaProportionRecalcJob", "log")));
    }

    @Override
    public void run()
    {
        setStatus(TaskStatus.running);
        TableSelector selector = new TableSelector(TargetedMSManager.getTableInfoRuns());
        long totalRuns = selector.getRowCount();

        getLogger().info("Starting to recalculate area proportions for " + totalRuns + " Skyline documents");

        MutableLong count = new MutableLong(0);

        selector.forEach(TargetedMSRun.class, run ->
        {
            if (count.incrementAndGet() % 100 == 0)
            {
                getLogger().info("Updating Skyline document " + count);
            }
            TargetedMSManager.updateModifiedAreaProportions(null, run);
        });

        getLogger().info("All done!");
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
        return "Recalculating area proportions";
    }
}
