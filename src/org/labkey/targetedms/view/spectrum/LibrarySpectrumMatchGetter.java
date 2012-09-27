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
import org.labkey.targetedms.query.PrecursorManager;

import java.util.ArrayList;
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

        // Precursors are sorted by label type (light label first) and charge
        // If there are precursors with different charge we want to display MS/MS spectra for all of them.

        Set<Integer> precursorCharges = new HashSet<Integer>();
        List<LibrarySpectrumMatch> matchedSpectra = new ArrayList<LibrarySpectrumMatch>();

        Map<Integer, Double> modLocationMassMap = ModificationManager.getPeptideStructuralMods(peptide.getId());

        for(Precursor precursor: precursors)
        {
            if(precursorCharges.contains(precursor.getCharge()))
                continue;

            for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
            {
                BlibSpectrum spectrum = BlibSpectrumReader.getSpectrum(libraryFilePathsMap.get(library),
                        precursor.getModifiedSequence(),
                        precursor.getCharge());

                if(spectrum != null)
                {
                    LibrarySpectrumMatch pepSpec = new LibrarySpectrumMatch();
                    pepSpec.setCharge(precursor.getCharge());
                    pepSpec.setPeptide(peptide.getSequence());
                    pepSpec.setModifiedSequence(precursor.getModifiedSequence());
                    pepSpec.setLibrary(library);
                    pepSpec.setSpectrum(spectrum);
                    matchedSpectra.add(pepSpec);

                    // Add any structural modifications
                    pepSpec.setStructuralModifications(modLocationMassMap);

                    break;  // return spectrum from the first library that has a match
                }
            }
        }
        return matchedSpectra;
    }
}
