/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

/**
 * User: vsharma
 * Date: 12/30/12
 * Time: 4:46 PM
 */
public class LibStructuralModLoss implements ObjectWithId
{
    private int _id;
    private int _structuralModId;
    private String _formula;
    private Double _massDiffMono;
    private Double _massDiffAvg;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getStructuralModId()
    {
        return _structuralModId;
    }

    public void setStructuralModId(int structuralModId)
    {
        _structuralModId = structuralModId;
    }

    public String getFormula()
    {
        return _formula;
    }

    public void setFormula(String formula)
    {
        _formula = formula;
    }

    public Double getMassDiffMono()
    {
        return _massDiffMono;
    }

    public void setMassDiffMono(Double massDiffMono)
    {
        _massDiffMono = massDiffMono;
    }

    public Double getMassDiffAvg()
    {
        return _massDiffAvg;
    }

    public void setMassDiffAvg(Double massDiffAvg)
    {
        _massDiffAvg = massDiffAvg;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibStructuralModLoss)) return false;

        LibStructuralModLoss that = (LibStructuralModLoss) o;

        if (_formula != null ? !_formula.equals(that._formula) : that._formula != null) return false;
        if (_massDiffAvg != null ? !_massDiffAvg.equals(that._massDiffAvg) : that._massDiffAvg != null) return false;
        if (_massDiffMono != null ? !_massDiffMono.equals(that._massDiffMono) : that._massDiffMono != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _formula != null ? _formula.hashCode() : 0;
        result = 31 * result + (_massDiffMono != null ? _massDiffMono.hashCode() : 0);
        result = 31 * result + (_massDiffAvg != null ? _massDiffAvg.hashCode() : 0);
        return result;
    }
}
