package org.labkey.targetedms.model;

import org.json.JSONObject;

import java.util.Date;

public class RawGuideSet
{
    Date trainingStart;
    Date trainingEnd;
    String seriesLabel;
    Double metricValue;
    String metricType;
    Date acquiredTime;
    String seriesType;
    String comment;
    String referenceEnd;
    int guideSetId;

    public Date getTrainingStart()
    {
        return trainingStart;
    }

    public void setTrainingStart(Date trainingStart)
    {
        this.trainingStart = trainingStart;
    }

    public Date getTrainingEnd()
    {
        return trainingEnd;
    }

    public void setTrainingEnd(Date trainingEnd)
    {
        this.trainingEnd = trainingEnd;
    }

    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    public Double getMetricValue()
    {
        return metricValue;
    }

    public void setMetricValue(Double metricValue)
    {
        this.metricValue = metricValue;
    }

    public String getMetricType()
    {
        return metricType;
    }

    public void setMetricType(String metricType)
    {
        this.metricType = metricType;
    }

    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        this.acquiredTime = acquiredTime;
    }

    public String getSeriesType()
    {
        return seriesType;
    }

    public void setSeriesType(String seriesType)
    {
        this.seriesType = seriesType;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getReferenceEnd()
    {
        return referenceEnd;
    }

    public void setReferenceEnd(String referenceEnd)
    {
        this.referenceEnd = referenceEnd;
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    public void setGuideSetId(int guideSetId)
    {
        this.guideSetId = guideSetId;
    }

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("TrainingStart", trainingStart);
        jsonObject.put("TrainingEnd", trainingEnd);
        jsonObject.put("SeriesLabel", seriesLabel);
        jsonObject.put("MetricType", metricType);
        jsonObject.put("MetricValue", metricValue);
        jsonObject.put("AcquiredTime", acquiredTime);
        jsonObject.put("SeriesType",  seriesType);
        jsonObject.put("Comment",  comment);
        jsonObject.put("ReferenceEnd",  referenceEnd);
        jsonObject.put("GuideSetId",  guideSetId);

        return jsonObject;
    }
}
