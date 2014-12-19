package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.InsertPage;

public class PanoramaInsertAnnotation extends InsertPage
{
    private static final String DEFAULT_TITLE = "Insert qcannotationtype";

    public static final String INSTRUMENT_CHANGE = "Instrumentation Change";
    public static final String REAGENT_CHANGE = "Reagent Change";
    public static final String TECHNICIAN_CHANGE = "Technician Change";

    public PanoramaInsertAnnotation(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    // consider value returning...
    public void insert(String annotationType, String description, String date)
    {
        Elements elements = elements();
        _test.selectOptionByText(elements.annotationType, annotationType);
        _test.setFormElement(elements.description, description);
        _test.setFormElement(elements.date, date);
        _test.clickAndWait(elements.submit);
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends InsertPage.Elements
    {
        public Locator.XPathLocator annotationType = body.append(Locator.tagWithName("select", "quf_QCAnnotationTypeId"));
        public Locator.XPathLocator description = body.append(Locator.tagWithName("textarea", "quf_Description"));
        public Locator.XPathLocator date = body.append(Locator.tagWithName("input", "quf_Date"));
    }
}
