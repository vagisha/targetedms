package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.util.DataRegionTable;

/**
 * Created by cnathe on 4/13/2015.
 */
public class GuideSetWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Guide Set";
    private DataRegionTable _dataRegionTable;

    public GuideSetWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public GuideSetWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, index);
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegionTable == null)
            _dataRegionTable = DataRegionTable.findDataRegionWithin(_test, elements().webPart.findElement(_test.getDriver()));
        return _dataRegionTable;
    }

    public GuideSetPage startInsert()
    {
        getDataRegion().clickHeaderButtonByText("Insert New");
        return new GuideSetPage(_test);
    }
}
