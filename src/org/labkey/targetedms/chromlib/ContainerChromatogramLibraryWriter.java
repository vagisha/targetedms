/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.RunRepresentativeDataState;
import org.labkey.api.util.FileUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.MoleculeTransition;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.TransitionSettings;
import org.labkey.targetedms.query.InstrumentManager;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.MoleculeTransitionManager;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TransitionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * User: vsharma
 * Date: 1/7/13
 * Time: 2:28 PM
 */
public class ContainerChromatogramLibraryWriter
{
    private final String _panoramaServer;
    private final Container _container;
    private final List<Long> _representativeRunIds;
    private ChromatogramLibraryWriter _libWriter;

    private int _precursorCount = 0;
    private int _transitionCount = 0;

    // SampleFileId(Panorama) -> SampleFileId(SQLite Library)
    private Map<Long, Integer> _sampleFileIdMap;
    // IsotopeLabelId(Panorama) -> LabelName
    private Map<Long, String> _isotopeLabelMap;
    // ModificationId(Panorama) -> ModificationId(SQLite Library)
    private Map<Long, Integer> _isotopeModificationMap;
    // ModificationId(Panorama) -> IsotopeLabelId(Panorama)
    private Map<Long, Long> _isotopeModificationAndLabelMap;
    // ModificationId(Panorama) -> ModificationId(SQLite Library)
    private Map<Long, Integer> _structuralModificationMap;
    // PredictorId(Panorama) -> PredictorId(SQLite Library)
    private Map<Long, Integer> _predictorIdMap;

    private RunRepresentativeDataState _libraryType = null;
    private Long _bestReplicateIdForCurrentPeptideGroup;

    private final User _user;

    public ContainerChromatogramLibraryWriter(String panoramaServer, Container container, List<Long> representativeRunIds, User user)
    {
        _panoramaServer = panoramaServer;
        _container = container;
        _user = user;
        _representativeRunIds = representativeRunIds != null ? representativeRunIds : Collections.emptyList();
    }

    public String writeLibrary(LocalDirectory localDirectory, int libraryRevision) throws SQLException, IOException
    {
        Path tempChromLibFile = ChromatogramLibraryUtils.getChromLibTempFile(_container, localDirectory, libraryRevision);

        try
        {
            _libWriter = new ChromatogramLibraryWriter();
            _libWriter.openLibrary(tempChromLibFile);

            _sampleFileIdMap = new HashMap<>();
            _isotopeLabelMap = new HashMap<>();
            _isotopeModificationMap = new HashMap<>();
            _isotopeModificationAndLabelMap = new HashMap<>();
            _structuralModificationMap = new HashMap<>();
            _predictorIdMap = new HashMap<>();

            for(Long runId: _representativeRunIds)
            {
                writeRepresentativeDataInRun(runId);
            }

            writeIrtLibrary();

            writeLibInfo(libraryRevision);

        }
        finally
        {
            close();
        }

        Path finalChromLibFile = ChromatogramLibraryUtils.getChromLibFile(_container, libraryRevision,
                true /*Create the lib directory if it does not already exist */);

        // Rename the temp file
        if(Files.exists(finalChromLibFile))
        {
            Path oldFile = finalChromLibFile.resolveSibling(FileUtil.getFileName(finalChromLibFile) + ".old");
            Files.move(finalChromLibFile, oldFile);
            Files.deleteIfExists(oldFile);
        }

        Files.move(tempChromLibFile, finalChromLibFile);

        return FileUtil.getAbsolutePath(_container, finalChromLibFile);
    }

    public void close() throws SQLException
    {
        _libWriter.closeLibrary();
    }

    private void writeLibInfo(int libraryRevision) throws SQLException
    {
        LibInfo libInfo = new LibInfo();
        libInfo.setPanoramaServer(_panoramaServer);
        libInfo.setContainer(_container.getPath());
        libInfo.setCreated(new Date());
        libInfo.setSchemaVersion(Constants.SCHEMA_VERSION);
        libInfo.setLibraryRevision(libraryRevision);
        libInfo.setProteins(_libWriter.getProteinCount());
        libInfo.setPeptides(_libWriter.getPeptideCount());
        libInfo.setPrecursors(_precursorCount);
        libInfo.setTransitions(_transitionCount);
        _libWriter.writeLibInfo(libInfo);
    }

    private void writeRepresentativeDataInRun(Long runId) throws SQLException
    {
        // Write the replicates and sample files for this run.
        saveSampleFiles(runId);

        // Read the isotope labels for this run.
        List<PeptideSettings.IsotopeLabel> isotopeLabels = IsotopeLabelManager.getIsotopeLabels(runId);
        for(PeptideSettings.IsotopeLabel label: isotopeLabels)
        {
            _isotopeLabelMap.put(label.getId(), label.getName());
        }

        // Write the structural modifications for this run.
        saveRunStructuralModifications(runId);

        // Write the isotope modifications for this run.
        saveRunIsotopeModifications(runId);

        // Write the representative data.
        TargetedMSRun run = TargetedMSManager.getRun(runId);

        if(_libraryType == null)
        {
            _libraryType = run.getRepresentativeDataState();
        }
        else if(_libraryType != run.getRepresentativeDataState())
        {
            throw new IllegalStateException("Run representative state "+run.getRepresentativeDataState()+
                                             " does not match library type "+_libraryType);
        }

        if(run.getRepresentativeDataState() == RunRepresentativeDataState.Representative_Protein)
        {
            // If the run has representative protein data write the representative peptide groups.
            List<PeptideGroup> pepGroups = PeptideGroupManager.getRepresentativePeptideGroups(runId);
            for(PeptideGroup pepGroup: pepGroups)
            {
                savePeptideGroup(pepGroup, run);
            }
        }
        else
        {   // Otherwise, write the representative precursors in the run.
            saveRepresentativePrecursors(run);
        }
    }

    private void writeIrtLibrary() throws SQLException
    {
        // Retrieve the values from the database to hand off to be written into the SQLite Db
        // Many other tables get their values by way of static methods on their own Manager classes,
        // but that's overkill here.
        List<Integer> scaleIds = TargetedMSManager.getIrtScaleIds(_container);

        List<LibIrtLibrary> irtPeptides = new ArrayList<>();
        for (Integer scaleId : scaleIds)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("iRTScaleId"), scaleId);
            CaseInsensitiveHashSet columns = new CaseInsensitiveHashSet("ModifiedSequence", "iRTStandard", "iRTValue", "TimeSource");
            irtPeptides.addAll(new TableSelector(TargetedMSManager.getTableInfoiRTPeptide(),columns,filter,null).getArrayList(LibIrtLibrary.class));
        }

        _libWriter.writeIrtLibrary(irtPeptides);
    }

    private void saveRunIsotopeModifications(Long runId) throws SQLException
    {
        List<PeptideSettings.RunIsotopeModification> isotopeMods = ModificationManager.getIsotopeModificationsForRun(runId);
        for(PeptideSettings.RunIsotopeModification mod: isotopeMods)
        {
            // If we have already saved this modification, don't save it again.
            if(_isotopeModificationMap.containsKey(mod.getId()))
                continue;
            LibIsotopeModification libIsotopeMod = new LibIsotopeModification();
            libIsotopeMod.setName(mod.getName());
            String isotopeLabel = _isotopeLabelMap.get(mod.getIsotopeLabelId());
            if(isotopeLabel == null)
            {
                throw new IllegalStateException("Could not find an isotope label name for label Id "+mod.getIsotopeLabelId());
            }
            libIsotopeMod.setIsotopeLabel(isotopeLabel);
            libIsotopeMod.setAminoAcid(mod.getAminoAcid());
            libIsotopeMod.setTerminus(mod.getTerminus() != null ? mod.getTerminus().charAt(0) : null);
            libIsotopeMod.setFormula(mod.getFormula());
            libIsotopeMod.setMassDiffMono(mod.getMassDiffMono());
            libIsotopeMod.setMassDiffAvg(mod.getMassDiffAvg());
            libIsotopeMod.setLabel13C(mod.getLabel13C());
            libIsotopeMod.setLabel15N(mod.getLabel15N());
            libIsotopeMod.setLabel18O(mod.getLabel18O());
            libIsotopeMod.setLabel2H(mod.getLabel2H());

            _libWriter.writeIsotopeModification(libIsotopeMod);

            _isotopeModificationMap.put(mod.getId(), libIsotopeMod.getId());
            _isotopeModificationAndLabelMap.put(mod.getId(), mod.getIsotopeLabelId());
        }
    }

    private void saveRunStructuralModifications(Long runId) throws SQLException
    {
        List<PeptideSettings.RunStructuralModification> structuralMods = ModificationManager.getStructuralModificationsForRun(runId);
        for(PeptideSettings.RunStructuralModification mod: structuralMods)
        {
            // If we have already saved this modification, don't save it again.
            if(_structuralModificationMap.containsKey(mod.getId()))
                continue;

            LibStructuralModification libStrMod = new LibStructuralModification();
            libStrMod.setName(mod.getName());
            libStrMod.setAminoAcid(mod.getAminoAcid());
            libStrMod.setTerminus(mod.getTerminus() != null ? mod.getTerminus().charAt(0) : null);
            libStrMod.setFormula(mod.getFormula());
            libStrMod.setMassDiffMono(mod.getMassDiffMono());
            libStrMod.setMassDiffAvg(mod.getMassDiffAvg());
            libStrMod.setUnimodId(mod.getUnimodId());
            libStrMod.setVariable(mod.isVariable());
            libStrMod.setExplicitMod(mod.getExplicitMod());

            // Look up any mod losses
            List<PeptideSettings.PotentialLoss> losses = ModificationManager.getPotentialLossesForStructuralMod(mod.getId());
            for(PeptideSettings.PotentialLoss loss: losses)
            {
                LibStructuralModLoss libModLoss = new LibStructuralModLoss();
                libModLoss.setFormula(loss.getFormula());
                libModLoss.setMassDiffAvg(loss.getMassDiffAvg());
                libModLoss.setMassDiffMono(loss.getMassDiffMono());
                libStrMod.addModLoss(libModLoss);
            }
            _libWriter.writeStructuralModification(libStrMod);

            _structuralModificationMap.put(mod.getId(), libStrMod.getId());
        }
    }

    private void saveSampleFiles(Long runId) throws SQLException
    {
        List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(runId);
        for(SampleFile sampleFile: sampleFiles)
        {
            LibSampleFile libSampleFile = new LibSampleFile();
            libSampleFile.setFilePath(sampleFile.getFilePath());
            libSampleFile.setSampleName(sampleFile.getSampleName());
            libSampleFile.setAcquiredTime(sampleFile.getAcquiredTime());
            libSampleFile.setModifiedTime(sampleFile.getModifiedTime());

            if(sampleFile.getInstrumentId() != null)
            {
                Instrument instrument = InstrumentManager.getInstrument(sampleFile.getInstrumentId());
                libSampleFile.setInstrumentIonizationType(instrument.getIonizationType());
                libSampleFile.setInstrumentAnalyzer(instrument.getAnalyzer());
                libSampleFile.setInstrumentDetector(instrument.getDetector());
            }

            savePredictor(sampleFile, libSampleFile);

            _libWriter.writeSampleFile(libSampleFile);

            _sampleFileIdMap.put(sampleFile.getId(), libSampleFile.getId());
        }
    }

    private void savePredictor(SampleFile sampleFile, LibSampleFile libSampleFile) throws SQLException
    {
        Replicate replicate = ReplicateManager.getReplicate(sampleFile.getReplicateId());
        if (null != replicate.getCePredictorId())
        {
            // if we have already saved this cePredictorId, don't save it again
            if (_predictorIdMap.containsKey(replicate.getCePredictorId()))
            {
                libSampleFile.setCePredictorId(_predictorIdMap.get(replicate.getCePredictorId()));
                return;
            }
            TransitionSettings.Predictor cePredictor = ReplicateManager.getReplicatePredictor(replicate.getCePredictorId());
            LibPredictor libPredictor = new LibPredictor();
            libPredictor.setName(cePredictor.getName());
            libPredictor.setStepCount(cePredictor.getStepCount());
            libPredictor.setStepSize(cePredictor.getStepSize());

            _libWriter.writePredictor(libPredictor);
            _predictorIdMap.put(replicate.getCePredictorId(), libPredictor.getId());
            libSampleFile.setCePredictorId(libPredictor.getId());
        }

        if (null != replicate.getDpPredictorId())
        {
            // if we have already saved this dpPredictorId, don't save it again
            if (_predictorIdMap.containsKey(replicate.getDpPredictorId()))
            {
                libSampleFile.setDpPredictorId(_predictorIdMap.get(replicate.getDpPredictorId()));
                return;
            }
            TransitionSettings.Predictor dpPredictor = ReplicateManager.getReplicatePredictor(replicate.getDpPredictorId());
            LibPredictor libPredictor = new LibPredictor();
            libPredictor.setName(dpPredictor.getName());
            libPredictor.setStepCount(dpPredictor.getStepCount());
            libPredictor.setStepSize(dpPredictor.getStepSize());

            _libWriter.writePredictor(libPredictor);
            _predictorIdMap.put(replicate.getDpPredictorId(), libPredictor.getId());
            libSampleFile.setDpPredictorId(libPredictor.getId());
        }
    }

    private void saveRepresentativePrecursors(TargetedMSRun run)
    {
        saveProteomicsPrecursors(run);
        saveMoleculePrecursors(run);
    }

    private void saveProteomicsPrecursors(TargetedMSRun run)
    {
        List<Precursor> precursors = PrecursorManager.getRepresentativePrecursors(run.getId());
        // Sort by peptideId.
        precursors.sort(Comparator.comparingLong(Precursor::getGeneralMoleculeId));

        long lastPeptideId = 0;
        List<Precursor> peptidePrecursors = new ArrayList<>();
        for(Precursor precursor: precursors)
        {
            if(precursor.getGeneralMoleculeId() != lastPeptideId)
            {
                if(peptidePrecursors.size() > 0)
                {
                    Peptide peptide = PeptideManager.getPeptide(_container, lastPeptideId);
                    LibPeptide libPeptide = makeLibPeptide(peptide, peptidePrecursors, run);
                    peptidePrecursors.clear();

                    _libWriter.writePeptide(libPeptide, peptide);
                }
                lastPeptideId = precursor.getGeneralMoleculeId();
                peptidePrecursors = new ArrayList<>();
            }
            peptidePrecursors.add(precursor);
        }
        if(peptidePrecursors.size() > 0)
        {
            Peptide peptide = PeptideManager.getPeptide(_container, lastPeptideId);
            LibPeptide libPeptide = makeLibPeptide(peptide, peptidePrecursors, run);
            _libWriter.writePeptide(libPeptide, peptide);
        }
    }

    private void saveMoleculePrecursors(TargetedMSRun run)
    {
        List<MoleculePrecursor> precursors = MoleculePrecursorManager.getRepresentativeMoleculePrecursors(run.getId());
        // Sort by id
        precursors.sort(Comparator.comparingLong(MoleculePrecursor::getGeneralMoleculeId));

        long lastMoleculeId = 0;
        List<MoleculePrecursor> moleculePrecursors = new ArrayList<>();
        for(MoleculePrecursor precursor: precursors)
        {
            if(precursor.getGeneralMoleculeId() != lastMoleculeId)
            {
                if(moleculePrecursors.size() > 0)
                {
                    Molecule molecule = MoleculeManager.getMolecule(_container, lastMoleculeId);
                    LibPeptide libMolecule = makeLibMolecule(molecule, moleculePrecursors, run);
                    moleculePrecursors.clear();

                    _libWriter.writeMolecule(libMolecule, molecule);
                }
                lastMoleculeId = precursor.getGeneralMoleculeId();
                moleculePrecursors = new ArrayList<>();
            }
            moleculePrecursors.add(precursor);
        }
        if(moleculePrecursors.size() > 0)
        {
            Molecule molecule = MoleculeManager.getMolecule(_container, lastMoleculeId);
            LibPeptide libMolecule = makeLibMolecule(molecule, moleculePrecursors, run);
            _libWriter.writeMolecule(libMolecule, molecule);
        }
    }

    private void savePeptideGroup(PeptideGroup pepGroup, TargetedMSRun run)
    {
        // Create an entry in the Protein table.
        LibProtein libProtein = new LibProtein(pepGroup);

        // Get the replicate that has the maximum overall peak area for this protein
        getBestReplicateIdForPeptideGroup(pepGroup);

        // Add peptides.
        addPeptides(pepGroup, libProtein, run);

        // Save the protein.
        _libWriter.writeProtein(pepGroup.getId(), libProtein);
    }

    private void getBestReplicateIdForPeptideGroup(PeptideGroup pepGroup)
    {
        _bestReplicateIdForCurrentPeptideGroup = PeptideGroupManager.getBestReplicateId(pepGroup);
    }

    private void addPeptides(PeptideGroup pepGroup, LibProtein protein, TargetedMSRun run)
    {
        TargetedMSSchema schema = new TargetedMSSchema(_user, _container);

        Collection<Peptide> peptides = PeptideManager.getPeptidesForGroup(pepGroup.getId(), schema);
        for(Peptide peptide: peptides)
        {
            List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptide.getId(), schema);
            if(precursors.size() == 0)
            {
                throw new IllegalStateException(String.format("No precursors found for peptide '%s'. Empty peptides are not allowed in library folders." +
                        " Empty peptides can be removed in Skyline by selecting Refine > Remove Empty Peptides.", peptide.getSequence()));
            }
            LibPeptide libPeptide = makeLibPeptide(peptide, precursors, run);
            protein.addChild(libPeptide);
        }
    }

    private LibPeptide makeLibPeptide(Peptide peptide, List<Precursor> precursors, TargetedMSRun run)
    {
        LibPeptide libPeptide = makeLibPeptide(peptide);

        // Get the isotope modifications for the peptide.
        List<Peptide.IsotopeModification> pepIsotopeMods = ModificationManager.getPeptideIsotopelModifications(peptide.getId());
        // IsotopeLabelId(Panorama) -> List<Peptide.IsotopeModification>
        Map<Long, List<Peptide.IsotopeModification>> precIsotopeModMap = new HashMap<>();
        for(Peptide.IsotopeModification isotopeMod: pepIsotopeMods)
        {
            Long isotopeLabelId = _isotopeModificationAndLabelMap.get(isotopeMod.getIsotopeModId());
            if(isotopeLabelId == null)
            {
                throw new IllegalStateException("Could not find isotope label for isotope modification Id "+isotopeMod.getIsotopeModId());
            }
            List<Peptide.IsotopeModification> isotopeMods = precIsotopeModMap.computeIfAbsent(isotopeLabelId, k -> new ArrayList<>());
            isotopeMods.add(isotopeMod);
        }

        for(Precursor precursor: precursors)
        {
            List<Peptide.IsotopeModification> precIsotopeMods = precIsotopeModMap.get(precursor.getIsotopeLabelId());
            precIsotopeMods = (precIsotopeMods != null) ? precIsotopeMods : Collections.emptyList();

            LibPrecursor libPrecursor = makeLibPrecursor(precursor, precIsotopeMods, run);
            libPeptide.addPrecursor(libPrecursor);
        }
        return libPeptide;
    }

    private LibPeptide makeLibMolecule(Molecule molecule, List<MoleculePrecursor> precursors, TargetedMSRun run)
    {
        LibPeptide libMolecule = makeLibMolecule(molecule);

        for(MoleculePrecursor precursor: precursors)
        {
            LibPrecursor libPrecursor = makeLibPrecursor(precursor, run);
            libMolecule.addPrecursor(libPrecursor);
        }
        return libMolecule;
    }

    private LibPeptide makeLibPeptide(Peptide peptide)
    {
        LibPeptide libPeptide = new LibPeptide();
        libPeptide.setSequence(peptide.getSequence());
        libPeptide.setStartIndex(peptide.getStartIndex());
        libPeptide.setEndIndex(peptide.getEndIndex());
        String previousAa = peptide.getPreviousAa();
        if(previousAa != null && previousAa.length() > 0)
        {
            libPeptide.setPreviousAa(previousAa.charAt(0));
        }
        String nextAa = peptide.getNextAa();
        if(nextAa != null && nextAa.length() > 0)
        {
            libPeptide.setNextAa(nextAa.charAt(0));
        }
        libPeptide.setCalcNeutralMass(peptide.getCalcNeutralMass());
        libPeptide.setNumMissedCleavages(peptide.getNumMissedCleavages());

        // Get the structural modifications for the peptide
        List<Peptide.StructuralModification> strMods = ModificationManager.getPeptideStructuralModifications(peptide.getId());
        for(Peptide.StructuralModification pepMod: strMods)
        {
            LibPeptideStructuralModification libPepMod = new LibPeptideStructuralModification();
            libPepMod.setIndexAa(pepMod.getIndexAa());
            libPepMod.setMassDiff(pepMod.getMassDiff());
            Integer libStrModId = _structuralModificationMap.get(pepMod.getStructuralModId());
            if(libStrModId == null)
            {
                throw new IllegalStateException("Library Id not found for structural modification Id "+pepMod.getStructuralModId());
            }
            libPepMod.setStructuralModificationId(libStrModId);

            libPeptide.addStructuralModification(libPepMod);
        }
        return libPeptide;
    }

    private LibPeptide makeLibMolecule(Molecule molecule)
    {
        LibPeptide result = new LibPeptide();
        result.setChemicalFormula(molecule.getIonFormula());
        result.setMoleculeName(molecule.getCustomIonName());
        result.setMassMonoisotopic(molecule.getMassMonoisotopic());
        result.setMassAverage(molecule.getMassAverage());
        result.setMoleculeAccession(molecule.getMoleculeId());
        return result;
    }

    private PrecursorChromInfo getBestPrecursorChromInfo(GeneralPrecursor<?> precursor)
    {
        if(_libraryType == RunRepresentativeDataState.Representative_Peptide)
        {
            // Get the precursor chrom info for this precursor that has the max total area across all replicates.
            return PrecursorManager.getBestPrecursorChromInfoForPrecursor(precursor.getId());
        }
        else if(_libraryType == RunRepresentativeDataState.Representative_Protein)
        {
            if(_bestReplicateIdForCurrentPeptideGroup != null)
            {
                return PrecursorManager.getBestPrecursorChromInfoForPrecursorAndReplicate(precursor.getId(), _bestReplicateIdForCurrentPeptideGroup);
            }
        }
        return null;
    }

    private LibPrecursor makeLibPrecursor(Precursor precursor,
                                          List<Peptide.IsotopeModification> precursorIsotopeMods, TargetedMSRun run)
    {
        PrecursorChromInfo bestChromInfo = getBestPrecursorChromInfo(precursor);
        LibPrecursor libPrecursor = new LibPrecursor(precursor, _isotopeLabelMap, bestChromInfo, run, _sampleFileIdMap);

        // Add the precursor isotope modifications
        addPrecursorIsotopeModifications(precursorIsotopeMods, libPrecursor);

        // Add precursor retention times
        addPrecursorRetentionTimes(libPrecursor, precursor);

        // Add transitions.
        Collection<Transition> transitions = TransitionManager.getTransitionsForPrecursor(precursor.getId(), _user, _container);
        addTransitions(libPrecursor, transitions, bestChromInfo, (t, tci) -> new LibTransition(t, tci, precursor, TransitionManager.getOptimizations(t.getId()), TargetedMSManager.getTransitionFullScanSettings(run.getId())));
        _precursorCount++;
        return libPrecursor;
    }

    private LibPrecursor makeLibPrecursor(MoleculePrecursor precursor, TargetedMSRun run)
    {
        PrecursorChromInfo bestChromInfo = getBestPrecursorChromInfo(precursor);
        LibPrecursor libPrecursor = new LibPrecursor(precursor, _isotopeLabelMap, bestChromInfo, run, _sampleFileIdMap);

        // Add precursor retention times
        addPrecursorRetentionTimes(libPrecursor, precursor);

        Collection<MoleculeTransition> transitions = MoleculeTransitionManager.getTransitionsForPrecursor(precursor.getId(), _user, _container);
        // Add transitions.
        addTransitions(libPrecursor, transitions, bestChromInfo, (t, tci) -> new LibTransition(t, tci, precursor, TransitionManager.getOptimizations(t.getId()), run.fetchFullScanSettings()));
        _precursorCount++;
        return libPrecursor;
    }

    private void addPrecursorRetentionTimes(LibPrecursor libPrecursor, GeneralPrecursor<?> precursor)
    {
        // Get the precursor chrom infos
        List<PrecursorChromInfo> precursorChromInfos = PrecursorManager.getSortedPrecursorChromInfosForPrecursor(precursor.getId());

        for(PrecursorChromInfo chromInfo: precursorChromInfos)
        {
            if(chromInfo.getBestRetentionTime() == null)
                continue;

            long sampleFileId = chromInfo.getSampleFileId();
            Integer libSampleFileId = _sampleFileIdMap.get(sampleFileId);
            if(libSampleFileId == null)
            {
                throw new IllegalStateException("Could not find an Id in the library for sample file Id "+sampleFileId);
            }

            LibPrecursorRetentionTime precRetTime = new LibPrecursorRetentionTime();
            precRetTime.setSampleFileId(libSampleFileId);
            precRetTime.setRetentionTime(chromInfo.getBestRetentionTime());
            precRetTime.setStartTime(chromInfo.getMinStartTime());
            precRetTime.setEndTime(chromInfo.getMaxEndTime());
            precRetTime.setOptimizationStep(chromInfo.getOptimizationStep());

            libPrecursor.addRetentionTime(precRetTime);
        }
    }

    private void addPrecursorIsotopeModifications(List<Peptide.IsotopeModification> precursorIsotopeMods, LibPrecursor libPrecursor)
    {
        for(Peptide.IsotopeModification precIsotopeMod: precursorIsotopeMods)
        {
            LibPrecursorIsotopeModification libPrecIsotopeMod = new LibPrecursorIsotopeModification();
            libPrecIsotopeMod.setIndexAa(precIsotopeMod.getIndexAa());
            libPrecIsotopeMod.setMassDiff(precIsotopeMod.getMassDiff());
            Integer libIsotopeModId = _isotopeModificationMap.get(precIsotopeMod.getIsotopeModId());
            if(libIsotopeModId == null)
            {
                throw new IllegalStateException("Library Id not found for isotope modification Id "+precIsotopeMod.getIsotopeModId());
            }
            libPrecIsotopeMod.setIsotopeModificationId(libIsotopeModId);

            libPrecursor.addIsotopeModification(libPrecIsotopeMod);
        }
    }

    private <T extends GeneralTransition> void addTransitions(LibPrecursor precToSave, Collection<T> transitions, PrecursorChromInfo precursorChromInfo, BiFunction<T, TransitionChromInfo, LibTransition> factory)
    {
        for(T transition: transitions)
        {
            TransitionChromInfo tci = null;
            if(precursorChromInfo != null)
            {
                tci = TransitionManager.getTransitionChromInfoForTransition(transition.getId(), precursorChromInfo.getId());
            }
            LibTransition transitionToSave = factory.apply(transition, tci);
            precToSave.addTransition(transitionToSave);
            _transitionCount++;
        }
    }
}
