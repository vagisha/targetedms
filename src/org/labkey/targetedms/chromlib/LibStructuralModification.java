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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/29/12
 * Time: 9:56 PM
 */
public class LibStructuralModification implements ObjectWithId
{
    private int _id;
    private String _name;
    private String _aminoAcid;
    private Character _terminus;
    private String _formula;
    private Double _massDiffMono;
    private Double _massDiffAvg;
    private Integer _unimodId;
    private Boolean _variable;
    private Boolean _explicitMod;

    private List<LibStructuralModLoss> _modLosses;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getAminoAcid()
    {
        return _aminoAcid;
    }

    public void setAminoAcid(String aminoAcid)
    {
        _aminoAcid = aminoAcid;
    }

    public Character getTerminus()
    {
        return _terminus;
    }

    public void setTerminus(Character terminus)
    {
        _terminus = terminus;
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

    public Integer getUnimodId()
    {
        return _unimodId;
    }

    public void setUnimodId(Integer unimodId)
    {
        _unimodId = unimodId;
    }

    public Boolean getVariable()
    {
        return _variable;
    }

    public void setVariable(Boolean variable)
    {
        _variable = variable;
    }

    public Boolean getExplicitMod()
    {
        return _explicitMod;
    }

    public void setExplicitMod(Boolean explicitMod)
    {
        _explicitMod = explicitMod;
    }

    public void addModLoss(LibStructuralModLoss modLoss)
    {
        if(_modLosses == null)
        {
            _modLosses = new ArrayList<>();
        }
        _modLosses.add(modLoss);
    }

    List<LibStructuralModLoss> getModLosses()
    {
        if(_modLosses == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_modLosses);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibStructuralModification)) return false;

        LibStructuralModification that = (LibStructuralModification) o;

        if (_aminoAcid != null ? !_aminoAcid.equals(that._aminoAcid) : that._aminoAcid != null) return false;
        if (_explicitMod != null ? !_explicitMod.equals(that._explicitMod) : that._explicitMod != null) return false;
        if (_formula != null ? !_formula.equals(that._formula) : that._formula != null) return false;
        if (_massDiffAvg != null ? !_massDiffAvg.equals(that._massDiffAvg) : that._massDiffAvg != null) return false;
        if (_massDiffMono != null ? !_massDiffMono.equals(that._massDiffMono) : that._massDiffMono != null)
            return false;
        if (_modLosses != null ? !_modLosses.equals(that._modLosses) : that._modLosses != null) return false;
        if (!_name.equals(that._name)) return false;
        if (_terminus != null ? !_terminus.equals(that._terminus) : that._terminus != null) return false;
        if (_unimodId != null ? !_unimodId.equals(that._unimodId) : that._unimodId != null) return false;
        if (!_variable.equals(that._variable)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _name.hashCode();
        result = 31 * result + (_aminoAcid != null ? _aminoAcid.hashCode() : 0);
        result = 31 * result + (_terminus != null ? _terminus.hashCode() : 0);
        result = 31 * result + (_formula != null ? _formula.hashCode() : 0);
        result = 31 * result + (_massDiffMono != null ? _massDiffMono.hashCode() : 0);
        result = 31 * result + (_massDiffAvg != null ? _massDiffAvg.hashCode() : 0);
        result = 31 * result + (_unimodId != null ? _unimodId.hashCode() : 0);
        result = 31 * result + _variable.hashCode();
        result = 31 * result + (_explicitMod != null ? _explicitMod.hashCode() : 0);
        result = 31 * result + (_modLosses != null ? _modLosses.hashCode() : 0);
        return result;
    }
}
