package org.labkey.targetedms.model;

import org.jetbrains.annotations.Nullable;
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
    Integer guideSetId;

    @Nullable
    public Date getTrainingStart()
    {
        return trainingStart;
    }

    public void setTrainingStart(Date trainingStart)
    {
        this.trainingStart = trainingStart;
    }

    @Nullable
    public Date getTrainingEnd()
    {
        return trainingEnd;
    }

    public void setTrainingEnd(Date trainingEnd)
    {
        this.trainingEnd = trainingEnd;
    }

    @Nullable
    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    @Nullable
    public Double getMetricValue()
    {
        return metricValue;
    }

    public void setMetricValue(Double metricValue)
    {
        this.metricValue = metricValue;
    }

    @Nullable
    public String getMetricType()
    {
        return metricType;
    }

    public void setMetricType(String metricType)
    {
        this.metricType = metricType;
    }

    @Nullable
    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        this.acquiredTime = acquiredTime;
    }

    @Nullable
    public String getSeriesType()
    {
        return seriesType;
    }

    public void setSeriesType(String seriesType)
    {
        this.seriesType = seriesType;
    }

    @Nullable
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @Nullable
    public String getReferenceEnd()
    {
        return referenceEnd;
    }

    public void setReferenceEnd(String referenceEnd)
    {
        this.referenceEnd = referenceEnd;
    }

    @Nullable
    public Integer getGuideSetId()
    {
        return guideSetId;
    }

    public void setGuideSetId(Integer guideSetId)
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
