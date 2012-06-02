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

    private List<Instrument> _instrumentList;

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

    public List<Instrument> getInstrumentList()
    {
        return _instrumentList;
    }

    public void setInstrumentList(List<Instrument> instrumentList)
    {
        _instrumentList = instrumentList;
    }
}
