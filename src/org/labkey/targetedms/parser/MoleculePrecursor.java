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

import org.labkey.api.util.Formats;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.targetedms.chart.LabelFactory;

/**
 * User: binalpatel
 * Date: 2/23/2016
 */
public class MoleculePrecursor extends GeneralPrecursor<MoleculeTransition>
{
    private String _ionFormula;
    private String _customIonName;
    private Double _massMonoisotopic;
    private Double _massAverage;

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

    public String getAdduct()
    {
        return extractAdduct(_ionFormula);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean hasCustomName = getCustomIonName() != null;
        if (hasCustomName)
        {
            sb.append(getCustomIonName());
        }
        if (getIonFormula() != null)
        {
            if (hasCustomName)
            {
                sb.append(" - ");
            }
            sb.append(getIonFormula());
        }
        return sb.toString();
    }

    public HtmlString getHtml()
    {
        HtmlStringBuilder hsb = HtmlStringBuilder.of(toString());
        hsb.append(" - ");
        hsb.append(Formats.f4.format(getMz()));
        hsb.append(LabelFactory.getChargeLabel(getCharge()));
        return hsb.getHtmlString();
    }
}
