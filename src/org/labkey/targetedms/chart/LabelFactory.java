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

package org.labkey.targetedms.chart;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.Formats;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TransitionManager;

import java.util.Map;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 3:40 PM
 */
public class LabelFactory
{
    private static final String[] CHARGE = {"",
                                           "+",
                                           "++",
                                           "+++"};

    private LabelFactory() {}

    public static String transitionLabel(int transitionId, User user, Container container)
    {
        return transitionLabel(TransitionManager.get(transitionId, user, container));
    }

    public static String transitionLabel(Transition transition)
    {
        StringBuilder label = new StringBuilder();
        if(transition.isPrecursorIon())
        {
            label.append("M");
            Integer massIndex = transition.getMassIndex();
            if(massIndex != null && massIndex != 0)
            {
                label.append("+").append(transition.getMassIndex());
            }
        }
        else
        {
            label.append(transition.getFragmentType());
            if(transition.getFragmentOrdinal() != null)
            {
                label.append(transition.getFragmentOrdinal());
            }

            Double neutralLossMass = transition.getNeutralLossMass();
            if(neutralLossMass != null && neutralLossMass > 0.0)
            {
                label.append(" -").append(Formats.f0.format(neutralLossMass));
            }
        }
        label.append(" - ").append(Formats.f4.format(transition.getMz()));
        if(transition.getCharge() != null)
        {
            label.append(getChargeLabel(transition.getCharge()));
        }
        return label.toString();
    }

    public static String precursorLabel(int precursorId, User user, Container container)
    {
        Map<String, Object> precursorSummary = PrecursorManager.getPrecursorSummary(precursorId);

        StringBuilder label = new StringBuilder();
        label.append(precursorSummary.get("sequence"));

        label.append(" - ").append(Formats.f4.format(precursorSummary.get("mz")));
        label.append(getChargeLabel((Integer)precursorSummary.get("charge")));
        String isotopeLabel = (String) precursorSummary.get("label");
        if(!PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(isotopeLabel))
        {
            label.append(" (").append(isotopeLabel).append(")");
        }

        return label.toString();
    }

    public static String precursorChromInfoLabel(PrecursorChromInfo pChromInfo, User user, Container container)
    {
        if(pChromInfo.isOptimizationPeak())
        {
            return "Step " + pChromInfo.getOptimizationStep();
        }
        else
        {
            return precursorLabel(pChromInfo.getPrecursorId(), user, container);
        }
    }

    public static String peptideChromInfoChartTitle(GeneralMoleculeChromInfo pepChromInfo, User user, Container container)
    {
        SampleFile sampleFile = ReplicateManager.getSampleFile(pepChromInfo.getSampleFileId());
        Replicate replicate = ReplicateManager.getReplicate(sampleFile.getReplicateId());
        Peptide peptide = PeptideManager.getPeptide(container, pepChromInfo.getGeneralMoleculeId());

        StringBuilder label = new StringBuilder();
        label.append(replicate.getName());
        if(!sampleFile.getSampleName().contains(replicate.getName()))
        {
            label.append(" (").append(sampleFile.getSampleName()).append(')');
        }
        label.append('\n');
        label.append(peptide.getSequence());
        return label.toString();
    }

    public static String precursorChromInfoChartTitle(PrecursorChromInfo pChromInfo, User user, Container container)
    {
        SampleFile sampleFile = ReplicateManager.getSampleFile(pChromInfo.getSampleFileId());
        Replicate replicate = ReplicateManager.getReplicate(sampleFile.getReplicateId());
        TargetedMSRun run = TargetedMSManager.getRun(replicate.getRunId());
        String precursorLabel = precursorLabel(pChromInfo.getPrecursorId(), user, container);

        StringBuilder label = new StringBuilder();
        label.append(replicate.getName());
        if(!sampleFile.getSampleName().contains(replicate.getName()))
        {
            label.append(" (").append(sampleFile.getSampleName()).append(')');
        }
        if (run != null && sampleFile.getAcquiredTime() != null)
        {
            label.append(", ");
            label.append(DateUtil.formatDateTime(run.getContainer(), sampleFile.getAcquiredTime()));
        }
        label.append('\n');
        label.append(precursorLabel);
        return label.toString();
    }

    public static String getChargeLabel(int charge)
    {
        return getChargeLabel(charge, true);
    }

    public static String getChargeLabel(int charge, boolean addSeparator)
    {
        if(charge < 0)
            return "";
        if(charge > CHARGE.length - 1)
        {
            String plusStr = addSeparator ? ", +" : "+";
            return plusStr + charge;
        }
        return CHARGE[charge];
    }
}
