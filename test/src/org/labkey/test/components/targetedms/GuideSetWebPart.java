/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.util.DataRegionTable;

import java.io.IOException;

public class GuideSetWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Guide Set";
    private BaseWebDriverTest _test;
    private DataRegionTable _dataRegionTable;
    private String _projectName;

    public GuideSetWebPart(BaseWebDriverTest test, String projectName)
    {
        this(test, 0);
        _projectName = projectName;
    }

    public GuideSetWebPart(BaseWebDriverTest test, int index)
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

    public GuideSetPage startInsert()
    {
        getDataRegion().clickInsertNewRow();
        return new GuideSetPage(_test.getDriver());
    }

    public Integer getRowId(GuideSet guideSet)
    {
        try {
            Connection cn = _test.createDefaultConnection(false);
            SelectRowsCommand selectCmd = new SelectRowsCommand("targetedms", "GuideSet");
            selectCmd.addFilter(new Filter("Comment", guideSet.getComment()));
            SelectRowsResponse selResp = selectCmd.execute(cn, _projectName);

            // guide sets created from brushing in the QC plot will not have a comment
            if (selResp.getRows().size() == 0)
            {
                selectCmd = new SelectRowsCommand("targetedms", "GuideSet");
                selectCmd.addFilter(new Filter("TrainingStart", guideSet.getStartDate()));
                selectCmd.addFilter(new Filter("TrainingEnd", guideSet.getEndDate()));
                selResp = selectCmd.execute(cn, _projectName);
            }

            if (selResp.getRows().size() > 0)
            {
                String rowIdStr = selResp.getRows().get(0).get("RowId").toString();
                return Integer.parseInt(rowIdStr);
            }
        }
        catch (IOException | CommandException rethrow)
        {
            throw new RuntimeException(rethrow);
        }

        return null;
    }
}
