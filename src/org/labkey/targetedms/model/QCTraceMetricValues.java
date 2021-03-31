package org.labkey.targetedms.model;

public class QCTraceMetricValues
{
    private int _id;
    private int _metric;
    private float _value;
    private long _sampleFileId;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getMetric()
    {
        return _metric;
    }

    public void setMetric(int metric)
    {
        _metric = metric;
    }

    public float getValue()
    {
        return _value;
    }

    public void setValue(float value)
    {
        _value = value;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }
}
