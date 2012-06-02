package org.labkey.targetedms.parser;

import java.util.List;

/**
 * User: jeckels
 * Date: Apr 18, 2012
 */
public class Replicate extends SkylineEntity
{
    private int _runId;
    private String _name;

    private Integer _cePredictorId;
    private Integer _dpPredictorId;

    private List<SampleFile> _sampleFileList;


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

    public Integer getCePredictorId()
    {
        return _cePredictorId;
    }

    public void setCePredictorId(Integer cePredictorId)
    {
        _cePredictorId = cePredictorId;
    }

    public Integer getDpPredictorId()
    {
        return _dpPredictorId;
    }

    public void setDpPredictorId(Integer dpPredictorId)
    {
        _dpPredictorId = dpPredictorId;
    }

    public List<SampleFile> getSampleFileList()
    {
        return _sampleFileList;
    }

    public void setSampleFileList(List<SampleFile> sampleFileList)
    {
        _sampleFileList = sampleFileList;
    }
}
