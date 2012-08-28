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

package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.labkey.api.ProteinService;
import org.labkey.api.collections.CsvSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.XarContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.parser.*;
import org.labkey.targetedms.query.LibraryManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final boolean _representative;
    protected String _description, _fileName, _path;

    protected int _runId;

    // Use passed in logger for import status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static Logger _systemLog = Logger.getLogger(SkylineDocImporter.class);
    protected final XarContext _context;

    // protected Connection _conn;
    // private static final int BATCH_SIZE = 100;

    public SkylineDocImporter(User user, Container c, String description, File file, Logger log, XarContext context, boolean representative)
    {
        _context = context;
        _user = user;
        _container = c;
        _representative = representative;

        _path = file.getParent();
        _fileName = file.getName();

        if (null != description)
            _description = description;
        else
        {
            int extension = _fileName.lastIndexOf(".");

            if (-1 != extension)
                _description = _fileName.substring(0, extension);
        }

        _log = (null == log ? _systemLog : log);
    }

    public TargetedMSRun importRun(RunInfo runInfo) throws IOException, SQLException, XMLStreamException, DataFormatException
    {
        _runId = runInfo.getRunId();

        TargetedMSRun run = TargetedMSManager.getRun(_runId);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == SkylineDocImporter.STATUS_SUCCESS)
        {
            _log.info(_fileName + " has already been imported so it does not need to be imported again");
            return run;
        }

        try
        {
            updateRunStatus(IMPORT_STARTED);
            importSkylineDoc(run);
        }
        catch (FileNotFoundException fnfe)
        {
            logError("Skyline document import failed due to a missing file.", fnfe);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw fnfe;
        }
        catch (SQLException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (IOException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (XMLStreamException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (RuntimeException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (DataFormatException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }

        updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);
        return TargetedMSManager.getRun(_runId);
    }


    private void importSkylineDoc(TargetedMSRun run) throws XMLStreamException, IOException, SQLException, DataFormatException
    {
        // TODO - Consider if this is too big to fit in a single transaction. If so, need to blow away all existing
        // data for this run before restarting the import in the case of a retry
        TargetedMSManager.getSchema().getScope().ensureTransaction();
        SkylineDocumentParser parser = null;
        File zipDir = null;
        try
        {
            File f = new File(_path + "/" + _fileName);

            NetworkDrive.ensureDrive(f.getPath());

            // If this is a zip file extract the contents to a folder
            String ext = FileUtil.getExtension(f.getName());
            if("zip".equalsIgnoreCase(ext))
            {
                zipDir = new File(f.getParent()+File.separator+FileUtil.getBaseName(f));
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

            ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);

            parser = new SkylineDocumentParser(f, _log);
            parser.readSettings();

            // Store the document settings
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
            predictionSettings = Table.insert(_user, TargetedMSManager.getTableInfoTransitionPredictionSettings(), predictionSettings);

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
            }

            // 2. Replicates and sample files
            Map<String, Integer> skylineIdSampleFileIdMap = new HashMap<String, Integer>();
            Map<String, Integer> filePathSampleFileIdMap = new HashMap<String, Integer>();
            for(Replicate replicate: parser.getReplicates())
            {
                replicate.setRunId(_runId);
                replicate = Table.insert(_user, TargetedMSManager.getTableInfoReplicate(), replicate);

                for(SampleFile sampleFile: replicate.getSampleFileList())
                {
                    sampleFile.setReplicateId(replicate.getId());
                    sampleFile = Table.insert(_user, TargetedMSManager.getTableInfoSampleFile(), sampleFile);
                    // Remember the ids we inserted so we can reference them later
                    skylineIdSampleFileIdMap.put(sampleFile.getSkylineId(), sampleFile.getId());
                    filePathSampleFileIdMap.put(sampleFile.getFilePath(), sampleFile.getId());

                    for (Instrument instrument : sampleFile.getInstrumentList())
                    {
                        instrument.setRunId(_runId);
                        instrument = Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
                    }
                }
            }

            // 3. Peptide settings
            Map<String, Integer> isotopeLabelIdMap = new HashMap<String, Integer>();
            Set<Integer> internalStandardLabelIds = new HashSet<Integer>();
            Map<String, Integer> structuralModNameIdMap = new HashMap<String, Integer>();
            Map<Integer, Collection<PeptideSettings.PotentialLoss>> structuralModLossesMap = new HashMap<Integer, Collection<PeptideSettings.PotentialLoss>>();
            Map<String, Integer> isotopeModNameIdMap = new HashMap<String, Integer>();
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
                    // TODO: check if this modification already exists in the database
                    mod = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeModification(), mod);
                    isotopeModNameIdMap.put(mod.getName(), mod.getId());

                    mod.setRunId(_runId);
                    mod.setIsotopeModId(mod.getId());
                    mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
                    Table.insert(_user, TargetedMSManager.getTableInfoRunIsotopeModification(), mod);
                }
            }

            // Spectrum library settings
            Map<String, Integer> librarySourceTypes = LibraryManager.getLibrarySourceTypes();
            Map<String, Integer> libraryNameIdMap = new HashMap<String, Integer>();
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
                PeptideSettings.Enzyme existingEnzyme = new TableSelector(TargetedMSManager.getTableInfoEnzyme(), Table.ALL_COLUMNS, filter, null).getObject(PeptideSettings.Enzyme.class);
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
                digestSettings = Table.insert(_user, TargetedMSManager.getTableInfoRunEnzyme(), digestSettings);
            }

            // TODO: bulk insert of precursor, transition, chrom info etc.

            // Store the data
            // 1. peptide group
            while (parser.hasNextPeptideGroup())
            {
                PeptideGroup pepGroup = parser.nextPeptideGroup();
                insertPeptideGroup(proteinService, insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, isotopeModNameIdMap, libraryNameIdMap, pepGroup);
            }

            run.setPeptideGroupCount(parser.getPeptideGroupCount());
            run.setPeptideCount(parser.getPeptideCount());
            run.setPrecursorCount(parser.getPrecursorCount());
            run.setTransitionCount(parser.getTransitionCount());
            Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

            if (_representative)
            {
                resolveRepresentativeData(run);
            }

            TargetedMSManager.getSchema().getScope().commitTransaction();
        }
        finally
        {
            TargetedMSManager.getSchema().getScope().closeConnection();
            if (parser != null)
                parser.close();
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

    private void resolveRepresentativeData(TargetedMSRun run) throws SQLException
    {
        // Mark everything in this run that doesn't already have representative data in this container as being active
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoPeptideGroup());
        makeActiveSQL.append(" SET ActiveRepresentativeData = ? ");
        makeActiveSQL.add(true);
        makeActiveSQL.append(" WHERE RunId=? ");
        makeActiveSQL.add(run.getId());
        makeActiveSQL.append(" AND( ");
        // If this peptide group has a SequenceId make sure we don't have another peptide group in this container
        // with the same SequenceId that has been previously marked as representative
        makeActiveSQL.append(" (SequenceId IS NOT NULL AND (SequenceId NOT IN (SELECT SequenceId FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        makeActiveSQL.append(" WHERE pg1.RunId = r1.Id AND r1.Container=? AND pg1.ActiveRepresentativeData=?))) ");
        makeActiveSQL.add(_container);
        makeActiveSQL.add(true);
        // If the peptide group does not have a SequenceId or there isn't an older peptide group with the same
        // SequenceId, compare the Labels to look for conflicting proteins.
        makeActiveSQL.append(" OR (Label NOT IN (SELECT Label FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r2");
        makeActiveSQL.append(" WHERE pg2.RunID = r2.Id AND r2.Container=? AND pg2.ActiveRepresentativeData=?)) ");
        makeActiveSQL.add(_container);
        makeActiveSQL.add(true);
        makeActiveSQL.append(")");
        new SqlExecutor(TargetedMSManager.getSchema(), makeActiveSQL).execute();

        // See how many conflict with existing representative data
        SQLFragment remainingConflictsSQL = new SQLFragment("SELECT COUNT(*) FROM ");
        remainingConflictsSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        remainingConflictsSQL.append(" WHERE ActiveRepresentativeData = ? AND RunId = ?");
        remainingConflictsSQL.add(false);
        remainingConflictsSQL.add(run.getId());
        int conflictCount = new SqlSelector(TargetedMSManager.getSchema(), remainingConflictsSQL).getObject(Integer.class);

        if (conflictCount == 0)
        {
            // If there are no conflicts, mark the run as being resolved
            run.setRepresentativeDataState(TargetedMSRun.RepresentativeDataState.Representative);
            run = Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());
            _log.info("Run contains representative data. No conflicts with existing representative data in current container found");
        }
        else
        {
            _log.info("Run contains representative data. " + conflictCount + " conflicts with existing representative data in current container found, manual reconciliation required");
        }
    }

    private void insertPeptideGroup(ProteinService proteinService, boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, Collection<PeptideSettings.PotentialLoss>> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap, Map<String, Integer> libraryNameIdMap, PeptideGroup pepGroup)
            throws SQLException
    {
        pepGroup.setRunId(_runId);

        if(pepGroup.isProtein())
        {
            int seqId = proteinService.ensureProtein(pepGroup.getSequence(), null, pepGroup.getLabel(), pepGroup.getDescription());
            pepGroup.setSequenceId(seqId);
        }
        pepGroup = Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroup(), pepGroup);

        for (PeptideGroupAnnotation annotation : pepGroup.getAnnotations())
        {
            annotation.setPeptideGroupId(pepGroup.getId());
            annotation = Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroupAnnotation(), annotation);
        }

        // 2. peptide
        for(Peptide peptide: pepGroup.getPeptideList())
        {
            insertPeptide(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, isotopeModNameIdMap, libraryNameIdMap, pepGroup, peptide);
        }
    }

    private void insertPeptide(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, Collection<PeptideSettings.PotentialLoss>> structuralModLossesMap, Map<String, Integer> isotopeModNameIdMap, Map<String, Integer> libraryNameIdMap, PeptideGroup pepGroup, Peptide peptide)
            throws SQLException
    {
        peptide.setPeptideGroupId(pepGroup.getId());
        peptide = Table.insert(_user, TargetedMSManager.getTableInfoPeptide(), peptide);

        for (PeptideAnnotation annotation : peptide.getAnnotations())
        {
            annotation.setPeptideId(peptide.getId());
            annotation = Table.insert(_user, TargetedMSManager.getTableInfoPeptideAnnotation(), annotation);
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

        Map<Integer, Integer> sampleFileIdPeptideChromInfoIdMap = new HashMap<Integer, Integer>();

        // Store the peak areas of the peptides labeled with different isotope labels  (in different sample files)
        // The key is the sample file name. The value is also a map with the isotope label ID as the key
        // Peak areas are the sum of the areas of the precursors of this peptide
        // labeleled with a specific isotope label
        Map<String, Map<Integer, InternalStdArea>> intStandardPeptideAreas = new HashMap<String, Map<Integer, InternalStdArea>>();
        Map<String, Map<Integer, InternalStdArea>> lightPeptideAreas = new HashMap<String, Map<Integer, InternalStdArea>>();

        for(PeptideChromInfo peptideChromInfo: peptide.getPeptideChromInfoList())
        {
            int sampleFileId = skylineIdSampleFileIdMap.get(peptideChromInfo.getSkylineSampleFileId());
            peptideChromInfo.setPeptideId(peptide.getId());
            peptideChromInfo.setSampleFileId(sampleFileId);
            peptideChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPeptideChromInfo(), peptideChromInfo);

            sampleFileIdPeptideChromInfoIdMap.put(sampleFileId, peptideChromInfo.getId());

            // We will have one PeptideChromInfo for each file
            String skylineSampleFileId = peptideChromInfo.getSkylineSampleFileId();
            // Iterate over the isotope labels
            for(Integer labelId: isotopeLabelIdMap.values())
            {
                // this is a heavy label
                if(internalStandardLabelIds.contains(labelId))
                {
                    Map<Integer, InternalStdArea> areasForFile = intStandardPeptideAreas.get(skylineSampleFileId);
                    if(areasForFile == null)
                    {
                        areasForFile = new HashMap<Integer, InternalStdArea>();
                        intStandardPeptideAreas.put(skylineSampleFileId, areasForFile);
                    }
                    areasForFile.put(labelId,  new InternalStdArea(labelId,
                                                                   peptideChromInfo.getId(),
                                                                   0)); // Add areas later when we iterate over the precursors
                }
                // this is a light label
                else
                {
                   Map<Integer, InternalStdArea> areasForFile = lightPeptideAreas.get(skylineSampleFileId);
                    if(areasForFile == null)
                    {
                        areasForFile = new HashMap<Integer, InternalStdArea>();
                        lightPeptideAreas.put(skylineSampleFileId, areasForFile);
                    }
                    areasForFile.put(labelId,  new InternalStdArea(labelId,
                                                                   peptideChromInfo.getId(),
                                                                   0)); // Add areas later when we iterate over the precursors
                }
            }
        }

        // Store the peak areas of precursors (in different sample files) labeled with an internal standard.
        // The key in the map is the precursor charge + sample file name. Value is also a map with the
        // isotope label id as the key
        Map<String, Map<Integer, InternalStdArea>> intStandardPrecursorAreas = new HashMap<String, Map<Integer, InternalStdArea>>();
        // Store the peak areas of the transitions (in different sample files) labeled with an internal standard.
        // The key in the map is the precursor charge + transitiontype+transitionOrdinal + sample file name.
        // Value is also a map with the isotope label id as the key
        Map<String,  Map<Integer, InternalStdArea>> intStandardTransitionAreas = new HashMap<String,  Map<Integer, InternalStdArea>>();


        // 3. precursor
        for(Precursor precursor: peptide.getPrecursorList())
        {
            insertPrecursor(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, isotopeLabelIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, libraryNameIdMap, peptide, sampleFileIdPeptideChromInfoIdMap, intStandardPeptideAreas, lightPeptideAreas, intStandardPrecursorAreas, intStandardTransitionAreas, precursor);
        }

        // Calculate the peak area ratios of the peaks not labeled with an internal standard
        if(internalStandardLabelIds.size() > 0) {
            for(String sampleFile: lightPeptideAreas.keySet())
            {
                Map<Integer, InternalStdArea> intStdSampleFileAreas = intStandardPeptideAreas.get(sampleFile);
                Map<Integer, InternalStdArea> lightSampleFileAreas = lightPeptideAreas.get(sampleFile);

                for(Map.Entry<Integer, InternalStdArea> lightLabelArea: lightSampleFileAreas.entrySet())
                {
                    for(Map.Entry<Integer, InternalStdArea> heavyLabelArea: intStdSampleFileAreas.entrySet())
                    {
                        PeptideAreaRatio peptideAreaRatio = new PeptideAreaRatio();
                        peptideAreaRatio.setIsotopeLabelId(lightLabelArea.getKey());
                        peptideAreaRatio.setIsotopeLabelStdId(heavyLabelArea.getKey());
                        peptideAreaRatio.setPeptideChromInfoId(lightLabelArea.getValue().getChromInfoId());
                        peptideAreaRatio.setPeptideChromInfoStdId(heavyLabelArea.getValue().getChromInfoId());
                        peptideAreaRatio.setAreaRatio(getAreaRatio(lightLabelArea.getValue().getArea(),
                                                                   heavyLabelArea.getValue().getArea()));
                        Table.insert(_user, TargetedMSManager.getTableInfoPeptideAreaRatio(), peptideAreaRatio);
                    }
                }
            }

            for(Precursor precursor: peptide.getPrecursorList())
            {
                int labelId = precursor.getIsotopeLabelId();

                if(!internalStandardLabelIds.contains(labelId))
                {
                    // For the precursors
                    for(PrecursorChromInfo precChromInfo: precursor.getChromInfoList())
                    {
                        String key = getPrecChromInfoKey(precursor, precChromInfo);
                        Map<Integer, InternalStdArea> areasForStdLabels = intStandardPrecursorAreas.get(key);

                        if(areasForStdLabels == null)
                        {
                            _log.info("No internal standard precursor area found for precursor key "+key);
                            continue;
                        }

                        for(Integer stdIsotopeLabelId: areasForStdLabels.keySet())
                        {
                            InternalStdArea internalStdArea = areasForStdLabels.get(stdIsotopeLabelId);
                            PrecursorAreaRatio precAreaRatio = new PrecursorAreaRatio();
                            precAreaRatio.setPrecursorChromInfoId(precChromInfo.getId());
                            precAreaRatio.setPrecursorChromInfoStdId(internalStdArea.getChromInfoId());
                            precAreaRatio.setIsotopeLabelId(labelId);
                            precAreaRatio.setIsotopeLabelStdId(internalStdArea.getInternalStdLabelId());
                            precAreaRatio.setAreaRatio(getAreaRatio(precChromInfo.getTotalArea(), internalStdArea.getArea()));
                            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAreaRatio(), precAreaRatio);
                        }
                    }

                    // For the transitions
                    for(Transition transition: precursor.getTransitionList())
                    {
                        for(TransitionChromInfo transChromInfo: transition.getChromInfoList())
                        {
                            String key = getTransitionChromInfoKey(precursor, transition, transChromInfo);
                            Map<Integer, InternalStdArea> areasForStdLabels = intStandardTransitionAreas.get(key);

                            if(areasForStdLabels == null)
                            {
                                _log.info("No internal standard transition area found for transition key "+key);
                                continue;
                            }

                            for(Integer stdIsotopeLabelId: areasForStdLabels.keySet())
                            {
                                InternalStdArea internalStdArea = areasForStdLabels.get(stdIsotopeLabelId);
                                TransitionAreaRatio transAreaRatio = new TransitionAreaRatio();
                                transAreaRatio.setTransitionChromInfoId(transChromInfo.getId());
                                transAreaRatio.setTransitionChromInfoStdId(internalStdArea.getChromInfoId());
                                transAreaRatio.setIsotopeLabelId(labelId);
                                transAreaRatio.setIsotopeLabelStdId(internalStdArea.getInternalStdLabelId());
                                transAreaRatio.setAreaRatio(getAreaRatio(transChromInfo.getArea(), internalStdArea.getArea()));
                                Table.insert(_user, TargetedMSManager.getTableInfoTransitionAreaRatio(), transAreaRatio);
                            }
                        }
                    }
                }
            } // End for(Precursor precursor: peptide.getPrecursorList())
        } // End if(internalStandardLabelIds.size() > 0)
    }

    private void insertPrecursor(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Map<String, Integer> isotopeLabelIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, Collection<PeptideSettings.PotentialLoss>> structuralModLossesMap, Map<String, Integer> libraryNameIdMap, Peptide peptide, Map<Integer, Integer> sampleFileIdPeptideChromInfoIdMap, Map<String, Map<Integer, InternalStdArea>> intStandardPeptideAreas, Map<String, Map<Integer, InternalStdArea>> lightPeptideAreas, Map<String, Map<Integer, InternalStdArea>> intStandardPrecursorAreas, Map<String, Map<Integer, InternalStdArea>> intStandardTransitionAreas, Precursor precursor)
            throws SQLException
    {
        precursor.setPeptideId(peptide.getId());
        precursor.setIsotopeLabelId(isotopeLabelIdMap.get(precursor.getIsotopeLabel()));

        precursor = Table.insert(_user, TargetedMSManager.getTableInfoPrecursor(), precursor);

        for (PrecursorAnnotation annotation : precursor.getAnnotations())
        {
            annotation.setPrecursorId(precursor.getId());
            annotation = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorAnnotation(), annotation);
        }

        Map<Integer, Integer> sampleFileIdPrecursorChromInfoIdMap = new HashMap<Integer, Integer>();

        Precursor.LibraryInfo libInfo = precursor.getLibraryInfo();
        if(libInfo != null)
        {
            libInfo.setPrecursorId(precursor.getId());
            libInfo.setSpectrumLibraryId(libraryNameIdMap.get(libInfo.getLibraryName()));
            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorLibInfo(), libInfo);
        }

        for (PrecursorChromInfo precursorChromInfo: precursor.getChromInfoList())
        {
            int sampleFileId = skylineIdSampleFileIdMap.get(precursorChromInfo.getSkylineSampleFileId());
            precursorChromInfo.setPrecursorId(precursor.getId());
            precursorChromInfo.setSampleFileId(sampleFileId);
            precursorChromInfo.setPeptideChromInfoId(sampleFileIdPeptideChromInfoIdMap.get(sampleFileId));

            precursorChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfo(), precursorChromInfo);
            sampleFileIdPrecursorChromInfoIdMap.put(sampleFileId, precursorChromInfo.getId());

            for (PrecursorChromInfoAnnotation annotation : precursorChromInfo.getAnnotations())
            {
                annotation.setPrecursorChromInfoId(precursorChromInfo.getId());
                annotation = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfoAnnotation(), annotation);
            }

            // If this precursor is labeled with an internal standard store the peak areas for the
            // individual sample files
            if(internalStandardLabelIds.contains(precursor.getIsotopeLabelId()))
            {
                // key is charge + sample file name
                String key = getPrecChromInfoKey(precursor, precursorChromInfo);
                Map<Integer, InternalStdArea> areasForPrecKey= intStandardPeptideAreas.get(key);
                if(areasForPrecKey == null)
                {
                    areasForPrecKey = new HashMap<Integer, InternalStdArea>();
                    intStandardPrecursorAreas.put(key, areasForPrecKey);
                }

                if(areasForPrecKey.containsKey(precursor.getIsotopeLabelId()))
                {
                    throw new IllegalStateException("Duplicate area information found for label "+precursor.getIsotopeLabel()+
                                                    " and precursor chrom info key, "+key+", while calculating peak area ratios.");
                }
                InternalStdArea internalStdArea = new InternalStdArea(precursor.getIsotopeLabelId(),
                        precursorChromInfo.getId(), precursorChromInfo.getTotalArea());
                areasForPrecKey.put(precursor.getIsotopeLabelId(), internalStdArea);

                // Add the precursor area to the peptide peak areas for this sample file and heavy label type
                Map<Integer, InternalStdArea> areasForFile = intStandardPeptideAreas.get(precursorChromInfo.getSkylineSampleFileId());
                if(areasForFile == null)
                {
                    throw new IllegalStateException("Peptide peak areas not found for file "+
                                                     precursorChromInfo.getSkylineSampleFileId()+" and peptide "+peptide.getSequence());
                }
                InternalStdArea areaForLabel = areasForFile.get(precursor.getIsotopeLabelId());
                areaForLabel.addArea(precursorChromInfo.getTotalArea());
            }
            else
            {
                // Add the precursor area to the peptide peak areas for this sample file and light label type
                Map<Integer, InternalStdArea> areasForFile = lightPeptideAreas.get(precursorChromInfo.getSkylineSampleFileId());
                if(areasForFile == null)
                {
                    throw new IllegalStateException("Peptide peak areas not found for file "+
                                                     precursorChromInfo.getSkylineSampleFileId()+" and peptide "+peptide.getSequence());
                }
                InternalStdArea areaForLabel = areasForFile.get(precursor.getIsotopeLabelId());
                areaForLabel.addArea(precursorChromInfo.getTotalArea());
            }
        }

        // 4. transition
        for(Transition transition: precursor.getTransitionList())
        {
            insertTransition(insertCEOptmizations, insertDPOptmizations, skylineIdSampleFileIdMap, internalStandardLabelIds, structuralModNameIdMap, structuralModLossesMap, intStandardTransitionAreas, precursor, sampleFileIdPrecursorChromInfoIdMap, transition);
        }
    }

    private void insertTransition(boolean insertCEOptmizations, boolean insertDPOptmizations, Map<String, Integer> skylineIdSampleFileIdMap, Set<Integer> internalStandardLabelIds, Map<String, Integer> structuralModNameIdMap, Map<Integer, Collection<PeptideSettings.PotentialLoss>> structuralModLossesMap, Map<String, Map<Integer, InternalStdArea>> intStandardTransitionAreas, Precursor precursor, Map<Integer, Integer> sampleFileIdPrecursorChromInfoIdMap, Transition transition)
            throws SQLException
    {
        transition.setPrecursorId(precursor.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoTransition(), transition);

        // transition annotations
        for (TransitionAnnotation annotation : transition.getAnnotations())
        {
            annotation.setTransitionId(transition.getId());
            annotation = Table.insert(_user, TargetedMSManager.getTableInfoTransitionAnnotation(), annotation);
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
            int sampleFileId = skylineIdSampleFileIdMap.get(transChromInfo.getSkylineSampleFileId());
            transChromInfo.setTransitionId(transition.getId());
            transChromInfo.setSampleFileId(sampleFileId);
            transChromInfo.setPrecursorChromInfoId(sampleFileIdPrecursorChromInfoIdMap.get(sampleFileId));
            Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfo(), transChromInfo);

            for (TransitionChromInfoAnnotation annotation : transChromInfo.getAnnotations())
            {
                annotation.setTransitionChromInfoId(transChromInfo.getId());
                annotation = Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), annotation);
            }

            // If this transition is labeled with an internal standard store the peak areas for the
            // individual sample files
            if(internalStandardLabelIds.contains(precursor.getIsotopeLabelId()))
            {
                // Key is charge + fragmentType+fragmentOrdinal + fragment_charge + sample file name
                String key = getTransitionChromInfoKey(precursor, transition, transChromInfo);
                Map<Integer, InternalStdArea> areasForLabels = intStandardTransitionAreas.get(key);
                if(areasForLabels == null)
                {
                    areasForLabels = new HashMap<Integer, InternalStdArea>();
                    intStandardTransitionAreas.put(key, areasForLabels);
                }

                if(areasForLabels.containsKey(precursor.getIsotopeLabelId()))
                {
                    throw new IllegalStateException("Duplicate area information found for label "+precursor.getIsotopeLabel()+
                                                    " and transition chrom info key, "+key+", while calculating peak area ratios.");
                }

                InternalStdArea internalStdArea = new InternalStdArea(precursor.getIsotopeLabelId(),
                        transChromInfo.getId(), transChromInfo.getArea());
                areasForLabels.put(precursor.getIsotopeLabelId(), internalStdArea);
            }
        }

        // transition neutral losses
        for (TransitionLoss loss : transition.getNeutralLosses())
        {
            Integer modificationId = structuralModNameIdMap.get(loss.getModificationName());
            if (modificationId == null)
            {
                throw new IllegalStateException("No such structural modification found: " + loss.getModificationName());
            }

            Collection<PeptideSettings.PotentialLoss> potentialLosses = structuralModLossesMap.get(modificationId);
            if (potentialLosses == null)
            {
                potentialLosses = new TableSelector(TargetedMSManager.getTableInfoStructuralModLoss(), new SimpleFilter("structuralmodid", modificationId), null).getCollection(PeptideSettings.PotentialLoss.class);
                structuralModLossesMap.put(modificationId, potentialLosses);
            }
            boolean foundMatch = false;
            for (PeptideSettings.PotentialLoss potentialLoss : potentialLosses)
            {
                if (loss.matches(potentialLoss))
                {
                    loss.setTransitionId(transition.getId());
                    loss.setStructuralModLossId(potentialLoss.getId());
                    Table.insert(_user, TargetedMSManager.getTableInfoTransitionLoss(), loss);
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch)
            {
                throw new IllegalStateException("No matching potential loss found for structural modification '" + loss.getModificationName() + "'. " + loss);
            }

        }
    }

    private static double getAreaRatio(double numerator, double denominator)
    {
        return (denominator == 0 ? Double.MAX_VALUE : (numerator / denominator));
    }

    private static String getPrecChromInfoKey(Precursor precursor, PrecursorChromInfo chromInfo)
    {
        StringBuilder key = new StringBuilder();
           key.append(precursor.getCharge())
           .append("_")
           .append(chromInfo.getSkylineSampleFileId());
       return key.toString();
    }

    private static String getTransitionChromInfoKey(Precursor precursor, Transition transition, TransitionChromInfo chromInfo)
    {
        StringBuilder key = new StringBuilder();
           key.append(precursor.getCharge())
           .append("_")
           .append(transition.getFragmentType())
           .append(transition.getFragmentOrdinal())
           .append("_")
           .append(transition.getCharge())
           .append("_")
           .append(chromInfo.getSkylineSampleFileId());
       return key.toString();
    }

    private static final class InternalStdArea
    {
        private int _internalStdLabelId;
        private double _area;
        private int _chromInfoId;

        public InternalStdArea(int labelId, int chromInfoId, double totalArea)
        {
            _internalStdLabelId = labelId;
            _chromInfoId = chromInfoId;
            _area = totalArea;
        }

        public int getInternalStdLabelId()
        {
            return _internalStdLabelId;
        }

        public int getChromInfoId()
        {
            return _chromInfoId;
        }

        public double getArea()
        {
            return _area;
        }

        public void addArea(double area)
        {
           _area += area;
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
        try
        {
            Table.execute(TargetedMSManager.getSchema(), "UPDATE " + TargetedMSManager.getTableInfoRuns() + " SET Status = ?, StatusId = ? WHERE Id = ?",
                    status, statusId, runId);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private static final Object _schemaLock = new Object();

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

    protected RunInfo prepareRun(boolean restart) throws SQLException
    {
        try
        {
            boolean alreadyImported = false;
            TargetedMSManager.getSchema().getScope().ensureTransaction();

            synchronized (_schemaLock)
            {
                // Don't import if we've already imported this file (undeleted run exists matching this file name)
                _runId = getRun();
                if (_runId != -1)
                {
                    if (!restart)
                    {
                        alreadyImported = true;
                    }
                    else
                    {
                        _log.info("Restarting import from " + _fileName);
                    }
                }
                else
                {
                    _log.info("Starting import from " + _fileName);
                    _runId = createRun();
                }
            }

            TargetedMSManager.getSchema().getScope().commitTransaction();
            return new RunInfo(_runId, alreadyImported);
        }
        finally
        {
            TargetedMSManager.getSchema().getScope().closeConnection();
        }
    }

    protected int getRun() throws SQLException
    {
        SimpleFilter filter = new SimpleFilter("Path", _path);
        filter.addCondition("FileName", _fileName);
        filter.addCondition("Container", _container.getId());
        filter.addCondition("Deleted", Boolean.FALSE);
        ResultSet rs = Table.select(TargetedMSManager.getTableInfoRuns(), new CsvSet("Id,Path,FileName,Container,Deleted"), filter, null);

        int runId = -1;

        if (rs.next())
            runId = rs.getInt("Id");

        rs.close();
        return runId;
    }

    protected int createRun() throws SQLException
    {
        HashMap<String, Object> runMap = new HashMap<String, Object>();

        TargetedMSRun run = TargetedMSManager.getRunByFileName(_path, _fileName, _container);
        if (run != null)
        {
            throw new IllegalStateException("There is already a run for " + _path + "/" + _fileName + " in " + _container.getPath());
        }

        run = new TargetedMSRun();
        run.setDescription(_description);
        run.setContainer(_container);
        run.setPath(_path);
        run.setFileName(_fileName);
        run.setStatus(IMPORT_STARTED);
        run.setRepresentativeDataState(_representative ? TargetedMSRun.RepresentativeDataState.Conflicted : TargetedMSRun.RepresentativeDataState.NotRepresentative);

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
