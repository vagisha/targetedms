package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.InsertPage;

/**
 * Created by cnathe on 4/13/2015.
 */
public class GuideSetPage extends InsertPage
{
    private static final String DEFAULT_TITLE = "Insert GuideSet";

    public GuideSetPage(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public void insert(String trainingStart, String trainingEnd, String comment)
    {
        Elements elements = elements();
        _test.setFormElement(elements.trainingStartDate, trainingStart);
        _test.setFormElement(elements.trainingEndDate, trainingEnd);
        _test.setFormElement(elements.comment, comment);
        _test.clickAndWait(elements.submit);
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends InsertPage.Elements
    {
        public Locator.XPathLocator trainingStartDate = body.append(Locator.tagWithName("input", "quf_TrainingStart"));
        public Locator.XPathLocator trainingEndDate = body.append(Locator.tagWithName("input", "quf_TrainingEnd"));
        public Locator.XPathLocator comment = body.append(Locator.tagWithName("textarea", "quf_Comment"));
    }
}
