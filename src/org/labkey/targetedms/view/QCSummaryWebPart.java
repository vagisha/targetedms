package org.labkey.targetedms.view;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.targetedms.TargetedMSController;

public class QCSummaryWebPart extends JspView<Integer>
{
    public QCSummaryWebPart(ViewContext context, @Nullable Integer sampleLimit)
    {
        super("/org/labkey/targetedms/view/qcSummary.jsp", sampleLimit);
        setTitleHref(new ActionURL(TargetedMSController.QCSummaryHistoryAction.class, context.getContainer()));
        addClientDependency(ClientDependency.fromPath("Ext4"));
        setTitle("QC Summary");
        setFrame(WebPartView.FrameType.PORTAL);
    }
}
