package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class OutlierCounts
{
    private final Integer _metricId;
    private int _CUSUMmP;
    private int _CUSUMvP;
    private int _CUSUMmN;
    private int _CUSUMvN;
    private int _mR;
    private int _leveyJennings;

    /** Total number of data points under consideration */
    private int _totalCount;

    public OutlierCounts()
    {
        _metricId = null;
    }

    public OutlierCounts(int metricId)
    {
        _metricId = metricId;
    }

    public int getCUSUMm()
    {
        return _CUSUMmP + _CUSUMmN;
    }

    public int getCUSUMv()
    {
        return getCUSUMvP() + getCUSUMvN();
    }

    public int getCUSUMmN()
    {
        return _CUSUMmN;
    }

    public void setCUSUMmN(int CUSUMmN)
    {
        _CUSUMmN = CUSUMmN;
    }

    public int getCUSUMmP()
    {
        return _CUSUMmP;
    }

    public void setCUSUMmP(int CUSUMmP)
    {
        _CUSUMmP = CUSUMmP;
    }

    public int getCUSUMvP()
    {
        return _CUSUMvP;
    }

    public void setCUSUMvP(int CUSUMvP)
    {
        _CUSUMvP = CUSUMvP;
    }

    public int getCUSUMvN()
    {
        return _CUSUMvN;
    }

    public void setCUSUMvN(int CUSUMvN)
    {
        _CUSUMvN = CUSUMvN;
    }

    public int getmR()
    {
        return _mR;
    }

    public void setmR(int mR)
    {
        _mR = mR;
    }

    public int getLeveyJennings()
    {
        return _leveyJennings;
    }

    public void setLeveyJennings(int leveyJennings)
    {
        _leveyJennings = leveyJennings;
    }

    public int getTotalCount()
    {
        return _totalCount;
    }

    public void setTotalCount(int totalCount)
    {
        this._totalCount = totalCount;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("TotalCount", getTotalCount());
        jsonObject.put("CUSUMm", getCUSUMm());
        jsonObject.put("CUSUMv", getCUSUMv());
        jsonObject.put("CUSUMmN", getCUSUMmN());
        jsonObject.put("CUSUMmP", getCUSUMmP());
        jsonObject.put("CUSUMvN", getCUSUMvN());
        jsonObject.put("CUSUMvP", getCUSUMvP());
        jsonObject.put("mR", getmR());
        jsonObject.put("LeveyJennings", getLeveyJennings());
        if (_metricId != null)
        {
            jsonObject.put("MetricId", _metricId);
        }

        return jsonObject;
    }
}
