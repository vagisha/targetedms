/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.chart.ChartColors;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.HashMap;
import java.util.Map;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 7:33 PM
 */
public class ModifiedPeptideHtmlMaker
{
    /** PeptideId -> Peptide */
    private Map<Integer, Peptide> _peptides = new HashMap<>();
    /** PeptideId -> (Index->MassDiff) */
    private Map<Integer, Map<Integer, Double>> _structuralMods = new HashMap<>();
    /** PeptideId/IsotopeLabelId -> (Index->MassDiff) */
    private Map<Pair<Integer, Integer>, Map<Integer, Double>> _isotopeMods = new HashMap<>();
    /** PeptideId -> LightIsotopeLabelId */
    private Map<Integer, Integer> _lightLabelIds = new HashMap<>();

    private final static String[] HEX_PADDING = new String[] {
                                                        "",
                                                        "0",
                                                        "00",
                                                        "000",
                                                        "0000",
                                                        "00000",
                                                        "000000"
    };

    public ModifiedPeptideHtmlMaker() {}

    public String getHtml(Precursor precursor)
    {
        // Get the peptide

        Peptide peptide = _peptides.get(precursor.getPeptideId());
        if (peptide == null)
        {
            peptide = PeptideManager.get(precursor.getPeptideId());
            _peptides.put(precursor.getPeptideId(), peptide);
        }

        Integer lightIsotopeLabelId = _lightLabelIds.get(peptide.getId());
        if (lightIsotopeLabelId == null)
        {
            lightIsotopeLabelId = IsotopeLabelManager.getLightIsotopeLabelId(peptide.getId());
            _lightLabelIds.put(peptide.getId(), lightIsotopeLabelId);
        }

        return getHtml(peptide, precursor, lightIsotopeLabelId);
    }

    public String getHtml(Peptide peptide, Precursor precursor, Integer lightLabelId)
    {
        return getHtml(peptide.getId(), peptide.getSequence(), precursor.getModifiedSequence(), precursor.getIsotopeLabelId(), lightLabelId);
    }

    public String getHtml(Peptide peptide)
    {
        String altSequence = peptide.getPeptideModifiedSequence();
        if(StringUtils.isBlank(altSequence))
        {
            altSequence = peptide.getSequence();
        }

        return getHtml(peptide.getId(), peptide.getSequence(), altSequence, null, null);
    }

    private String getHtml(Integer peptideId, String sequence, String altSequence, @Nullable Integer isotopteLabelId,  @Nullable Integer lightLabelId)
    {
        Map<Integer, Double> strModIndexMassDiff;
        Map<Integer, Double> isotopeModIndexMassDiff;

        // Get the structural modifications for this peptide
        strModIndexMassDiff = _structuralMods.get(peptideId);
        if (strModIndexMassDiff == null)
        {
            strModIndexMassDiff = ModificationManager.getPeptideStructuralModsMap(peptideId);
            _structuralMods.put(peptideId, strModIndexMassDiff);
        }

        if(isotopteLabelId != null)
        {
            Pair<Integer, Integer> isotopeKey = new Pair<>(peptideId, isotopteLabelId);
            isotopeModIndexMassDiff = _isotopeMods.get(isotopeKey);
            if (isotopeModIndexMassDiff == null)
            {
                // Get the Isotope modifications for the peptide and label type
                isotopeModIndexMassDiff = ModificationManager.getPeptideIsotopeModsMap(peptideId,
                        isotopteLabelId);
                _isotopeMods.put(isotopeKey, isotopeModIndexMassDiff);
            }
        }
        else
        {
            isotopeModIndexMassDiff = new HashMap<>(0);
        }


        StringBuilder result = new StringBuilder();

        result.append("<span title='").append(altSequence).append("'>");
        String labelModColor = "black";
        if(lightLabelId != null && isotopteLabelId != null)
        {
            labelModColor = toHex(ChartColors.getIsotopeColor(isotopteLabelId - lightLabelId).getRGB());
        }

        for(int i = 0; i < sequence.length(); i++)
        {
            Double strMassDiff = strModIndexMassDiff.get(i);
            Double isotopeMassDiff = isotopeModIndexMassDiff.get(i);

            if(isotopeMassDiff != null || strMassDiff != null)
            {
                StringBuilder style = new StringBuilder("style='font-weight:bold;");
                if(isotopeMassDiff != null)
                {
                    style.append("color:").append(labelModColor).append(";");
                }
                if(strMassDiff != null)
                {
                    style.append("text-decoration:underline;");
                }
                style.append("'");
                result.append("<span ").append(style).append(">").append(sequence.charAt(i)).append("</span>");
            }
            else
            {
                result.append(sequence.charAt(i));
            }
        }
        result.append("</span>");
        return result.toString();
    }

    public String toHex(int rgb)
    {
        String hex = Integer.toHexString(rgb & 0x00ffffff);
        return "#"+ HEX_PADDING[6 - hex.length()] + hex;
    }
}
