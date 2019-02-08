package org.labkey.targetedms.model;

import org.json.JSONObject;

import java.util.List;

public class Outlier
{
    List<JSONObject> dataRowsLJ;
    List<JSONObject> rawGuideSet;
    List<JSONObject> rawMetricDataSet;

    public List<JSONObject> getDataRowsLJ()
    {
        return dataRowsLJ;
    }

    public void setDataRowsLJ(List<JSONObject> dataRowsLJ)
    {
        this.dataRowsLJ = dataRowsLJ;
    }

    public List<JSONObject> getRawGuideSet()
    {
        return rawGuideSet;
    }

    public void setRawGuideSet(List<JSONObject> rawGuideSet)
    {
        this.rawGuideSet = rawGuideSet;
    }

    public List<JSONObject> getRawMetricDataSet()
    {
        return rawMetricDataSet;
    }

    public void setRawMetricDataSet(List<JSONObject> rawMetricDataSet)
    {
        this.rawMetricDataSet = rawMetricDataSet;
    }

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("dataRowsLJ", getDataRowsLJ());
        jsonObject.put("rawGuideSet", getRawGuideSet());
        jsonObject.put("rawMetricDatSet", getRawMetricDataSet());

        return jsonObject;
    }
}
