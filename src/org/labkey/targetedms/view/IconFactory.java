/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.labkey.api.settings.AppProps;
import org.labkey.targetedms.parser.StandardType;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;

/**
 * User: vsharma
 * Date: 1/7/14
 * Time: 10:48 AM
 */
public class IconFactory
{
    private IconFactory(){}

    public static String getPeptideIconPath(int peptideId, Integer runId, boolean isDecoy, String standardType)
    {
        boolean hasLibInfo = PeptideManager.hasSpectrumLibraryInformation(peptideId, runId);

        String iconFile = "/TargetedMS/images/Peptide.png";
        StandardType standardTypeEnum = StandardType.parse(standardType);

        if(hasLibInfo)
        {
            if (standardTypeEnum == null)
            {
                iconFile = isDecoy ? "/TargetedMS/images/PeptideDecoyLib.png" : "/TargetedMS/images/PeptideLib.png";
            }
            else
            {
                switch (standardTypeEnum)
                {
                    case Normalization:
                    case SurrogateStandard:
                        iconFile = "/TargetedMS/images/PeptideStandardLib.png";
                        break;
                    case QC:
                        iconFile = "/TargetedMS/images/PeptideQcLib.png";
                        break;
                    case iRT:
                        iconFile = "/TargetedMS/images/PeptideIrtLib.png";
                        break;
                }
            }
        }
        else
        {
            if(standardTypeEnum == null)
            {
                iconFile =  isDecoy ? "/TargetedMS/images/PeptideDecoy.png" : "/TargetedMS/images/Peptide.png";
            }
            else
            {
                switch (standardTypeEnum)
                {
                    case Normalization:
                    case SurrogateStandard:
                        iconFile = "/TargetedMS/images/PeptideStandard.png";
                        break;
                    case QC:
                        iconFile = "/TargetedMS/images/PeptideQc.png";
                        break;
                    case iRT:
                        iconFile = "/TargetedMS/images/PeptideIrt.png";
                        break;
                }
            }
        }

        return AppProps.getInstance().getContextPath() + iconFile;
    }

    public static String getPrecursorIconPath(int precursorId, boolean isDecoy)
    {
        return getPrecursorIconPath(precursorId, null, isDecoy);
    }

    public static String getPrecursorIconPath(int precursorId, Integer runId, boolean isDecoy)
    {
        String iconPath;

        boolean hasLibSpectrum = PrecursorManager.hasLibrarySpectra(precursorId, runId);
        boolean hasChromatograms = PrecursorManager.hasChromatograms(precursorId, runId);

        if(hasLibSpectrum)
        {
            iconPath = hasChromatograms ? (isDecoy ? "/TargetedMS/images/TransitionGroupLibDecoy.png"
                                                   : "/TargetedMS/images/TransitionGroupLib.png")
                                        : "/TargetedMS/images/spectrum.gif";
        }
        else
        {
            iconPath = hasChromatograms ? (isDecoy ? "/TargetedMS/images/TransitionGroupDecoy.png"
                                                   : "/TargetedMS/images/TransitionGroup.png")
                                        : "/TargetedMS/images/blank.gif"; // no chromatogram AND no spectrum
        }

        return AppProps.getInstance().getContextPath() + iconPath;
    }

    public static String getTransitionGroupIconPath()
    {
        return AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroup.png";
    }

    public static String getMoleculeIconPath()
    {
       return AppProps.getInstance().getContextPath() + "/TargetedMS/images/Molecule.png";
    }
}
