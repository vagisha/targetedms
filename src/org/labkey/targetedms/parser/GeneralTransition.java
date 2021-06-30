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

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GeneralTransition extends AnnotatedEntity<TransitionAnnotation>
{

    protected long generalPrecursorId;
    protected double mz;
    protected Integer charge;
    protected String fragmentType;  // 'a', 'b', 'c', 'x', 'y', 'z' or 'precursor'
    protected Integer isotopeDistRank;
    protected Double isotopeDistProportion;
    protected String note;
    protected Integer massIndex;
    protected Double explicitCollisionEnergy;
    protected Double explicitSLens;
    protected Double explicitConeVoltage;
    protected Double explicitDeclusteringPotential;
    private List<TransitionChromInfo> _chromInfoList;
    private Boolean quantitative;
    private Double explicitIonMobilityHighEnergyOffset;
    private Double collisionEnergy;
    private Double declusteringPotential;
    private Integer rank;
    private Double intensity;

    protected static final String CUSTOM = "custom";
    protected static final String PRECURSOR = "precursor";
    private static final String Y_ION = "y";
    private static final String Z_ION = "z";
    private static final String X_ION = "x";
    private static final String B_ION = "b";
    private static final String C_ION = "c";
    private static final String A_ION = "a";

    public long getGeneralPrecursorId()
    {
        return generalPrecursorId;
    }

    public void setGeneralPrecursorId(long precursorId)
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

    public Double getExplicitSLens()
    {
        return explicitSLens;
    }

    public void setExplicitSLens(Double explicitSLens)
    {
        this.explicitSLens = explicitSLens;
    }

    public Double getExplicitConeVoltage()
    {
        return explicitConeVoltage;
    }

    public void setExplicitConeVoltage(Double explicitConeVoltage)
    {
        this.explicitConeVoltage = explicitConeVoltage;
    }


    public Double getExplicitDeclusteringPotential()
    {
        return explicitDeclusteringPotential;
    }

    public void setExplicitDeclusteringPotential(Double explicitDeclusteringPotential)
    {
        this.explicitDeclusteringPotential = explicitDeclusteringPotential;
    }

    public List<TransitionChromInfo> getChromInfoList()
    {
        return _chromInfoList;
    }

    public void setChromInfoList(List<TransitionChromInfo> chromInfoList)
    {
        _chromInfoList = chromInfoList;
    }

    public Boolean getQuantitative()
    {
        return quantitative;
    }

    public void setQuantitative(Boolean quantitative)
    {
        this.quantitative = quantitative;
    }

    public boolean explicitQuantitative()
    {
        return quantitative == null || quantitative;
    }

    // Look at TransitionDocNode.IsQuantitative(SrmSettings settings) in the Skyline code
    public boolean isQuantitative(@Nullable TransitionSettings.FullScanSettings settings)
    {
        if (!explicitQuantitative())
        {
            return false;
        }
        return isMs1() || !isFullScanAcquisitionDDA(settings);
    }

    boolean isMs1()
    {
        return isPrecursorIon();
    }

    private boolean isFullScanAcquisitionDDA(TransitionSettings.FullScanSettings fullScanSettings)
    {
        return fullScanSettings != null && "DDA".equals(fullScanSettings.getAcquisitionMethod());
    }

    public Double getExplicitIonMobilityHighEnergyOffset()
    {
        return explicitIonMobilityHighEnergyOffset;
    }

    public void setExplicitIonMobilityHighEnergyOffset(Double explicitIonMobilityHighEnergyOffset)
    {
        this.explicitIonMobilityHighEnergyOffset = explicitIonMobilityHighEnergyOffset;
    }

    public Double getCollisionEnergy()
    {
        return collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        this.collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        this.declusteringPotential = declusteringPotential;
    }

    public Integer getRank()
    {
        return rank;
    }

    public void setRank(Integer rank)
    {
        this.rank = rank;
    }

    public Double getIntensity()
    {
        return intensity;
    }

    public void setIntensity(Double intensity)
    {
        this.intensity = intensity;
    }

    public boolean isCustomIon()
    {
        return CUSTOM.equalsIgnoreCase(fragmentType);
    }

    public boolean isPrecursorIon()
    {
        return PRECURSOR.equalsIgnoreCase(fragmentType);
    }

    public boolean isNterm()
    {
        return fragmentType != null && (fragmentType.equalsIgnoreCase(B_ION)
                || fragmentType.equalsIgnoreCase(C_ION)
                || fragmentType.equalsIgnoreCase(A_ION));
    }


    public boolean isCterm()
    {
        return fragmentType != null && (fragmentType.equalsIgnoreCase(Y_ION)
                || fragmentType.equalsIgnoreCase(Z_ION)
                || fragmentType.equalsIgnoreCase(X_ION));
    }
}
