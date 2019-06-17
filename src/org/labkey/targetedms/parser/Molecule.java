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

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * User: vsharma
 * Date: 1/27/2015
 * Time: 2:58 PM
 */
public class Molecule extends GeneralMolecule
{
    private String _ionFormula;
    private Double _massMonoisotopic; // not null
    private Double _massAverage; // not null
    private String _customIonName;
    private List<MoleculePrecursor> _moleculePrecursorsList;

    public String getIonFormula()
    {
        return _ionFormula;
    }

    public void setIonFormula(String ionFormula)
    {
        _ionFormula = ionFormula;
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

    public String getCustomIonName()
    {
        return _customIonName;
    }

    public void setCustomIonName(String customIonName)
    {
        _customIonName = customIonName;
    }

    public List<MoleculePrecursor> getMoleculePrecursorsList()
    {
        return _moleculePrecursorsList;
    }

    public void setMoleculePrecursorsList(List<MoleculePrecursor> moleculePrecursorsList)
    {
        _moleculePrecursorsList = moleculePrecursorsList;
    }

    @Override
    public String getPrecursorKey(GeneralMolecule gm, GeneralPrecursor gp)
    {
        StringBuilder key = new StringBuilder();
        key.append(((Molecule) gm).getMassMonoisotopic());
        key.append("_").append(gp.getCharge());
        return key.toString();
    }

    @Override
    public String getTextId()
    {
        return getName();
    }

    /**
     * Determines whether the specified textId matches this molecule.
     * The textId is stored in the .skyd file. For small molecules, the textId is formatted as a set
     * of components separated by "$". We only compare the first two components (name and formula),
     * and ignore later components (which is currently just accession numbers).
     */
    @Override
    public boolean textIdMatches(String textId)
    {
        if (!textId.startsWith("#"))
        {
            // Comment from Nick's commit (bdb9f707) in the Skyline repo
            // "Older .skyd files used just the name of the molecule as the TextId.
            // We can't rely on the formatversion in the .skyd, because of the way that .skyd files can get merged."
            return textId.equals(getCustomIonName());
        }
        // The separator is whatever appears between the first two "#". Usually it's "$", but could be
        // followed by any number of underscores.
        int ichSeparatorEnd = textId.indexOf('#', 1);
        if (ichSeparatorEnd < 0)
        {
            return false;
        }
        String separator = textId.substring(1, ichSeparatorEnd);

        String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(
                textId.substring(ichSeparatorEnd + 1), separator);
        if (parts.length > 0)
        {
            String name = getCustomIonName();
            if (name == null)
            {
                name = "";
            }
            if (!name.equals(parts[0]))
            {
                return false;
            }
        }
        if (parts.length > 1)
        {
            String formula = getIonFormula();
            if (!StringUtils.isEmpty(formula))
            {
                if (!formula.equals(parts[1]))
                {
                    return false;
                }
            }
            else
            {
                String expectedText = String.format("%.9f/%.9f", getMassMonoisotopic(), getMassAverage());
                if (!expectedText.equals(parts[1]))
                {
                    return false;
                }
            }
        }
        return true;

    }

    public String getName()
    {
        return CustomIon.getName(this);
    }
}
