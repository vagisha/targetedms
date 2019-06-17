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
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibTransition implements ObjectWithId
{
    private int _id;
    private int _precursorId;
    private Double _mz;
    private Integer _charge;
    private Double _neutralMass;
    private Double _neutralLossMass;
    private String _fragmentType;
    private Integer _fragmentOrdinal;
    private Integer _massIndex;
    private Double _area;
    private Double _height;
    private Double _fwhm;
    private Integer _chromatogramIndex;
    private Double _massErrorPPM;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
    }

    public Integer getCharge()
    {
        return _charge;
    }

    public void setCharge(Integer charge)
    {
        _charge = charge;
    }

    public Double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(Double neutralMass)
    {
        _neutralMass = neutralMass;
    }

    public Double getNeutralLossMass()
    {
        return _neutralLossMass;
    }

    public void setNeutralLossMass(Double neutralLossMass)
    {
        _neutralLossMass = neutralLossMass;
    }

    public String getFragmentType()
    {
        return _fragmentType;
    }

    public void setFragmentType(String fragmentType)
    {
        _fragmentType = fragmentType;
    }

    public Integer getFragmentOrdinal()
    {
        return _fragmentOrdinal;
    }

    public void setFragmentOrdinal(Integer fragmentOrdinal)
    {
        _fragmentOrdinal = fragmentOrdinal;
    }

    public Integer getMassIndex()
    {
        return _massIndex;
    }

    public void setMassIndex(Integer massIndex)
    {
        _massIndex = massIndex;
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        _area = area;
    }

    public Double getHeight()
    {
        return _height;
    }

    public void setHeight(Double height)
    {
        _height = height;
    }

    public Double getFwhm()
    {
        return _fwhm;
    }

    public void setFwhm(Double fwhm)
    {
        _fwhm = fwhm;
    }

    public Integer getChromatogramIndex()
    {
        return _chromatogramIndex;
    }

    public void setChromatogramIndex(Integer chromatogramIndex)
    {
        _chromatogramIndex = chromatogramIndex;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibTransition)) return false;

        LibTransition that = (LibTransition) o;

        if (_precursorId != that._precursorId) return false;
        if (!_area.equals(that._area)) return false;
        if (_charge != null ? !_charge.equals(that._charge) : that._charge != null) return false;
        if (_chromatogramIndex != null ? !_chromatogramIndex.equals(that._chromatogramIndex) : that._chromatogramIndex != null)
            return false;
        if (_fragmentOrdinal != null ? !_fragmentOrdinal.equals(that._fragmentOrdinal) : that._fragmentOrdinal != null)
            return false;
        if (!_fragmentType.equals(that._fragmentType)) return false;
        if (!_fwhm.equals(that._fwhm)) return false;
        if (!_height.equals(that._height)) return false;
        if (_massIndex != null ? !_massIndex.equals(that._massIndex) : that._massIndex != null) return false;
        if (_mz != null ? !_mz.equals(that._mz) : that._mz != null) return false;
        if (_neutralLossMass != null ? !_neutralLossMass.equals(that._neutralLossMass) : that._neutralLossMass != null)
            return false;
        if (_neutralMass != null ? !_neutralMass.equals(that._neutralMass) : that._neutralMass != null) return false;
        if (_massErrorPPM != null ? !_massErrorPPM.equals(that._massErrorPPM) : that._massErrorPPM != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _precursorId;
        result = 31 * result + (_mz != null ? _mz.hashCode() : 0);
        result = 31 * result + (_charge != null ? _charge.hashCode() : 0);
        result = 31 * result + (_neutralMass != null ? _neutralMass.hashCode() : 0);
        result = 31 * result + (_neutralLossMass != null ? _neutralLossMass.hashCode() : 0);
        result = 31 * result + _fragmentType.hashCode();
        result = 31 * result + (_fragmentOrdinal != null ? _fragmentOrdinal.hashCode() : 0);
        result = 31 * result + (_massIndex != null ? _massIndex.hashCode() : 0);
        result = 31 * result + _area.hashCode();
        result = 31 * result + _height.hashCode();
        result = 31 * result + _fwhm.hashCode();
        result = 31 * result + (_chromatogramIndex != null ? _chromatogramIndex.hashCode() : 0);
        result = 31 * result + (_massErrorPPM != null ? _massErrorPPM.hashCode() : 0);
        return result;
    }

    public void setMassErrorPPM(Double massErrorPPM)
    {
        _massErrorPPM = massErrorPPM;
    }

    public Double getMassErrorPPM()
    {
        return _massErrorPPM;
    }
}
