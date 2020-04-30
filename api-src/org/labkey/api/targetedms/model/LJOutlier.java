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
 
package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Date;

public class LJOutlier extends OutlierCounts
{
    Integer _guideSetId;
    String _metricId;
    String _metricName;
    String _metricLabel;
    String _sampleFile;
    Date _acquiredTime;
    boolean _ignoreInQC;
    int _totalCount;
    String _containerPath;

    @Nullable
    public Integer getGuideSetId()
    {
        return _guideSetId == null ? 0 : _guideSetId;
    }

    public void setGuideSetId(Integer guideSetId)
    {
        _guideSetId = guideSetId;
    }

    @Nullable
    public String getMetricId()
    {
        return _metricId;
    }

    public void setMetricId(String metricId)
    {
        _metricId = metricId;
    }

    @Nullable
    public String getMetricName()
    {
        return _metricName;
    }

    public void setMetricName(String metricName)
    {
        _metricName = metricName;
    }

    @Nullable
    public String getMetricLabel()
    {
        return _metricLabel;
    }

    public void setMetricLabel(String metricLabel)
    {
        _metricLabel = metricLabel;
    }

    @Nullable
    public String getSampleFile()
    {
        return _sampleFile;
    }

    public void setSampleFile(String sampleFile)
    {
        _sampleFile = sampleFile;
    }

    @Nullable
    public Date getAcquiredTime()
    {
        return _acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        _acquiredTime = acquiredTime;
    }

    public boolean isIgnoreInQC()
    {
        return _ignoreInQC;
    }

    public void setIgnoreInQC(boolean ignoreInQC)
    {
        _ignoreInQC = ignoreInQC;
    }

    public int getTotalCount()
    {
        return _totalCount;
    }

    public void setTotalCount(int totalCount)
    {
        _totalCount = totalCount;
    }

    @Nullable
    public String getContainerPath()
    {
        return _containerPath;
    }

    public void setContainerPath(String containerPath)
    {
        _containerPath = containerPath;
    }

    public JSONObject toJSON()
    {
        JSONObject jsonObject = super.toJSON();

        jsonObject.put("GuideSetId", _guideSetId == null ? 0 : _guideSetId);
        jsonObject.put("MetricId", _metricId);
        jsonObject.put("MetricName", _metricName);
        jsonObject.put("MetricLabel", _metricLabel);
        jsonObject.put("SampleFile", _sampleFile);
        jsonObject.put("AcquiredTime", _acquiredTime);
        jsonObject.put("IgnoreInQC", _ignoreInQC);
        jsonObject.put("TotalCount", _totalCount);
        jsonObject.put("ContainerPath", _containerPath);

        return jsonObject;
    }
}
