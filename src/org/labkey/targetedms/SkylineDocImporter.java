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

package org.labkey.targetedms;

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
import org.labkey.targetedms.parser.*;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.RepresentativeStateManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class SkylineDocImporter
{
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    private static final String IMPORT_STARTED = "Importing... (refresh to check status)";
    private static final String IMPORT_SUCCEEDED = "";

    protected User _user;
    protected Container _container;
    private final TargetedMSRun.RepresentativeDataState _representative;
    private final ExpData _expData;
    protected String _description;

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
    protected static Logger _systemLog = Logger.getLogger(SkylineDocImporter.class);
    protected final XarContext _context;
    private int blankLabelIndex;

    // protected Connection _conn;
    // private static final int BATCH_SIZE = 100;

    public SkylineDocImporter(User user, Container c, String description, ExpData expData, Logger log, XarContext context, TargetedMSRun.RepresentativeDataState representative)
    {
        _context = context;
        _user = user;
        _container = c;
        _representative = representative;

        _expData = expData;

        if (null != description)
            _description = description;
        else
        {
            _description = FileUtil.getBaseName(_expData.getFile().getName());
        }

        _log = (null == log ? _systemLog : log);
    }

    public TargetedMSRun importRun(RunInfo runInfo) throws IOException, SQLException, XMLStreamException, DataFormatException, PipelineJobException
    {
        _runId = runInfo.getRunId();

        TargetedMSRun run = TargetedMSManager.getRun(_runId);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == SkylineDocImporter.STATUS_SUCCESS)
        {
            _log.info(_expData.getFile().getName() + " has already been imported so it does not need to be imported again");
            return run;
        }

        TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(_container);
        _isProteinLibraryDoc = folderType == TargetedMSModule.FolderType.LibraryProtein;
        _isPeptideLibraryDoc = folderType == TargetedMSModule.FolderType.Library;

        try
        {
            updateRunStatus(IMPORT_STARTED);
            _log.info("Starting to import Skyline document from " + run.getFileName());
            importSkylineDoc(run);
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
        catch (SQLException | DataFormatException | IOException | XMLStreamException | RuntimeException | PipelineJobException e)
        {
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }
    }


    private void importSkylineDoc(TargetedMSRun run) throws XMLStreamException, IOException, SQLException, DataFormatException, PipelineJobException
    {
        // TODO - Consider if this is too big to fit in a single transaction. If so, need to blow away all existing
        // data for this run before restarting the import in the case of a retry

        File f = _expData.getFile();
        NetworkDrive.ensureDrive(f.getPath());
        f = extractIfZip(f);

        try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction();
             SkylineDocumentParser parser = new SkylineDocumentParser(f, _user, _container, _log))
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
            Map<String, Integer> skylineIdSampleFileIdMap = new HashMap<>();
            Map<Instrument, Integer> instrumentIdMap = new HashMap<>();

            for(Replicate replicate: parser.getReplicates())
            {
                replicate.setRunId(_runId);
                replicate = Table.insert(_user, TargetedMSManager.getTableInfoReplicate(), replicate);

                for(SampleFile sampleFile: replicate.getSampleFileList())
                {
                    String sampleFileKey = getSampleFileKey(replicate, sampleFile);
                    if(skylineIdSampleFileIdMap.containsKey(sampleFileKey))
                    {
                        throw new IllegalStateException("Sample file id '" + sampleFile.getSkylineId() + "' for replicate '" + replicate.getName() + "' has already been seen in the document.");
                    }

                    sampleFile.setReplicateId(replicate.getId());

                    List<Instrument> instrumentInfoList = sampleFile.getInstrumentInfoList();
                    if(instrumentInfoList != null && instrumentInfoList.size() > 0)
                    {
                        Instrument instrument = combineInstrumentInfos(instrumentInfoList);

                        Integer instrumentId = instrumentIdMap.get(instrument);
                        if(instrumentId == null)
                        {
                            instrument.setRunId(_runId);
                            instrument = Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
                            instrumentIdMap.put(instrument, instrument.getId());
                            instrumentId = instrument.getId();
                        }

                        sampleFile.setInstrumentId(instrumentId);
                    }

                    sampleFile = Table.insert(_user, TargetedMSManager.getTableInfoSampleFile(), sampleFile);

                    // Remember the ids we inserted so we can reference them later
                    skylineIdSampleFileIdMap.put(sampleFileKey, sampleFile.getId());
                }
                for (ReplicateAnnotation annotation : replicate.getAnnotations())
                {
                    annotation.setReplicateId(replicate.getId());
                    Table.insert(_user, TargetedMSManager.getTableInfoReplicateAnnotation(), annotation);
                }
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
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("cut"), enzyme.getCut());
                if (enzyme.getNoCut() == null)
                {
                    filter.addCondition(FieldKey.fromParts("nocut"), null, CompareType.ISBLANK);
                }
                else
                {
                    filter.addCondition(FieldKey.fromParts("nocut"), enzyme.getNoCut());
                }
                filter.addCondition(FieldKey.fromParts("sense"), enzyme.getSense());
                filter.addCondition(FieldKey.fromParts("name"), enzyme.getName());
                PeptideSettings.Enzyme existingEnzyme = new TableSelector(TargetedMSManager.getTableInfoEnzyme(), filter, null).getObject(PeptideSettings.Enzyme.class);
                if (existingEnzyme == null)
                {
                    enzyme = Table.insert(_user, TargetedMSManager.getTableInfoEnzyme(), enzyme);
                }
                else
                {
                    enzyme = existingEnzyme;
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
            if(peptidePredictionSettings != null)
            {
                peptidePredictionSettings.setRunId(_runId);
                Table.insert(_user, TargetedMSManager.getTableInfoRetentionTimePredictionSettings(), peptidePredictionSettings);
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
            while (parser.hasNextPeptideGroup())
            {
                PeptideGroup pepGroup = parser.nextPeptideGroup();
                insertPeptideGroup(proteinService, insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, isotopeModNameIdMap, libraryNameIdMap, pepGroup, parser);
                if (++peptideGroupCount % 100 == 0)
                {
                    _log.info("Imported " + peptideGroupCount + " peptide groups.");
                }
            }

            run.setPeptideGroupCount(parser.getPeptideGroupCount());
            run.setPeptideCount(parser.getPeptideCount());
            run.setPrecursorCount(parser.getPrecursorCount());
            run.setTransitionCount(parser.getTransitionCount());
            Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

            if (run.isRepresentative())
            {
                resolveRepresentativeData(run);
            }

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

    private File extractIfZip(File f) throws IOException
    {
        String ext = FileUtil.getExtension(f.getName());
        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(ext))
        {
            File zipDir = new File(f.getParent(), SkylineFileUtils.getBaseName(f.getName()));
            List<File> files = ZipUtil.unzipToDirectory(f, zipDir, _log);
            File skyFile = null;
            for(File file: files)
            {
                ext = FileUtil.getExtension(file.getName());
                if("sky".equalsIgnoreCase(ext))
                {
                    skyFile = file;
                    break;
                }
            }

            if(skyFile == null)
            {
                throw new IOException("zip file "+skyFile+" does not contain a .sky file");
            }
            f = skyFile;
        }
        return f;
    }

    /**
     * Insert new iRT scale/peptides, or update existing scale for library folders
     *
     * @param parser
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
     * @param existingScale
     * @param importScale
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
     * @param scale
     * @param standards
     * @param library
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
        RepresentativeStateManager.setRepresentativeState(_user, _container, run, run.getRepresentativeDataState());
    }

    private void insertPeptideGroup(ProteinService proteinService, boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap, Map<String, Integer> libraryNameIdMap, PeptideGroup pepGroup, SkylineDocumentParser parser)
            throws SQLException, XMLStreamException, IOException, DataFormatException
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
                    throw new IllegalStateException("Duplicate protein found: "+pepGroup.getLabel()+", seqId "+pepGroup.getSequenceId()
                    + ". Documents uploaded to a protein library folder should contain unique proteins.");
                }
                else
                {
                    _libProteinSequenceIds.add(pepGroup.getSequenceId());
                }
            }
            if(_libProteinLabels.contains(pepGroup.getLabel()))
            {
                throw new IllegalStateException("Duplicate protein label found: "+pepGroup.getLabel()
                + ". Documents uploaded to a protein library folder should contain unique proteins.");
            }
            else
            {
                _libProteinLabels.add(pepGroup.getLabel());
            }
        }

        // CONSIDER: If there is already an identical entry in the PeptideGroup table re-use it.
        pepGroup = Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroup(), pepGroup);

        for (PeptideGroupAnnotation annotation : pepGroup.getAnnotations())
        {
            annotation.setPeptideGroupId(pepGroup.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroupAnnotation(), annotation);
        }

        // Read peptides for this protein
        while(parser.hasNextPeptide())
        {
            Peptide peptide = parser.nextPeptide();
            insertPeptide(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, isotopeModNameIdMap, libraryNameIdMap, pepGroup, peptide);
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

    private void insertPeptide(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap, Map<String, Integer> libraryNameIdMap, PeptideGroup pepGroup, Peptide peptide)
            throws SQLException
    {
        peptide.setPeptideGroupId(pepGroup.getId());
        // If the peptide modified sequence has not been set, use the light precursor sequence
        if (peptide.getPeptideModifiedSequence() == null )
        {
            for(Precursor precursor: peptide.getPrecursorList())
            {
                if ( PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(precursor.getIsotopeLabel()) )
                    peptide.setPeptideModifiedSequence(precursor.getModifiedSequence());
            }
        }

        peptide = Table.insert(_user, TargetedMSManager.getTableInfoPeptide(), peptide);

        for (PeptideAnnotation annotation : peptide.getAnnotations())
        {
            annotation.setPeptideId(peptide.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPeptideAnnotation(), annotation);
        }

        for(Peptide.StructuralModification mod: peptide.getStructuralMods())
        {
            int modId = structuralModNameIdMap.get(mod.getModificationName());
            mod.setStructuralModId(modId);
            mod.setPeptideId(peptide.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPeptideStructuralModification(), mod);
        }

        for(Peptide.IsotopeModification mod: peptide.getIsotopeMods())
        {
            int modId = isotopeModNameIdMap.get(mod.getModificationName());
            mod.setIsotopeModId(modId);
            mod.setPeptideId(peptide.getId());
            mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
            Table.insert(_user, TargetedMSManager.getTableInfoPeptideIsotopeModification(), mod);
        }

        Map<Integer, Integer> sampleFileIdPeptideChromInfoIdMap = new HashMap<>();

        for(PeptideChromInfo peptideChromInfo: peptide.getPeptideChromInfoList())
        {
            String sampleFileKey = getSampleFileKey(peptideChromInfo);
            Integer sampleFileId = skylineIdSampleFileIdMap.get(sampleFileKey);
            if(sampleFileId == null)
            {
                throw new IllegalStateException("Database ID not found for Skyline samplefile id "+peptideChromInfo.getSkylineSampleFileId() + " in replicate " + peptideChromInfo.getReplicateName());
            }

            peptideChromInfo.setPeptideId(peptide.getId());
            peptideChromInfo.setSampleFileId(sampleFileId);
            peptideChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPeptideChromInfo(), peptideChromInfo);

            sampleFileIdPeptideChromInfoIdMap.put(sampleFileId, peptideChromInfo.getId());
        }

        // 3. precursor
        for(Precursor precursor: peptide.getPrecursorList())
        {
            insertPrecursor(insertCEOptmizations, insertDPOptmizations,
                           skylineIdSampleFileIdMap,
                           isotopeLabelIdMap,
                           structuralModNameIdMap,
                           structuralModLossesMap,
                           libraryNameIdMap,
                           peptide,
                           sampleFileIdPeptideChromInfoIdMap,
                           precursor);
        }

        // 4. Calculate and insert peak area ratios
        PeakAreaRatioCalculator areaRatioCalculator = new PeakAreaRatioCalculator(peptide);
        areaRatioCalculator.init();
        // Insert area ratios for each combination of 2 isotope labels
        for(Integer numLabelId: isotopeLabelIdMap.values())
        {
            for(Integer denomLabelId: isotopeLabelIdMap.values())
            {
                if(!internalStandardLabelIds.contains(denomLabelId))
                    continue;

                if(numLabelId.equals(denomLabelId))
                    continue;

                for(Integer sampleFileId: skylineIdSampleFileIdMap.values())
                {
                    PeptideAreaRatio ratio = areaRatioCalculator.getPeptideAreaRatio(sampleFileId, numLabelId, denomLabelId);
                    if(ratio != null)
                    {
                         Table.insert(_user, TargetedMSManager.getTableInfoPeptideAreaRatio(), ratio);
                    }

                    for(Precursor precursor: peptide.getPrecursorList())
                    {
                        if(precursor.getIsotopeLabelId() != numLabelId)
                            continue;

                        PrecursorAreaRatio pRatio = areaRatioCalculator.getPrecursorAreaRatio(sampleFileId,
                                                                                              precursor,
                                                                                              numLabelId,
                                                                                              denomLabelId);
                        if(pRatio != null)
                        {
                            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAreaRatio(), pRatio);
                        }

                        for(Transition transition: precursor.getTransitionList())
                        {
                            TransitionAreaRatio tRatio = areaRatioCalculator.getTransitionAreaRatio(sampleFileId,
                                                                                                    precursor,
                                                                                                    transition,
                                                                                                    numLabelId,
                                                                                                    denomLabelId);
                            if(tRatio != null)
                            {
                                Table.insert(_user, TargetedMSManager.getTableInfoTransitionAreaRatio(), tRatio);
                            }
                        }
                    }
                }
            }
        }
    }

    private void insertPrecursor(boolean insertCEOptmizations, boolean insertDPOptmizations,
                                 Map<String, Integer> skylineIdSampleFileIdMap,
                                 Map<String, Integer> isotopeLabelIdMap,
                                 Map<String, Integer> structuralModNameIdMap,
                                 Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap,
                                 Map<String, Integer> libraryNameIdMap,
                                 Peptide peptide,
                                 Map<Integer, Integer> sampleFileIdPeptideChromInfoIdMap,
                                 Precursor precursor)
            throws SQLException
    {
        if(_isPeptideLibraryDoc)
        {
            String precursorKey = new StringBuilder(precursor.getModifiedSequence()).append(", charge ").append(precursor.getCharge()).toString();
            if(_libPrecursors.contains(precursorKey))
            {
                throw new IllegalStateException("Duplicate precursor found: " + precursorKey
                + ". Documents uploaded to a peptide library folder should contain unique precursors.");
            }
            else
            {
                _libPrecursors.add(precursorKey);
            }
        }
        precursor.setPeptideId(peptide.getId());
        precursor.setIsotopeLabelId(isotopeLabelIdMap.get(precursor.getIsotopeLabel()));

        precursor = Table.insert(_user, TargetedMSManager.getTableInfoPrecursor(), precursor);

        for (PrecursorAnnotation annotation : precursor.getAnnotations())
        {
            annotation.setPrecursorId(precursor.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAnnotation(), annotation);
        }

        Map<Integer, Integer> sampleFileIdPrecursorChromInfoIdMap = new HashMap<>();

        Precursor.LibraryInfo libInfo = precursor.getLibraryInfo();
        if(libInfo != null)
        {
            libInfo.setPrecursorId(precursor.getId());
            Integer specLibId = libraryNameIdMap.get(libInfo.getLibraryName());
            if(specLibId == null)
            {
                throw new IllegalStateException(libraryNameIdMap.get(libInfo.getLibraryName()) + " library not found in settings.");
            }
            libInfo.setSpectrumLibraryId(specLibId);

            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorLibInfo(), libInfo);
        }

        for (PrecursorChromInfo precursorChromInfo: precursor.getChromInfoList())
        {
            Integer sampleFileId = skylineIdSampleFileIdMap.get(getSampleFileKey(precursorChromInfo));
            if(sampleFileId == null)
            {
                throw new IllegalStateException("Database ID not found for Skyline samplefile id "+precursorChromInfo.getSkylineSampleFileId() + " in replicate " + precursorChromInfo.getReplicateName());
            }
            if(sampleFileIdPrecursorChromInfoIdMap.containsKey(sampleFileId))
            {
                throw new IllegalStateException("Multiple precursor chrom infos found for precursor "+
                           precursor.getModifiedSequence()+" and sample file "+precursorChromInfo.getSkylineSampleFileId()+
                           " in replicate "+precursorChromInfo.getReplicateName());
            }
            precursorChromInfo.setPrecursorId(precursor.getId());
            precursorChromInfo.setSampleFileId(sampleFileId);
            precursorChromInfo.setPeptideChromInfoId(sampleFileIdPeptideChromInfoIdMap.get(sampleFileId));

            precursorChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfo(), precursorChromInfo);
            sampleFileIdPrecursorChromInfoIdMap.put(sampleFileId, precursorChromInfo.getId());

            for (PrecursorChromInfoAnnotation annotation : precursorChromInfo.getAnnotations())
            {
                annotation.setPrecursorChromInfoId(precursorChromInfo.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfoAnnotation(), annotation);
            }
        }

        // 4. transition
        for(Transition transition: precursor.getTransitionList())
        {
            insertTransition(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, structuralModNameIdMap, structuralModLossesMap, precursor, sampleFileIdPrecursorChromInfoIdMap, transition);
        }
    }

    private void insertTransition(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> structuralModNameIdMap, Map<Integer, PeptideSettings.PotentialLoss[]> structuralModLossesMap, Precursor precursor, Map<Integer, Integer> sampleFileIdPrecursorChromInfoIdMap, Transition transition)
            throws SQLException
    {
        transition.setPrecursorId(precursor.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoTransition(), transition);

        // transition annotations
        for (TransitionAnnotation annotation : transition.getAnnotations())
        {
            annotation.setTransitionId(transition.getId());
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionAnnotation(), annotation);
        }

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
        for(TransitionChromInfo transChromInfo: transition.getChromInfoList())
        {
            Integer sampleFileId = skylineIdSampleFileIdMap.get(getSampleFileKey(transChromInfo));
            if(sampleFileId == null)
            {
                throw new IllegalStateException("Database ID not found for Skyline samplefile id "+transChromInfo.getSkylineSampleFileId() + " in replicate " + transChromInfo.getReplicateName());
            }
            transChromInfo.setTransitionId(transition.getId());
            transChromInfo.setSampleFileId(sampleFileId);
            transChromInfo.setPrecursorChromInfoId(sampleFileIdPrecursorChromInfoIdMap.get(sampleFileId));
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfo(), transChromInfo);

            for (TransitionChromInfoAnnotation annotation : transChromInfo.getAnnotations())
            {
                annotation.setTransitionChromInfoId(transChromInfo.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), annotation);
            }
        }

        // transition neutral losses
        for (TransitionLoss loss : transition.getNeutralLosses())
        {
            if(loss.getModificationName() != null)
            {
                Integer modificationId = structuralModNameIdMap.get(loss.getModificationName());
                if (modificationId == null)
                {
                    throw new IllegalStateException("No such structural modification found: " + loss.getModificationName());
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
                    throw new IllegalStateException("No loss index found for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.getLabel()
                                                    +"; Precursor: "+precursor.getModifiedSequence());
                }
                if(loss.getLossIndex() < 0 || loss.getLossIndex() >= potentialLosses.length)
                {
                    throw new IllegalStateException("Loss index out of bounds for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.getLabel()
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
                throw new IllegalStateException(" Unsupported custom neutral loss found."
                                                +" Loss: "+loss.toString()
                                                +"; Transition: "+transition.getLabel()
                                                +"; Precursor: "+precursor.getModifiedSequence());
            }
        }
    }

    private static String getSampleFileKey(ChromInfo chromInfo)
    {
        return getSampleFileKey(chromInfo.getReplicateName(), chromInfo.getSkylineSampleFileId());
    }

    private String getSampleFileKey(Replicate replicate, SampleFile sampleFile)
    {
        return getSampleFileKey(replicate.getName(), sampleFile.getSkylineId());
    }

    private static String getSampleFileKey(String replicateName, String skylineSampleFileId)
    {
        return new StringBuilder(replicateName).append("_").append(skylineSampleFileId).toString();
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
                _log.info("Starting import from " + _expData.getFile().getName());
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
            throw new IllegalStateException("There is already a run for " + _expData.getFile() + " in " + _container.getPath());
        }

        run = new TargetedMSRun();
        run.setDescription(_description);
        run.setContainer(_container);
        run.setFileName(_expData.getFile().getName());
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
}
