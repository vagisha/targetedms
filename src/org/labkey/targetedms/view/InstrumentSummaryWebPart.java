package org.labkey.targetedms.view;

import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

public class InstrumentSummaryWebPart extends QueryView
{
    public InstrumentSummaryWebPart (ViewContext viewContext)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        setSettings(new QuerySettings(getViewContext(), "InstrumentSummary", "QCInstrumentSummary"));
        setTitle("Instruments Summary");
        setShowDetailsColumn(false);

        setShowBorders(true);
        setShadeAlternatingRows(true);
        setContainerFilter(null);
    }

}
