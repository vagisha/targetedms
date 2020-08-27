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

import org.labkey.targetedms.parser.SkylineEntity;

public class ListItemValue extends SkylineEntity
{
    private long _listItemId;
    private int _columnIndex;
    private String _textValue;
    private Double _numericValue;

    public long getListItemId()
    {
        return _listItemId;
    }

    public void setListItemId(long listItemId)
    {
        _listItemId = listItemId;
    }

    public int getColumnIndex()
    {
        return _columnIndex;
    }

    public void setColumnIndex(int columnIndex)
    {
        _columnIndex = columnIndex;
    }

    public String getTextValue()
    {
        return _textValue;
    }

    public void setTextValue(String textValue)
    {
        _textValue = textValue;
    }

    public Double getNumericValue()
    {
        return _numericValue;
    }

    public void setNumericValue(Double numericValue)
    {
        _numericValue = numericValue;
    }

    public void setValue(Object value)
    {
        if (value == null)
        {
            setTextValue(null);
            setNumericValue(null);
        }
        else if (value instanceof String)
        {
            setTextValue((String) value);
        }
        else if (value instanceof Number)
        {
            setNumericValue(((Number)value).doubleValue());
        }
        else if (value instanceof Boolean)
        {
            setNumericValue(((Boolean) value)?1.0:0.0);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
}
