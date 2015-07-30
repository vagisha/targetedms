package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.ParetoPlotsWebPart;
import org.labkey.test.pages.PortalBodyPanel;

public class ParetoPlotPage extends PortalBodyPanel
{
    private ParetoPlotsWebPart _paretoPlotsWebPart;

    public ParetoPlotPage(BaseWebDriverTest test)
    {
        super(test);
        _paretoPlotsWebPart = new ParetoPlotsWebPart(test);
    }

    public ParetoPlotsWebPart getParetoPlotsWebPart()
    {
        return _paretoPlotsWebPart;
    }
}
