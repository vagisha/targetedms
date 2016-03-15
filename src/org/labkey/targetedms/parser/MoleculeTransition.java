/*
 * Copyright (c) 2015 LabKey Corporation
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

    public static class MoleculeTransitionComparator implements Comparator<MoleculeTransition>
    {
        @Override
        public int compare(MoleculeTransition t1, MoleculeTransition t2)
        {
            int result = t1.getCharge().compareTo(t2.getCharge());
            if(result == 0)
            {
                return Double.valueOf(t2.getMz()).compareTo(t1.getMz());
            }
            return result;
        }
    }
}
