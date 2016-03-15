package org.labkey.targetedms.parser;

import java.util.List;

public class GeneralTransition extends AnnotatedEntity<TransitionAnnotation>
{

    protected int generalPrecursorId;
    protected double mz;
    protected Integer charge;
    protected String fragmentType;  // 'a', 'b', 'c', 'x', 'y', 'z' or 'precursor'
    protected Integer isotopeDistRank;
    protected Double isotopeDistProportion;
    protected Boolean decoy;
    protected String note;
    protected Integer massIndex;
    protected Double explicitCollisionEnergy;
    protected Double sLens;
    protected Double coneVoltage;
    protected Double explicitCompensationVoltage;
    protected Double explicitDeclusteringPotential;
    protected Double explicitDriftTimeMSec;
    protected Double explicitDriftTimeHighEnergyOffsetMSec;
    private List<TransitionChromInfo> _chromInfoList;

    public int getGeneralPrecursorId()
    {
        return generalPrecursorId;
    }

    public void setGeneralPrecursorId(int precursorId)
    {
        this.generalPrecursorId = precursorId;
    }

    public String getFragmentType()
    {
        return fragmentType;
    }

    public void setFragmentType(String fragmentType)
    {
        this.fragmentType = fragmentType;
    }

    public Integer getCharge()
    {
        return charge;
    }

    public void setCharge(Integer charge)
    {
        this.charge = charge;
    }

    public Integer getMassIndex()
    {
        return massIndex;
    }

    public void setMassIndex(Integer massIndex)
    {
        this.massIndex = massIndex;
    }

    public Integer getIsotopeDistRank()
    {
        return isotopeDistRank;
    }

    public void setIsotopeDistRank(Integer isotopeDistRank)
    {
        this.isotopeDistRank = isotopeDistRank;
    }

    public Double getIsotopeDistProportion()
    {
        return isotopeDistProportion;
    }

    public void setIsotopeDistProportion(Double isotopeDistProportion)
    {
        this.isotopeDistProportion = isotopeDistProportion;
    }

    public Boolean isDecoy()
    {
        return decoy;
    }

    public void setDecoy(Boolean decoy)
    {
        this.decoy = decoy;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public double getMz()
    {
        return mz;
    }

    public void setMz(double productMz)
    {
        this.mz = productMz;
    }

    public Double getExplicitCollisionEnergy()
    {
        return explicitCollisionEnergy;
    }

    public void setExplicitCollisionEnergy(Double explicitCollisionEnergy)
    {
        this.explicitCollisionEnergy = explicitCollisionEnergy;
    }

    public Double getsLens()
    {
        return sLens;
    }

    public void setsLens(Double sLens)
    {
        this.sLens = sLens;
    }

    public Double getConeVoltage()
    {
        return coneVoltage;
    }

    public void setConeVoltage(Double coneVoltage)
    {
        this.coneVoltage = coneVoltage;
    }

    public Double getExplicitCompensationVoltage()
    {
        return explicitCompensationVoltage;
    }

    public void setExplicitCompensationVoltage(Double explicitCompensationVoltage)
    {
        this.explicitCompensationVoltage = explicitCompensationVoltage;
    }

    public Double getExplicitDeclusteringPotential()
    {
        return explicitDeclusteringPotential;
    }

    public void setExplicitDeclusteringPotential(Double explicitDeclusteringPotential)
    {
        this.explicitDeclusteringPotential = explicitDeclusteringPotential;
    }

    public Double getExplicitDriftTimeMSec()
    {
        return explicitDriftTimeMSec;
    }

    public void setExplicitDriftTimeMSec(Double explicitDriftTimeMSec)
    {
        this.explicitDriftTimeMSec = explicitDriftTimeMSec;
    }

    public Double getExplicitDriftTimeHighEnergyOffsetMSec()
    {
        return explicitDriftTimeHighEnergyOffsetMSec;
    }

    public void setExplicitDriftTimeHighEnergyOffsetMSec(Double explicitDriftTimeHighEnergyOffsetMSec)
    {
        this.explicitDriftTimeHighEnergyOffsetMSec = explicitDriftTimeHighEnergyOffsetMSec;
    }

    public List<TransitionChromInfo> getChromInfoList()
    {
        return _chromInfoList;
    }

    public void setChromInfoList(List<TransitionChromInfo> chromInfoList)
    {
        _chromInfoList = chromInfoList;
    }
}
