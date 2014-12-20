/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.WebPart;
import org.labkey.test.pages.targetedms.PanoramaInsertAnnotationType;
import org.labkey.test.util.DataRegionTable;

public class QCAnnotationTypeWebPart extends WebPart
{
    public static final String DEFAULT_TITLE = "QC Annotation Type";

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
