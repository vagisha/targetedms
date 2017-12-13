/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import java.util.List;

public abstract class GeneralMolecule extends AnnotatedEntity<GeneralMoleculeAnnotation>
{
    protected int _peptideGroupId;
    protected Double _rtCalculatorScore;
    protected Double _predictedRetentionTime;
    protected Double _avgMeasuredRetentionTime;  // average measured retention time over all replicates
    protected String _note;
    protected Double _explicitRetentionTime;
    protected String _normalizationMethod;
    protected Double _internalStandardConcentration;
    protected Double _concentrationMultiplier;
    protected String _standardType;
    private List<GeneralMoleculeChromInfo> _generalMoleculeChromInfoList;

    public int getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(int peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }
    public Double getPredictedRetentionTime()
    {
        return _predictedRetentionTime;
    }

    public void setPredictedRetentionTime(Double predictedRetentionTime)
    {
        _predictedRetentionTime = predictedRetentionTime;
    }

    public Double getAvgMeasuredRetentionTime()
    {
        return _avgMeasuredRetentionTime;
    }

    public void setAvgMeasuredRetentionTime(Double avgMeasuredRetentionTime)
    {
        _avgMeasuredRetentionTime = avgMeasuredRetentionTime;
    }

    public Double getRtCalculatorScore()
    {
        return _rtCalculatorScore;
    }

    public void setRtCalculatorScore(Double rtCalculatorScore)
    {
        _rtCalculatorScore = rtCalculatorScore;
    }
    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public Double getExplicitRetentionTime()
    {
        return _explicitRetentionTime;
    }

    public void setExplicitRetentionTime(Double explicitRetentionTime)
    {
        _explicitRetentionTime = explicitRetentionTime;
    }
    public List<GeneralMoleculeChromInfo> getGeneralMoleculeChromInfoList()
    {
        return _generalMoleculeChromInfoList;
    }

    public void setGeneralMoleculeChromInfoList(List<GeneralMoleculeChromInfo> generalMoleculeChromInfoList)
    {
        _generalMoleculeChromInfoList = generalMoleculeChromInfoList;
    }

    public String getStandardType()
    {
        return _standardType;
    }

    public void setStandardType(String standardType)
    {
        _standardType = standardType;
    }

    public abstract String getPrecursorKey(GeneralMolecule gm, GeneralPrecursor gp);

    public abstract String getTextId();

    public abstract boolean textIdMatches(String textId);

    public String getNormalizationMethod()
    {
        return _normalizationMethod;
    }

    public void setNormalizationMethod(String normalizationMethod)
    {
        _normalizationMethod = normalizationMethod;
    }

    public Double getInternalStandardConcentration()
    {
        return _internalStandardConcentration;
    }

    public void setInternalStandardConcentration(Double internalStandardConcentration)
    {
        _internalStandardConcentration = internalStandardConcentration;
    }

    public Double getConcentrationMultiplier()
    {
        return _concentrationMultiplier;
    }

    public void setConcentrationMultiplier(Double concentrationMultiplier)
    {
        _concentrationMultiplier = concentrationMultiplier;
    }
}
