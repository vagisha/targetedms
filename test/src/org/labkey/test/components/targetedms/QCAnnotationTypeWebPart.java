/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.targetedms.AnnotationTypeInsertPage;
import org.labkey.test.util.DataRegionTable;

public class QCAnnotationTypeWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "QC Annotation Type";
    private DataRegionTable _dataRegionTable;
    private BaseWebDriverTest _test;

    public QCAnnotationTypeWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public QCAnnotationTypeWebPart(BaseWebDriverTest test, int index)
    {
        super(test.getDriver(), DEFAULT_TITLE, index);
        _test = test;
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegionTable == null)
            _dataRegionTable = DataRegionTable.DataRegion(_test.getDriver()).find(getComponentElement());
        return _dataRegionTable;
    }

    public AnnotationTypeInsertPage startInsert()
    {
        getDataRegion().clickInsertNewRow();
        return new AnnotationTypeInsertPage(_test.getDriver());
    }
}
