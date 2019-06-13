/*
 * Copyright (c) 2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
