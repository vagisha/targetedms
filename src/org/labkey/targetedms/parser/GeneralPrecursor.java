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
}
