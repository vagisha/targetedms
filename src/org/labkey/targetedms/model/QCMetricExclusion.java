package org.labkey.targetedms.model;

public class QCMetricExclusion
{
    private Integer _replicateId;
    private Integer _metricId;

    public QCMetricExclusion()
    {}

    public QCMetricExclusion(Integer replicateId, Integer metricId)
    {
        _replicateId = replicateId;
        _metricId = metricId;
    }

    public Integer getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(Integer replicateId)
    {
        _replicateId = replicateId;
    }

    public Integer getMetricId()
    {
        return _metricId;
    }

    public void setMetricId(Integer metricId)
    {
        _metricId = metricId;
    }
}
