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

/**
 * Created by nicksh on 3/25/2016.
 */
public class CalibrationCurveEntity extends SkylineEntity
{
    private int _runId;
    private int _quantificationSettingsId;
    private Integer _generalMoleculeId;
    private Double _slope;
    private Double _intercept;
    private Integer _pointCount;
    private Double _quadraticCoefficient;
    private Double _rSquared;
    private String _errorMessage;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public int getQuantificationSettingsId()
    {
        return _quantificationSettingsId;
    }

    public void setQuantificationSettingsId(int quantificationSettingsId)
    {
        _quantificationSettingsId = quantificationSettingsId;
    }

    public Integer getGeneralMoleculeId()
    {
        return _generalMoleculeId;
    }

    public void setGeneralMoleculeId(Integer generalMoleculeId)
    {
        _generalMoleculeId = generalMoleculeId;
    }

    public Double getSlope()
    {
        return _slope;
    }

    public void setSlope(Double slope)
    {
        _slope = slope;
    }

    public Double getIntercept()
    {
        return _intercept;
    }

    public void setIntercept(Double intercept)
    {
        _intercept = intercept;
    }

    public Integer getPointCount()
    {
        return _pointCount;
    }

    public void setPointCount(Integer pointCount)
    {
        _pointCount = pointCount;
    }

    public Double getQuadraticCoefficient()
    {
        return _quadraticCoefficient;
    }

    public void setQuadraticCoefficient(Double quadraticCoefficient)
    {
        _quadraticCoefficient = quadraticCoefficient;
    }

    public Double getRSquared()
    {
        return _rSquared;
    }

    public void setRSquared(Double rSquared)
    {
        _rSquared = rSquared;
    }

    public String getErrorMessage()
    {
        return _errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        _errorMessage = errorMessage;
    }
}
