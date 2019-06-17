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
package org.labkey.targetedms.model;

import org.labkey.api.data.Entity;

import java.util.Date;

/**
 * Created by cnathe on 4/9/2015.
 */
public class GuideSet extends Entity
{
    private int _rowId;

    private String _comment;
    private Date _trainingStart;
    private Date _trainingEnd;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public Date getTrainingStart()
    {
        return _trainingStart;
    }

    public void setTrainingStart(Date trainingStart)
    {
        _trainingStart = trainingStart;
    }

    public Date getTrainingEnd()
    {
        return _trainingEnd;
    }

    public void setTrainingEnd(Date trainingEnd)
    {
        _trainingEnd = trainingEnd;
    }
}
