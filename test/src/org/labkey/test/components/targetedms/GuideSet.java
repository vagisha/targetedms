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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cnathe on 4/28/15.
 */
public class GuideSet
{
    private int _rowId;
    private String _startDate;
    private String _endDate;
    private String _comment;
    private List<GuideSetStats> _stats;
    private Integer _brushSelectedPoints;

    public GuideSet(String startDate, String endDate, String comment, Integer brushSelectedPoints)
    {
        this(startDate, endDate, comment);
        _brushSelectedPoints = brushSelectedPoints;
    }

    public GuideSet(String startDate, String endDate, String comment)
    {
        _startDate = startDate;
        _endDate = endDate;
        _comment = comment;
        _stats = new ArrayList<>();
    }

    public String getStartDate()
    {
        return _startDate;
    }

    public String getEndDate()
    {
        return _endDate;
    }

    public String getComment()
    {
        return _comment;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public void addStats(GuideSetStats stats)
    {
        _stats.add(stats);
    }

    public List<GuideSetStats> getStats()
    {
        return _stats;
    }

    public Integer getBrushSelectedPoints()
    {
        return _brushSelectedPoints;
    }
}
