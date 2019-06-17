/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
import java.util.HashSet;
import java.util.Set;

/**
 * User: vsharma
 * Date: 2/18/2015
 * Time: 4:23 PM
 */
public class MoleculeTransition extends GeneralTransition
{
    private int _transitionId;

    private String _ionFormula;
    private String _customIonName;
    private Double _massMonoisotopic;
    private Double _massAverage;

    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        _transitionId = transitionId;
    }

    public String getIonFormula()
    {
        return _ionFormula;
    }

    public void setIonFormula(String ionFormula)
    {
        _ionFormula = ionFormula;
    }

    public String getCustomIonName()
    {
        return _customIonName;
    }

    public void setCustomIonName(String customIonName)
    {
        _customIonName = customIonName;
    }

    public Double getMassMonoisotopic()
    {
        return _massMonoisotopic;
    }

    public void setMassMonoisotopic(Double massMonoisotopic)
    {
        _massMonoisotopic = massMonoisotopic;
    }

    public Double getMassAverage()
    {
        return _massAverage;
    }

    public void setMassAverage(Double massAverage)
    {
        _massAverage = massAverage;
    }

    public static Set<String> getColumns()
    {
        Set<String> colNames = new HashSet<>();
        colNames.addAll(TargetedMSManager.getTableInfoMoleculeTransition().getColumnNameSet());
        colNames.addAll(TargetedMSManager.getTableInfoGeneralTransition().getColumnNameSet());
        return colNames;
    }

    @Override
    public String toString()
    {
        return LabelFactory.transitionLabel(this);
    }

    public String getName()
    {
        return CustomIon.getName(this);
    }

    public static class MoleculeTransitionComparator implements Comparator<MoleculeTransition>
    {
        @Override
        public int compare(MoleculeTransition t1, MoleculeTransition t2)
        {
            if(t1.isPrecursorIon() && t2.isPrecursorIon())
            {
                // Precursor ions are ordered M, M+1, M+2.
                return t1.getMassIndex().compareTo(t2.getMassIndex());
            }

            int result = nullSafeCompareTo(t1.getName(), t2.getName());

            if(result == 0)
            {
                result = Double.valueOf(t2.getMz()).compareTo(t1.getMz());
            }
            if(result == 0)
            {
                result = nullSafeCompareTo(t1.getCharge(), t2.getCharge());
            }

            return result;
        }

        private static <T extends Comparable<T>> int nullSafeCompareTo(T o1, T o2)
        {
            // null is greater
           return (o1 == null && o2 == null) ? 0 : (o1 == null ? 1 : (o2 == null ? -1 : o1.compareTo(o2)));
        }
    }
}
