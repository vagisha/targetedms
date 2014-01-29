package org.labkey.targetedms.view;

import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSModule;

/**
 * User: vsharma
 * Date: 1/12/14
 * Time: 4:32 PM
 */
public class TargetedMSRunsWebPartView extends VBox
{
    public TargetedMSRunsWebPartView(ViewContext viewContext)
    {
        addView(new JspView("/org/labkey/targetedms/view/conflictSummary.jsp"));

        TargetedMsRunListView runListView = TargetedMsRunListView.createView(viewContext);
        runListView.setFrame(WebPartView.FrameType.NONE);
        this.addView(runListView);

        setFrame(WebPartView.FrameType.PORTAL);
        setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
        setTitleHref(new ActionURL(TargetedMSController.ShowListAction.class, viewContext.getContainer()));
    }
}
