/*
 * Copyright (c) 2014 LabKey Corporation
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

    public static String getPeptideIconPath(int peptideId)
    {
        String iconPath = AppProps.getInstance().getContextPath() + "/TargetedMS/images/Peptide.gif";

        if(PeptideManager.hasSpectrumLibraryInformation(peptideId))
        {
            iconPath =  AppProps.getInstance().getContextPath() + "/TargetedMS/images/PeptideLib.gif";
        }

        return iconPath;
    }

    public static String getPrecursorIconPath(int precursorId)
    {
        String iconPath = AppProps.getInstance().getContextPath() + "/TargetedMS/images/blank.gif";

        boolean hasLibSpectrum = PrecursorManager.hasSpectrumLibraryInformation(precursorId);
        boolean hasChromatograms = PrecursorManager.hasChromatogramInformation(precursorId);
        if(hasLibSpectrum && hasChromatograms)
        {
            iconPath =  AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroupLib.gif";
        }
        else if(hasChromatograms)
        {
            iconPath =  AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroup.gif";
        }
        else if(hasLibSpectrum)
        {
            iconPath =  AppProps.getInstance().getContextPath() + "/TargetedMS/images/spectrum.gif";
        }

        return iconPath;
    }
}
