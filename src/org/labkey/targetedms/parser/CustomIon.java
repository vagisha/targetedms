/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

/**
 * Created by vsharma on 9/23/2016.
 */
public class CustomIon
{
    private final static String massFormat = "{0} [{1:F06}/{2:F06}]";

    private CustomIon(){}

    public static String getName(Molecule molecule)
    {
        return getName(molecule.getCustomIonName(), molecule.getIonFormula(), molecule.getMassMonoisotopic(), molecule.getMassAverage());
    }

    public static String getName(MoleculeTransition transition)
    {
        return getName(transition.getCustomIonName(), transition.getIonFormula(), transition.getMassMonoisotopic(), transition.getMassAverage());
    }

    private static String getName(String customIonName, String ionFormula, Double massMonoisotopic, double massAverage)
    {
        if(customIonName != null)
        {
            return customIonName;
        }
        if(ionFormula != null)
        {
            return ionFormula;
        }
        return String.format(massFormat, "Ion", massMonoisotopic, massAverage);
    }
}
