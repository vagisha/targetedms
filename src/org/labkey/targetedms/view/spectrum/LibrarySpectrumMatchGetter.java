/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.blib.BlibSpectrum;
import org.labkey.targetedms.parser.blib.BlibSpectrumReader;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 12:02 PM
 */
public class LibrarySpectrumMatchGetter
{
    public static List<LibrarySpectrumMatch> getMatches(Peptide peptide)
    {
        // Get the precursor of this peptide, sorted by label type and charge.
        List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptide.getId());

        TargetedMSRun run = TargetedMSManager.getRunForPeptide(peptide.getId());

        // Get the spectrum libraries for this run
        List<PeptideSettings.SpectrumLibrary> libraries = LibraryManager.getLibraries(run.getId());
        Map<PeptideSettings.SpectrumLibrary, String> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId(), libraries);

        // Precursors are sorted by charge and label type (light label first).
        // If there are precursors with different charge we want to display MS/MS spectra for all of them.

        Set<Integer> precursorCharges = new HashSet<>();
        List<LibrarySpectrumMatch> matchedSpectra = new ArrayList<>();

        List<Peptide.StructuralModification> structuralModifications= ModificationManager.getPeptideStructuralModifications(peptide.getId());
        Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap = new HashMap<>();
        for(Peptide.StructuralModification mod: structuralModifications)
        {
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getStructuralModId());
            potentialLossMap.put(mod.getStructuralModId(), losses);
        }

        for(Precursor precursor: precursors)
        {
            if(precursorCharges.contains(precursor.getCharge()))
                continue;  // We already have a match for this charge state

            for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
            {
                BlibSpectrum spectrum = BlibSpectrumReader.getSpectrum(libraryFilePathsMap.get(library),
                        precursor.getModifiedSequence(),
                        precursor.getCharge());

                // Make sure that the Bibliospec spectrum has peaks.  Minimized libraries in Skyline can have
                // library entries with no spectrum peaks.  This should be fixed in Skyline.
                if(spectrum != null && spectrum.getNumPeaks() > 0)
                {
                    LibrarySpectrumMatch pepSpec = new LibrarySpectrumMatch();
                    pepSpec.setCharge(precursor.getCharge());
                    pepSpec.setPeptide(peptide.getSequence());
                    pepSpec.setModifiedSequence(precursor.getModifiedSequence());
                    pepSpec.setLibrary(library);
                    pepSpec.setSpectrum(spectrum);
                    matchedSpectra.add(pepSpec);

                    // Add any structural modifications
                    pepSpec.setStructuralModifications(structuralModifications);
                    // Add any potential losses
                    pepSpec.setPotentialLosses(potentialLossMap);

                    // Add any isotope modifications (can be different for each precursor)
                    List<Peptide.IsotopeModification> isotopeModifications = ModificationManager.getPeptideIsotopelModifications(peptide.getId(), precursor.getIsotopeLabelId());
                    pepSpec.setIsotopeModifications(isotopeModifications);

                    precursorCharges.add(precursor.getCharge());
                    break;  // return spectrum from the first library that has a match
                }
            }
        }
        return matchedSpectra;
    }

    public static List<LibrarySpectrumMatch> getMatches(Precursor precursor)
    {
        TargetedMSRun run = TargetedMSManager.getRunForPeptide(precursor.getPeptideId());

        // Get the spectrum libraries for this run
        List<PeptideSettings.SpectrumLibrary> libraries = LibraryManager.getLibraries(run.getId());
        Map<PeptideSettings.SpectrumLibrary, String> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId(), libraries);

        // Precursors are sorted by charge and label type (light label first).
        // If there are precursors with different charge we want to display MS/MS spectra for all of them.

        List<LibrarySpectrumMatch> matchedSpectra = new ArrayList<>();

        List<Peptide.StructuralModification> structuralModifications= ModificationManager.getPeptideStructuralModifications(precursor.getPeptideId());
        Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap = new HashMap<>();
        for(Peptide.StructuralModification mod: structuralModifications)
        {
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getStructuralModId());
            potentialLossMap.put(mod.getStructuralModId(), losses);
        }


        for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
        {
            BlibSpectrum spectrum = BlibSpectrumReader.getSpectrum(libraryFilePathsMap.get(library),
                    precursor.getModifiedSequence(),
                    precursor.getCharge());

            // Make sure that the Bibliospec spectrum has peaks.  Minimized libraries in Skyline can have
            // library entries with no spectrum peaks.  This should be fixed in Skyline.
            if(spectrum != null && spectrum.getNumPeaks() > 0)
            {
                LibrarySpectrumMatch pepSpec = new LibrarySpectrumMatch();
                pepSpec.setCharge(precursor.getCharge());
                pepSpec.setPeptide(PeptideManager.get(precursor.getPeptideId()).getSequence());
                pepSpec.setModifiedSequence(precursor.getModifiedSequence());
                pepSpec.setLibrary(library);
                pepSpec.setSpectrum(spectrum);
                matchedSpectra.add(pepSpec);

                // Add any structural modifications
                pepSpec.setStructuralModifications(structuralModifications);
                // Add any potential losses
                pepSpec.setPotentialLosses(potentialLossMap);

                // Add any isotope modifications (can be different for each precursor)
                List<Peptide.IsotopeModification> isotopeModifications = ModificationManager.getPeptideIsotopelModifications(precursor.getPeptideId(), precursor.getIsotopeLabelId());
                pepSpec.setIsotopeModifications(isotopeModifications);

                break;  // return spectrum from the first library that has a match
            }
        }

        return matchedSpectra;
    }
}
