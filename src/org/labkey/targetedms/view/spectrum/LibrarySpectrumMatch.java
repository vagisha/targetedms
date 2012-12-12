/*
 * Copyright (c) 2012 LabKey Corporation
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
    Map<Integer, Double> _isotopeModLocationMassMap;

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

    public void setIsotopeModifications(Map<Integer, Double> isotopeModLocationMassMap)
    {
        _isotopeModLocationMassMap = isotopeModLocationMassMap;
    }

    public String getStructuralModifications()
    {
        if(getPeptide() == null || (_modLocationMassMap == null && _isotopeModLocationMassMap == null))
            return "[]";

        // Example: [{index: 8, modMass: 42.0, aminoAcid: 'K'}]
        StringBuilder mods = new StringBuilder();
        mods.append("[");

        // Return all modifications (structural and isotopic) in the same set so that the modified residues
        // show up as highlighed in the spectrum viewer.
        mods.append(appendModifications(_modLocationMassMap));
        mods.append(appendModifications(_isotopeModLocationMassMap));

        mods.append("\n]\n");
        return mods.toString();
    }

    private String appendModifications(Map<Integer, Double> modLocationMassMap)
    {
        if(modLocationMassMap == null)
            return "";

        StringBuilder mods = new StringBuilder();
        for(Integer location: modLocationMassMap.keySet())
        {
            mods.append("\n");
            mods.append("{index: ")
                .append(location + 1) // Lorikeet uses a 1-based index
                .append(", modMass: ")
                .append(modLocationMassMap.get(location))
                .append(", aminoAcid: '")
                .append(_peptide.charAt(location))
                .append("'}");
            mods.append(",");
        }
        return mods.toString();
    }
}
