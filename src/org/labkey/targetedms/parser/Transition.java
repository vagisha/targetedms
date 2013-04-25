/*
 * Copyright (c) 2012-2013 LabKey Corporation
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


import org.labkey.targetedms.chart.LabelFactory;

import java.util.List;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Transition extends AnnotatedEntity<TransitionAnnotation>
{
    private int precursorId;

    private String fragmentType;  // 'a', 'b', 'c', 'x', 'y', 'z' or 'precursor'
    private Integer fragmentOrdinal;  // e.g. Value is 9 for the y9 fragment
    private Integer charge;

    private Double neutralMass;
    private Double neutralLossMass;

    private String cleavageAa;

    private Double decoyMassShift;

    // Library values
    private Integer _libraryRank;
    private Double _libraryIntensity;

    // These fields will be set if the fragmentType is 'precursor'.
    private Integer massIndex;
    private Integer isotopeDistRank;
    private Double isotopeDistProportion;

    private double mz;
    private double precursorMz;

    private Double collisionEnergy;
    private Double declusteringPotential;

    private List<TransitionChromInfo> _chromInfoList;
    private List<TransitionLoss> _neutralLosses;

    private static final String PRECURSOR = "precursor";

    public int getPrecursorId()
    {
        return precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        this.precursorId = precursorId;
    }

    public String getFragmentType()
    {
        return fragmentType;
    }

    public void setFragmentType(String fragmentType)
    {
        this.fragmentType = fragmentType;
    }

    public Integer getFragmentOrdinal()
    {
        return fragmentOrdinal;
    }

    public void setFragmentOrdinal(Integer fragmentOrdinal)
    {
        this.fragmentOrdinal = fragmentOrdinal;
    }

    public Integer getCharge()
    {
        return charge;
    }

    public void setCharge(Integer charge)
    {
        this.charge = charge;
    }

    public Double getNeutralMass()
    {
        return neutralMass;
    }

    public void setNeutralMass(Double neutralMass)
    {
        this.neutralMass = neutralMass;
    }

    public Double getNeutralLossMass()
    {
        return neutralLossMass;
    }

    public void setNeutralLossMass(Double neutralLossMass)
    {
        this.neutralLossMass = neutralLossMass;
    }

    public String getCleavageAa()
    {
        return cleavageAa;
    }

    public void setCleavageAa(String cleavageAa)
    {
        this.cleavageAa = cleavageAa;
    }

    public Double getDecoyMassShift()
    {
        return decoyMassShift;
    }

    public void setDecoyMassShift(Double decoyMassShift)
    {
        this.decoyMassShift = decoyMassShift;
    }

    public Integer getLibraryRank()
    {
        return _libraryRank;
    }

    public void setLibraryRank(Integer libraryRank)
    {
        _libraryRank = libraryRank;
    }

    public Double getLibraryIntensity()
    {
        return _libraryIntensity;
    }

    public void setLibraryIntensity(Double libraryIntensity)
    {
        _libraryIntensity = libraryIntensity;
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

    public double getMz()
    {
        return mz;
    }

    public void setMz(double productMz)
    {
        this.mz = productMz;
    }

    public double getPrecursorMz()
    {
        return precursorMz;
    }

    public void setPrecursorMz(double precursorMz)
    {
        this.precursorMz = precursorMz;
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

    public List<TransitionChromInfo> getChromInfoList()
    {
        return _chromInfoList;
    }

    public void setChromInfoList(List<TransitionChromInfo> chromInfoList)
    {
        _chromInfoList = chromInfoList;
    }

    public List<TransitionLoss> getNeutralLosses()
    {
        return _neutralLosses;
    }

    public void setNeutralLosses(List<TransitionLoss> neutralLosses)
    {
        _neutralLosses = neutralLosses;
    }

    public boolean isPrecursorIon()
    {
        return fragmentType != null ? fragmentType.equalsIgnoreCase(PRECURSOR) : false;
    }

    public String getLabel()
    {
        return LabelFactory.transitionLabel(this);
    }
}
