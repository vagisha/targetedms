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
    private Map<Integer, Peptide> _peptides = new HashMap<Integer, Peptide>();
    /** PeptideId -> (Index->MassDiff) */
    private Map<Integer, Map<Integer, Double>> _structuralMods = new HashMap<Integer, Map<Integer, Double>>();
    /** PeptideId/IsotopeLabelId -> (Index->MassDiff) */
    private Map<Pair<Integer, Integer>, Map<Integer, Double>> _isotopeMods = new HashMap<Pair<Integer, Integer>, Map<Integer, Double>>();
    /** PeptideId -> LightIsotopeLabelId */
    private Map<Integer, Integer> _lightLabelIds = new HashMap<Integer, Integer>();

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
        Map<Integer, Double> strModIndexMassDiff;
        Map<Integer, Double> isotopeModIndexMassDiff;

        // Get the structural modifications for this peptide
        strModIndexMassDiff = _structuralMods.get(precursor.getPeptideId());
        if (strModIndexMassDiff == null)
        {
            strModIndexMassDiff = ModificationManager.getPeptideStructuralModsMap(precursor.getPeptideId());
            _structuralMods.put(precursor.getPeptideId(), strModIndexMassDiff);
        }

        Pair<Integer, Integer> isotopeKey = new Pair<Integer, Integer>(precursor.getPeptideId(), precursor.getIsotopeLabelId());
        isotopeModIndexMassDiff = _isotopeMods.get(isotopeKey);
        if (isotopeModIndexMassDiff == null)
        {
            // Get the Isotope modifications for the peptide and label type
            isotopeModIndexMassDiff = ModificationManager.getPeptideIsotopeModsMap(precursor.getPeptideId(),
                    precursor.getIsotopeLabelId());
            _isotopeMods.put(isotopeKey, isotopeModIndexMassDiff);
        }

        StringBuilder result = new StringBuilder();

        result.append("<span title='").append(precursor.getModifiedSequence()).append("'>");
        String labelModColor = "black";
        if(lightLabelId != null)
        {
            labelModColor = toHex(ChartColors.getIsotopeColor(precursor.getIsotopeLabelId() - lightLabelId).getRGB());
        }

        String sequence = peptide.getSequence();
        for(int i = 0; i < sequence.length(); i++)
        {
            Double strMassDiff = strModIndexMassDiff.get(i);
            Double isotopeMassDiff = isotopeModIndexMassDiff.get(i);
            if(strMassDiff != null && isotopeMassDiff != null)
            {
                throw new IllegalStateException("Found both structural and isotope modifications for index "+i+" of peptide "+sequence);
            }

            if(isotopeMassDiff != null)
            {
                result.append("<span style='font-weight:bold;color:").append(labelModColor).append(";'>").append(sequence.charAt(i)).append("</span>");
            }
            else if(strMassDiff != null)
            {
                result.append("<span style='font-weight:bold;text-decoration:underline;'>").append(sequence.charAt(i)).append("</span>");
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
