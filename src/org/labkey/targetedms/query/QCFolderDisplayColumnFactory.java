package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class QCFolderDisplayColumnFactory implements DisplayColumnFactory
{
    public QCFolderDisplayColumnFactory()
    {
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                final User user = ctx.getViewContext().getUser();
                String serialNumber = String.valueOf(getBoundColumn().getValue(ctx));
                var currentRunId = ctx.get("runId");
                var instrumentRunIds = TargetedMSManager.getRunIdsByInstrument(serialNumber);
                Set<Container> qcContainers = new TreeSet<>();
                instrumentRunIds.forEach(runId -> {
                    TargetedMSRun run = TargetedMSManager.getRun(runId);
                    if (null != run && Objects.equals(TargetedMSModule.getFolderType(run.getContainer()), TargetedMSService.FolderType.QC))
                    {
                        if (run.getContainer().hasPermission(user, ReadPermission.class))
                        {
                            qcContainers.add(run.getContainer());
                        }
                    }
                });
                StringBuilder sb = new StringBuilder();
                qcContainers.forEach(qcContainer -> {
                    var url = qcContainer.getStartURL(user);
                    if (null != currentRunId)
                    {
                        url.addParameter("RunId", currentRunId.toString());
                    }
                    url.addReturnURL(ctx.getViewContext().getActionURL());
                    sb.append("<div><a href=\"")
                            .append(PageFlowUtil.filter(url))
                            .append("\"")
                            .append(">")
                            .append(PageFlowUtil.filter(qcContainer.getName()))
                            .append("</a></div>");
                });
                out.write(sb.toString());
            }
        };
    }
}
