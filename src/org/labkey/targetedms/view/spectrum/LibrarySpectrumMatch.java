package org.labkey.targetedms.view.spectrum;

import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.blib.BlibSpectrum;

import java.util.Map;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 11:43 AM
 */
public class LibrarySpectrumMatch
{
    private BlibSpectrum _spectrum;
    private String _peptide;
    private String _modifiedSequence;
    private int _charge;
    private PeptideSettings.SpectrumLibrary _library;
    private String _lorikeetId;
    Map<Integer, Double> _modLocationMassMap;

    public String getPeptide()
    {
        return _peptide;
    }

    public void setPeptide(String peptide)
    {
        _peptide = peptide;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public BlibSpectrum getSpectrum()
    {
        return _spectrum;
    }

    public void setSpectrum(BlibSpectrum spectrum)
    {
        _spectrum = spectrum;
    }

    public PeptideSettings.SpectrumLibrary getLibrary()
    {
        return _library;
    }

    public void setLibrary(PeptideSettings.SpectrumLibrary library)
    {
        _library = library;
    }

    public String getLorikeetId()
    {
        return _lorikeetId;
    }

    public void setLorikeetId(int index)
    {
        _lorikeetId = "lorikeet_"+index;
    }

    public String getPeaks()
    {
        if(getSpectrum() == null)
            return "[]";

        double[] mzs = getSpectrum().getMz();
        float[] intensities = getSpectrum().getIntensity();

        StringBuilder peaks = new StringBuilder();
        peaks.append("[\n");
        boolean firstPeak = true;
        for (int i = 0; i < mzs.length; i++)
        {
            if(!firstPeak)
                peaks.append(",\n");
            peaks.append("[").append(mzs[i]).append(",").append(intensities[i]).append("]");
            firstPeak = false;
        }
        peaks.append("\n]\n");

        return peaks.toString();
    }

    public void setStructuralModifications(Map<Integer, Double> modLocationMassMap)
    {
        _modLocationMassMap = modLocationMassMap;
    }

    public String getStructuralModifications()
    {
        if(getPeptide() == null || _modLocationMassMap == null)
            return "[]";

        // Example: [{index: 8, modMass: 42.0, aminoAcid: 'K'}]
        StringBuilder mods = new StringBuilder();
        mods.append("[\n");
        boolean first = true;
        for(Integer location: _modLocationMassMap.keySet())
        {
            if(!first)
                mods.append("\n,");

            char aa = _peptide.charAt(location - 1);
            mods.append("{index: ")
                .append(location + 1) // Lorikeet uses a 1-based index
                .append(", modMass: ")
                .append(_modLocationMassMap.get(location))
                .append(", aminoAcid: '")
                .append(_peptide.charAt(location))
                .append("'}");
            first = false;
        }
        mods.append("\n]\n");
        return mods.toString();
    }
}
