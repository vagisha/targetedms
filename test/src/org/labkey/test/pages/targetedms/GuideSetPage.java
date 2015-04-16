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
