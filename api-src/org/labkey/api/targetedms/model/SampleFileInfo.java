package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class SampleFileInfo extends OutlierCounts
{
    final long sampleId;
    final String sampleFile;
    final Date acquiredTime;
    final int guideSetId;
    final String filePath;
    final long replicateId;
    /** Use a TreeMap to keep the metrics sorted by name */
    final Map<String, OutlierCounts> byMetric = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private Boolean _inGuideSetTrainingRange;
    private boolean ignoreForAllMetric;

    public SampleFileInfo(long sampleId, Date acquiredTime, String sampleFile, int guideSetId, String filePath, Long replicateId)
    {
        this.sampleId = sampleId;
        this.acquiredTime = acquiredTime;
        this.sampleFile = sampleFile;
        this.guideSetId = guideSetId;
        this.filePath = filePath;
        this.replicateId = replicateId;
    }

    public String getSampleFile()
    {
        return sampleFile;
    }

    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    public boolean isIgnoreForAllMetric()
    {
        return ignoreForAllMetric;
    }

    public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
    {
        this.ignoreForAllMetric = ignoreForAllMetric;
    }

    public long getSampleId()
    {
        return sampleId;
    }

    public Map<String, OutlierCounts> getByMetric()
    {
        return byMetric;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public long getReplicateId()
    {
        return replicateId;
    }

    public JSONArray getMetricsJSON()
    {
        JSONArray result = new JSONArray();
        for (Map.Entry<String, OutlierCounts> entry : byMetric.entrySet())
        {
            JSONObject metricCounts = entry.getValue().toJSON();
            metricCounts.put("MetricLabel", entry.getKey());
            result.put(metricCounts);
        }
        return result;
    }

    public OutlierCounts getMetricCounts(String metricLabel, int metricId)
    {
        return byMetric.computeIfAbsent(metricLabel, x -> new OutlierCounts(metricId));
    }

    @Override @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = super.toJSON();

        jsonObject.put("SampleId", getSampleId());
        jsonObject.put("SampleFile", getSampleFile());
        jsonObject.put("AcquiredTime", getAcquiredTime());
        jsonObject.put("GuideSetId", getGuideSetId());
        jsonObject.put("IgnoreForAllMetric", isIgnoreForAllMetric());
        jsonObject.put("Metrics", getMetricsJSON());

        return jsonObject;
    }

    public JSONObject toQCPlotJSON()
    {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("SampleId", getSampleId());
        jsonObject.put("ReplicateId", getReplicateId());
        jsonObject.put("FilePath", getFilePath());
        jsonObject.put("AcquiredTime", getAcquiredTime());
        jsonObject.put("GuideSetId", getGuideSetId());
        // Intentionally dereference wrapper object to be sure it's always populated with a true/false value in this codepath
        jsonObject.put("InGuideSetTrainingRange", _inGuideSetTrainingRange.booleanValue());

        return jsonObject;
    }

    public void setInGuideSetTrainingRange(Boolean inGuideSetTrainingRange)
    {
        _inGuideSetTrainingRange = inGuideSetTrainingRange;
    }

    public boolean getInGuideSetTrainingRange()
    {
        return _inGuideSetTrainingRange;
    }
}
