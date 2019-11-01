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
import org.labkey.targetedms.query.SkylineListManager;
import org.labkey.targetedms.query.SkylineListSchema;

import java.util.List;
import java.util.Objects;

/** Bean class for a list definition imported from a Skyline document */
public class ListDefinition extends SkylineEntity
{
    private int _runId;
    private String _name;
    private Integer _pkColumnIndex;
    private Integer _displayColumnIndex;
    private List<ListColumn> _columns;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public Integer getPkColumnIndex()
    {
        return _pkColumnIndex;
    }

    public void setPkColumnIndex(Integer pkColumnIndex)
    {
        _pkColumnIndex = pkColumnIndex;
    }

    public Integer getDisplayColumnIndex()
    {
        return _displayColumnIndex;
    }

    public void setDisplayColumnIndex(Integer displayColumnIndex)
    {
        _displayColumnIndex = displayColumnIndex;
    }

    /** @return a generated name not tied to this specific list definition, but for unioning all lists of the same shape */
    public String getUserSchemaTableName()
    {
        if (getName().length() > 50)
        {
            // List names can be very long, so truncate and include RowId to be sure it's unique
            return getRunId() + SkylineListSchema.ID_SEPARATOR + getId() + SkylineListSchema.ID_SEPARATOR + getName().substring(0, 50);
        }
        return getRunId() + SkylineListSchema.ID_SEPARATOR + getName();
    }

    /** @return a generated name unique to this specific list definition */
    public String getUnionUserSchemaTableName()
    {
        String suffix;
        if (getName().length() > 50)
        {
            // List names can be very long, so truncate and include a hash to avoid collisions
            suffix = SkylineListSchema.ID_SEPARATOR + Math.abs(getName().hashCode() % 10000) + getName().substring(0, 50);
        }
        else
        {
            suffix = getName();
        }
        return SkylineListSchema.UNION_PREFIX + suffix;
    }

    /** Fetch and cache the columns for this list. Not a getter so as not to confuse reflection-based binding */
    public List<ListColumn> fetchColumns()
    {
        if (_columns == null)
        {
            _columns = SkylineListManager.getListColumns(this);
        }
        return _columns;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_runId, _name, _pkColumnIndex, _displayColumnIndex, _columns);
    }

    /** @return true if the lists describe the same design - identical column lists, title, and key columns */
    public boolean matches(ListDefinition that)
    {
        if(Objects.equals(_name, that._name) &&
                Objects.equals(_pkColumnIndex, that._pkColumnIndex) &&
                Objects.equals(_displayColumnIndex, that._displayColumnIndex))
        {
            List<ListColumn> thisColumns = fetchColumns();
            List<ListColumn> thatColumns = that.fetchColumns();
            if (thisColumns.size() == thatColumns.size())
            {
                for (int i = 0; i < thisColumns.size(); i++)
                {
                    if (!thisColumns.get(i).matches(thatColumns.get(i)))
                    {
                        return false;
                    }
                }
                // Basic list setup matches, and all columns match, so we're OK to union
                return true;
            }
        }
        return false;
    }
}
