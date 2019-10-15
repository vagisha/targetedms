/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.parser.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListData
{
    private ListDefinition _listDefinition;
    private List<ListColumn> _columns;
    private List<List<Object>> _columnDatas;

    public ListData()
    {
        _listDefinition = new ListDefinition();
        _columns = new ArrayList<>();
        _columnDatas = new ArrayList<>();
    }

    public int getItemCount()
    {
        if (_columnDatas.isEmpty()) {
            return 0;
        }
        return _columnDatas.get(0).size();
    }

    public int getColumnCount() {
        return _columns.size();
    }

    public ListDefinition getListDefinition() {
        return _listDefinition;
    }

    public ListColumn getColumnDef(int i) {
        return _columns.get(i);
    }

    public Object getColumnValue(int iRow, int iCol)
    {
        var data = _columnDatas.get(iCol);
        if (iRow >= data.size())
        {
            return null;
        }
        return data.get(iRow);
    }

    public void addColumnDefinition(ListColumn column)
    {
        _columns.add(column);
    }

    public void addColumnData(List<Object> data)
    {
        _columnDatas.add(data);
    }

    public List<ListColumn> getColumnDefinitions()
    {
        return Collections.unmodifiableList(_columns);
    }

    public List<List<Object>> getColumnDatas() {
        return Collections.unmodifiableList(_columnDatas);
    }
}
