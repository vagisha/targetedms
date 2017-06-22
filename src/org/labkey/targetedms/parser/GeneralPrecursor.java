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

public class GeneralPrecursor<TransitionType extends GeneralTransition> extends AnnotatedEntity<PrecursorAnnotation>
{
    protected int _generalMoleculeId;
    protected int _charge;
    protected double _mz;
    protected Double _collisionEnergy;
    protected Double _declusteringPotential;
    protected Boolean _decoy;
    protected String _note;
    protected RepresentativeDataState _representativeDataState = RepresentativeDataState.NotRepresentative;
    protected Double _explicitCollisionEnergy;
    protected Double _explicitDriftTimeMsec;
    protected Double _explicitDriftTimeHighEnergyOffsetMsec;
    private List<PrecursorChromInfo> _chromInfoList;
    private List<TransitionType> _transitionsList;
    private String _isotopeLabel;
    private int _isotopeLabelId;

    public int getGeneralMoleculeId()
    {
        return _generalMoleculeId;
    }

    public void setGeneralMoleculeId(int gmId)
    {
        this._generalMoleculeId = gmId;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        this._charge = charge;
    }

    public double getMz()
    {
        return _mz;
    }

    public void setMz(double mz)
    {
        this._mz = mz;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        this._collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        this._declusteringPotential = declusteringPotential;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public Double getExplicitCollisionEnergy()
    {
        return _explicitCollisionEnergy;
    }

    public void setExplicitCollisionEnergy(Double explicitCollisionEnergy)
    {
        _explicitCollisionEnergy = explicitCollisionEnergy;
    }

    public Double getExplicitDriftTimeMsec()
    {
        return _explicitDriftTimeMsec;
    }

    public void setExplicitDriftTimeMsec(Double explicitDriftTimeMsec)
    {
        _explicitDriftTimeMsec = explicitDriftTimeMsec;
    }

    public Double getExplicitDriftTimeHighEnergyOffsetMsec()
    {
        return _explicitDriftTimeHighEnergyOffsetMsec;
    }

    public void setExplicitDriftTimeHighEnergyOffsetMsec(Double explicitDriftTimeHighEnergyOffsetMsec)
    {
        _explicitDriftTimeHighEnergyOffsetMsec = explicitDriftTimeHighEnergyOffsetMsec;
    }

    public Boolean isDecoy()
    {
        return _decoy;
    }

    public void setDecoy(Boolean decoy)
    {
        _decoy = decoy;
    }

    public RepresentativeDataState getRepresentativeDataState()
    {
        return _representativeDataState;
    }

    public void setRepresentativeDataState(RepresentativeDataState representativeDataState)
    {
        _representativeDataState = representativeDataState;
    }

    public List<PrecursorChromInfo> getChromInfoList()
    {
        return _chromInfoList;
    }

    public void setChromInfoList(List<PrecursorChromInfo> chromInfoList)
    {
        _chromInfoList = chromInfoList;
    }

    public List<TransitionType> getTransitionsList()
    {
        return _transitionsList;
    }

    public void setTransitionsList(List<TransitionType> transitionsList)
    {
        _transitionsList = transitionsList;
    }

    public SignedMz getSignedMz()
    {
        return new SignedMz(_mz, _charge < 0);
    }

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        this._isotopeLabelId = isotopeLabelId;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        this._isotopeLabel = isotopeLabel;
    }
}
