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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: binalpatel
 * Date: 2/23/2016
 */
public class MoleculePrecursor extends GeneralPrecursor
{
    private int _Id;

    private String _ionFormula;
    private String _customIonName;
    private Double _massMonoisotopic;
    private Double _massAverage;
    private List<MoleculeTransition> _moleculeTransitionsList;

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

    public List<MoleculeTransition> getMoleculeTransitionsList()
    {
        return _moleculeTransitionsList;
    }

    public void setMoleculeTransitionsList(List<MoleculeTransition> moleculeTransitionsList)
    {
        _moleculeTransitionsList = moleculeTransitionsList;
    }

    public static Set<String> getColumns()
    {
        Set<String> colNames = new HashSet<>();
        colNames.add("Id");
        colNames.add("GeneralMoleculeId");
        colNames.add("Mz");
        colNames.add("Charge");
        colNames.add("CollisionEnergy");
        colNames.add("DeclusteringPotential");
        colNames.add("Decoy");
        colNames.add("Note");
        colNames.add("Modified");
        colNames.add("RepresentativeDataState");
        colNames.add("ExplicitCollisionEnergy");
        colNames.add("ExplicitDriftTimeMsec");
        colNames.add("ExplicitDriftTimeHighEnergyOffsetMsec");
        colNames.add("IonFormula");
        colNames.add("CustomIonName");
        colNames.add("MassAverage");
        colNames.add("MassMonoisotopic");
        return colNames;
    }
}
