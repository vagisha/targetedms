/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

import java.util.Date;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 18, 2012
 */
public class SampleFile extends SkylineEntity
{
    private int _replicateId;
    private Integer _instrumentId;
    private String _filePath;
    private String _sampleName;
    private String _skylineId;
    private Date _acquiredTime;
    private Date _modifiedTime;

    private List<Instrument> _instrumentInfoList;
    private boolean _skip;

    public int getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(int replicateId)
    {
        _replicateId = replicateId;
    }

    public String getFilePath()
    {
        return _filePath;
    }

    public void setFilePath(String filePath)
    {
        _filePath = filePath;
    }

    public String getSampleName()
    {
        return _sampleName;
    }

    public void setSampleName(String sampleName)
    {
        _sampleName = sampleName;
    }

    public Date getAcquiredTime()
    {
        return _acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        _acquiredTime = acquiredTime;
    }

    public Date getModifiedTime()
    {
        return _modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime)
    {
        _modifiedTime = modifiedTime;
    }

    public Integer getInstrumentId()
    {
        return _instrumentId;
    }

    public void setInstrumentId(Integer instrumentId)
    {
        _instrumentId = instrumentId;
    }

    public String getSkylineId()
    {
        return _skylineId;
    }

    public void setSkylineId(String skylineId)
    {
        _skylineId = skylineId;
    }

    public List<Instrument> getInstrumentInfoList()
    {
        return _instrumentInfoList;
    }

    public void setInstrumentInfoList(List<Instrument> instrumentInfoList)
    {
        _instrumentInfoList = instrumentInfoList;
    }

}
