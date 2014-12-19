package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCAnnotationTypeWebPart;
import org.labkey.test.components.targetedms.QCAnnotationWebPart;
import org.labkey.test.pages.PortalBodyPanel;

public class PanoramaAnnotations extends PortalBodyPanel
{
    private QCAnnotationWebPart _qcAnnotationWebPart;
    private QCAnnotationTypeWebPart _qcAnnotationTypeWebPart;

    public PanoramaAnnotations(BaseWebDriverTest test)
    {
        super(test);
        _qcAnnotationWebPart = new QCAnnotationWebPart(test);
        _qcAnnotationTypeWebPart = new QCAnnotationTypeWebPart(test);
    }

    public QCAnnotationWebPart getQcAnnotationWebPart()
    {
        return _qcAnnotationWebPart;
    }

    public QCAnnotationTypeWebPart getQcAnnotationTypeWebPart()
    {
        return _qcAnnotationTypeWebPart;
    }
}
