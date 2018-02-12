/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.targetedms;

import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.targetedms.calculations.quantification.RegressionFit;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.SkylinePort.Irt.IRegressionFunction;
import org.labkey.targetedms.SkylinePort.Irt.IrtRegressionCalculator;
import org.labkey.targetedms.SkylinePort.Irt.RetentionTimeProviderImpl;
import org.labkey.targetedms.calculations.RunQuantifier;
import org.labkey.targetedms.model.QCMetricExclusion;
import org.labkey.targetedms.parser.*;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.RepresentativeStateManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

/**
 * Drives import of the Skyline document, and handles the high-level iteration for inserting
 * the document's contents into the database but not the parsing of the XML.
 * User: vsharma
 * Date: 4/1/12
 */
public class SkylineDocImporter
{
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    private static final String IMPORT_STARTED = "Importing... (refresh to check status)";
    private static final String IMPORT_SUCCEEDED = "";

    private User _user;
    private Container _container;
    private final TargetedMSRun.RepresentativeDataState _representative;
    private final ExpData _expData;
    private String _description;

    protected int _runId;
    private boolean _isProteinLibraryDoc = false;
    private boolean _isPeptideLibraryDoc = false;
    private Set<Integer> _libProteinSequenceIds;
    private Set<String> _libProteinLabels;
    private Set<String> _libPrecursors;

    // Use passed in logger for import status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static final Logger _systemLog = Logger.getLogger(SkylineDocImporter.class);
    protected final XarContext _context;
    private int blankLabelIndex;

    private final LocalDirectory _localDirectory;
    private final PipeRoot _pipeRoot;

    private File _blibSourceDir;
    private final List<Path> _blibSourcePaths = new ArrayList<>();

    // protected Connection _conn;
    // private static final int BATCH_SIZE = 100;

    public SkylineDocImporter(User user, Container c, String description, ExpData expData, Logger log, XarContext context,
                              TargetedMSRun.RepresentativeDataState representative, @Nullable LocalDirectory localDirectory, @Nullable PipeRoot pipeRoot)
    {
        _context = context;
        _user = user;
        _container = c;
        _representative = representative;

        _expData = expData;
        _localDirectory = localDirectory;
        _pipeRoot = pipeRoot;

        if (null != description)
            _description = description;
        else
        {
            _description = FileUtil.getBaseName(_expData.getName());
        }

        _log = (null == log ? _systemLog : log);
    }

    public TargetedMSRun importRun(RunInfo runInfo) throws IOException, XMLStreamException, DataFormatException, PipelineJobException
    {
        _runId = runInfo.getRunId();

        TargetedMSRun run = TargetedMSManager.getRun(_runId);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == SkylineDocImporter.STATUS_SUCCESS)
        {
            _log.info(_expData.getName() + " has already been imported so it does not need to be imported again");
            return run;
        }

        TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(_container);
        _isProteinLibraryDoc = folderType == TargetedMSModule.FolderType.LibraryProtein;
        _isPeptideLibraryDoc = folderType == TargetedMSModule.FolderType.Library;

        try
        {
            File inputFile = getInputFile();
            if (null == inputFile)
                throw new FileNotFoundException();

            updateRunStatus(IMPORT_STARTED);
            _log.info("Starting to import Skyline document from " + run.getFileName());
            importSkylineDoc(run, inputFile);
            _log.info("Completed import of Skyline document from " + run.getFileName());

            updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);
            return TargetedMSManager.getRun(_runId);
        }
        catch (FileNotFoundException fnfe)
        {
            logError("Skyline document import failed due to a missing file.", fnfe);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw fnfe;
        }
        catch (DataFormatException | IOException | XMLStreamException | RuntimeException | PipelineJobException e)
        {
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }
    }


    private void importSkylineDoc(TargetedMSRun run, File f) throws XMLStreamException, IOException, DataFormatException, PipelineJobException
    {
        // TODO - Consider if this is too big to fit in a single transaction. If so, need to blow away all existing
        // data for this run before restarting the import in the case of a retry

        NetworkDrive.ensureDrive(f.getPath());
        f = extractIfZip(f);

        try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction();
             SkylineDocumentParser parser = new SkylineDocumentParser(f, _log))
        {
                run.setFormatVersion(parser.getFormatVersion());
                run.setSoftwareVersion(parser.getSoftwareVersion());

                new SqlExecutor(TargetedMSManager.getSchema()).execute("UPDATE " + TargetedMSManager.getTableInfoRuns() +
                        " SET formatVersion = ?, softwareVersion = ? WHERE Id = ?",
                        parser.getFormatVersion(), parser.getSoftwareVersion(), run.getId());

                ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
                parser.readSettings();

                // Store the document settings
                // 0. iRT information
                run.setiRTscaleId(insertiRTData(parser));


                // 1. Transition settings
                TransitionSettings transSettings = parser.getTransitionSettings();
                TransitionSettings.InstrumentSettings instrumentSettings = transSettings.getInstrumentSettings();
                instrumentSettings.setRunId(_runId);
                Table.insert(_user, TargetedMSManager.getTableInfoTransInstrumentSettings(), instrumentSettings);

                boolean insertCEOptmizations = false;
                boolean insertDPOptmizations = false;

                TransitionSettings.PredictionSettings predictionSettings = transSettings.getPredictionSettings();
                predictionSettings.setRunId(_runId);
                TransitionSettings.Predictor cePredictor = predictionSettings.getCePredictor();
                if (cePredictor != null)
                {
                    insertCEOptmizations = predictionSettings.getOptimizeBy() != null && !"none".equalsIgnoreCase(predictionSettings.getOptimizeBy());
                    List<TransitionSettings.PredictorSettings> predictorSettingsList = cePredictor.getSettings();
                    cePredictor = Table.insert(_user, TargetedMSManager.getTableInfoPredictor(), cePredictor);
                    predictionSettings.setCePredictorId(cePredictor.getId());
                    for (TransitionSettings.PredictorSettings s : predictorSettingsList)
                    {
                        s.setPredictorId(cePredictor.getId());
                        Table.insert(_user, TargetedMSManager.getTableInfoPredictorSettings(), s);
                    }
                }
                TransitionSettings.Predictor dpPredictor = predictionSettings.getDpPredictor();
                if (dpPredictor != null)
                {
                    insertDPOptmizations = predictionSettings.getOptimizeBy() != null && !"none".equalsIgnoreCase(predictionSettings.getOptimizeBy());
                    List<TransitionSettings.PredictorSettings> predictorSettingsList = dpPredictor.getSettings();
                    dpPredictor = Table.insert(_user, TargetedMSManager.getTableInfoPredictor(), dpPredictor);
                    predictionSettings.setDpPredictorId(dpPredictor.getId());
                    for (TransitionSettings.PredictorSettings s : predictorSettingsList)
                    {
                        s.setPredictorId(cePredictor.getId());
                        Table.insert(_user, TargetedMSManager.getTableInfoPredictorSettings(), s);
                    }
                }
                Table.insert(_user, TargetedMSManager.getTableInfoTransitionPredictionSettings(), predictionSettings);

                TransitionSettings.FullScanSettings fullScanSettings = transSettings.getFullScanSettings();
                if (fullScanSettings != null)
                {
                    fullScanSettings.setRunId(_runId);
                    fullScanSettings = Table.insert(_user, TargetedMSManager.getTableInfoTransitionFullScanSettings(), fullScanSettings);

                    for (TransitionSettings.IsotopeEnrichment isotopeEnrichment : fullScanSettings.getIsotopeEnrichmentList())
                    {
                        isotopeEnrichment.setRunId(_runId);
                        Table.insert(_user, TargetedMSManager.getTableInfoIsotopeEnrichment(), isotopeEnrichment);
                    }

                    TransitionSettings.IsolationScheme isolationScheme = fullScanSettings.getIsolationScheme();
                    if(isolationScheme != null)
                    {
                        isolationScheme.setRunId(_runId);
                        Table.insert(_user, TargetedMSManager.getTableInfoIsolationScheme(), isolationScheme);
                        for(TransitionSettings.IsolationWindow iWindow: isolationScheme.getIsolationWindowList())
                        {
                            iWindow.setIsolationSchemeId(isolationScheme.getId());
                            Table.insert(_user, TargetedMSManager.getTableInfoIsolationWindow(), iWindow);
                        }
                    }
                }

                // 2. Replicates and sample files
                Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap = new HashMap<>();
                Map<Instrument, Integer> instrumentIdMap = new HashMap<>();

                TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(run.getContainer());
                Set<URI> files = new HashSet<>();
                SampleFile srcFile;

                for(SkylineReplicate skyReplicate: parser.getReplicates())
                {
                    Replicate replicate = new Replicate();
                    replicate.setName(skyReplicate.getName());
                    replicate.setSampleFileList(skyReplicate.getSampleFileList());
                    replicate.setAnnotations(skyReplicate.getAnnotations());
                    replicate.setSampleType(skyReplicate.getSampleType());
                    replicate.setAnalyteConcentration(skyReplicate.getAnalyteConcentration());

                    replicate.setRunId(_runId);
                    if(cePredictor != null && skyReplicate.getCePredictor() != null && cePredictor.equals(skyReplicate.getCePredictor()))
                    {
                        replicate.setCePredictorId(cePredictor.getId());
                    }
                    if(dpPredictor != null && skyReplicate.getDpPredictor() != null && dpPredictor.equals(skyReplicate.getDpPredictor()))
                    {
                        replicate.setDpPredictorId(dpPredictor.getId());
                    }

                    if (folderType == TargetedMSModule.FolderType.QC)
                    {
                        // In QC folders insert a replicate only if at least one of the associated sample files will be inserted
                        for (SampleFile sampleFile : replicate.getSampleFileList())
                        {
                            // It's possible that a data file is referenced in multiple replicates, so handle that case
                            for (SampleFile existingSample : TargetedMSManager.getSampleFile(sampleFile.getFilePath(), sampleFile.getAcquiredTime(), run.getContainer()))
                            {
                                Replicate existingReplicate = TargetedMSManager.getReplicate(existingSample.getReplicateId(), run.getContainer());
                                if (existingReplicate.getRunId() != run.getId())
                                {
                                    srcFile = TargetedMSManager.deleteSampleFileAndDependencies(existingSample.getId());
                                    _log.info("Updating previously imported data for sample file " + sampleFile.getFilePath() + " in QC folder.");

                                    if (null != srcFile && !srcFile.getFilePath().isEmpty())
                                    {
                                        try
                                        {
                                            files.add(new URI(srcFile.getFilePath()));
                                        }
                                        catch (URISyntaxException e)
                                        {
                                            _log.error("Unable to delete file " + srcFile.getFilePath() + ". May be an invalid path. This file is no longer needed on the server.");
                                        }
                                    }
                                }
                            }
                        }
                    }

                    replicate = Table.insert(_user, TargetedMSManager.getTableInfoReplicate(), replicate);

                    // special case for the ignore_in_QC annotation in QC folders
                    ReplicateAnnotation ignoreInQcAnnot = null;

                    for (ReplicateAnnotation annotation : replicate.getAnnotations())
                    {
                        annotation.setReplicateId(replicate.getId());
                        annotation.setSource(ReplicateAnnotation.SOURCE_SKYLINE);
                        Table.insert(_user, TargetedMSManager.getTableInfoReplicateAnnotation(), annotation);

                        if (annotation.isIgnoreInQC() && folderType == TargetedMSModule.FolderType.QC)
                            ignoreInQcAnnot = annotation;
                    }

                    handleReplicateExclusions(replicate, ignoreInQcAnnot);

                    insertSampleFiles(skylineIdSampleFileIdMap, instrumentIdMap, replicate);
                }

                // 3. Peptide settings
                Map<String, Integer> isotopeLabelIdMap = new HashMap<>();
                Set<Integer> internalStandardLabelIds = new HashSet<>();
                Map<String, Integer> structuralModNameIdMap = new HashMap<>();
                Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap = new HashMap<>();
                Map<String, Integer> isotopeModNameIdMap = new HashMap<>();
                PeptideSettings pepSettings = parser.getPeptideSettings();
                PeptideSettings.PeptideModifications modifications = pepSettings.getModifications();
                if(modifications != null)
                {
                    // Insert isotope labels
                    List<PeptideSettings.IsotopeLabel> isotopeLabels = modifications.getIsotopeLabels();
                    for(PeptideSettings.IsotopeLabel isotopeLabel: isotopeLabels)
                    {
                        isotopeLabel.setRunId(_runId);
                        isotopeLabel = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeLabel(), isotopeLabel);
                        isotopeLabelIdMap.put(isotopeLabel.getName(), isotopeLabel.getId());

                        if(isotopeLabel.isStandard())
                        {
                            internalStandardLabelIds.add(isotopeLabel.getId());
                        }
                    }

                    // Insert peptide modification settings
                    PeptideSettings.ModificationSettings modSettings = modifications.getModificationSettings();
                    if(modSettings != null)
                    {
                        modSettings.setRunId(_runId);
                        Table.insert(_user, TargetedMSManager.getTableInfoModificationSettings(), modSettings);
                    }

                    // Insert structural modifications
                    List<PeptideSettings.RunStructuralModification> structuralMods = modifications.getStructuralModifications();
                    for(PeptideSettings.RunStructuralModification mod: structuralMods)
                    {
                        PeptideSettings.StructuralModification existingMod = findExistingStructuralModification(mod);
                        if (existingMod != null)
                        {
                            mod.setStructuralModId(existingMod.getId());
                            structuralModNameIdMap.put(mod.getName(), existingMod.getId());
                        }
                        else
                        {
                            mod = Table.insert(_user, TargetedMSManager.getTableInfoStructuralModification(), mod);
                            mod.setStructuralModId(mod.getId());
                            structuralModNameIdMap.put(mod.getName(), mod.getId());

                            for (PeptideSettings.PotentialLoss potentialLoss : mod.getPotentialLosses())
                            {
                                potentialLoss.setStructuralModId(mod.getId());
                                Table.insert(_user, TargetedMSManager.getTableInfoStructuralModLoss(), potentialLoss);
                            }
                        }

                        mod.setRunId(_runId);
                        Table.insert(_user, TargetedMSManager.getTableInfoRunStructuralModification(), mod);
                    }

                    // Insert isotope modifications
                    List<PeptideSettings.RunIsotopeModification> isotopeMods = modifications.getIsotopeModifications();
                    for(PeptideSettings.RunIsotopeModification mod: isotopeMods)
                    {
                        PeptideSettings.IsotopeModification existingIsotopeMod = findExistingIsotopeModification(mod);
                        if (existingIsotopeMod != null)
                        {
                            mod.setIsotopeModId(existingIsotopeMod.getId());
                        }
                        else
                        {
                            PeptideSettings.IsotopeModification newMod = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeModification(), mod);
                            mod.setIsotopeModId(newMod.getId());
                        }

                        isotopeModNameIdMap.put(mod.getName(), mod.getIsotopeModId());
                        mod.setRunId(_runId);
                        mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
                        Table.insert(_user, TargetedMSManager.getTableInfoRunIsotopeModification(), mod);
                    }
                }

                // Spectrum library settings
                Map<String, Integer> librarySourceTypes = LibraryManager.getLibrarySourceTypes();
                Map<String, Integer> libraryNameIdMap = new HashMap<>();
                PeptideSettings.SpectrumLibrarySettings librarySettings = pepSettings.getLibrarySettings();
                if(librarySettings != null)
                {
                    librarySettings.setRunId(_runId);
                    Table.insert(_user, TargetedMSManager.getTableInfoLibrarySettings(), librarySettings);

                    for(PeptideSettings.SpectrumLibrary library: librarySettings.getLibraries())
                    {
                        library.setRunId(_runId);
                        if(library.getLibraryType().equals("bibliospec_lite") ||
                           library.getLibraryType().equals("bibliospec"))
                        {
                            library.setLibrarySourceId(librarySourceTypes.get("BiblioSpec"));
                        }
                        else if(library.getLibraryType().equals("hunter"))
                        {
                            library.setLibrarySourceId(librarySourceTypes.get("GPM"));
                        }
                        else if(library.getLibraryType().equals("nist") ||
                                library.getLibraryType().equals("spectrast"))
                        {
                            library.setLibrarySourceId(librarySourceTypes.get("NIST"));
                        }
                        library = Table.insert(_user, TargetedMSManager.getTableInfoSpectrumLibrary(), library);
                        libraryNameIdMap.put(library.getName(), library.getId());
                    }
                }

                PeptideSettings.Enzyme enzyme = pepSettings.getEnzyme();
                if (enzyme != null)
                {
                    SimpleFilter filter = new SimpleFilter();
                    filter.addCondition(FieldKey.fromParts("cut"), enzyme.getCut(), (enzyme.getCut() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("nocut"), enzyme.getNoCut(), (enzyme.getNoCut() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("sense"), enzyme.getSense(), (enzyme.getSense() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("name"), enzyme.getName());
                    filter.addCondition(FieldKey.fromParts("cutC"), enzyme.getCutC(), (enzyme.getCutC() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("noCutC"), enzyme.getNoCutC(), (enzyme.getNoCutC() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("cutN"), enzyme.getCutN(), (enzyme.getCutN() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    filter.addCondition(FieldKey.fromParts("noCutN"), enzyme.getNoCutN(), (enzyme.getNoCutN() == null ? CompareType.ISBLANK : CompareType.EQUAL));
                    PeptideSettings.Enzyme[] existingEnzymes = new TableSelector(TargetedMSManager.getTableInfoEnzyme(), filter, null).getArray(PeptideSettings.Enzyme.class);
                    if (existingEnzymes == null || existingEnzymes.length == 0)
                    {
                        enzyme = Table.insert(_user, TargetedMSManager.getTableInfoEnzyme(), enzyme);
                    }
                    else
                    {
                        enzyme = existingEnzymes[0];
                    }

                    PeptideSettings.EnzymeDigestionSettings digestSettings = pepSettings.getEnzymeDigestionSettings();
                    if (digestSettings == null)
                    {
                        digestSettings = new PeptideSettings.EnzymeDigestionSettings();
                    }
                    digestSettings.setRunId(_runId);
                    digestSettings.setEnzymeId(enzyme.getId());
                    Table.insert(_user, TargetedMSManager.getTableInfoRunEnzyme(), digestSettings);
                }

                // Peptide prediction settings
                PeptideSettings.PeptidePredictionSettings peptidePredictionSettings = pepSettings.getPeptidePredictionSettings();
                PeptideSettings.RetentionTimePredictionSettings rtPredictionSettings = peptidePredictionSettings == null ? null : peptidePredictionSettings.getRtPredictionSettings();
                if(rtPredictionSettings != null)
                {
                    rtPredictionSettings.setRunId(_runId);
                    Table.insert(_user, TargetedMSManager.getTableInfoRetentionTimePredictionSettings(), rtPredictionSettings);
                }

                // Drift time prediction settings.
                PeptideSettings.DriftTimePredictionSettings dtPredictionSettings = peptidePredictionSettings == null ? null : peptidePredictionSettings.getDtPredictionSettings();
                if(dtPredictionSettings != null)
                {
                    dtPredictionSettings.setRunId(_runId);
                    Table.insert(_user, TargetedMSManager.getTableInfoDriftTimePredictionSettings(), dtPredictionSettings);

                    List<PeptideSettings.MeasuredDriftTime> driftTimes = dtPredictionSettings.getMeasuredDriftTimes();
                    for(PeptideSettings.MeasuredDriftTime dt: driftTimes)
                    {
                        dt.setDriftTimePredictionSettingsId(dtPredictionSettings.getId());
                        Table.insert(_user, TargetedMSManager.getTableInfoMeasuredDriftTime(), dt);
                    }
                }

                // Data settings -- these are the annotation settings
                DataSettings dataSettings = parser.getDataSettings();
                for(AnnotationSetting annotSetting: dataSettings.getAnnotationSettings())
                {
                    annotSetting.setRunId(_runId);
                    Table.insert(_user, TargetedMSManager.getTableInfoAnnotationSettings(), annotSetting);
                }

                // TODO: bulk insert of precursor, transition, chrom info etc.

                // Store the data
                // 1. peptide group
                if(_isProteinLibraryDoc)
                {
                    _libProteinSequenceIds = new HashSet<>();
                    _libProteinLabels = new HashSet<>();
                }
                if(_isPeptideLibraryDoc)
                {
                    _libPrecursors = new HashSet<>();
                }
                int peptideGroupCount = 0;
                Set<String> peptides = new TreeSet<>();
                Set<String> smallMolecules = new TreeSet<>();

                while (parser.hasNextPeptideGroup())
                {
                    PeptideGroup pepGroup = parser.nextPeptideGroup();
                    insertPeptideGroup(proteinService, insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap,
                            isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap,
                            isotopeModNameIdMap, libraryNameIdMap, pepGroup, parser, peptides, smallMolecules);
                    if (++peptideGroupCount % 100 == 0)
                    {
                        _log.info("Imported " + peptideGroupCount + " peptide groups.");
                    }
                }
            QuantificationSettings quantificationSettings = pepSettings.getQuantificationSettings();
            if (quantificationSettings != null) {
                quantificationSettings.setRunId(_runId);
                Table.insert(_user, TargetedMSManager.getTableInfoQuantificationSettings(), quantificationSettings);
            }

                if (folderType == TargetedMSModule.FolderType.QC)
                {
                    Set<String> expectedPeptides = TargetedMSManager.getDistinctPeptides(_container);
                    Set<String> expectedMolecules = TargetedMSManager.getDistinctMolecules(_container);

                    if (!expectedMolecules.isEmpty() || !expectedPeptides.isEmpty())
                    {
                        if (!expectedPeptides.equals(peptides))
                        {
                            throw new PipelineJobException("QC folders require that all documents use the same peptides, but they did not match. Please create a new QC folder if you wish to work with different peptides. Attempted import: " + peptides + ". Previously imported: " + expectedPeptides + ".");
                        }
                        if (!expectedMolecules.equals(smallMolecules))
                        {
                            throw new PipelineJobException("QC folders require that all documents use the same molecule, but they did not match. Please create a new QC folder if you wish to work with different molecules. Attempted import: " + smallMolecules + ". Previously imported: " + expectedMolecules + ".");
                        }
                    }
                }

            run.setPeptideGroupCount(parser.getPeptideGroupCount());
            run.setPeptideCount(parser.getPeptideCount());
            run.setSmallMoleculeCount(parser.getSmallMoleculeCount());
            run.setPrecursorCount(parser.getPrecursorCount());
            run.setTransitionCount(parser.getTransitionCount());
            run.setReplicateCount(parser.getReplicateCount());

            run.setDocumentGUID(parser.getDocumentGUID());
            Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

            List<GroupComparisonSettings> groupComparisons = new ArrayList<>(dataSettings.getGroupComparisons());
            for (GroupComparisonSettings groupComparison : groupComparisons) {
                groupComparison.setRunId(_runId);
                Table.insert(_user, TargetedMSManager.getTableInfoGroupComparisonSettings(), groupComparison);
            }

                if (run.isRepresentative())
                {
                    resolveRepresentativeData(run);
                }

            quantifyRun(run, quantificationSettings, groupComparisons);

            if (folderType == TargetedMSModule.FolderType.QC)
            {
                TargetedMSManager.purgeUnreferencedReplicates(_container);
                List<String> msgs = TargetedMSManager.purgeUnreferencedFiles(files, _container, _user);
                for (String msg : msgs)
                {
                    _log.info(msg);
                }
            }

            if (_pipeRoot.isCloudRoot())
                copyBlibsToCloud();
            transaction.commit();
        }
        finally
        {
            // TODO: We are not deleting the directory so that we can query any Bibliospec spectrum library
            //       files contained in the zip file.
            //       Think about adding tables either in the TargetedMS schema or another LabKey schema
            //       to store spectrum libraries.
            // If we uncompressed the contents of a zip file to a directory, delete the directory.
//            if(zipDir != null)
//            {
//                FileUtil.deleteDir(zipDir);
//            }
        }
    }

    private void handleReplicateExclusions(Replicate replicate, ReplicateAnnotation ignoreInQcAnnot)
    {
        // keep any existing exclusions for this replicate by name
        List<QCMetricExclusion> existingExclusions = ReplicateManager.getReplicateExclusions(replicate.getName(), _container);
        boolean hasExistingExcludeAllMetrics = false;
        for (QCMetricExclusion existingExclusion : existingExclusions)
        {
            ReplicateManager.insertReplicateExclusion(_user, replicate.getId(), existingExclusion.getMetricId());
            if (existingExclusion.getMetricId() == null)
                hasExistingExcludeAllMetrics = true;
        }

        // handle insert or check based on an ignore_in_QC annotation for this replicate
        if (ignoreInQcAnnot != null)
        {
            boolean shouldExcludeFromAnnot = "true".equalsIgnoreCase(ignoreInQcAnnot.getValue());

            if (existingExclusions.isEmpty())
            {
                // If there is an ignore_in_QC annotation for this replicate and we don't currently have any
                // existing exclusions, insert it into the targetedms.QCMetricExclusion table.
                if (shouldExcludeFromAnnot)
                    ReplicateManager.insertReplicateExclusion(_user, replicate.getId(), null);
            }
            else
            {
                // If there is an ignore_in_QC annotation and we already have existing exclusions, don't insert but compare and
                // give a warning if there is a mismatch.
                if (!shouldExcludeFromAnnot)
                    _log.warn("Replicate " + replicate.getName() + " has an ignore_in_QC=false annotation but there are existing exclusions that were added within Panorama or from a previous import.");
                else if (!hasExistingExcludeAllMetrics)
                    _log.warn("Replicate " + replicate.getName() + " has an ignore_in_QC=true annotation but there are existing metric specific exclusions that were added within Panorama.");
            }
        }
    }

    private File extractIfZip(File f) throws IOException
    {
        String ext = FileUtil.getExtension(f.getName());
        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(ext))
        {
            File zipDir = new File(f.getParent(), SkylineFileUtils.getBaseName(f.getName()));
            _blibSourceDir = zipDir;
            List<File> files = ZipUtil.unzipToDirectory(f, zipDir, _log);
            File skyFile = null;
            for(File file: files)
            {
                ext = FileUtil.getExtension(file.getName());
                if(SkylineFileUtils.EXT_SKY.equalsIgnoreCase(ext) && null == skyFile)
                {
                    skyFile = file;
                }
                else if (SkylineFileUtils.EXT_BLIB.equalsIgnoreCase(ext))
                {
                    _blibSourcePaths.add(file.toPath());
                }
            }

            if(skyFile == null)
            {
                throw new IOException("zip file " + f + " does not contain a .sky file");
            }
            f = skyFile;
        }
        return f;
    }

    /**
     * Insert new iRT scale/peptides, or update existing scale for library folders
     *
     * @return The existing or new iRT Scale Id for the imported iRT Peptide set. null if no iRT Data for this run.
     * @throws PipelineJobException
     */
    private @Nullable Integer insertiRTData(SkylineDocumentParser parser) throws PipelineJobException
    {

        Integer iRTScaleId = null; // Not all imports are expected to have iRT data
        List<IrtPeptide> importScale = parser.getiRTScaleSettings();

        if (! importScale.isEmpty())
        {
            boolean newScale = false;
            List<Integer> scaleIds = TargetedMSManager.getIrtScaleIds(_container);

            // Experiment folders get a new scale for every imported run
            // Library folders have a single scale which gets updated with a weighted average of observed values on each import.
            if (scaleIds.isEmpty() || TargetedMSManager.getFolderType(_container) == TargetedMSModule.FolderType.Experiment)
                newScale = true;
            else
            {
                iRTScaleId = scaleIds.get(0);
                SimpleFilter iRTFilter = new SimpleFilter(FieldKey.fromParts("iRTScaleId"), iRTScaleId);
                ArrayList<IrtPeptide> existingScale = new TableSelector(TargetedMSManager.getTableInfoiRTPeptide(), iRTFilter, null).getArrayList(IrtPeptide.class);

                List<IrtPeptide> updatedPeptides = normalizeIrtImportAndReweighValues(existingScale, importScale);

                // Write the new weighted average values
                for (IrtPeptide peptide : updatedPeptides)
                {
                    Table.update(_user, TargetedMSManager.getTableInfoiRTPeptide(), peptide, peptide.getId());
                }
            }

            if (newScale)
            {
                // first insert the iRTScale and get the Id
                Map<String, Object> iRTScaleRow = new CaseInsensitiveHashMap<>();
                iRTScaleRow.put("container", _container);
                Map<String, Object> iRTScaleResult = Table.insert(_user, TargetedMSManager.getTableInfoiRTScale(), iRTScaleRow);
                iRTScaleId = (int) iRTScaleResult.get("id");
            }

            // insert any new iRT Peptides into the database
            for (IrtPeptide peptide : importScale)
            {
                if (!newScale)
                    peptide.setiRTStandard(false);
                peptide.setiRTScaleId(iRTScaleId);
                Table.insert(_user, TargetedMSManager.getTableInfoiRTPeptide(), peptide);
            }
        }
        return iRTScaleId;
    }

    /**
     * Verify the standards of the imported scale match the existing standards, and determine which of the import peptides are new
     * to be inserted vs. updates to existing values.
     * @return The list of existing peptides which have recalculated weighted average values.
     * @throws PipelineJobException
     */
    private ArrayList<IrtPeptide> normalizeIrtImportAndReweighValues(ArrayList<IrtPeptide> existingScale, List<IrtPeptide> importScale) throws PipelineJobException
    {
        Map<String, IrtPeptide> existingStandards = new LinkedHashMap<>();
        Map<String, IrtPeptide> existingLibrary = new LinkedHashMap<>(existingScale.size());

        separateIrtScale(existingScale, existingStandards, existingLibrary);

        IRegressionFunction regressionLine = IrtRegressionCalculator.calcRegressionLine(new RetentionTimeProviderImpl(importScale), new ArrayList<>(existingStandards.values()), new ArrayList<>(existingLibrary.values()), _log);

        if (regressionLine == null)
            throw new PipelineJobException(makeIrtStandardsErrorMsg(existingScale.get(0).getiRTScaleId(), new ArrayList<>(existingStandards.values()), importScale));

        applyIrtRegressionLine(regressionLine, importScale);


        // For each of the imported peptides, determine if it is new to be inserted, or already exists to have a new weighted average value calculated
        ArrayList<IrtPeptide> reweighedPeptides = new ArrayList<>();
        Iterator<IrtPeptide> iter = importScale.iterator();
        while (iter.hasNext())
        {
            IrtPeptide imported = iter.next();
            if (existingStandards.containsKey(imported.getModifiedSequence()))
                iter.remove(); // never touch standards after the initial import
            else
            {
                IrtPeptide entryToUpdate = existingLibrary.get(imported.getModifiedSequence());
                if (entryToUpdate != null)
                {
                    entryToUpdate.reweighValue(imported.getiRTValue());
                    reweighedPeptides.add(entryToUpdate);
                    iter.remove(); // Take it out of the import list
                }
            }

        }

        return reweighedPeptides;
    }

    private void applyIrtRegressionLine(IRegressionFunction regressionLine, List<IrtPeptide> importScale)
    {
        for (IrtPeptide importPeptide : importScale)
        {
            importPeptide.setiRTValue(regressionLine.GetY(importPeptide.getiRTValue()));
        }
    }

    private String makeIrtStandardsErrorMsg(int scaleId, List<IrtPeptide> existingStandards, List<IrtPeptide> importScale)
    {
        StringBuilder sb = new StringBuilder();
        Map<String, Double> importStandards = new LinkedHashMap<>();
        RetentionTimeProviderImpl rtp = new RetentionTimeProviderImpl(importScale);
        sb.append("Unable to find sufficient correlation in standard or shared library peptides (iRT Scale Id = " + scaleId + ") for this folder: " + _container.getPath() +"\n");
        sb.append("Standards:\n");
        int i = 0;
        for (IrtPeptide pep : existingStandards)
        {
            sb.append(++i + ")  " + pep.getModifiedSequence() + "\t" + pep.getiRTValue() + "\n");
            importStandards.put(pep.getModifiedSequence(), rtp.GetRetentionTime(pep.getModifiedSequence()));
        }
        i = 0;
        sb.append("Standards in import:\n"); // The library standards the import had, not what's flagged as standard
        for (Map.Entry<String, Double> pep : importStandards.entrySet())
        {
            if (pep.getValue() != null)
            {
                sb.append(++i + ")  " + pep.getKey() + "\t" + pep.getValue());
                sb.append("\n");
            }
            if (i == 0) // Import didn't contain any of the standards for the library
                sb.append("NONE\n");

        }
        return sb.toString();
    }

    /**
     * Separate list of peptides from iRT scale into map of standards and map of library peptides
     */
    private void separateIrtScale(List<IrtPeptide> scale, Map<String, IrtPeptide> standards, Map<String, IrtPeptide> library)
    {
        for (IrtPeptide peptide : scale)
        {
            if (peptide.isiRTStandard())
                standards.put(peptide.getModifiedSequence(), peptide);
            else library.put(peptide.getModifiedSequence(), peptide);
        }
    }

    // Skyline documents may contain multiple <instrument_info> elements that contain
    // values of different analyzers or detectors in the same instrument. Combine all the
    // values into a single instrument object.
    private Instrument combineInstrumentInfos(List<Instrument> instrumentInfoList)
    {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        List<String> ionizationTypes = new ArrayList<>();
        List<String> analyzers = new ArrayList<>();
        List<String> detectors = new ArrayList<>();

        for(Instrument instrumentInfo: instrumentInfoList)
        {
            String model = instrumentInfo.getModel();
            if (StringUtils.isNotBlank(model))
                models.add(model);

            String ionizationType = instrumentInfo.getIonizationType();
            if (StringUtils.isNotBlank(ionizationType))
                ionizationTypes.add(ionizationType);

            String analyzer = instrumentInfo.getAnalyzer();
            if (StringUtils.isNotBlank(analyzer))
                analyzers.add(analyzer);

            String detector = instrumentInfo.getDetector();
            if (StringUtils.isNotBlank(detector))
                detectors.add(detector);
        }

        Instrument instrument = new Instrument();
        instrument.setModel(StringUtils.join(models, ","));
        instrument.setIonizationType(StringUtils.join(ionizationTypes, ","));
        instrument.setAnalyzer(StringUtils.join(analyzers, ","));
        instrument.setDetector(StringUtils.join(detectors, ","));

        return instrument;
    }


    private void resolveRepresentativeData(TargetedMSRun run)
    {
        RepresentativeStateManager.setRepresentativeState(_user, _container, _localDirectory, run, run.getRepresentativeDataState());
    }

    private void insertPeptideGroup(ProteinService proteinService, boolean insertCEOptmizations,
                                    boolean insertDPOptmizations, Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                    Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds,
                                    Map<String, Integer> structuralModNameIdMap, Map<Integer,
            PeptideSettings.PotentialLoss[]> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap,
                                    Map<String, Integer> libraryNameIdMap, PeptideGroup pepGroup, SkylineDocumentParser parser, Set<String> peptides, Set<String> smallMolecules)
            throws XMLStreamException, IOException, DataFormatException, PipelineJobException
    {
        pepGroup.setRunId(_runId);

        String pepGrpLabel = pepGroup.getLabel();
        if(!StringUtils.isBlank(pepGrpLabel))
        {
            if(pepGroup.isProtein())
            {
                Map<String, Set<String>> identifierMap = getIdentifiers(proteinService, pepGroup);

                // prot.sequences table limits the name to 50 characters
                String protName = pepGrpLabel.length() > 50 ? pepGrpLabel.substring(0, 50) : pepGrpLabel;
                int seqId = proteinService.ensureProtein(pepGroup.getSequence(), null, protName, pepGroup.getDescription());

                pepGroup.setSequenceId(seqId);

                proteinService.ensureIdentifiers(seqId, identifierMap);
            }

            // targetedms.peptidegroup table limits the label and name to 255 characters.
            // Truncate to 255 characters after we have parsed identifiers.
            pepGroup.setLabel(pepGrpLabel.substring(0, Math.min(255, pepGrpLabel.length())));
            String pepGrpName = pepGroup.getName();
            pepGrpName = pepGrpName != null ? pepGrpName.substring(0, Math.min(255, pepGrpName.length())) : null;
            pepGroup.setName(pepGrpName);
        }
        else
        {
            // Add a dummy label. The "label" column in targetedms.PeptideGroup is not nullable.
            pepGroup.setLabel("PEPTIDE_GROUP_" + blankLabelIndex++);
        }

        if(_isProteinLibraryDoc)
        {
            if(pepGroup.getSequenceId() != null)
            {
                if(_libProteinSequenceIds.contains(pepGroup.getSequenceId()))
                {
                    throw new PanoramaBadDataException("Duplicate protein sequence found: "+pepGroup.getLabel()+", seqId "+pepGroup.getSequenceId()
                    + ". Documents uploaded to a protein library folder should contain unique proteins.");
                }
                else if(!pepGroup.isDecoy())
                {
                    _libProteinSequenceIds.add(pepGroup.getSequenceId());
                }
            }
            if(_libProteinLabels.contains(pepGroup.getLabel()))
            {
                throw new PanoramaBadDataException("Duplicate protein label found: "+pepGroup.getLabel()
                + ". Documents uploaded to a protein library folder should contain unique proteins.");
            }
            else if(!pepGroup.isDecoy())
            {
                _libProteinLabels.add(pepGroup.getLabel());
            }
        }

        // CONSIDER: If there is already an identical entry in the PeptideGroup table re-use it.
        _log.info("Inserting " + pepGroup.getLabel());
        pepGroup = Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroup(), pepGroup);

        for (PeptideGroupAnnotation annotation : pepGroup.getAnnotations())
        {
            annotation.setPeptideGroupId(pepGroup.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroupAnnotation(), annotation);
        }

        // Read peptides for this protein
        SkylineDocumentParser.MoleculeType molType;
        // Issue 24571: Keep track of the peptides in this protein if this is document is being uploaded to a protein library folder.
        Set<String> libProteinPeptides = new HashSet<>();
        int count = 1;

        while((molType = parser.hasNextPeptideOrMolecule()) != null)
        {
            GeneralMolecule generalMolecule = null;
            switch(molType)
            {
                case PEPTIDE:
                    Peptide peptide = parser.nextPeptide();
                    peptides.add(peptide.getSequence());
                    generalMolecule = peptide;
                    if(_isProteinLibraryDoc)
                    {
                        Peptide p = (Peptide) generalMolecule;
                        // Issue 24571: Proteins in protein library folders should not have duplicate peptides.
                        if (libProteinPeptides.contains(p.getPeptideModifiedSequence()))
                        {
                            throw new PanoramaBadDataException("Duplicate peptide ("+ p.getPeptideModifiedSequence() + ") found for protein " + pepGroup.getLabel()
                                    + ". Proteins in documents uploaded to a protein library folder should contain unique peptides.");
                        }
                        else
                        {
                            libProteinPeptides.add(p.getPeptideModifiedSequence());
                        }
                    }
                    break;
                case MOLECULE:
                    Molecule molecule = parser.nextMolecule();
                    if (molecule.getIonFormula() != null)
                    {
                        // Some molecules only have a mass, no formula. Just omit them from the check.
                        smallMolecules.add(molecule.getIonFormula());
                    }
                    generalMolecule = molecule;
                    break;
            }

            insertPeptideOrSmallMolecule(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap,
                    internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, isotopeModNameIdMap,
                    libraryNameIdMap, pepGroup, generalMolecule);
        }
    }

    private static final String SKYLINE_IDENT_TYPE = "Skyline";
    private Map<String, Set<String>> getIdentifiers(ProteinService proteinService, PeptideGroup pepGroup)
    {
        String label = StringUtils.trimToNull(pepGroup.getLabel());
        String name = StringUtils.trimToNull(pepGroup.getName());
        String description = pepGroup.getDescription();

        Map<String, Set<String>> identifierMap;
        if(description != null && description.startsWith("IPI") && description.contains("|"))
        {
            // org.labkey.ms2.protein.fasta.Protein.identParse() parses the fastaIdentifierString
            // and wholeHeader differently
            // Here is an example from a Skyline document where all the identifiers are in the description
            //   <protein name="ABAT.IPI00009532" description="IPI:IPI00009532.5|SWISS-PROT:P80404|TREMBL:B7Z1V4|ENSEMBL:ENSP00000268251;ENSP00000379845;ENSP00000411916|REFSEQ:NP_000654;NP_001120920;NP_065737|H-INV:HIT000272519|VEGA:OTTHUMP00000045876;OTTHUMP00000080085;OTTHUMP00000080086 Tax_Id=9606 Gene_Symbol=ABAT cDNA FLJ56034, highly similar to 4-aminobutyrate aminotransferase, mitochondrial">
            // In this case we want to parse the description as fastaIdentifierString
            identifierMap = proteinService.getIdentifiers(null, name, label, description);
        }
        else
        {
            identifierMap = proteinService.getIdentifiers(description, name, label);
        }

        Set<String> skylineIdentifiers = new HashSet<>();
        if(label != null)
        {
            // prot.identifers table limits the name to 50 characters
            skylineIdentifiers.add(label.substring(0, Math.min(50, label.length())));
        }
        if(name != null)
        {
            skylineIdentifiers.add(name.substring(0, Math.min(50, name.length())));
        }

        identifierMap.put(SKYLINE_IDENT_TYPE, skylineIdentifiers);

        return identifierMap;
    }

    private void insertPeptideOrSmallMolecule(boolean insertCEOptmizations, boolean insertDPOptmizations,
                               Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap,
                               Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer,
                               PeptideSettings.PotentialLoss[]> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap, Map<String,
                               Integer> libraryNameIdMap, PeptideGroup pepGroup, GeneralMolecule generalMolecule)
    {
        Peptide peptide = null;

        generalMolecule.setPeptideGroupId(pepGroup.getId());

        Table.insert(_user, TargetedMSManager.getTableInfoGeneralMolecule(), generalMolecule);

        // If the peptide modified sequence has not been set, use the light precursor sequence
        if (generalMolecule instanceof Peptide)
        {
            peptide = (Peptide) generalMolecule;

            if(peptide.getPeptideModifiedSequence() == null)
            {
                for (Precursor precursor : peptide.getPrecursorList())
                {
                    if (PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(precursor.getIsotopeLabel()))
                        peptide.setPeptideModifiedSequence(precursor.getModifiedSequence());
                }
            }
            Table.insert(_user, TargetedMSManager.getTableInfoPeptide(), peptide);
        }
        else if (generalMolecule instanceof Molecule)
        {
            Molecule molecule = (Molecule) generalMolecule;
            molecule.setId(generalMolecule.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoMolecule(), molecule);

            Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap = insertGeneralMoleculeChromInfos(generalMolecule.getId(),
                    molecule.getGeneralMoleculeChromInfoList(), skylineIdSampleFileIdMap);

            for (MoleculePrecursor moleculePrecursor : molecule.getMoleculePrecursorsList())
            {
                insertMoleculePrecursor(molecule, moleculePrecursor, skylineIdSampleFileIdMap, isotopeLabelIdMap, sampleFileIdGeneralMolChromInfoIdMap);
            }
        }

        for (GeneralMoleculeAnnotation annotation : generalMolecule.getAnnotations())
        {
            annotation.setGeneralMoleculeId(generalMolecule.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoGeneralMoleculeAnnotation(), annotation);
        }

        if (peptide != null)
        {
            for (Peptide.StructuralModification mod : peptide.getStructuralMods())
            {
                int modId = structuralModNameIdMap.get(mod.getModificationName());
                mod.setStructuralModId(modId);
                mod.setPeptideId(peptide.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoPeptideStructuralModification(), mod);
            }

            for (Peptide.IsotopeModification mod : peptide.getIsotopeMods())
            {
                int modId = isotopeModNameIdMap.get(mod.getModificationName());
                mod.setIsotopeModId(modId);
                mod.setPeptideId(peptide.getId());
                mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
                Table.insert(_user, TargetedMSManager.getTableInfoPeptideIsotopeModification(), mod);
            }

            Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap = insertGeneralMoleculeChromInfos(generalMolecule.getId(),
                    peptide.getGeneralMoleculeChromInfoList(), skylineIdSampleFileIdMap);

            // 3. precursor
            for (Precursor precursor : peptide.getPrecursorList())
            {
                insertPrecursor(insertCEOptmizations, insertDPOptmizations,
                        skylineIdSampleFileIdMap,
                        isotopeLabelIdMap,
                        structuralModNameIdMap,
                        structuralModLossesMap,
                        libraryNameIdMap,
                        peptide,
                        sampleFileIdGeneralMolChromInfoIdMap,
                        precursor);
            }

            // 4. Calculate and insert peak area ratios
            PeakAreaRatioCalculator areaRatioCalculator = new PeakAreaRatioCalculator(peptide);
            areaRatioCalculator.init(skylineIdSampleFileIdMap);
            // Insert area ratios for each combination of 2 isotope labels
            for (Integer numLabelId : isotopeLabelIdMap.values())
            {
                for (Integer denomLabelId : isotopeLabelIdMap.values())
                {
                    if (!internalStandardLabelIds.contains(denomLabelId))
                        continue;

                    if (numLabelId.equals(denomLabelId))
                        continue;

                    for (SampleFile sampleFile : skylineIdSampleFileIdMap.values())
                    {
                        PeptideAreaRatio ratio = areaRatioCalculator.getPeptideAreaRatio(sampleFile.getId(), numLabelId, denomLabelId);
                        if (ratio != null)
                        {
                            Table.insert(_user, TargetedMSManager.getTableInfoPeptideAreaRatio(), ratio);
                        }

                        for (Precursor precursor : peptide.getPrecursorList())
                        {
                            if (precursor.getIsotopeLabelId() != numLabelId)
                                continue;

                            PrecursorAreaRatio pRatio = areaRatioCalculator.getPrecursorAreaRatio(sampleFile.getId(),
                                    precursor,
                                    numLabelId,
                                    denomLabelId);
                            if (pRatio != null)
                            {
                                Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAreaRatio(), pRatio);
                            }

                            for (Transition transition : precursor.getTransitionsList())
                            {
                                TransitionAreaRatio tRatio = areaRatioCalculator.getTransitionAreaRatio(sampleFile.getId(),
                                        precursor,
                                        transition,
                                        numLabelId,
                                        denomLabelId);
                                if (tRatio != null)
                                {
                                    Table.insert(_user, TargetedMSManager.getTableInfoTransitionAreaRatio(), tRatio);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void insertMoleculePrecursor(Molecule molecule, MoleculePrecursor moleculePrecursor,
                                         Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                         Map<String, Integer> isotopeLabelIdMap,
                                         Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap)
    {
        GeneralPrecursor gp = new GeneralPrecursor();
        gp.setGeneralMoleculeId(molecule.getId());
        gp.setMz(moleculePrecursor.getMz());
        gp.setCharge(moleculePrecursor.getCharge());
        gp.setCollisionEnergy(moleculePrecursor.getCollisionEnergy());
        gp.setDeclusteringPotential(moleculePrecursor.getDeclusteringPotential());
        gp.setDecoy(moleculePrecursor.isDecoy());
        gp.setNote(moleculePrecursor.getNote());
        gp.setExplicitCollisionEnergy(moleculePrecursor.getExplicitCollisionEnergy());
        gp.setExplicitDriftTimeMsec(moleculePrecursor.getExplicitDriftTimeMsec());
        gp.setExplicitDriftTimeHighEnergyOffsetMsec(moleculePrecursor.getExplicitDriftTimeHighEnergyOffsetMsec());
        gp.setIsotopeLabelId(isotopeLabelIdMap.get(moleculePrecursor.getIsotopeLabel()));
        gp = Table.insert(_user, TargetedMSManager.getTableInfoGeneralPrecursor(), gp);

        moleculePrecursor.setIsotopeLabelId(gp.getIsotopeLabelId());
        moleculePrecursor.setId(gp.getId());

        moleculePrecursor = Table.insert(_user, TargetedMSManager.getTableInfoMoleculePrecursor(), moleculePrecursor);

        //small molecule precursor annotations
        insertPrecursorAnnotation(moleculePrecursor.getAnnotations(), gp, moleculePrecursor.getId()); //adding small molecule precursor annotation in PrecursorAnnotation table. We might need to change this if we decide to have a separate MoleculePrecursorAnnotation table in the future.

        Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap = insertPrecursorChromInfos(gp.getId(),
                moleculePrecursor.getCustomIonName(), moleculePrecursor.getChromInfoList(), skylineIdSampleFileIdMap, sampleFileIdGeneralMolChromInfoIdMap);

        for(MoleculeTransition moleculeTransition: moleculePrecursor.getTransitionsList())
        {
            insertMoleculeTransition(moleculePrecursor, moleculeTransition, skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap);
        }
    }

    private void insertPrecursorAnnotation(List<PrecursorAnnotation> precursorAnnotations, GeneralPrecursor gp, int id)
    {
        for (PrecursorAnnotation annotation : precursorAnnotations)
        {
            annotation.setPrecursorId(id);
            annotation.setGeneralPrecursorId(gp.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAnnotation(), annotation);
        }
    }

    private void insertMoleculeTransition(MoleculePrecursor moleculePrecursor, MoleculeTransition moleculeTransition,
                                          Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                          Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap)
    {
        GeneralTransition gt = new GeneralTransition();
        gt.setGeneralPrecursorId(moleculePrecursor.getId());
        gt.setMz(moleculeTransition.getMz());
        gt.setCharge(moleculeTransition.getCharge());
        gt.setFragmentType(moleculeTransition.getFragmentType());
        gt.setIsotopeDistRank(moleculeTransition.getIsotopeDistRank());
        gt.setIsotopeDistProportion(moleculeTransition.getIsotopeDistProportion());
        gt.setMassIndex(moleculeTransition.getMassIndex());
        gt.setExplicitCollisionEnergy(moleculeTransition.getExplicitCollisionEnergy());
        gt.setsLens(moleculeTransition.getsLens());
        gt.setConeVoltage(moleculeTransition.getConeVoltage());
        gt.setExplicitCompensationVoltage(moleculeTransition.getExplicitCompensationVoltage());
        gt.setExplicitDeclusteringPotential(moleculeTransition.getExplicitDeclusteringPotential());
        gt.setExplicitDriftTimeMSec(moleculeTransition.getExplicitDriftTimeMSec());
        gt.setExplicitDriftTimeHighEnergyOffsetMSec(moleculeTransition.getExplicitDriftTimeHighEnergyOffsetMSec());
        gt = Table.insert(_user, TargetedMSManager.getTableInfoGeneralTransition(), gt);

        moleculeTransition.setTransitionId(gt.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoMoleculeTransition(), moleculeTransition);

        //small molecule transition annotations
        insertTransitionAnnotation(moleculeTransition.getAnnotations(), moleculeTransition.getId()); //adding small molecule transition annotation in TransitionAnnotation table. We might need to change this if we decide to have a separate MoleculeTransitionAnnotation table in the future.

        insertTransitionChromInfos(gt.getId(), moleculeTransition.getChromInfoList(), skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap);
    }

    private void insertTransitionAnnotation(List<TransitionAnnotation> annotations, int id)
    {
        for (TransitionAnnotation annotation : annotations)
        {
            annotation.setTransitionId(id);
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionAnnotation(), annotation);
        }
    }

    private void insertPrecursor(boolean insertCEOptmizations, boolean insertDPOptmizations,
                                 Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                 Map<String, Integer> isotopeLabelIdMap,
                                 Map<String, Integer> structuralModNameIdMap,
                                 Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap,
                                 Map<String, Integer> libraryNameIdMap,
                                 Peptide peptide,
                                 Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap,
                                 Precursor precursor)
    {
        if(_isPeptideLibraryDoc)
        {
            String precursorKey = precursor.getModifiedSequence() + ", charge " + precursor.getCharge() +", mz " + precursor.getMz();
            if(_libPrecursors.contains(precursorKey))
            {
                throw new PanoramaBadDataException("Duplicate precursor found: " + precursorKey
                        + ". Documents uploaded to a peptide library folder should contain unique precursors.");
            }
            else if(!(peptide.isDecoyPeptide() || peptide.isStandardTypePeptide()))
            {
                _libPrecursors.add(precursorKey);
            }
        }

        //setting values for GeneralPrecursor here seems odd - is there a better way?
        GeneralPrecursor gp = new GeneralPrecursor();
        gp.setGeneralMoleculeId(peptide.getId());
        gp.setMz(precursor.getMz());
        gp.setCharge(precursor.getCharge());
        gp.setCollisionEnergy(precursor.getCollisionEnergy());
        gp.setDeclusteringPotential(precursor.getDeclusteringPotential());
        gp.setDecoy(precursor.isDecoy());
        gp.setNote(precursor.getNote());
        gp.setExplicitCollisionEnergy(precursor.getExplicitCollisionEnergy());
        gp.setExplicitDriftTimeMsec(precursor.getExplicitDriftTimeMsec());
        gp.setExplicitDriftTimeHighEnergyOffsetMsec(precursor.getExplicitDriftTimeHighEnergyOffsetMsec());
        gp.setIsotopeLabel(precursor.getIsotopeLabel());
        gp.setIsotopeLabelId(isotopeLabelIdMap.get(precursor.getIsotopeLabel()));
        gp = Table.insert(_user, TargetedMSManager.getTableInfoGeneralPrecursor(), gp);

        precursor.setIsotopeLabelId(gp.getIsotopeLabelId());
        precursor.setId(gp.getId());
        precursor = Table.insert(_user, TargetedMSManager.getTableInfoPrecursor(), precursor);

        insertPrecursorAnnotation(precursor.getAnnotations(), gp, precursor.getId());

        Precursor.LibraryInfo libInfo = precursor.getLibraryInfo();
        if(libInfo != null)
        {
            libInfo.setPrecursorId(precursor.getId());
            Integer specLibId = libraryNameIdMap.get(libInfo.getLibraryName());
            if(specLibId == null)
            {
                throw new PanoramaBadDataException("'" + libInfo.getLibraryName() + "' library not found in settings.");
            }
            libInfo.setSpectrumLibraryId(specLibId);
            libInfo.setGeneralPrecursorId(gp.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorLibInfo(), libInfo);
        }

        Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap = insertPrecursorChromInfos(gp.getId(),
                precursor.getModifiedSequence(), precursor.getChromInfoList(), skylineIdSampleFileIdMap, sampleFileIdGeneralMolChromInfoIdMap);

        // 4. transition
        for(Transition transition: precursor.getTransitionsList())
        {
            insertTransition(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, structuralModNameIdMap, structuralModLossesMap, precursor, sampleFilePrecursorChromInfoIdMap, transition);
        }
    }

    private void insertTransition(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap, Map<String, Integer> structuralModNameIdMap, Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap, Precursor precursor, Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap, Transition transition)
    {
        GeneralTransition gt = new GeneralTransition();
        gt.setGeneralPrecursorId(precursor.getId());
        gt.setMz(transition.getMz());
        gt.setCharge(transition.getCharge());
        gt.setFragmentType(transition.getFragmentType());
        gt.setIsotopeDistRank(transition.getIsotopeDistRank());
        gt.setIsotopeDistProportion(transition.getIsotopeDistProportion());
        gt.setMassIndex(transition.getMassIndex());
        gt.setExplicitCollisionEnergy(transition.getExplicitCollisionEnergy());
        gt.setsLens(transition.getsLens());
        gt.setConeVoltage(transition.getConeVoltage());
        gt.setExplicitCompensationVoltage(transition.getExplicitCompensationVoltage());
        gt.setExplicitDeclusteringPotential(transition.getExplicitDeclusteringPotential());
        gt.setExplicitDriftTimeMSec(transition.getExplicitDriftTimeMSec());
        gt.setExplicitDriftTimeHighEnergyOffsetMSec(transition.getExplicitDriftTimeHighEnergyOffsetMSec());
        gt = Table.insert(_user, TargetedMSManager.getTableInfoGeneralTransition(), gt);

        transition.setId(gt.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoTransition(), transition);

        // transition annotations
        insertTransitionAnnotation(transition.getAnnotations(), transition.getId());

        // Insert appropriate CE and DP transition optimizations
        if (insertCEOptmizations && transition.getCollisionEnergy() != null)
        {
            TransitionOptimization ceOpt = new TransitionOptimization();
            ceOpt.setTransitionId(transition.getId());
            ceOpt.setOptValue(transition.getCollisionEnergy());
            ceOpt.setOptimizationType("ce");
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionOptimization(), ceOpt);
        }
        if (insertDPOptmizations && transition.getDeclusteringPotential() != null)
        {
            TransitionOptimization dpOpt = new TransitionOptimization();
            dpOpt.setTransitionId(transition.getId());
            dpOpt.setOptValue(transition.getDeclusteringPotential());
            dpOpt.setOptimizationType("dp");
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionOptimization(), dpOpt);
        }

        // transition results
        insertTransitionChromInfos(gt.getId(), transition.getChromInfoList(), skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap);

        // transition neutral losses
        for (TransitionLoss loss : transition.getNeutralLosses())
        {
            if(loss.getModificationName() != null)
            {
                Integer modificationId = structuralModNameIdMap.get(loss.getModificationName());
                if (modificationId == null)
                {
                    throw new PanoramaBadDataException("No such structural modification found: " + loss.getModificationName());
                }
                PeptideSettings.PotentialLoss[] potentialLosses = structuralModLossesMap.get(modificationId);
                if (potentialLosses == null)
                {
                    potentialLosses = new TableSelector(TargetedMSManager.getTableInfoStructuralModLoss(),
                                                        new SimpleFilter(FieldKey.fromString("structuralmodid"), modificationId),
                                                        new Sort("id")) // Sort by insertion id so that we can find the right match
                                                                        // if there were multiple potential losses defined for the modification
                                                        .getArray(PeptideSettings.PotentialLoss.class);
                    structuralModLossesMap.put(modificationId, potentialLosses);
                }

                if(loss.getLossIndex() == null)
                {
                    throw new PanoramaBadDataException("No loss index found for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.toString()
                                                    +"; Precursor: "+precursor.getModifiedSequence());
                }
                if(loss.getLossIndex() < 0 || loss.getLossIndex() >= potentialLosses.length)
                {
                    throw new PanoramaBadDataException("Loss index out of bounds for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.toString()
                                                    +"; Precursor: "+precursor.getModifiedSequence());
                }

                loss.setStructuralModLossId(potentialLosses[loss.getLossIndex()].getId());
                loss.setTransitionId(transition.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoTransitionLoss(), loss);
            }
            else
            {
                // This is a custom neutral loss; it is not associated with a structural modifcation.
                // Skyline does not yet support this case.
                throw new PanoramaBadDataException(" Unsupported custom neutral loss found."
                                                +" Loss: "+loss.toString()
                                                +"; Transition: "+transition.toString()
                                                +"; Precursor: "+precursor.getModifiedSequence());
            }
        }
    }

    private Map<Integer, Integer> insertGeneralMoleculeChromInfos(int gmId, List<GeneralMoleculeChromInfo> generalMoleculeChromInfos,
                                                                  Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap)
    {
        Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap = new HashMap<>();

        for (GeneralMoleculeChromInfo generalMoleculeChromInfo : generalMoleculeChromInfos)
        {
            SampleFileKey sampleFileKey = SampleFileKey.getKey(generalMoleculeChromInfo);
            SampleFile sampleFile = skylineIdSampleFileIdMap.get(sampleFileKey);
            if (sampleFile == null)
            {
                throw new PanoramaBadDataException("Database ID not found for Skyline samplefile id " +
                        generalMoleculeChromInfo.getSkylineSampleFileId() + " in replicate " +
                        generalMoleculeChromInfo.getReplicateName());
            }
            generalMoleculeChromInfo.setGeneralMoleculeId(gmId);
            generalMoleculeChromInfo.setSampleFileId(sampleFile.getId());
            generalMoleculeChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(),
                    generalMoleculeChromInfo);

            sampleFileIdGeneralMolChromInfoIdMap.put(sampleFile.getId(), generalMoleculeChromInfo.getId());

        }

        return sampleFileIdGeneralMolChromInfoIdMap;
    }

    private Map<SampleFileOptStepKey, Integer> insertPrecursorChromInfos(int gpId, String label,
                                                                         List<PrecursorChromInfo> precursorChromInfos,
                                                                         Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                                                         Map<Integer, Integer> sampleFileIdGeneralMolChromInfoIdMap)
    {
        Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap = new HashMap<>();

        for (PrecursorChromInfo precursorChromInfo: precursorChromInfos)
        {
            SampleFile sampleFile = skylineIdSampleFileIdMap.get(SampleFileKey.getKey(precursorChromInfo));
            if (sampleFile == null)
            {
                throw new PanoramaBadDataException("Database ID not found for Skyline samplefile id "+precursorChromInfo.getSkylineSampleFileId() + " in replicate " + precursorChromInfo.getReplicateName());
            }

            SampleFileOptStepKey sampleFileKey = SampleFileOptStepKey.getKey(precursorChromInfo);

            if (sampleFilePrecursorChromInfoIdMap.containsKey(sampleFileKey))
            {
                throw new PanoramaBadDataException("Multiple precursor chrom infos found for precursor " +
                        label + " and sample file " + precursorChromInfo.getSkylineSampleFileId() +
                        " in replicate " + precursorChromInfo.getReplicateName());
            }
            precursorChromInfo.setContainer(_container);
            precursorChromInfo.setPrecursorId(gpId);
            precursorChromInfo.setSampleFileId(sampleFile.getId());
            precursorChromInfo.setGeneralMoleculeChromInfoId(sampleFileIdGeneralMolChromInfoIdMap.get(sampleFile.getId()));

            precursorChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfo(), precursorChromInfo);
            sampleFilePrecursorChromInfoIdMap.put(sampleFileKey, precursorChromInfo.getId());

            for (PrecursorChromInfoAnnotation annotation : precursorChromInfo.getAnnotations())
            {
                annotation.setPrecursorChromInfoId(precursorChromInfo.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfoAnnotation(), annotation);
            }
        }

        return sampleFilePrecursorChromInfoIdMap;
    }

    private void insertTransitionChromInfos(int gtId, List<TransitionChromInfo> transitionChromInfos,
                                            Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                            Map<SampleFileOptStepKey, Integer> sampleFilePrecursorChromInfoIdMap)
    {
        for(TransitionChromInfo transChromInfo: transitionChromInfos)
        {
            SampleFile sampleFile = skylineIdSampleFileIdMap.get(SampleFileKey.getKey(transChromInfo));
            if (sampleFile == null)
            {
                throw new PanoramaBadDataException("Database ID not found for Skyline samplefile id " + transChromInfo.getSkylineSampleFileId() + " in replicate " + transChromInfo.getReplicateName());
            }
            transChromInfo.setTransitionId(gtId);
            transChromInfo.setSampleFileId(sampleFile.getId());
            // Lookup a precursor chrom info measured in the same sample file with the same optimization step
            SampleFileOptStepKey sampleFileKey = SampleFileOptStepKey.getKey(transChromInfo);
            Integer precursorChromInfoId = sampleFilePrecursorChromInfoIdMap.get(sampleFileKey);
            if (precursorChromInfoId == null)
            {
                throw new PanoramaBadDataException("Could not find precursor peak for " + sampleFileKey.toString());
            }

            transChromInfo.setPrecursorChromInfoId(precursorChromInfoId);
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfo(), transChromInfo);

            for (TransitionChromInfoAnnotation annotation : transChromInfo.getAnnotations())
            {
                annotation.setTransitionChromInfoId(transChromInfo.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), annotation);
            }
        }
    }

    private void insertSampleFiles(Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap, Map<Instrument, Integer> instrumentIdMap, Replicate replicate)
    {
        for(SampleFile sampleFile: replicate.getSampleFileList())
        {
            SampleFileKey sampleFileKey = SampleFileKey.getKey(replicate, sampleFile);
            if(skylineIdSampleFileIdMap.containsKey(sampleFileKey))
            {
                throw new PanoramaBadDataException("Sample file id '" + sampleFile.getSkylineId() + "' for replicate '" + replicate.getName() + "' has already been seen in the document.");
            }

            sampleFile.setReplicateId(replicate.getId());

            List<Instrument> instrumentInfoList = sampleFile.getInstrumentInfoList();
            if (instrumentInfoList != null && instrumentInfoList.size() > 0)
            {
                Instrument instrument = combineInstrumentInfos(instrumentInfoList);

                Integer instrumentId = instrumentIdMap.get(instrument);
                if (instrumentId == null)
                {
                    instrument.setRunId(_runId);
                    instrument = Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
                    instrumentIdMap.put(instrument, instrument.getId());
                    instrumentId = instrument.getId();
                }

                sampleFile.setInstrumentId(instrumentId);
            }

            // In a QC folder data from a sample file will be imported only if the file wasn't imported in a previously uploaded
            // Skyline document.
            sampleFile = Table.insert(_user, TargetedMSManager.getTableInfoSampleFile(), sampleFile);


            // Remember the ids we inserted so we can reference them later
            skylineIdSampleFileIdMap.put(sampleFileKey, sampleFile);
        }
    }

    private void copyBlibsToCloud()
    {
        if (!_blibSourcePaths.isEmpty())
        {
            try
            {
                Path blibDir = _pipeRoot.getRootNioPath().resolve(_blibSourceDir.getName());
                if (!Files.exists(blibDir))
                    Files.createDirectory(blibDir);

                for (Path path : _blibSourcePaths)
                {
                    Path dest = blibDir.resolve(FileUtil.getFileName(path));
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    _log.info("Copied " + FileUtil.getFileName(path) + " to cloud storage.");
                }
            }
            catch (IOException e)
            {
                logError("Copy Blibs to cloud failed.", e);
            }
        }
    }

    public static class SampleFileKey
    {
        private final String _replicate;
        private final String _skylineSampleFileId;

        private SampleFileKey(String replicate, String skylineSampleFileId)
        {
            _replicate = replicate;
            _skylineSampleFileId = skylineSampleFileId;
        }

        public static SampleFileKey getKey(ChromInfo chromInfo)
        {
            return new SampleFileKey(chromInfo.getReplicateName(), chromInfo.getSkylineSampleFileId());
        }

        public static SampleFileKey getKey(Replicate replicate, SampleFile sampleFile)
        {
            return new SampleFileKey(replicate.getName(), sampleFile.getSkylineId());
        }

        @Override
        public String toString()
        {
            return "Sample file Id: " + _skylineSampleFileId + ", Replicate: " + _replicate;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SampleFileKey that = (SampleFileKey) o;

            if (_replicate != null ? !_replicate.equals(that._replicate) : that._replicate != null) return false;
            if (_skylineSampleFileId != null ? !_skylineSampleFileId.equals(that._skylineSampleFileId) : that._skylineSampleFileId != null)
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _replicate != null ? _replicate.hashCode() : 0;
            result = 31 * result + (_skylineSampleFileId != null ? _skylineSampleFileId.hashCode() : 0);
            return result;
        }
    }

    public static class SampleFileOptStepKey extends SampleFileKey
    {
        private final Integer _optimizationStep;

        public SampleFileOptStepKey(String replicate, String skylineSampleFileId, Integer optimizationStep)
        {
            super(replicate, skylineSampleFileId);
            _optimizationStep = optimizationStep;
        }

        public static SampleFileOptStepKey getKey(PrecursorChromInfo chromInfo)
        {
            return new SampleFileOptStepKey(chromInfo.getReplicateName(), chromInfo.getSkylineSampleFileId(), chromInfo.getOptimizationStep());
        }

        public static SampleFileOptStepKey getKey(TransitionChromInfo chromInfo)
        {
            return new SampleFileOptStepKey(chromInfo.getReplicateName(), chromInfo.getSkylineSampleFileId(), chromInfo.getOptimizationStep());
        }

        @Override
        public String toString()
        {
            return super.toString() + ", Optimization step: " + _optimizationStep;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SampleFileOptStepKey that = (SampleFileOptStepKey) o;

            if (_optimizationStep != null ? !_optimizationStep.equals(that._optimizationStep) : that._optimizationStep != null)
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = super.hashCode();
            result = 31 * result + (_optimizationStep != null ? _optimizationStep.hashCode() : 0);
            return result;
        }
    }

    private PeptideSettings.StructuralModification findExistingStructuralModification(PeptideSettings.RunStructuralModification mod)
    {
        // Find all of the structural modifications that match the values exactly
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoStructuralModification(), "sm");
        sql.append(" WHERE Name = ? ");
        sql.add(mod.getName());
        sql.append(getNullableCriteria("aminoacid", mod.getAminoAcid()));
        sql.append(getNullableCriteria("terminus", mod.getTerminus()));
        sql.append(getNullableCriteria("formula", mod.getFormula()));
        sql.append(getNullableCriteria("massdiffmono", mod.getMassDiffMono()));
        sql.append(getNullableCriteria("massdiffavg", mod.getMassDiffAvg()));
        sql.append(getNullableCriteria("unimodid", mod.getUnimodId()));

        PeptideSettings.StructuralModification[] structuralModifications = new SqlSelector(TargetedMSManager.getSchema().getScope(), sql).getArray(PeptideSettings.StructuralModification.class);

        // Then see if any have the same the exact same set of potential losses
        for (PeptideSettings.StructuralModification structuralModification : structuralModifications)
        {
            SQLFragment lossesSQL = new SQLFragment("SELECT * FROM ");
            lossesSQL.append(TargetedMSManager.getTableInfoStructuralModLoss(), "sml");
            lossesSQL.append(" WHERE StructuralModId = ?");
            lossesSQL.add(structuralModification.getId());

            PeptideSettings.PotentialLoss[] existingLosses = new SqlSelector(TargetedMSManager.getSchema().getScope(), lossesSQL).getArray(PeptideSettings.PotentialLoss.class);
            if (existingLosses.length == mod.getPotentialLosses().size())
            {
                // Whether we've found a mismatch overall for this modification
                boolean missingLoss = false;
                for (PeptideSettings.PotentialLoss potentialLoss : mod.getPotentialLosses())
                {
                    // Whether we've found an exact potential loss match for this specific loss yet
                    boolean foundMatch = false;
                    for (PeptideSettings.PotentialLoss existingLoss : existingLosses)
                    {
                        if (existingLoss.equals(potentialLoss))
                        {
                            // Stop looking for a match
                            foundMatch = true;
                            break;
                        }
                    }
                    if (!foundMatch)
                    {
                        // We didn't a matching potential loss, so this isn't the right structural modification
                        missingLoss = true;
                        break;
                    }
                }
                if (!missingLoss)
                {
                    // They all matched and we had the right number, so we can safely reuse this structural modification
                    return structuralModification;
                }
            }
        }
        // No match, so we'll need to insert a new one
        return null;
    }

    private PeptideSettings.IsotopeModification findExistingIsotopeModification(PeptideSettings.RunIsotopeModification mod)
    {
        // Find all of the isotope modifications that match the values exactly
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoIsotopeModification(), "imod");
        sql.append(" WHERE Name = ? ");
        sql.add(mod.getName());
        sql.append(getNullableCriteria("aminoacid", mod.getAminoAcid()));
        sql.append(getNullableCriteria("terminus", mod.getTerminus()));
        sql.append(getNullableCriteria("formula", mod.getFormula()));
        sql.append(getNullableCriteria("massdiffmono", mod.getMassDiffMono()));
        sql.append(getNullableCriteria("massdiffavg", mod.getMassDiffAvg()));
        sql.append(getNullableCriteria("label13c", mod.getLabel13C()));
        sql.append(getNullableCriteria("label15n", mod.getLabel15N()));
        sql.append(getNullableCriteria("label18o", mod.getLabel18O()));
        sql.append(getNullableCriteria("label2h", mod.getLabel2H()));
        sql.append(getNullableCriteria("unimodid", mod.getUnimodId()));

        PeptideSettings.IsotopeModification[] isotopeModifications = new SqlSelector(TargetedMSManager.getSchema().getScope(), sql).getArray(PeptideSettings.IsotopeModification.class);

        return isotopeModifications.length == 0 ? null : isotopeModifications[0];
    }

    private SQLFragment getNullableCriteria(String columnName, Object value)
    {
        SQLFragment result = new SQLFragment(" AND ");
        result.append(columnName);
        if (value == null)
        {
            result.append(" IS NULL ");
        }
        else
        {
            result.append(" = ? ");
            result.add(value);
        }
        return result;
    }

    protected void updateRunStatus(String status)
    {
        // Default statusId = running
        updateRunStatus(status, STATUS_RUNNING);
    }

    protected void updateRunStatus(String status, int statusId)
    {
        updateRunStatus(_runId, status, statusId);
    }

    protected static void updateRunStatus(int runId, String status, int statusId)
    {
        new SqlExecutor(TargetedMSManager.getSchema()).execute("UPDATE " + TargetedMSManager.getTableInfoRuns() + " SET Status = ?, StatusId = ? WHERE Id = ?",
                    status, statusId, runId);
    }

    private static final ReentrantLock _schemaLock = new ReentrantLock();

    public static class RunInfo implements Serializable
    {
        private final int _runId;
        private final boolean _alreadyImported;

        private RunInfo(int runId, boolean alreadyImported)
        {
            _runId = runId;

            _alreadyImported = alreadyImported;
        }

        public int getRunId()
        {
            return _runId;
        }

        public boolean isAlreadyImported()
        {
            return _alreadyImported;
        }
    }

    protected RunInfo prepareRun()
    {
        try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            boolean alreadyImported = false;

            // Don't import if we've already imported this file (undeleted run exists matching this file name)
            _runId = getRun();
            if (_runId != -1)
            {
                alreadyImported = true;
            }
            else
            {
                _log.info("Starting import from " + _expData.getName());
                _runId = createRun();
            }

            transaction.commit();
            return new RunInfo(_runId, alreadyImported);
        }
    }

    protected int getRun()
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("DataId"), _expData.getRowId());
        filter.addCondition(FieldKey.fromParts("Container"), _container.getId());
        filter.addCondition(FieldKey.fromParts("Deleted"), Boolean.FALSE);
        TargetedMSRun run = new TableSelector(TargetedMSManager.getTableInfoRuns(), filter, null).getObject(TargetedMSRun.class);
        return run != null ? run.getId() : -1;
    }

    protected int createRun()
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(_expData.getRowId(), _container);
        if (run != null)
        {
            throw new IllegalStateException("There is already a run for " + _expData.getName() + " in " + _container.getPath());
        }

        run = new TargetedMSRun();
        run.setDescription(_description);
        run.setContainer(_container);
        run.setFileName(_expData.getName());
        run.setDataId(_expData.getRowId());
        run.setStatus(IMPORT_STARTED);
        run.setRepresentativeDataState(_representative == null ? TargetedMSRun.RepresentativeDataState.NotRepresentative : _representative);

        run = Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
        return run.getId();
    }

    private void close()
    {
        // TODO: close connection and prepared statements used for bulk inserts
//        if (null != _conn)
//            TargetedMSManager.getSchema().getScope().releaseConnection(_conn);
    }

    protected void logError(String message, Exception e)
    {
        _systemLog.error(message, e);
        _log.error(message, e);
    }

    void quantifyRun(TargetedMSRun run, QuantificationSettings quantificationSettings, Collection<GroupComparisonSettings> groupComparisons)
    {
        RegressionFit regressionFit = RegressionFit.NONE;
        if (quantificationSettings != null)
        {
            regressionFit = RegressionFit.parse(quantificationSettings.getRegressionFit());
        }
        if (groupComparisons.isEmpty() && regressionFit == RegressionFit.NONE)
        {
            return;
        }

        RunQuantifier quantifier = new RunQuantifier(run, _user, _container);
        for (GroupComparisonSettings groupComparison : groupComparisons)
        {
            for (FoldChange foldChange : quantifier.calculateFoldChanges(groupComparison))
            {
                Table.insert(_user, TargetedMSManager.getTableInfoFoldChange(), foldChange);
            }
        }
        if (regressionFit != RegressionFit.NONE)
        {
            List<GeneralMoleculeChromInfo> moleculeChromInfos = new ArrayList<>();
            for (CalibrationCurveEntity calibrationCurve : quantifier.calculateCalibrationCurves(quantificationSettings, moleculeChromInfos))
            {
                Table.insert(_user, TargetedMSManager.getTableInfoCalibrationCurve(), calibrationCurve);
            }
            for (GeneralMoleculeChromInfo chromInfo : moleculeChromInfos)
            {
                Table.update(_user, TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), chromInfo, chromInfo.getId());
            }
        }
    }

    @Nullable
    private File getInputFile()
    {
        if (_expData.hasFileScheme())
            return _expData.getFile();

        if (null == _pipeRoot || null == _localDirectory)
            return null;

        return _localDirectory.copyToLocalDirectory(_expData.getDataFileUrl(), _log);
    }
}
