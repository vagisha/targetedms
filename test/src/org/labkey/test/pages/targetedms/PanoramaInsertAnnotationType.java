package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.InsertPage;

public class PanoramaInsertAnnotationType extends InsertPage
{
    private static final String DEFAULT_TITLE = "Insert qcannotation";

    public PanoramaInsertAnnotationType(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public void insert(String name, String description, String color)
    {
        Elements elements = elements();
        _test.setFormElement(elements.name, name);
        _test.setFormElement(elements.description, description);
        _test.setFormElement(elements.color, color);
        _test.clickAndWait(elements.submit);
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends InsertPage.Elements
    {
        public Locator.XPathLocator name = body.append(Locator.tagWithName("input", "quf_Name"));
        public Locator.XPathLocator description = body.append(Locator.tagWithName("textarea", "quf_Description"));
        public Locator.XPathLocator color = body.append(Locator.tagWithName("input", "quf_Color"));
    }
}
