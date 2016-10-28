/*
 * Copyright (c) 2012-2016 LabKey Corporation
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


import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.chart.LabelFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Transition extends GeneralTransition
{
    public enum Type
    {
        PRECURSOR,
        PRODUCT,
        ALL
    }

    private Integer fragmentOrdinal;  // e.g. Value is 9 for the y9 fragment
    private Double neutralMass;
    private Double neutralLossMass;
    private String cleavageAa;
    private Double decoyMassShift;

    // Library values
    private Integer _libraryRank;
    private Double _libraryIntensity;

    // These fields will be set if the fragmentType is 'precursor'.
    private double precursorMz;
    private Double collisionEnergy;
    private Double declusteringPotential;

    private List<TransitionLoss> _neutralLosses;

    // The name of the measured ion that this transition uses (Only for reporter ions and other non proteomic transitions)
    private String measuredIonName;

    public Integer getFragmentOrdinal()
    {
        return fragmentOrdinal;
    }

    public void setFragmentOrdinal(Integer fragmentOrdinal)
    {
        this.fragmentOrdinal = fragmentOrdinal;
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

    public List<TransitionLoss> getNeutralLosses()
    {
        return _neutralLosses;
    }

    public void setNeutralLosses(List<TransitionLoss> neutralLosses)
    {
        _neutralLosses = neutralLosses;
    }

    public String getMeasuredIonName()
    {
        return measuredIonName;
    }

    public void setMeasuredIonName(String measuredIonName)
    {
        this.measuredIonName = measuredIonName;
    }

    @Override
    public String toString()
    {
        return LabelFactory.transitionLabel(this);
    }

    public static Set<String> getColumns()
    {
        Set<String> colNames = new HashSet<>();
        colNames.addAll(TargetedMSManager.getTableInfoTransition().getColumnNameSet());
        colNames.addAll(TargetedMSManager.getTableInfoGeneralTransition().getColumnNameSet());
        return colNames;
    }

    public static class TransitionComparator implements Comparator<Transition>
    {
        private static Map<String, Integer> ionOrder;
        static{
            ionOrder = new HashMap<>();
            ionOrder.put("precursor", 1);
            ionOrder.put("y", 2);
            ionOrder.put("b", 3);
            ionOrder.put("z", 4);
            ionOrder.put("c", 5);
            ionOrder.put("x", 6);
            ionOrder.put("a", 7);
        }

        @Override
        public int compare(Transition t1, Transition t2)
        {
            int result = ionOrder.get(t1.getFragmentType()).compareTo(ionOrder.get(t2.getFragmentType()));
            if(result == 0)
            {
                if(t1.isPrecursorIon() && t2.isPrecursorIon())
                {
                    // Precursor ions are ordered M, M+1, M+2.
                    return t1.getMassIndex().compareTo(t2.getMassIndex());
                }
                else
                {
                    result = t1.getCharge().compareTo(t2.getCharge());
                    if(result == 0)
                    {
                        // c-term fragment ions are displayed in reverse order -- y9, y8, y7 etc.
                        // n-term fragment ions are displayed in forward order -- b1, b2, b3 etc.
                        if(t1.isNterm() && t2.isNterm())
                        {
                            result = t1.getFragmentOrdinal().compareTo(t2.getFragmentOrdinal());
                            if(result != 0)  return result;
                        }
                        else if(t1.isCterm() && t2.isCterm())
                        {
                            result = t2.getFragmentOrdinal().compareTo(t1.getFragmentOrdinal());
                            if(result != 0)  return result;
                        }
                        return Double.valueOf(t2.getMz()).compareTo(t1.getMz());
                    }
                    return result;
                }
            }
            return result;
        }
    }
}
