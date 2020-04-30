package org.labkey.api.targetedms.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SampleFileInfo extends OutlierCounts
{
    int index;
    String sampleFile;
    Date acquiredTime;
    int metrics;
    int totalCount;
    List<LJOutlier> items;
    int guideSetId;
    boolean ignoreForAllMetric;

    public SampleFileInfo()
    {
        items = new ArrayList<>();
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public String getSampleFile()
    {
        return sampleFile;
    }

    public void setSampleFile(String sampleFile)
    {
        this.sampleFile = sampleFile;
    }

    public Date getAcquiredTime()
    {
        return acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        this.acquiredTime = acquiredTime;
    }

    public int getMetrics()
    {
        return metrics;
    }

    public void setMetrics(int metrics)
    {
        this.metrics = metrics;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(int totalCount)
    {
        this.totalCount = totalCount;
    }

    public int getGuideSetId()
    {
        return guideSetId;
    }

    public void setGuideSetId(int guideSetId)
    {
        this.guideSetId = guideSetId;
    }

    public boolean isIgnoreForAllMetric()
    {
        return ignoreForAllMetric;
    }

    public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
    {
        this.ignoreForAllMetric = ignoreForAllMetric;
    }

    public List<LJOutlier> getItems()
    {
        return items;
    }

    public void setItems(List<LJOutlier> items)
    {
        this.items = items;
    }

    public List<JSONObject> getItemsJSON(List<LJOutlier> ljOutliers)
    {
        List<JSONObject> jsonLJOutliers = new ArrayList<>();
        for (LJOutlier ljOutlier : ljOutliers)
        {
            jsonLJOutliers.add(ljOutlier.toJSON());
        }
        return jsonLJOutliers;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject jsonObject = super.toJSON();

        jsonObject.put("Index", index);
        jsonObject.put("SampleFile", sampleFile);
        jsonObject.put("AcquiredTime", acquiredTime);
        jsonObject.put("Metrics", metrics);
        jsonObject.put("TotalCount",  totalCount);
        jsonObject.put("GuideSetId",  guideSetId);
        jsonObject.put("IgnoreForAllMetric",  ignoreForAllMetric);
        jsonObject.put("Items", getItemsJSON(items));

        return jsonObject;
    }
}
