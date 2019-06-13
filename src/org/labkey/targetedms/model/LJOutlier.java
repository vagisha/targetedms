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

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Date;

public class LJOutlier
{
    Integer _guideSetId;
    String _metricId;
    String _metricName;
    String _metricLabel;
    String _sampleFile;
    Date _acquiredTime;
    boolean _ignoreInQC;
    int _nonConformers;
    int _totalCount;
    int _CUSUMm;
    int _CUSUMv;
    int _CUSUMmN;
    int _CUSUMmP;
    int _CUSUMvP;
    int _CUSUMvN;
    int _mR;
    String _containerPath;

    public LJOutlier()
    {
        _CUSUMm = 0;
        _CUSUMv = 0;
        _CUSUMmN = 0;
        _CUSUMmP = 0;
        _CUSUMvP = 0;
        _CUSUMvN = 0;
        _mR = 0;
    }

    @Nullable
    public Integer getGuideSetId()
    {
        return _guideSetId;
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

    public int getNonConformers()
    {
        return _nonConformers;
    }

    public void setNonConformers(int nonConformers)
    {
        _nonConformers = nonConformers;
    }

    public int getTotalCount()
    {
        return _totalCount;
    }

    public void setTotalCount(int totalCount)
    {
        _totalCount = totalCount;
    }

    public int getCUSUMm()
    {
        return _CUSUMm;
    }

    public void setCUSUMm(int CUSUMm)
    {
        _CUSUMm = CUSUMm;
    }

    public int getCUSUMv()
    {
        return _CUSUMv;
    }

    public void setCUSUMv(int CUSUMv)
    {
        _CUSUMv = CUSUMv;
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("GuideSetId", _guideSetId);
        jsonObject.put("MetricId", _metricId);
        jsonObject.put("MetricName", _metricName);
        jsonObject.put("MetricLabel", _metricLabel);
        jsonObject.put("SampleFile", _sampleFile);
        jsonObject.put("AcquiredTime", _acquiredTime);
        jsonObject.put("IgnoreInQC", _ignoreInQC);
        jsonObject.put("NonConformers", _nonConformers);
        jsonObject.put("CUSUMm", _CUSUMm);
        jsonObject.put("CUSUMv", _CUSUMv);
        jsonObject.put("CUSUMmN", _CUSUMmN);
        jsonObject.put("CUSUMmP", _CUSUMmP);
        jsonObject.put("CUSUMvP", _CUSUMvP);
        jsonObject.put("CUSUMvN", _CUSUMvN);
        jsonObject.put("mR", _mR);
        jsonObject.put("TotalCount", _totalCount);
        jsonObject.put("ContainerPath", _containerPath);

        return jsonObject;
    }
}
