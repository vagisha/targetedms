package org.labkey.targetedms.model;

import org.json.JSONObject;

import java.util.List;

public class Outlier
{
    List<JSONObject> dataRowsLJ;

    public List<JSONObject> getDataRowsLJ()
    {
        return dataRowsLJ;
    }

    public void setDataRowsLJ(List<JSONObject> dataRowsLJ)
    {
        this.dataRowsLJ = dataRowsLJ;
    }

    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("dataRowsLJ", getDataRowsLJ());

        return jsonObject;
    }
}
