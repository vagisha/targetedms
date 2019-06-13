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
 * Date: 12/29/12
 * Time: 9:56 PM
 */
public class LibIsotopeModification implements ObjectWithId
{
    private int _id;
    private String _name;
    private String _isotopeLabel;
    private String _aminoAcid;
    private Character _terminus;
    private String _formula;
    private Double _massDiffMono;
    private Double _massDiffAvg;
    private Boolean _label13C;
    private Boolean _label15N;
    private Boolean _label18O;
    private Boolean _label2H;
    private Integer _unimodId;

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

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public Boolean getLabel13C()
    {
        return _label13C;
    }

    public void setLabel13C(Boolean label13C)
    {
        _label13C = label13C;
    }

    public Boolean getLabel15N()
    {
        return _label15N;
    }

    public void setLabel15N(Boolean label15N)
    {
        _label15N = label15N;
    }

    public Boolean getLabel18O()
    {
        return _label18O;
    }

    public void setLabel18O(Boolean label18O)
    {
        _label18O = label18O;
    }

    public Boolean getLabel2H()
    {
        return _label2H;
    }

    public void setLabel2H(Boolean label2H)
    {
        _label2H = label2H;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibIsotopeModification)) return false;

        LibIsotopeModification that = (LibIsotopeModification) o;

        if (_aminoAcid != null ? !_aminoAcid.equals(that._aminoAcid) : that._aminoAcid != null) return false;
        if (_formula != null ? !_formula.equals(that._formula) : that._formula != null) return false;
        if (!_isotopeLabel.equals(that._isotopeLabel)) return false;
        if (_label13C != null ? !_label13C.equals(that._label13C) : that._label13C != null) return false;
        if (_label15N != null ? !_label15N.equals(that._label15N) : that._label15N != null) return false;
        if (_label18O != null ? !_label18O.equals(that._label18O) : that._label18O != null) return false;
        if (_label2H != null ? !_label2H.equals(that._label2H) : that._label2H != null) return false;
        if (_massDiffAvg != null ? !_massDiffAvg.equals(that._massDiffAvg) : that._massDiffAvg != null) return false;
        if (_massDiffMono != null ? !_massDiffMono.equals(that._massDiffMono) : that._massDiffMono != null)
            return false;
        if (!_name.equals(that._name)) return false;
        if (_terminus != null ? !_terminus.equals(that._terminus) : that._terminus != null) return false;
        if (_unimodId != null ? !_unimodId.equals(that._unimodId) : that._unimodId != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _name.hashCode();
        result = 31 * result + _isotopeLabel.hashCode();
        result = 31 * result + (_aminoAcid != null ? _aminoAcid.hashCode() : 0);
        result = 31 * result + (_terminus != null ? _terminus.hashCode() : 0);
        result = 31 * result + (_formula != null ? _formula.hashCode() : 0);
        result = 31 * result + (_massDiffMono != null ? _massDiffMono.hashCode() : 0);
        result = 31 * result + (_massDiffAvg != null ? _massDiffAvg.hashCode() : 0);
        result = 31 * result + (_label13C != null ? _label13C.hashCode() : 0);
        result = 31 * result + (_label15N != null ? _label15N.hashCode() : 0);
        result = 31 * result + (_label18O != null ? _label18O.hashCode() : 0);
        result = 31 * result + (_label2H != null ? _label2H.hashCode() : 0);
        result = 31 * result + (_unimodId != null ? _unimodId.hashCode() : 0);
        return result;
    }
}
