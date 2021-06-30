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

import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.MoleculeTransition;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.TransitionOptimization;
import org.labkey.targetedms.parser.TransitionSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Objects;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibTransition extends AbstractLibEntity
{
    // Shared fields
    private long _precursorId;
    protected Double _mz;
    protected Integer _charge;
    protected String _fragmentType;
    protected Integer _fragmentOrdinal;
    protected Integer _massIndex;
    protected Double _area;
    protected Double _height;
    protected Double _fwhm;
    protected Integer _chromatogramIndex;
    protected Double _massErrorPPM;
    private Double _collisionEnergy;
    private Double _declusteringPotential;
    private Boolean _quantitative;


    // Proteomics fields
    private Double _neutralMass;
    private Double _neutralLossMass;

    // Small molecule fields
    private String _fragmentName;
    private String _chemicalFormula;
    private String _adduct;


    private List<LibTransitionOptimization> _optimizations = new ArrayList<>();

    public LibTransition() {}

    public LibTransition(Transition transition, TransitionChromInfo tci, Precursor precursor, List<TransitionOptimization> optimizations, TransitionSettings.FullScanSettings settings)
    {
        this((GeneralTransition) transition, tci, precursor, optimizations, settings);
        setNeutralMass(transition.getNeutralMass());
        setNeutralLossMass(transition.getNeutralLossMass());
        setFragmentOrdinal(transition.getFragmentOrdinal());
    }

    public LibTransition(MoleculeTransition transition, TransitionChromInfo tci, MoleculePrecursor precursor, List<TransitionOptimization> optimizations, TransitionSettings.FullScanSettings settings)
    {
        this((GeneralTransition) transition, tci, precursor, optimizations, settings);
        setFragmentName(transition.getCustomIonName());
        setChemicalFormula(transition.getChemicalFormula());
        setAdduct(transition.getAdduct());
    }

    private LibTransition(GeneralTransition transition, TransitionChromInfo tci, GeneralPrecursor<?> precursor, List<TransitionOptimization> optimizations, TransitionSettings.FullScanSettings settings)
    {
        setMz(transition.getMz());
        if (transition.getCharge() == null)
        {
            setCharge(precursor.getCharge());
        }
        else
        {
            setCharge(transition.getCharge());
        }
        setFragmentType(transition.getFragmentType());
        setMassIndex(transition.getMassIndex());

        if(tci != null)
        {
            setArea(tci.getArea() == null ? 0.0 : tci.getArea());
            setHeight(tci.getHeight() == null ? 0.0 : tci.getHeight());
            setFwhm(tci.getFwhm() == null ? 0.0 : tci.getFwhm());
            setChromatogramIndex(tci.getChromatogramIndex());
            setMassErrorPPM(tci.getMassErrorPPM());
        }
        else
        {
            setArea(0.0);
            setHeight(0.0);
            setFwhm(0.0);
        }

        if (transition.getCollisionEnergy() != null)
        {
            setCollisionEnergy(transition.getCollisionEnergy());
        }
        if (transition.getDeclusteringPotential() != null)
        {
            setDeclusteringPotential(transition.getDeclusteringPotential());
        }

        for (TransitionOptimization optimization : optimizations)
        {
            _optimizations.add(new LibTransitionOptimization(optimization));
        }
        _quantitative = transition.isQuantitative(settings);
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

    public String getFragmentType()
    {
        return _fragmentType;
    }

    public void setFragmentType(String fragmentType)
    {
        _fragmentType = fragmentType;
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

    public void setMassErrorPPM(Double massErrorPPM)
    {
        _massErrorPPM = massErrorPPM;
    }

    public Double getMassErrorPPM()
    {
        return _massErrorPPM;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        _collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        _declusteringPotential = declusteringPotential;
    }

    public long getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(long precursorId)
    {
        _precursorId = precursorId;
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

    public Integer getFragmentOrdinal()
    {
        return _fragmentOrdinal;
    }

    public void setFragmentOrdinal(Integer fragmentOrdinal)
    {
        _fragmentOrdinal = fragmentOrdinal;
    }

    public String getFragmentName()
    {
        return _fragmentName;
    }

    public void setFragmentName(String fragmentName)
    {
        _fragmentName = fragmentName;
    }

    public String getChemicalFormula()
    {
        return _chemicalFormula;
    }

    public void setChemicalFormula(String chemicalFormula)
    {
        _chemicalFormula = chemicalFormula;
    }

    public String getAdduct()
    {
        return _adduct;
    }

    public void setAdduct(String adduct)
    {
        _adduct = adduct;
    }

    public Boolean isQuantitative()
    {
        return _quantitative;
    }

    public void setQuantitative(Boolean quantitative)
    {
        _quantitative = quantitative;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibTransition that = (LibTransition) o;
        return _precursorId == that._precursorId && Objects.equals(_mz, that._mz) && Objects.equals(_charge, that._charge) && Objects.equals(_fragmentType, that._fragmentType) && Objects.equals(_fragmentOrdinal, that._fragmentOrdinal) && Objects.equals(_massIndex, that._massIndex) && Objects.equals(_area, that._area) && Objects.equals(_height, that._height) && Objects.equals(_fwhm, that._fwhm) && Objects.equals(_chromatogramIndex, that._chromatogramIndex) && Objects.equals(_massErrorPPM, that._massErrorPPM) && Objects.equals(_collisionEnergy, that._collisionEnergy) && Objects.equals(_declusteringPotential, that._declusteringPotential) && Objects.equals(_neutralMass, that._neutralMass) && Objects.equals(_neutralLossMass, that._neutralLossMass) && Objects.equals(_fragmentName, that._fragmentName) && Objects.equals(_chemicalFormula, that._chemicalFormula) && Objects.equals(_adduct, that._adduct);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_precursorId, _mz, _charge, _fragmentType, _fragmentOrdinal, _massIndex, _area, _height, _fwhm, _chromatogramIndex, _massErrorPPM, _collisionEnergy, _declusteringPotential, _neutralMass, _neutralLossMass, _fragmentName, _chemicalFormula, _adduct);
    }

    public List<LibTransitionOptimization> getOptimizations()
    {
        return _optimizations;
    }
}
