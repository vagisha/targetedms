/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ChartColors;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 7:33 PM
 */
public class ModifiedPeptideHtmlMaker
{
    // RunId -> IsotopeLabelId (The database ID of the first isotope label type for the run).
    // Used to get the display color for label types.
    private Map<Integer, Integer> _firstIsotopeLabelIdInDocMap;

    private final static String[] HEX_PADDING = new String[] {
                                                        "",
                                                        "0",
                                                        "00",
                                                        "000",
                                                        "0000",
                                                        "00000",
                                                        "000000"
    };

    public ModifiedPeptideHtmlMaker()
    {
        _firstIsotopeLabelIdInDocMap = new HashMap<>();
    }

    public String getPrecursorHtml(Precursor precursor, Integer runId, TargetedMSSchema schema)
    {
        Peptide peptide = PeptideManager.getPeptide(schema.getContainer(), precursor.getGeneralMoleculeId());
        return getPrecursorHtml(peptide, precursor, runId);
    }

    public String getPrecursorHtml(Peptide peptide, Precursor precursor, Integer runId)
    {
        return getPrecursorHtml(peptide.getId(), precursor.getIsotopeLabelId(), peptide.getSequence(), precursor.getModifiedSequence(), runId);
    }

    public String getPrecursorHtml(int peptideId, int isotopeLabelId, String peptideSequence, String precursorModifiedSequence, Integer runId)
    {
        return getHtml(peptideId, isotopeLabelId, peptideSequence, precursorModifiedSequence, runId);
    }

    public String getPeptideHtml(Peptide peptide, Integer runId)
    {
        return getPeptideHtml(peptide.getId(), peptide.getSequence(), peptide.getPeptideModifiedSequence(), runId);
    }

    public String getPeptideHtml(int peptideId, String sequence, String peptideModifiedSequence, Integer runId)
    {
        String altSequence = peptideModifiedSequence;
        if(StringUtils.isBlank(altSequence))
        {
            altSequence = sequence;
        }

        return getHtml(peptideId, null, sequence, altSequence, runId);
    }

    private String getHtml(int peptideId, @Nullable Integer isotopeLabelId, String sequence, String altSequence, Integer runId)
    {
        Integer firstIsotopeLabelIdInDoc = null;
        if(runId != null)
        {
            firstIsotopeLabelIdInDoc = _firstIsotopeLabelIdInDocMap.get(runId);
        }
        if (firstIsotopeLabelIdInDoc == null)
        {
            firstIsotopeLabelIdInDoc = IsotopeLabelManager.getLightIsotopeLabelId(peptideId);
            if(runId != null)
            {
                _firstIsotopeLabelIdInDocMap.put(runId, firstIsotopeLabelIdInDoc);
            }
        }

        Set<Integer> strModIndices = ModificationManager.getStructuralModIndexes(peptideId, runId);
        Set<Integer> isotopeModIndices = null;
        if(isotopeLabelId != null)
        {
            isotopeModIndices = ModificationManager.getIsotopeModIndexes(peptideId, isotopeLabelId, runId);
        }


        StringBuilder result = new StringBuilder();

        result.append("<span title='").append(altSequence).append("'>");
        String labelModColor = "black";
        StringBuilder error = new StringBuilder("");
        if(firstIsotopeLabelIdInDoc != null && isotopeLabelId != null)
        {
            if(isotopeLabelId >= firstIsotopeLabelIdInDoc)
            {
                labelModColor = toHex(ChartColors.getIsotopeColor(isotopeLabelId - firstIsotopeLabelIdInDoc).getRGB());
            }
            else
            {
                error.append("Error getting color for isotope label.");
            }
        }

        for(int i = 0; i < sequence.length(); i++)
        {
            boolean isStrModified = strModIndices == null ? false : strModIndices.contains(i);
            boolean isIsotopeModified = isotopeModIndices == null ? false : isotopeModIndices.contains(i);


            if(isIsotopeModified || isStrModified)
            {
                StringBuilder style = new StringBuilder("style='font-weight:bold;");
                if(isIsotopeModified)
                {
                    style.append("color:").append(labelModColor).append(";");
                }
                if(isStrModified)
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
        if(error.length() > 0)
        {
            result.append("<div style='color:red;'>" + error.toString() + "</div>");
        }
        return result.toString();
    }

    public String toHex(int rgb)
    {
        String hex = Integer.toHexString(rgb & 0x00ffffff);
        return "#"+ HEX_PADDING[6 - hex.length()] + hex;
    }
}
