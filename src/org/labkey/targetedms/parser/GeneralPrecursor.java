/*
 * Copyright (c) 2016-2018 LabKey Corporation
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

import org.labkey.api.targetedms.RepresentativeDataState;

import java.util.List;

public class GeneralPrecursor<TransitionType extends GeneralTransition> extends AnnotatedEntity<PrecursorAnnotation>
{
    protected long _generalMoleculeId;
    protected int _charge;
    protected double _mz;
    protected Double _collisionEnergy;
    protected Double _declusteringPotential;
    protected String _note;
    protected RepresentativeDataState _representativeDataState = RepresentativeDataState.NotRepresentative;
    protected Double _explicitIonMobility;
    private List<PrecursorChromInfo> _chromInfoList;
    private List<TransitionType> _transitionsList;
    private String _isotopeLabel;
    private long _isotopeLabelId;
    private Double _ccs;
    private String _explicitIonMobilityUnits;
    private Double _explicitCcsSqa;
    private Double _explicitCompensationVoltage;
    private Double _precursorConcentration;

    public long getGeneralMoleculeId()
    {
        return _generalMoleculeId;
    }

    public void setGeneralMoleculeId(long gmId)
    {
        _generalMoleculeId = gmId;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public double getMz()
    {
        return _mz;
    }

    public void setMz(double mz)
    {
        _mz = mz;
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

    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public Double getExplicitIonMobility()
    {
        return _explicitIonMobility;
    }

    public void setExplicitIonMobility(Double explicitIonMobility)
    {
        _explicitIonMobility = explicitIonMobility;
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

    public long getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(long isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public String getExplicitIonMobilityUnits()
    {
        return _explicitIonMobilityUnits;
    }

    public void setExplicitIonMobilityUnits(String explicitIonMobilityUnits)
    {
        _explicitIonMobilityUnits = explicitIonMobilityUnits;
    }

    public Double getExplicitCcsSqa()
    {
        return _explicitCcsSqa;
    }

    public void setExplicitCcsSqa(Double explicitCcsSqa)
    {
        _explicitCcsSqa = explicitCcsSqa;
    }

    public Double getExplicitCompensationVoltage()
    {
        return _explicitCompensationVoltage;
    }

    public void setExplicitCompensationVoltage(Double explicitCompensationVoltage)
    {
        _explicitCompensationVoltage = explicitCompensationVoltage;
    }

    public Double getPrecursorConcentration()
    {
        return _precursorConcentration;
    }

    public void setPrecursorConcentration(Double precursorConcentration)
    {
        _precursorConcentration = precursorConcentration;
    }
}
