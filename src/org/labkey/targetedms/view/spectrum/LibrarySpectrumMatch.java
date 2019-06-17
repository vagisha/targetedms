/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.blib.BlibSpectrum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 11:43 AM
 */
public class LibrarySpectrumMatch
{
    private int _precursorId;
    private BlibSpectrum _spectrum;
    private String _peptide;
    private String _modifiedSequence;
    private int _charge;
    private String _isotopeLabel;
    private PeptideSettings.SpectrumLibrary _library;
    private List<PeptideSettings.SpectrumLibrary> _libraries; // All libraries that have a match
    private String _lorikeetId;
    List<Peptide.StructuralModification> _structuralModifications;
    private Set<Integer> _variableStructuralMods = new HashSet<>();

    PeptideSettings.RunStructuralModification _ntermMod = null;
    PeptideSettings.RunStructuralModification _ctermMod = null;
    Map<Integer, List<PeptideSettings.PotentialLoss>> _potentialLossIdMap;
    List<Peptide.IsotopeModification> _isotopeModifications;
    private int _maxNeutralLosses;

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

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

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
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

    public List<PeptideSettings.SpectrumLibrary> getLibraries()
    {
        return _libraries;
    }

    public void addLibrary(PeptideSettings.SpectrumLibrary library)
    {
        if(_libraries == null)
        {
            _libraries = new ArrayList<>();
        }
        _libraries.add(library);
    }

    public String getLorikeetId()
    {
        return _lorikeetId;
    }

    public void setLorikeetId(int index)
    {
        _lorikeetId = "lorikeet_"+index;
    }

    public int getMaxNeutralLosses()
    {
        return _maxNeutralLosses;
    }

    public void setMaxNeutralLosses(int maxNeutralLosses)
    {
        _maxNeutralLosses = maxNeutralLosses;
    }

    public boolean hasRedundantSpectra()
    {
        return _spectrum != null && _spectrum.getRedundantSpectrumList().size() > 0;
    }

    public String getPeaks()
    {
        if(getSpectrum() == null)
            return "[]";

        List<BlibSpectrum.Peak> peakList = getSpectrum().getPeaks();

        StringBuilder peaks = new StringBuilder();
        peaks.append("[\n");
        boolean firstPeak = true;
        for (BlibSpectrum.Peak peak: peakList)
        {
            if(!firstPeak)
                peaks.append(",");
            peaks.append("[").append(peak.getMz()).append(",").append(peak.getIntensity()).append("]");
            firstPeak = false;
        }
        peaks.append("]");

        return peaks.toString();
    }

    public void setStructuralModifications(List<Peptide.StructuralModification> structuralModifications,
                                           List<PeptideSettings.RunStructuralModification> runMods)
    {
        _structuralModifications = structuralModifications;
        if(runMods != null)
        {
            for(PeptideSettings.RunStructuralModification mod: runMods)
            {
                if(mod.getAminoAcid() == null)
                {
                    if(mod.getTerminus() != null && mod.getTerminus().equalsIgnoreCase("N"))
                    {
                        _ntermMod = mod;
                    }
                    else if(mod.getTerminus() != null && mod.getTerminus().equalsIgnoreCase("C"))
                    {
                        _ctermMod =  mod;
                    }
                }
                if(mod.isVariable() || mod.isModExplicit())
                {
                    _variableStructuralMods.add(mod.getStructuralModId());
                }
            }
        }
    }

    public void setPotentialLosses(Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossIdMap)
    {
        _potentialLossIdMap = potentialLossIdMap;
    }

    public void setIsotopeModifications(List<Peptide.IsotopeModification> isotopeModifications)
    {
        _isotopeModifications = isotopeModifications;
    }

    public String getStructuralModifications()
    {
        if(getPeptide() == null || isEmpty(_structuralModifications))
            return "[]";

        // Example: [{modMass: 42.0, aminoAcid: 'K'}]
        StringBuilder mods = new StringBuilder();
        mods.append("[");

        // Return all static (not variable) structural modifications.
        mods.append(appendStructuralModifications(_structuralModifications, false)); // only static mods

        mods.append("]");
        return mods.toString();
    }

    public String getVariableModifications()
    {
        if(getPeptide() == null || (isEmpty(_structuralModifications) && isEmpty(_isotopeModifications)))
            return "[]";

        // Example: [{index: 8, modMass: 42.0, aminoAcid: 'K'}]
        StringBuilder mods = new StringBuilder();
        mods.append("[");

        // Return all isotopic and variable structural modifications.
        mods.append(appendStructuralModifications(_structuralModifications, true)); // only variable mods
        String isotopeMods = appendIsotopeModifications(_isotopeModifications);
        if(mods.length() > 1 && isotopeMods.length() > 1)
        {
           mods.append(",");
        }
        mods.append(isotopeMods);

        mods.append("]");
        return mods.toString();
    }

    public String getNtermModMass()
    {
        if(_ntermMod == null || _structuralModifications == null || _structuralModifications.isEmpty()) return "0";

        for(Peptide.StructuralModification mod: _structuralModifications)
        {
            if (mod.getStructuralModId() == _ntermMod.getStructuralModId())
            {
                return String.valueOf(mod.getMassDiff());
            }
        }
        return "0";
    }

    public String getCtermModMass()
    {
        if(_ctermMod == null || _structuralModifications == null || _structuralModifications.isEmpty()) return "0";

        for(Peptide.StructuralModification mod: _structuralModifications)
        {
            if (mod.getStructuralModId() == _ctermMod.getStructuralModId())
            {
                return String.valueOf(mod.getMassDiff());
            }
        }
        return "0";
    }

    private boolean isEmpty(List<?> list)
    {
        return (list == null || list.isEmpty());
    }

    private String appendIsotopeModifications(List<? extends Peptide.Modification> modifications)
    {
        if(modifications == null || modifications.isEmpty())
            return "";

        StringBuilder mods = new StringBuilder();
        String comma = "";
        for(Peptide.Modification mod: modifications)
        {
            mods.append(comma);
            comma = ",";
            mods.append("{\"index\": ")
                .append(mod.getIndexAa() + 1) // Lorikeet uses a 1-based index
                .append(", \"modMass\": ")
                .append(mod.getMassDiff())
                .append(", \"aminoAcid\": \"")
                .append(_peptide.charAt(mod.getIndexAa())).append("\"")
                .append(getNeutralLosses(mod))
                .append("}");
        }
        return mods.toString();
    }

    private String appendStructuralModifications(List<Peptide.StructuralModification> modifications,
                                             boolean variable)
    {
        if(modifications == null || modifications.isEmpty())
            return "";

        StringBuilder mods = new StringBuilder();
        String comma = "";
        for(Peptide.StructuralModification mod: modifications)
        {
            if(_ntermMod != null && (mod.getStructuralModId() == _ntermMod.getStructuralModId()))
            {
                continue; // This is a n-term modification
            }
            if(_ctermMod != null && (mod.getStructuralModId() == _ctermMod.getStructuralModId()))
            {
                continue; // This is a c-term modification
            }
            if(variable && !_variableStructuralMods.contains(mod.getStructuralModId()))
            {
                // This is not a variable modification
                continue;
            }
            if(!variable && _variableStructuralMods.contains(mod.getStructuralModId()))
            {
                // This is not a static modification
                continue;
            }
            mods.append(comma);
            comma = ",";
            mods.append("{");
            if(variable)
            {
                mods.append("\"index\": ")
                    .append(mod.getIndexAa() + 1) // Lorikeet uses a 1-based index
                    .append(", ");
            }

            mods.append("\"modMass\": ")
                    .append(mod.getMassDiff())
                    .append(", \"aminoAcid\": \"")
                    .append(_peptide.charAt(mod.getIndexAa())).append("\"")
                    .append(getNeutralLosses(mod))
                    .append("}");
        }
        return mods.toString();
    }

    private String getNeutralLosses(Peptide.Modification modification)
    {
        if(!(modification instanceof Peptide.StructuralModification))
        {
            return "";
        }
        int structuralModId = ((Peptide.StructuralModification)modification).getStructuralModId();
        List<PeptideSettings.PotentialLoss> losses = _potentialLossIdMap.get(structuralModId);
        if(losses == null || losses.isEmpty())
        {
            return "";
        }

        StringBuilder lossString = new StringBuilder();
        lossString.append(", \"losses\": [");
        int index = 0;
        for(PeptideSettings.PotentialLoss loss: losses)
        {
            Double massDiffMono = loss.getMassDiffMono();
            Double massDiffAvg = loss.getMassDiffAvg();
            if(massDiffMono == null || massDiffAvg == null)
            {
                double[] masses = LossMassCalculator.getMass(loss.getFormula());
                massDiffMono = masses[0];
                massDiffAvg = masses[1];
            }

            if(massDiffAvg == 0.0 || massDiffMono == 0.0)
            {
                continue;
            }
            if(index++ > 0)
                lossString.append(", ");
            lossString.append("{");
            lossString.append("\"monoLossMass\": ").append(massDiffMono)
                      .append(", \"avgLossMass\": ").append(massDiffAvg);
            if(loss.getFormula() != null)
            {
                lossString.append(", \"formula\": \"").append(loss.getFormula()).append("\"");
            }
            lossString.append("}");
        }
        lossString.append("] ");
        return lossString.toString();
    }

    // 08/30/13
    // Utility class to calculate the mono and avg masses for a loss based on the formula.
    // Older versions of Skyline (pre 1.5) did not write out masses for a loss in the Skyline
    // document if the formula was available.
    // atom -> double[] {mono_mass, avg_mass}
    private static final class LossMassCalculator
    {
        private static final Pattern formulaPattern = Pattern.compile("([A-Z]'*)([0-9]*)");
        public static double[] getMass(String formula)
        {
            if(formula == null || formula.trim().length() == 0)
                return new double[] {0.0, 0.0};

            double monoMass = 0.0;
            double avgMass = 0.0;

            boolean error = false;
            Matcher matcher = formulaPattern.matcher(formula);
            while(matcher.find())
            {
                int groupCount = matcher.groupCount();
                if(groupCount != 2)
                {
                    // Error in parsing, we will not calculate the mass for this formula.
                    error = true;
                    break;

                }
                String atom = matcher.group(1);
                int count = 0;
                try
                {
                    String num = matcher.group(2);
                    if(num.length() > 0)
                    {
                        count = Integer.parseInt(matcher.group(2));
                    }
                    else
                    {
                        count = 1;
                    }

                }
                catch(NumberFormatException e)
                {
                    // Error in parsing, we will not calculate the mass for this formula.
                    error = true;
                    break;
                }

                double[] masses = atomicMasses.get(atom.toUpperCase());
                if(masses == null)
                {
                    error = true;
                    break;
                }
                monoMass += masses[0] * count;
                avgMass += masses[1] * count;
            }
            if(error)
            {
                return new double[] {0.0, 0.0};
            }
            return new double[] {monoMass, avgMass};
        }

        private static Map<String, double[]> atomicMasses = new HashMap<String, double[]>();
        static
        {
            atomicMasses.put("H", new double[]{ 1.007825035, 1.00794}); //Unimod
            atomicMasses.put("H'", new double[]{ 2.014101779, 2.014101779}); // H2 //Unimod
            atomicMasses.put("O", new double[]{ 15.99491463, 15.9994}); //Unimod
            atomicMasses.put("O''", new double[]{ 16.9991315, 16.9991315}); // O17 (not used in Skyline?) //NIST
            atomicMasses.put("O'", new double[]{ 17.9991604, 17.9991604}); // O18 //NIST, Unimod=17.9991603
            atomicMasses.put("N", new double[]{ 14.003074, 14.0067}); //Unimod
            atomicMasses.put("N'", new double[]{ 15.0001088984, 15.0001088984}); // N15 //NIST, Unimod=15.00010897
            atomicMasses.put("C", new double[]{ 12.0, 12.01085}); //MacCoss average
            atomicMasses.put("C'", new double[]{ 13.0033548378, 13.0033548378}); // C13 //NIST, Unimod=13.00335483
            atomicMasses.put("S", new double[]{ 31.9720707, 32.065}); //Unimod
            atomicMasses.put("P", new double[]{ 30.973762, 30.973761}); //Unimod

            atomicMasses.put("Se", new double[]{ 79.9165196, 78.96}); //Unimod Most abundant Se isotope is 80
            atomicMasses.put("Li", new double[]{ 7.016003, 6.941}); //Unimod
            atomicMasses.put("F", new double[]{ 18.99840322,18.9984032}); //Unimod
            atomicMasses.put("Na", new double[]{ 22.9897677, 22.98977}); //Unimod
            atomicMasses.put("S", new double[]{ 31.9720707, 32.065}); //Unimod
            atomicMasses.put("Cl", new double[]{ 34.96885272, 35.453}); //Unimod
            atomicMasses.put("K", new double[]{ 38.9637074, 39.0983}); //Unimod
            atomicMasses.put("Ca", new double[]{ 39.9625906, 40.078}); //Unimod
            atomicMasses.put("Fe", new double[]{ 55.9349393, 55.845}); //Unimod
            atomicMasses.put("Ni", new double[]{ 57.9353462, 58.6934}); //Unimod
            atomicMasses.put("Cu", new double[]{ 62.9295989, 63.546}); //Unimod
            atomicMasses.put("Zn", new double[]{ 63.9291448, 65.409}); //Unimod
            atomicMasses.put("Br", new double[]{ 78.9183361, 79.904}); //Unimod
            atomicMasses.put("Mo", new double[]{ 97.9054073, 95.94}); //Unimod
            atomicMasses.put("Ag", new double[]{ 106.905092, 107.8682}); //Unimod
            atomicMasses.put("I", new double[]{ 126.904473, 126.90447}); //Unimod
            atomicMasses.put("Au", new double[]{ 196.966543, 196.96655}); //Unimod
            atomicMasses.put("Hg", new double[]{ 201.970617, 200.59}); //Unimod
            atomicMasses.put("B", new double[]{ 11.0093055, 10.811});
            atomicMasses.put("As", new double[]{ 74.9215942, 74.9215942});
            atomicMasses.put("Cd", new double[]{ 113.903357, 112.411});
            atomicMasses.put("Cr", new double[]{ 51.9405098, 51.9961});
            atomicMasses.put("Co", new double[]{ 58.9331976, 58.933195});
            atomicMasses.put("Mn", new double[]{ 54.9380471, 54.938045});
            atomicMasses.put("Mg", new double[]{ 23.9850423, 24.305});
        }
    }

    public String getRedundantSpectraList()
    {
        if(!hasRedundantSpectra())
        {
            return "[]";
        }

        StringBuilder spectraList = new StringBuilder();
        spectraList.append("[");
        int index = 0;
        for(BlibSpectrum.RedundantSpectrum spectrum: getSpectrum().getRedundantSpectrumList())
        {
            if(index++ > 0)
                spectraList.append(", ");
            spectraList.append("{");
            spectraList.append("\"redundantSpectrumId\": ").append(spectrum.getRedundantRefSpectrumId());
            spectraList.append(", ");
            spectraList.append("\"fileName\": \"").append(spectrum.getSourceFileName()).append("\"");
            spectraList.append(", ");
            spectraList.append("\"retentionTime\": ").append(spectrum.getRetentionTimeF2());
            spectraList.append(", ");
            spectraList.append("\"isReference\": ").append(spectrum.isBestSpectrum());
            spectraList.append(", ");
            spectraList.append("\"display\": \"").append(spectrum.getSourceFileName() + " (" + spectrum.getRetentionTimeF2() + ")\"");
            spectraList.append("}");
        }
        spectraList.append("] ");
        return spectraList.toString();
    }
}
