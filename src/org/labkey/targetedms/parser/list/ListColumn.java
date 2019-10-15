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

import org.labkey.targetedms.parser.DataSettings;
import org.labkey.targetedms.parser.SkylineEntity;

public class ListColumn extends SkylineEntity
{
    private int _listDefinitionId;
    private int _columnIndex;
    private String _name;
    private String _lookup;
    private String _annotationType;

    public int getListDefinitionId()
    {
        return _listDefinitionId;
    }

    public void setListDefinitionId(int listId)
    {
        _listDefinitionId = listId;
    }

    public int getColumnIndex()
    {
        return _columnIndex;
    }

    public void setColumnIndex(int columnIndex)
    {
        _columnIndex = columnIndex;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getLookup()
    {
        return _lookup;
    }

    public void setLookup(String lookup)
    {
        _lookup = lookup;
    }

    public String getAnnotationType()
    {
        return _annotationType;
    }

    public void setAnnotationType(String annotationType)
    {
        _annotationType = annotationType;
    }

    public DataSettings.AnnotationType getAnnotationTypeEnum() {
        return DataSettings.AnnotationType.fromString(getAnnotationType());
    }
}
