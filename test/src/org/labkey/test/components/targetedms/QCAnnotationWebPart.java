package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.WebPart;
import org.labkey.test.pages.targetedms.PanoramaInsertAnnotation;
import org.labkey.test.util.DataRegionTable;

public class QCAnnotationWebPart extends WebPart
{
    public static final String DEFAULT_TITLE = "QC Annotation";

    public QCAnnotationWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public QCAnnotationWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, 0);
    }

    public PanoramaInsertAnnotation getInsertPage()
    {
        DataRegionTable dataRegionTable = new DataRegionTable("qwp1", _test);
        dataRegionTable.clickHeaderButtonByText("Insert New");
        return new PanoramaInsertAnnotation(_test);
    }
}
