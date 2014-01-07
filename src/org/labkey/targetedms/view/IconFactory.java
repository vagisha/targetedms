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
