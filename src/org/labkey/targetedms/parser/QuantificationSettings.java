/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

import org.jetbrains.annotations.Nullable;

/**
 * Created by nicksh on 3/29/2016.
 */
public class QuantificationSettings extends SkylineEntity
{
    private int _runId;
    private String _regressionWeighting;
    private String _regressionFit;
    private String _normalizationMethod;
    private Integer _msLevel;
    private String _units;
    @Nullable private Double _maxLOQBias;
    @Nullable private Double _maxLOQCV;
    @Nullable private String _lodCalculation;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    @Nullable
    public String getRegressionWeighting()
    {
        return _regressionWeighting;
    }

    public void setRegressionWeighting(@Nullable String regressionWeighting)
    {
        _regressionWeighting = regressionWeighting;
    }

    @Nullable
    public String getRegressionFit()
    {
        return _regressionFit;
    }

    public void setRegressionFit(@Nullable String regressionFit)
    {
        _regressionFit = regressionFit;
    }

    @Nullable
    public String getNormalizationMethod()
    {
        return _normalizationMethod;
    }

    public void setNormalizationMethod(@Nullable String normalizationMethod)
    {
        _normalizationMethod = normalizationMethod;
    }

    @Nullable
    public Integer getMsLevel()
    {
        return _msLevel;
    }

    public void setMsLevel(@Nullable Integer msLevel)
    {
        _msLevel = msLevel;
    }


    @Nullable
    public String getUnits()
    {
        return _units;
    }

    public void setUnits(@Nullable String units)
    {
        _units = units;
    }

    @Nullable
    public Double getMaxLOQBias()
    {
        return _maxLOQBias;
    }

    public void setMaxLOQBias(@Nullable Double maxLOQBias)
    {
        _maxLOQBias = maxLOQBias;
    }

    public @Nullable Double getMaxLOQCV()
    {
        return _maxLOQCV;
    }

    public void setMaxLOQCV(@Nullable Double maxLOQCV)
    {
        _maxLOQCV = maxLOQCV;
    }

    public @Nullable String getLODCalculation()
    {
        return _lodCalculation;
    }

    public void setLODCalculation(@Nullable String lodCalculation)
    {
        _lodCalculation = lodCalculation;
    }
}
