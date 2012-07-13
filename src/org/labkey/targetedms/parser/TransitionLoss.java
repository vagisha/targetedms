/*
 * Copyright (c) 2012 LabKey Corporation
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
 * User: jeckels
 * Date: Jul 11, 2012
 */
public class TransitionLoss extends SkylineEntity
{
    // The fields needed to match it up correctly
    private String _modificationName;
    private String _formula;
    private Double _massDiffMono;
    private Double _massDiffAvg;

    // The database fields
    private int _transitionId;
    private int _structuralModLossId;

    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        _transitionId = transitionId;
    }

    public int getStructuralModLossId()
    {
        return _structuralModLossId;
    }

    public void setStructuralModLossId(int structuralModLossId)
    {
        _structuralModLossId = structuralModLossId;
    }

    public void setModificationName(String modificationName)
    {
        _modificationName = modificationName;
    }

    public String getModificationName()
    {
        return _modificationName;
    }

    public void setFormula(String formula)
    {
        _formula = formula;
    }

    public void setMassDiffMono(Double massDiffMono)
    {
        _massDiffMono = massDiffMono;
    }

    public void setMassDiffAvg(Double massDiffAvg)
    {
        _massDiffAvg = massDiffAvg;
    }

    public boolean matches(PeptideSettings.PotentialLoss loss)
    {
        if (_formula != null ? !_formula.equals(loss.getFormula()) : loss.getFormula() != null) return false;
        if (_massDiffAvg != null ? !_massDiffAvg.equals(loss.getMassDiffAvg()) : loss.getMassDiffAvg() != null)
            return false;
        if (_massDiffMono != null ? !_massDiffMono.equals(loss.getMassDiffMono()) : loss.getMassDiffMono() != null)
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        return "TransitionLoss{" +
                "_formula='" + _formula + '\'' +
                ", _massDiffMono=" + _massDiffMono +
                ", _massDiffAvg=" + _massDiffAvg +
                '}';
    }
}
