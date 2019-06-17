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

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.blib.BlibSpectrum;
import org.labkey.targetedms.parser.blib.BlibSpectrumReader;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 12:02 PM
 */
public class LibrarySpectrumMatchGetter
{
    private static final int CACHE_SIZE = 10;

    private static BlockingCache<PrecursorKey, List<PeptideIdRtInfo>> _peptideIdRtsCache =
            CacheManager.getBlockingCache(CACHE_SIZE, CacheManager.DAY, "TargetedMS peptide ID retention times",
                    new CacheLoader<PrecursorKey, List<PeptideIdRtInfo>>()
            {
                @Override
                public List<PeptideIdRtInfo> load(PrecursorKey precursor, @Nullable Object argument)
                {
                    if (!(argument instanceof Container))
                    {
                        throw new IllegalStateException("Expected Container argument in PeptideIdRts cache.");
                    }
                    TargetedMSRun run = TargetedMSManager.getRunForGeneralMolecule(precursor.getGeneralMoleculeId());

                    // Get the spectrum libraries for this run
                    Map<PeptideSettings.SpectrumLibrary, Path> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId());

                    for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
                    {
                        List<PeptideIdRtInfo> rtInfos = BlibSpectrumReader.getRetentionTimes((Container)argument,
                                libraryFilePathsMap.get(library), precursor.getModifiedSequence());

                        if(rtInfos.size() > 0)
                        {
                            return rtInfos;  // return matches from the first library that has a match
                        }
                    }

                    return Collections.emptyList();

                }
            });

    public static List<LibrarySpectrumMatch> getMatches(Peptide peptide, User user, Container container, Container pipeRootContainer)
    {
        // Get the precursor of this peptide, sorted by label type and charge.
        List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptide.getId(), new TargetedMSSchema(user, container));

        TargetedMSRun run = TargetedMSManager.getRunForGeneralMolecule(peptide.getId());

        // Get the spectrum libraries for this run
        LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId());

        List<Peptide.StructuralModification> structuralModifications= ModificationManager.getPeptideStructuralModifications(peptide.getId());
        List<PeptideSettings.RunStructuralModification> runStrMods = ModificationManager.getStructuralModificationsForRun(run.getId());
        Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap = new HashMap<>();
        for(Peptide.StructuralModification mod: structuralModifications)
        {
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getStructuralModId());
            potentialLossMap.put(mod.getStructuralModId(), losses);
        }

        List<LibrarySpectrumMatch> matchedSpectra = new ArrayList<>();

        // Precursors are sorted by charge and label type (light label first).
        // If there are precursors with different charge or isotope label we want to display the reference MS/MS spectra for all of them.
        for(Precursor precursor: precursors)
        {
            LibrarySpectrumMatch pepSpec = getMatch(peptide, precursor, pipeRootContainer, libraryFilePathsMap, structuralModifications, runStrMods, potentialLossMap);

            if(pepSpec != null)
            {
                matchedSpectra.add(pepSpec);
            }
        }
        return matchedSpectra;
    }
    
    public static List<LibrarySpectrumMatch> getMatches(Precursor precursor, Container container)
    {
        TargetedMSRun run = TargetedMSManager.getRunForGeneralMolecule(precursor.getGeneralMoleculeId());

        // Get the spectrum libraries for this run
        LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId());

        List<Peptide.StructuralModification> structuralModifications= ModificationManager.getPeptideStructuralModifications(precursor.getGeneralMoleculeId());
        List<PeptideSettings.RunStructuralModification> runStrMods = ModificationManager.getStructuralModificationsForRun(run.getId());
        Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap = new HashMap<>();
        for(Peptide.StructuralModification mod: structuralModifications)
        {
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getStructuralModId());
            potentialLossMap.put(mod.getStructuralModId(), losses);
        }

        Peptide peptide = PeptideManager.getPeptide(run.getContainer(), precursor.getGeneralMoleculeId());
        LibrarySpectrumMatch match = getMatch(peptide, precursor, container, libraryFilePathsMap, structuralModifications, runStrMods, potentialLossMap);

        return match != null ? Collections.singletonList(match) : Collections.emptyList();
    }

    private static LibrarySpectrumMatch getMatch(Peptide peptide, Precursor precursor, Container container,
                                                 LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> libraryFilePathsMap,
                                                 List<Peptide.StructuralModification> structuralModifications, List<PeptideSettings.RunStructuralModification> runStrMods,
                                                 Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap)
    {
        LibrarySpectrumMatch pepSpec = null;

        for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
        {
            if(libraryFilePathsMap.get(library) == null)
            {
                continue;
            }

            BlibSpectrum spectrum = BlibSpectrumReader.getSpectrum(container, libraryFilePathsMap.get(library),
                    precursor.getModifiedSequence(),
                    precursor.getCharge());

            // Make sure that the Bibliospec spectrum has peaks.  Minimized libraries in Skyline can have
            // library entries with no spectrum peaks.  This should be fixed in Skyline.
            if(spectrum != null && spectrum.getNumPeaks() > 0)
            {
                if(pepSpec == null)
                {
                    pepSpec = makeLibrarySpectrumMatch(spectrum, peptide, precursor, library, structuralModifications, runStrMods, potentialLossMap);
                }
                pepSpec.addLibrary(library); // Add this library to list of libraries that have a match for the precursor.
            }
        }
        return pepSpec;
    }

    public static LibrarySpectrumMatch getSpectrumMatch(TargetedMSRun run, Peptide peptide, Precursor precursor,
                                                              PeptideSettings.SpectrumLibrary library, Path libFilePath, Container container)
    {
        BlibSpectrum spectrum = BlibSpectrumReader.getSpectrum(container, libFilePath,
                precursor.getModifiedSequence(),
                precursor.getCharge());

        return makeLibrarySpectrumMatch(spectrum, run, peptide, precursor, library);
    }

    public static LibrarySpectrumMatch getRedundantSpectrumMatch(TargetedMSRun run, Peptide peptide, Precursor precursor,
                                                                 PeptideSettings.SpectrumLibrary library, Path redundantLibPath,
                                                                 Container container, int redundantRefSpecId)
    {
        BlibSpectrum spectrum = BlibSpectrumReader.getRedundantSpectrum(container, redundantLibPath, redundantRefSpecId);

        return makeLibrarySpectrumMatch(spectrum, run, peptide, precursor, library);
    }

    private static LibrarySpectrumMatch makeLibrarySpectrumMatch(BlibSpectrum spectrum, TargetedMSRun run, Peptide peptide, Precursor precursor,
                                                                 PeptideSettings.SpectrumLibrary library)
    {
        List<Peptide.StructuralModification> structuralModifications = ModificationManager.getPeptideStructuralModifications(precursor.getGeneralMoleculeId());
        List<PeptideSettings.RunStructuralModification> runStrMods = ModificationManager.getStructuralModificationsForRun(run.getId());
        Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap = new HashMap<>();
        for(Peptide.StructuralModification mod: structuralModifications)
        {
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getStructuralModId());
            potentialLossMap.put(mod.getStructuralModId(), losses);
        }

        LibrarySpectrumMatch pepSpec = null;

        // Make sure that the Bibliospec spectrum has peaks.  Minimized libraries in Skyline can have
        // library entries with no spectrum peaks.  This should be fixed in Skyline.
        if(spectrum != null && spectrum.getNumPeaks() > 0)
        {
            pepSpec = makeLibrarySpectrumMatch(spectrum, peptide, precursor, library, structuralModifications, runStrMods, potentialLossMap);
        }
        return pepSpec;
    }

    @NotNull
    private static LibrarySpectrumMatch makeLibrarySpectrumMatch(BlibSpectrum spectrum, Peptide peptide, Precursor precursor, PeptideSettings.SpectrumLibrary library,
                                                                 List<Peptide.StructuralModification> structuralModifications, List<PeptideSettings.RunStructuralModification> runStrMods,
                                                                 Map<Integer, List<PeptideSettings.PotentialLoss>> potentialLossMap)
    {
        LibrarySpectrumMatch pepSpec = new LibrarySpectrumMatch();
        pepSpec.setPrecursorId(precursor.getId());
        pepSpec.setCharge(precursor.getCharge());
        pepSpec.setPeptide(peptide.getSequence());
        PeptideSettings.IsotopeLabel label = IsotopeLabelManager.getIsotopeLabel(precursor.getIsotopeLabelId());
        pepSpec.setIsotopeLabel(label.getName());
        pepSpec.setModifiedSequence(precursor.getModifiedSequence());
        pepSpec.setLibrary(library);
        pepSpec.setSpectrum(spectrum);

        // Add any structural modifications
        pepSpec.setStructuralModifications(structuralModifications, runStrMods);
        // Add any potential losses
        pepSpec.setPotentialLosses(potentialLossMap);

        // Add any isotope modifications (can be different for each precursor)
        List<Peptide.IsotopeModification> isotopeModifications = ModificationManager.getPeptideIsotopelModifications(peptide.getId(), precursor.getIsotopeLabelId());
        pepSpec.setIsotopeModifications(isotopeModifications);
        return pepSpec;
    }

    public static List<PeptideIdRtInfo> getPeptideIdRts(Precursor precursor, SampleFile sampleFile, Container container)
    {
        if(precursor == null || sampleFile == null)
        {
            return Collections.emptyList();
        }

        List<PeptideIdRtInfo> peptideIdRtInfos = _peptideIdRtsCache.get(new PrecursorKey(precursor.getModifiedSequence(), precursor.getGeneralMoleculeId()), container);

        if(peptideIdRtInfos == null)
        {
            return Collections.emptyList();
        }

        List<PeptideIdRtInfo> rts = new ArrayList<>();
        // PeptideIdRtInfos cache has matches for the given precursor modified sequence in all sample files. Find the
        // ones in this sample file.
        for(PeptideIdRtInfo rtInfo: peptideIdRtInfos)
        {
            if(rtInfo.getSampleFileName() != null && sampleFile.getFilePath() != null)
            {
                // Use FileNameUtils.getBaseName() handle a file in either Unix or Windows format
                // Use getBaseName() to remove extension before comparing. Extension in the .blib file could be .mgf
                // and corresponding .raw file imported into Skyline.
                String sampleFileNameInLib = FilenameUtils.getBaseName(rtInfo.getSampleFileName());
                String sampleFileNameInSky = FilenameUtils.getBaseName(sampleFile.getFilePath());
                if (rtInfo.getCharge() == precursor.getCharge() &&
                        sampleFileNameInSky.equals(sampleFileNameInLib))
                {
                    rts.add(rtInfo);
                }
            }
        }

        return rts;
    }

    private static class PrecursorKey
    {
        private final String _modifiedSequence;
        private final int _generalMoleculeId;


        private PrecursorKey(String modifiedSequence, int generalMoleculeId)
        {
            _modifiedSequence = modifiedSequence;
            _generalMoleculeId = generalMoleculeId;
        }

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public int getGeneralMoleculeId()
        {
            return _generalMoleculeId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PrecursorKey that = (PrecursorKey) o;

            if (_generalMoleculeId != that._generalMoleculeId) return false;
            return _modifiedSequence.equals(that._modifiedSequence);

        }

        @Override
        public int hashCode()
        {
            int result = _modifiedSequence.hashCode();
            result = 31 * result + _generalMoleculeId;
            return result;
        }
    }

    public static class PeptideIdRtInfo
    {
        private final String _sampleFileName;
        private final String _modifiedSequence;
        private final int _charge;
        private final double _rt;
        private final boolean _bestSpectrum;

        public PeptideIdRtInfo(String sampleFileName, String modifiedSequence, int charge, double rt, boolean bestSpectrum)
        {
            _sampleFileName = sampleFileName;
            _modifiedSequence = modifiedSequence;
            _charge = charge;
            _rt = rt;
            _bestSpectrum = bestSpectrum;
        }

        public String getSampleFileName()
        {
            return _sampleFileName;
        }

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public int getCharge()
        {
            return _charge;
        }

        public double getRt()
        {
            return _rt;
        }

        public boolean isBestSpectrum()
        {
            return _bestSpectrum;
        }
    }
}
