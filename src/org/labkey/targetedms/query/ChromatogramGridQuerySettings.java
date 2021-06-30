/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.labkey.api.data.Table;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;

/**
 * User: vsharma
 * Date: 4/28/12
 * Time: 9:53 PM
 */
public class ChromatogramGridQuerySettings extends QuerySettings
{
    private int _maxRowSize = 2;

    public ChromatogramGridQuerySettings(ViewContext context, String dataRegionName)
    {
        super(dataRegionName);
        init(context);

        setAllowCustomizeView(false);
        setAllowChooseView(false);
    }

    @Override
    public void init(ViewContext context)
    {
        super.init(context);
        String param = _getParameter("maxRowSize");
        if (param != null)
        {
            try
            {
                _maxRowSize = Integer.parseInt(param);
            }
            catch (NumberFormatException ignored) {}
        }
        assert _maxRowSize >= 1 : _maxRowSize + " is an illegal value for maxRowSize; should be positive.";
        // Adjust _maxRows if _maxRowSize * number of displayed rows is more than _maxRows,
        // but only if we are not showing all rows.
        if(getMaxRows() != Table.ALL_ROWS)
        {
            if (getMaxRows() % _maxRowSize != 0)
            {
                setMaxRows(getMaxRows() + (_maxRowSize - getMaxRows() % _maxRowSize));
            }
        }
    }

    public int getMaxRowSize()
    {
        return _maxRowSize;
    }
}
