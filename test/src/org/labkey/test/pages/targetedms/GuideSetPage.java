/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.pages.InsertPage;

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

    public void insert(GuideSet guideSet, String expectErrorMsg)
    {
        Elements elements = elements();
        _test.setFormElement(elements.trainingStartDate, guideSet.getStartDate());
        _test.setFormElement(elements.trainingEndDate, guideSet.getEndDate());
        _test.setFormElement(elements.comment, guideSet.getComment());
        _test.clickAndWait(elements.submit);

        if (expectErrorMsg != null)
        {
            _test.assertElementPresent(elements.error.withText(expectErrorMsg));
            _test.clickAndWait(elements.cancel);
        }
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
        public Locator.XPathLocator error = body.append(Locator.tagWithClass("font", "labkey-error"));
    }
}
