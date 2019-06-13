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
 * Created by nicksh on 2/26/2016.
 */
public class GroupComparisonSettings extends SkylineEntity
{
    private int _runId;
    private String _name;
    private String _normalizationMethod;
    private double _confidenceLevel = .95;
    private String _controlAnnotation;
    private String _controlValue;
    private String _caseValue;
    private String _identityAnnotation;
    private boolean _perProtein;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getNormalizationMethod()
    {
        return _normalizationMethod;
    }

    public void setNormalizationMethod(String normalizationMethod)
    {
        _normalizationMethod = normalizationMethod;
    }

    public double getConfidenceLevel()
    {
        return _confidenceLevel;
    }

    public void setConfidenceLevel(double confidenceLevel)
    {
        _confidenceLevel = confidenceLevel;
    }

    public String getControlAnnotation()
    {
        return _controlAnnotation;
    }

    public void setControlAnnotation(String controlAnnotation)
    {
        _controlAnnotation = controlAnnotation;
    }

    public String getControlValue()
    {
        return _controlValue;
    }

    public void setControlValue(String controlValue)
    {
        _controlValue = controlValue;
    }

    public String getCaseValue()
    {
        return _caseValue;
    }

    public void setCaseValue(String caseValue)
    {
        _caseValue = caseValue;
    }

    public String getIdentityAnnotation()
    {
        return _identityAnnotation;
    }

    public void setIdentityAnnotation(String identityAnnotation)
    {
        _identityAnnotation = identityAnnotation;
    }

    public boolean isPerProtein()
    {
        return _perProtein;
    }

    public void setPerProtein(boolean perProtein)
    {
        _perProtein = perProtein;
    }
}
