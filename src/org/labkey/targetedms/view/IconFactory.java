/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

        String iconFile = "/TargetedMS/images/Peptide.bmp";
        StandardType standardTypeEnum = StandardType.parse(standardType);

        if(hasLibInfo)
        {
            if (standardTypeEnum == null)
            {
                iconFile = isDecoy ? "/TargetedMS/images/PeptideDecoyLib.bmp" : "/TargetedMS/images/PeptideLib.bmp";
            }
            else
            {
                switch (standardTypeEnum)
                {
                    case Normalization:
                    case SurrogateStandard:
                        iconFile = "/TargetedMS/images/PeptideStandardLib.bmp";
                        break;
                    case QC:
                        iconFile = "/TargetedMS/images/PeptideQcLib.bmp";
                        break;
                    case iRT:
                        iconFile = "/TargetedMS/images/PeptideIrtLib.bmp";
                        break;
                }
            }
        }
        else
        {
            if(standardTypeEnum == null)
            {
                iconFile =  isDecoy ? "/TargetedMS/images/PeptideDecoy.bmp" : "/TargetedMS/images/Peptide.bmp";
            }
            else
            {
                switch (standardTypeEnum)
                {
                    case Normalization:
                    case SurrogateStandard:
                        iconFile = "/TargetedMS/images/PeptideStandard.bmp";
                        break;
                    case QC:
                        iconFile = "/TargetedMS/images/PeptideQc.bmp";
                        break;
                    case iRT:
                        iconFile = "/TargetedMS/images/PeptideIrt.bmp";
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
            iconPath = hasChromatograms ? (isDecoy ? "/TargetedMS/images/TransitionGroupLibDecoy.bmp"
                                                   : "/TargetedMS/images/TransitionGroupLib.bmp")
                                        : "/TargetedMS/images/spectrum.gif";
        }
        else
        {
            iconPath = hasChromatograms ? (isDecoy ? "/TargetedMS/images/TransitionGroupDecoy.bmp"
                                                   : "/TargetedMS/images/TransitionGroup.bmp")
                                        : "/TargetedMS/images/blank.gif"; // no chromatogram AND no spectrum
        }

        return AppProps.getInstance().getContextPath() + iconPath;
    }

    public static String getTransitionGroupIconPath()
    {
        return AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroup.bmp";
    }

    public static String getMoleculeIconPath()
    {
       return AppProps.getInstance().getContextPath() + "/TargetedMS/images/Molecule.bmp";
    }
}
