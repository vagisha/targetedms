/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.targetedms.chart.ChartColors;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.util.Map;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 7:33 PM
 */
public class ModifiedPeptideHtmlMaker
{
    private final static String[] HEX_PADDING = new String[] {
                                                        "",
                                                        "0",
                                                        "00",
                                                        "000",
                                                        "0000",
                                                        "00000",
                                                        "000000"
    };

    private ModifiedPeptideHtmlMaker() {}

    public static String getHtml(int precursorId)
    {
        // Get the precursor
        Precursor precursor = PrecursorManager.get(precursorId);

        // Get the peptide
        Peptide peptide = PeptideManager.get(precursor.getPeptideId());


        Integer lightIsotopeLabelId = IsotopeLabelManager.getLightIsotopeLabelId(peptide.getId());

        return getHtml(peptide, precursor, lightIsotopeLabelId);
    }

    public static String getHtml(Peptide peptide, Precursor precursor, Integer lightLabelId)
    {
        Map<Integer, Double> strModIndexMassDiff;
        Map<Integer, Double> isotopeModIndexMassDiff;

        // Get the structural modifications for this peptide
        strModIndexMassDiff = ModificationManager.getPeptideStructuralMods(precursor.getPeptideId());

        // Get the Isotope modifications for the peptide and label type
        isotopeModIndexMassDiff = ModificationManager.getPeptideIsotopeMods(precursor.getPeptideId(),
                precursor.getIsotopeLabelId());

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

    public static String toHex(int rgb)
    {
        String hex = Integer.toHexString(rgb & 0x00ffffff);
        return "#"+ HEX_PADDING[6 - hex.length()] + hex;
    }
}
