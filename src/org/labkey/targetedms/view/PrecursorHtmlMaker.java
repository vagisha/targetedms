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

import org.labkey.api.util.Formats;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.LabelFactory;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;

/**
 * User: vsharma
 * Date: 5/7/12
 * Time: 8:13 PM
 */
public class PrecursorHtmlMaker
{
    private PrecursorHtmlMaker() {}

    public static String getHtml(Peptide peptide, Precursor precursor, String isotopeLabel, int runId)
    {
        StringBuilder html = new StringBuilder();
        html.append(new ModifiedPeptideHtmlMaker().getPrecursorHtml(peptide, precursor, runId));
        html.append("<span>");
        html.append(" - ").append(Formats.f4.format(precursor.getMz()));
        html.append(LabelFactory.getChargeLabel(precursor.getCharge()));
        if(!PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(isotopeLabel))
        {
            html.append(" (").append(isotopeLabel).append(")");
        }
        html.append("</span>");

        return html.toString();
    }

    public static String getModSeqChargeHtml(ModifiedPeptideHtmlMaker modifiedPeptideHtmlMaker, Precursor precursor,
                                             int runId, TargetedMSSchema schema)
    {
        StringBuilder html = new StringBuilder();
        html.append(modifiedPeptideHtmlMaker.getPrecursorHtml(precursor, runId, schema));
        html.append("<span>");
        html.append(LabelFactory.getChargeLabel(precursor.getCharge()));
        html.append("</span>");

        return html.toString();
    }
}
