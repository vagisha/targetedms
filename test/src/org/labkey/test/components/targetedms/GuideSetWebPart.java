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
package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.util.DataRegionTable;

/**
 * Created by cnathe on 4/13/2015.
 */
public class GuideSetWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Guide Set";
    private DataRegionTable _dataRegionTable;

    public GuideSetWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public GuideSetWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, index);
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegionTable == null)
            _dataRegionTable = DataRegionTable.findDataRegionWithin(_test, elements().webPart.findElement(_test.getDriver()));
        return _dataRegionTable;
    }

    public GuideSetPage startInsert()
    {
        getDataRegion().clickHeaderButtonByText("Insert New");
        return new GuideSetPage(_test);
    }

    public Integer getRowId(GuideSet guideSet)
    {
        DataRegionTable table = getDataRegion();
        if (table.getColumn("RowId") == -1)
        {
            _test._customizeViewsHelper.openCustomizeViewPanel();
            _test._customizeViewsHelper.showHiddenItems();
            _test._customizeViewsHelper.addCustomizeViewColumn("RowId");
            _test._customizeViewsHelper.applyCustomView();
        }

        // guide sets created from brushing in the QC plot will not have a comment
        String rowIdStr = table.getDataAsText(table.getRow("Comment", guideSet.getComment()), "RowId");
        if (rowIdStr == null || "".equals(rowIdStr))
        {
            table.setFilter("TrainingStart", "Equals", guideSet.getStartDate());
            table.setFilter("TrainingEnd", "Equals", guideSet.getEndDate());
            rowIdStr = table.getDataAsText(0, "RowId");
            table.clearAllFilters("TrainingStart");
        }

        return Integer.parseInt(rowIdStr);
    }
}
