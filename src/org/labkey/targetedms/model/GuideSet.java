/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
import org.labkey.api.data.Entity;
import org.labkey.api.targetedms.model.OutlierCounts;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.outliers.OutlierGenerator;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cnathe on 4/9/2015.
 */
public class GuideSet extends Entity
{
    private int _rowId;

    private String _comment;
    private Date _trainingStart;
    private Date _trainingEnd;
    private Date _referenceEnd;
    private boolean _isDefault;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public Date getTrainingStart()
    {
        return _trainingStart;
    }

    public void setTrainingStart(Date trainingStart)
    {
        _trainingStart = trainingStart;
    }

    public Date getTrainingEnd()
    {
        return _trainingEnd;
    }

    public void setTrainingEnd(Date trainingEnd)
    {
        _trainingEnd = trainingEnd;
    }

    public Date getReferenceEnd()
    {
        return _referenceEnd;
    }

    public void setReferenceEnd(Date referenceEnd)
    {
        _referenceEnd = referenceEnd;
    }

    public boolean isDefault()
    {
        return _isDefault;
    }

    public void setIsDefault(boolean isDefault)
    {
        _isDefault = isDefault;
    }

    public JSONObject toJSON(List<RawMetricDataSet> dataRows, Map<Integer, QCMetricConfiguration> metrics, Map<GuideSetKey, GuideSetStats> stats)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("RowId", getRowId());
        jsonObject.put("Comment", getComment());
        jsonObject.put("TrainingStart", getTrainingStart());
        jsonObject.put("TrainingEnd", getTrainingEnd());
        jsonObject.put("ReferenceEnd", getReferenceEnd());

        // Metric ID and label form the key
        Map<Pair<Integer, String>, OutlierCounts> allMetricOutliers = new HashMap<>();

        for (RawMetricDataSet dataRow : dataRows)
        {
            if (dataRow.getGuideSetId() == getRowId())
            {
                String metricLabel = OutlierGenerator.get().getMetricLabel(metrics, dataRow);

                OutlierCounts counts = allMetricOutliers.computeIfAbsent(new Pair<>(dataRow.getMetricId(), metricLabel), x -> new OutlierCounts());
                GuideSetStats s = stats.get(dataRow.getGuideSetKey());
                dataRow.increment(counts, s);
            }
        }

        JSONObject metricCounts = new JSONObject();
        for (Map.Entry<Pair<Integer, String>, OutlierCounts> entry : allMetricOutliers.entrySet())
        {
            JSONObject counts = entry.getValue().toJSON();
            counts.put("MetricId", entry.getKey().first);
            metricCounts.put(entry.getKey().second, counts);
        }

        jsonObject.put("MetricCounts", metricCounts);

        return jsonObject;
    }

}
