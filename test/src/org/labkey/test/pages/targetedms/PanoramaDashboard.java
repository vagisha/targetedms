package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.PortalBodyPanel;

public class PanoramaDashboard extends PortalBodyPanel
{
    private QCPlotsWebPart _qcPlotsWebPart;
    private QCSummaryWebPart _qcSummaryWebPart;

    public PanoramaDashboard(BaseWebDriverTest test)
    {
        super(test);
        _qcPlotsWebPart = new QCPlotsWebPart(test);
        _qcSummaryWebPart = new QCSummaryWebPart(test);
    }

    public QCPlotsWebPart getQcPlotsWebPart()
    {
        return _qcPlotsWebPart;
    }

    public QCSummaryWebPart getQcSummaryWebPart()
    {
        return _qcSummaryWebPart;
    }
}
