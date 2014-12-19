package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.WebPart;
import org.labkey.test.pages.targetedms.PanoramaInsertAnnotationType;
import org.labkey.test.util.DataRegionTable;

public class QCAnnotationTypeWebPart extends WebPart
{
    private static final String DEFAULT_TITLE = "QC Annotation Type";

    public QCAnnotationTypeWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public QCAnnotationTypeWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, index);
    }

    public PanoramaInsertAnnotationType getInsertPage()
    {
        DataRegionTable dataRegionTable = new DataRegionTable("qwp2", _test);
        dataRegionTable.clickHeaderButtonByText("Insert New");
        return new PanoramaInsertAnnotationType(_test);
    }
}
