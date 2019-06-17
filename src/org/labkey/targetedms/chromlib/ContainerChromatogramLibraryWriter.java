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
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.FileUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.InstrumentManager;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ModificationManager;
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

/**
 * User: vsharma
 * Date: 1/7/13
 * Time: 2:28 PM
 */
public class ContainerChromatogramLibraryWriter
{
    private String _panoramaServer;
    private Container _container;
    private List<Integer> _representativeRunIds;
    private ChromatogramLibraryWriter _libWriter;

    private int _proteinCount = 0;
    private int _peptideCount = 0;
    private int _precursorCount = 0;
    private int _transitionCount = 0;

    // SampleFileId(Panorama) -> SampleFileId(SQLite Library)
    private Map<Integer, Integer> _sampleFileIdMap;
    // IsotopeLabelId(Panorama) -> LabelName
    private Map<Integer, String> _isotopeLabelMap;
    // ModificationId(Panorama) -> ModificationId(SQLite Library)
    private Map<Integer, Integer> _isotopeModificationMap;
    // ModificationId(Panorama) -> IsotopeLabelId(Panorama)
    private Map<Integer, Integer> _isotopeModificationAndLabelMap;
    // ModificationId(Panorama) -> ModificationId(SQLite Library)
    private Map<Integer, Integer> _structuralModificationMap;

    private ProteinService _proteinService;

    private TargetedMSRun.RepresentativeDataState _libraryType = null;
    private Integer _bestReplicateIdForCurrentPeptideGroup;

    private User _user;

    public ContainerChromatogramLibraryWriter(String panoramaServer, Container container, List<Integer> representativeRunIds, User user)
    {
        _panoramaServer = panoramaServer;
        _container = container;
        _user = user;
        _representativeRunIds = representativeRunIds != null ? representativeRunIds : Collections.emptyList();

        _proteinService = ProteinService.get();
    }

    public String writeLibrary(LocalDirectory localDirectory, int libraryRevision) throws SQLException, IOException
    {
        Path tempChromLibFile = ChromatogramLibraryUtils.getChromLibTempFile(_container, localDirectory, libraryRevision);

        try
        {
            _libWriter = new ChromatogramLibraryWriter();
            _libWriter.setMaxCacheSize(20000);
            _libWriter.openLibrary(tempChromLibFile);

            _sampleFileIdMap = new HashMap<>();
            _isotopeLabelMap = new HashMap<>();
            _isotopeModificationMap = new HashMap<>();
            _isotopeModificationAndLabelMap = new HashMap<>();
            _structuralModificationMap = new HashMap<>();

            for(Integer runId: _representativeRunIds)
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

        Path finalChromLibFile = ChromatogramLibraryUtils.getChromLibFile(_container, libraryRevision);
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
        libInfo.setProteins(_proteinCount);
        libInfo.setPeptides(_peptideCount);
        libInfo.setPrecursors(_precursorCount);
        libInfo.setTransitions(_transitionCount);
        _libWriter.writeLibInfo(libInfo);
    }

    private void writeRepresentativeDataInRun(Integer runId) throws SQLException
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

        if(run.getRepresentativeDataState() == TargetedMSRun.RepresentativeDataState.Representative_Protein)
        {
            // If the run has representative protein data write the representative peptide groups.
            List<PeptideGroup> pepGroups = PeptideGroupManager.getRepresentativePeptideGroups(runId);
            for(PeptideGroup pepGroup: pepGroups)
            {
                savePeptideGroup(pepGroup);
            }
        }
        else
        {   // Otherwise, write the representative precursors in the run.
            saveRepresentativePrecursors(runId);
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

    private void saveRunIsotopeModifications(Integer runId) throws SQLException
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

    private void saveRunStructuralModifications(Integer runId) throws SQLException
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

    private void saveSampleFiles(Integer runId) throws SQLException
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

            _libWriter.writeSampleFile(libSampleFile);

            _sampleFileIdMap.put(sampleFile.getId(), libSampleFile.getId());
        }
    }

    private void saveRepresentativePrecursors(Integer runId) throws SQLException
    {
        List<Precursor> precursors = PrecursorManager.getRepresentativePrecursors(runId);
        // Sort by peptideId.
        precursors.sort(Comparator.comparingInt(Precursor::getGeneralMoleculeId));

        int lastPeptideId = 0;
        List<Precursor> peptidePrecursors = new ArrayList<>();
        for(Precursor precursor: precursors)
        {
            if(precursor.getGeneralMoleculeId() != lastPeptideId)
            {
                if(peptidePrecursors.size() > 0)
                {
                    Peptide peptide = PeptideManager.getPeptide(_container, lastPeptideId);
                    LibPeptide libPeptide = makeLibPeptide(peptide, peptidePrecursors);
                    peptidePrecursors.clear();

                    _libWriter.writePeptide(libPeptide);
                }
                lastPeptideId = precursor.getGeneralMoleculeId();
                peptidePrecursors = new ArrayList<>();
            }
            peptidePrecursors.add(precursor);
        }
        if(peptidePrecursors.size() > 0)
        {
            Peptide peptide = PeptideManager.getPeptide(_container, lastPeptideId);
            LibPeptide libPeptide = makeLibPeptide(peptide, peptidePrecursors);
            _libWriter.writePeptide(libPeptide);
        }
    }

    private void savePeptideGroup(PeptideGroup pepGroup) throws SQLException
    {
        // Create an entry in the Protein table.
        LibProtein libProtein = new LibProtein();
        libProtein.setName(pepGroup.getLabel());
        libProtein.setDescription(pepGroup.getDescription());
        if(pepGroup.getSequenceId() != null)
        {
            libProtein.setSequence(_proteinService.getProteinSequence(pepGroup.getSequenceId()));
        }

        // Get the replicate that has the maximum overall peak area for this protein
        getBestReplicateIdForPeptideGroup(pepGroup);

        // Add peptides.
        addPeptides(pepGroup, libProtein);

        // Save the protein.
        _proteinCount++;
        _libWriter.writeProtein(libProtein);
    }

    private void getBestReplicateIdForPeptideGroup(PeptideGroup pepGroup)
    {
        _bestReplicateIdForCurrentPeptideGroup = PeptideGroupManager.getBestReplicateId(pepGroup);
    }

    private void addPeptides(PeptideGroup pepGroup, LibProtein protein)
    {
        TargetedMSSchema schema = new TargetedMSSchema(_user, _container);

        Collection<Peptide> peptides = PeptideManager.getPeptidesForGroup(pepGroup.getId(), schema);
        for(Peptide peptide: peptides)
        {
            List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptide.getId(), schema);

            LibPeptide libPeptide = makeLibPeptide(peptide, precursors);

            protein.addPeptide(libPeptide);
        }
    }

    private LibPeptide makeLibPeptide(Peptide peptide, List<Precursor> precursors)
    {
        LibPeptide libPeptide = makeLibPeptide(peptide);

        // Get the isotope modifications for the peptide.
        List<Peptide.IsotopeModification> pepIsotopeMods = ModificationManager.getPeptideIsotopelModifications(peptide.getId());
        // IsotopeLabelId(Panorama) -> List<Peptide.IsotopeModification>
        Map<Integer, List<Peptide.IsotopeModification>> precIsotopeModMap = new HashMap<>();
        for(Peptide.IsotopeModification isotopeMod: pepIsotopeMods)
        {
            Integer isotopeLabelId = _isotopeModificationAndLabelMap.get(isotopeMod.getIsotopeModId());
            if(isotopeLabelId == null)
            {
                throw new IllegalStateException("Could not find isotope label for isotope modification Id "+isotopeMod.getIsotopeModId());
            }
            List<Peptide.IsotopeModification> isotopeMods = precIsotopeModMap.get(isotopeLabelId);
            if(isotopeMods == null)
            {
                isotopeMods = new ArrayList<>();
                precIsotopeModMap.put(isotopeLabelId, isotopeMods);
            }
            isotopeMods.add(isotopeMod);
        }

        for(Precursor precursor: precursors)
        {
            List<Peptide.IsotopeModification> precIsotopeMods = precIsotopeModMap.get(precursor.getIsotopeLabelId());
            precIsotopeMods = (precIsotopeMods != null) ? precIsotopeMods : Collections.emptyList();

            LibPrecursor libPrecursor = makeLibPrecursor(precursor, precIsotopeMods);
            libPeptide.addPrecursor(libPrecursor);
        }
        return libPeptide;
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
        _peptideCount++;
        return libPeptide;
    }

    private PrecursorChromInfo getBestPrecursorChromInfo(Precursor precursor)
    {
        if(_libraryType == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
        {
            // Get the precursor chrom info for this precursor that has the max total area across all replicates.
            return PrecursorManager.getBestPrecursorChromInfoForPrecursor(precursor.getId());
        }
        else if(_libraryType == TargetedMSRun.RepresentativeDataState.Representative_Protein)
        {
            if(_bestReplicateIdForCurrentPeptideGroup != null)
            {
                return PrecursorManager.getBestPrecursorChromInfoForPrecursorAndReplicate(precursor.getId(), _bestReplicateIdForCurrentPeptideGroup);
            }
        }
        return null;
    }

    private LibPrecursor makeLibPrecursor(Precursor precursor,
                                          List<Peptide.IsotopeModification> precursorIsotopeMods)
    {
        LibPrecursor libPrecursor = new LibPrecursor();
        String isotopeLabel = _isotopeLabelMap.get(precursor.getIsotopeLabelId());
        if(isotopeLabel == null)
        {
            throw new IllegalStateException("Isotope label name not found for Id "+precursor.getIsotopeLabelId());
        }
        libPrecursor.setIsotopeLabel(isotopeLabel);
        libPrecursor.setMz(precursor.getMz());
        libPrecursor.setCharge(precursor.getCharge());
        libPrecursor.setNeutralMass(precursor.getNeutralMass());
        libPrecursor.setModifiedSequence(precursor.getModifiedSequence());
        libPrecursor.setCollisionEnergy(precursor.getCollisionEnergy());
        libPrecursor.setDeclusteringPotential(precursor.getDeclusteringPotential());

        PrecursorChromInfo bestChromInfo = getBestPrecursorChromInfo(precursor);
        if(bestChromInfo != null)
        {
            libPrecursor.setTotalArea(bestChromInfo.getTotalArea() == null ? 0.0 : bestChromInfo.getTotalArea());
            libPrecursor.setChromatogram(bestChromInfo.getChromatogram());
            libPrecursor.setUncompressedSize(bestChromInfo.getUncompressedSize());
            libPrecursor.setChromatogramFormat(bestChromInfo.getChromatogramFormat());
            libPrecursor.setNumTransitions(bestChromInfo.getNumTransitions());
            libPrecursor.setNumPoints(bestChromInfo.getNumPoints());
            libPrecursor.setAverageMassErrorPPM(bestChromInfo.getAverageMassErrorPPM());

            int sampleFileId = bestChromInfo.getSampleFileId();
            Integer libSampleFileId = _sampleFileIdMap.get(sampleFileId);
            if(libSampleFileId == null)
            {
                throw new IllegalStateException("Could not find an Id in the library for sample file Id "+sampleFileId);
            }
            libPrecursor.setSampleFileId(libSampleFileId.intValue());
        }
        else
        {
            libPrecursor.setTotalArea(0.0);
            libPrecursor.setNumTransitions(0);
            libPrecursor.setNumPoints(0);
        }

        // Add the precursor isotope modifications
        addPrecursorIsotopeModifications(precursorIsotopeMods, libPrecursor);

        // Add precursor retention times
        addPrecursorRetentionTimes(libPrecursor, precursor);

        // Add transitions.
        addTransitions(libPrecursor, precursor, bestChromInfo);

        _precursorCount++;
        return libPrecursor;
    }

    private void addPrecursorRetentionTimes(LibPrecursor libPrecursor, Precursor precursor)
    {
        // Get the precursor chrom infos
        List<PrecursorChromInfo> precursorChromInfos = PrecursorManager.getSortedPrecursorChromInfosForPrecursor(precursor.getId());

        for(PrecursorChromInfo chromInfo: precursorChromInfos)
        {
            if(chromInfo.getBestRetentionTime() == null)
                continue;

            int sampleFileId = chromInfo.getSampleFileId();
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

    private void addTransitions(LibPrecursor precToSave, Precursor precursor, PrecursorChromInfo precursorChromInfo)
    {
        List<Transition> transitions = new ArrayList<>(TransitionManager.getTransitionsForPrecursor(precursor.getId(), _user, _container));
        for(Transition transition: transitions)
        {
            TransitionChromInfo tci = null;
            if(precursorChromInfo != null)
            {
                tci = TransitionManager.getTransitionChromInfoForTransition(transition.getId(), precursorChromInfo.getId());
            }
            LibTransition transitionToSave = makeLibTransition(transition, tci);

            precToSave.addTransition(transitionToSave);
        }
    }

    private LibTransition makeLibTransition(Transition transition, TransitionChromInfo tci)
    {
        LibTransition transitionToSave = new LibTransition();
        transitionToSave.setMz(transition.getMz());
        transitionToSave.setCharge(transition.getCharge());
        transitionToSave.setNeutralMass(transition.getNeutralMass());
        transitionToSave.setNeutralLossMass(transition.getNeutralLossMass());
        transitionToSave.setFragmentType(transition.getFragmentType());
        transitionToSave.setFragmentOrdinal(transition.getFragmentOrdinal());
        transitionToSave.setMassIndex(transition.getMassIndex());

        if(tci != null)
        {
            transitionToSave.setArea(tci.getArea() == null ? 0.0 : tci.getArea());
            transitionToSave.setHeight(tci.getHeight() == null ? 0.0 : tci.getHeight());
            transitionToSave.setFwhm(tci.getFwhm() == null ? 0.0 : tci.getFwhm());
            transitionToSave.setChromatogramIndex(tci.getChromatogramIndex());
            transitionToSave.setMassErrorPPM(tci.getMassErrorPPM());
        }
        else
        {
            transitionToSave.setArea(0.0);
            transitionToSave.setHeight(0.0);
            transitionToSave.setFwhm(0.0);
        }

        _transitionCount++;
        return transitionToSave;
    }
}
