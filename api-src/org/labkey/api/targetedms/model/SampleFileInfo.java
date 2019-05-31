package org.labkey.api.targetedms.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SampleFileInfo
{
    int index;
    String sampleFile;
    Date acquiredTime;
    int metrics;
    int nonConformers;
    int totalCount;
    List<LJOutlier> items;
    int guideSetId;
    boolean ignoreForAllMetric;
    int CUSUMm;
    int CUSUMv;
    int CUSUMmP;
    int CUSUMvP;
    int CUSUMmN;
    int CUSUMvN;
    int mR;
    boolean hasOutliers;

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

    public int getNonConformers()
    {
        return nonConformers;
    }

    public void setNonConformers(int nonConformers)
    {
        this.nonConformers = nonConformers;
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

    public int getCUSUMm()
    {
        return CUSUMm;
    }

    public void setCUSUMm(int CUSUMm)
    {
        this.CUSUMm = CUSUMm;
    }

    public int getCUSUMv()
    {
        return CUSUMv;
    }

    public void setCUSUMv(int CUSUMv)
    {
        this.CUSUMv = CUSUMv;
    }

    public int getCUSUMmP()
    {
        return CUSUMmP;
    }

    public void setCUSUMmP(int CUSUMmP)
    {
        this.CUSUMmP = CUSUMmP;
    }

    public int getCUSUMvP()
    {
        return CUSUMvP;
    }

    public void setCUSUMvP(int CUSUMvP)
    {
        this.CUSUMvP = CUSUMvP;
    }

    public int getCUSUMmN()
    {
        return CUSUMmN;
    }

    public void setCUSUMmN(int CUSUMmN)
    {
        this.CUSUMmN = CUSUMmN;
    }

    public int getCUSUMvN()
    {
        return CUSUMvN;
    }

    public void setCUSUMvN(int CUSUMvN)
    {
        this.CUSUMvN = CUSUMvN;
    }

    public int getmR()
    {
        return mR;
    }

    public void setmR(int mR)
    {
        this.mR = mR;
    }

    public boolean isHasOutliers()
    {
        return hasOutliers;
    }

    public void setHasOutliers(boolean hasOutliers)
    {
        this.hasOutliers = hasOutliers;
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

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("Index", index);
        jsonObject.put("SampleFile", sampleFile);
        jsonObject.put("AcquiredTime", acquiredTime);
        jsonObject.put("Metrics", metrics);
        jsonObject.put("NonConformers", nonConformers);
        jsonObject.put("TotalCount",  totalCount);
        jsonObject.put("GuideSetId",  guideSetId);
        jsonObject.put("IgnoreForAllMetric",  ignoreForAllMetric);
        jsonObject.put("Items", getItemsJSON(items));
        jsonObject.put("CUSUMm", CUSUMm);
        jsonObject.put("CUSUMv", CUSUMv);
        jsonObject.put("CUSUMmN", CUSUMmN);
        jsonObject.put("CUSUMmP", CUSUMmP);
        jsonObject.put("CUSUMvN", CUSUMvN);
        jsonObject.put("CUSUMvP", CUSUMvP);
        jsonObject.put("mR", mR);
        jsonObject.put("hasOutliers", hasOutliers);

        return jsonObject;
    }
}
