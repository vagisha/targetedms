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

package org.labkey.targetedms;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.CancelledException;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.Portal;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.SkylinePort.Irt.IRegressionFunction;
import org.labkey.targetedms.SkylinePort.Irt.IrtRegressionCalculator;
import org.labkey.targetedms.SkylinePort.Irt.RetentionTimeProviderImpl;
import org.labkey.targetedms.calculations.RunQuantifier;
import org.labkey.targetedms.calculations.quantification.RegressionFit;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.parser.*;
import org.labkey.targetedms.parser.list.ListData;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;
import org.labkey.targetedms.query.ConflictResultsManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.RepresentativeStateManager;
import org.labkey.targetedms.query.SkylineListManager;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.stream.Collectors;

import static org.labkey.targetedms.TargetedMSManager.getTableInfoPrecursorChromInfo;
import static org.labkey.targetedms.TargetedMSManager.getTableInfoTransitionChromInfo;

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

    protected long _runId;
    private boolean _isProteinLibraryDoc = false;
    private boolean _isPeptideLibraryDoc = false;
    private Set<Integer> _libProteinSequenceIds;
    private Set<String> _libProteinLabels;
    private Set<String> _libPrecursors;

    // Use passed in logger for import status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;
    private ProgressMonitor _progressMonitor;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static final Logger _systemLog = LogManager.getLogger(SkylineDocImporter.class);
    protected final XarContext _context;
    private int blankLabelIndex;

    private final LocalDirectory _localDirectory;
    private final PipeRoot _pipeRoot;

    private File _blibSourceDir;
    private final List<Path> _blibSourcePaths = new ArrayList<>();

    // Hold on to statements so that we can reuse them through the import process
    private transient PreparedStatement _transitionChromInfoAnnotationStmt;
    private transient PreparedStatement _transitionAnnotationStmt;
    private transient PreparedStatement _precursorChromInfoAnnotationStmt;
    private transient PreparedStatement _generalMoleculeAnnotationStmt;
    private transient PreparedStatement _precursorAnnotationStmt;
    private transient PreparedStatement _transitionChromInfoStmt;
    private transient PreparedStatement _precursorChromInfoStmt;
    private transient PreparedStatement _precursorChromInfoIndicesStmt;
    private File _auditLogFile;

    private final Set<String> _missingLibraries = new HashSet<>();

    @JsonCreator
    private SkylineDocImporter(@JsonProperty("_expData") ExpData expData, @JsonProperty("_context") XarContext context,
                               @JsonProperty("_representative") TargetedMSRun.RepresentativeDataState representative,
                               @JsonProperty("_localDirectory") LocalDirectory localDirectory, @JsonProperty("_pipeRoot") PipeRoot pipeRoot)
    {
        _representative = representative;
        _expData = expData;
        _context = context;
        _localDirectory = localDirectory;
        _pipeRoot = pipeRoot;
    }

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

    public TargetedMSRun importRun(RunInfo runInfo, PipelineJob job) throws IOException, XMLStreamException, PipelineJobException, AuditLogException
    {
        _runId = runInfo.getRunId();

        TargetedMSRun run = TargetedMSManager.getRun(_runId);

        if (run == null)
        {
            throw new IllegalStateException("Could not find record for runId " + _runId + ", it may have already been deleted. Please try importing again.");
        }

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run.getStatusId() == SkylineDocImporter.STATUS_SUCCESS)
        {
            _log.info(_expData.getName() + " has already been imported so it does not need to be imported again");
            return run;
        }

        TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(_container);
        _isProteinLibraryDoc = folderType == TargetedMSService.FolderType.LibraryProtein;
        _isPeptideLibraryDoc = folderType == TargetedMSService.FolderType.Library;

        if(_isProteinLibraryDoc || _isPeptideLibraryDoc)
        {
            var conflictCount = ConflictResultsManager.getConflictCount(_user, _container);
            if(conflictCount > 0)
            {
                var conflictTarget = _isProteinLibraryDoc ? "protein" : "peptide";
                throw new PipelineJobException("The library folder has conflicts." +
                        " The last document imported in to the folder had " + StringUtilsLabKey.pluralize(conflictCount, conflictTarget)
                        + " that were already included in the library, resulting in conflicts." +
                        " Please resolve the conflicts by choosing the version of each " + conflictTarget + " that should be included in the library." +
                        " New Skyline documents can be added to the folder after the conflicts have been resolved.");
            }
        }

        _progressMonitor = new ProgressMonitor(job, folderType, _log);

        try
        {
            File inputFile = getInputFile();
            if (null == inputFile)
                throw new FileNotFoundException();

            // Set the size of the Skyline document (.sky.zip) file.
            run.setDocumentSize(Files.size(inputFile.toPath()));
            saveRunDocumentSize(run);

            updateRunStatus(IMPORT_STARTED);
            _log.info("Starting to import Skyline document from " + run.getFileName());
            importSkylineDoc(run, inputFile);
            _log.info("Completed import of Skyline document from " + run.getFileName());

            updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);

            _progressMonitor.complete();
            return TargetedMSManager.getRun(_runId);
        }
        catch (FileNotFoundException fnfe)
        {
            logError("Skyline document import failed due to a missing file.", fnfe);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw fnfe;
        }
        catch (CancelledException e)
        {
            _log.info("Cancelled  Skyline document import.");
            updateRunStatus("Import cancelled (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (IOException | XMLStreamException | RuntimeException | PipelineJobException | AuditLogException e)
        {
            _log.error("Import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
    }


    private void importSkylineDoc(TargetedMSRun run, File f) throws XMLStreamException, IOException, PipelineJobException, AuditLogException
    {
        // TODO - Consider if this is too big to fit in a single transaction. If so, need to blow away all existing
        // data for this run before restarting the import in the case of a retry
        DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().getCurrentTransaction();
        if (transaction == null)
        {
            throw new IllegalStateException("Callers should start their own transaction");
        }

        NetworkDrive.ensureDrive(f.getPath());
        f = extractIfZip(f);

        TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(run.getContainer());

        try (SkylineDocumentParser parser = new SkylineDocumentParser(f, _log, run.getContainer(), _progressMonitor.getParserProgressTracker()))
        {
            // Persist all of the indices into the chromatograms from the SKYD file separately. We need to retain it
            // at the PrecursorChromInfo level if we end up not storing the TransitionChromInfos. We will update the
            // PrecursorChromInfo to include the data if we end up needing it.
            final String suffix = StringUtilsLabKey.getPaddedUniquifier(9);
            final String precursorChromInfoIndicesTempTableName = TargetedMSManager.getSqlDialect().getTempTablePrefix() +  "PrecursorChromInfoIndices" + suffix;
            new SqlExecutor(TargetedMSSchema.getSchema()).execute("CREATE " +
                    TargetedMSManager.getSqlDialect().getTempTableKeyword() + " TABLE " + precursorChromInfoIndicesTempTableName + " ( " +
                    "\tPrecursorChromInfoId BIGINT NOT NULL PRIMARY KEY,\n" +
                    "\tIndices " + TargetedMSManager.getSqlDialect().getBinaryDataType() +
                    ")");
            _precursorChromInfoIndicesStmt = ensureStatement(null,"INSERT INTO " + precursorChromInfoIndicesTempTableName + "(PrecursorChromInfoId, Indices) VALUES (?, ?)", false);

            run.setFormatVersion(parser.getFormatVersion());
            run.setSoftwareVersion(parser.getSoftwareVersion());

            ProteinService proteinService = ProteinService.get();
            ExpData skydData = parser.readSettings(_container, _user);
            if (skydData != null)
            {
                run.setSkydDataId(skydData.getRowId());
            }

            // At this point we have read the settings so we know if we have any group comparisons or calibration curves to calculate
            // Adjust the progress parts with this information
            adjustProgressParts(_progressMonitor, parser.getDataSettings(), parser.getPeptideSettings().getQuantificationSettings());

            // Store the document settings
            // 0. iRT information
            run.setiRTscaleId(insertiRTData(parser));

            // 1. Transition settings
            OptimizationInfo optimizationInfo = insertTransitionSettings(parser.getTransitionSettings());

            // 2. Replicates and sample files
            ReplicateInfo replicateInfo = insertReplicates(run, parser, optimizationInfo, folderType);

            // 3. Peptide settings
            PeptideSettings pepSettings = parser.getPeptideSettings();
            ModificationInfo modInfo = insertPeptideSettings(pepSettings);

            // Spectrum library settings
            Map<String, Long> libraryNameIdMap = insertSpectrumLibrarySettings(pepSettings);

            // Data settings -- these are the annotation settings
            List<GroupComparisonSettings> groupComparisons = insertDataSettings(parser);

            // calculate and insert values for trace metrics
            insertTraceCalculations(run);

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
            Set<String> peptides = new TreeSet<>();
            Set<String> smallMolecules = new TreeSet<>();

            // If this is a QC folder get the set of peptides (or small molecules) in the container before we start parsing the targets in the document.
            // In PostgreSQL's default, "Read Committed" isolation level: "..SELECT does see the effects of previous updates executed within its own transaction,
            // even though they are not yet committed". https://www.postgresql.org/docs/current/transaction-iso.html
            Set<String> expectedPeptides = Collections.emptySet();
            Set<String> expectedMolecules = Collections.emptySet();
            if (folderType == TargetedMSService.FolderType.QC)
            {
                expectedPeptides = TargetedMSManager.getDistinctPeptides(_container);
                expectedMolecules = TargetedMSManager.getDistinctMolecules(_container);
            }

            int peptideGroupCount = 0;
            while (parser.hasNextPeptideGroup())
            {
                // TODO: bulk insert of precursor, transition, chrom info etc.
                PeptideGroup pepGroup = parser.nextPeptideGroup();
                insertPeptideGroup(proteinService, replicateInfo.skylineIdSampleFileIdMap,
                        modInfo, libraryNameIdMap, pepGroup, parser, peptides, smallMolecules, parser.getTransitionSettings());
                if (++peptideGroupCount % 100 == 0)
                {
                    _log.info("Imported " + peptideGroupCount + " peptide groups.");
                }
            }

            if (!parser.shouldSaveTransitionChromInfos())
            {
                _log.info("None of the " + parser.getTransitionChromInfoCount() + " TransitionChromInfos in the file were imported because they exceed the limit of " + SkylineDocumentParser.MAX_TRANSITION_CHROM_INFOS);
                SQLFragment whereClause = new SQLFragment("WHERE r.Id = ?", _runId);

                // Clear out any of the TransitionChromInfo and related tables that we inserted before we exceeded
                // the max
                TargetedMSManager.deleteTransitionChromInfoDependent(TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), whereClause);
                TargetedMSManager.deleteTransitionChromInfoDependent(TargetedMSManager.getTableInfoTransitionAreaRatio(), whereClause);
                TargetedMSManager.deleteGeneralTransitionDependent(getTableInfoTransitionChromInfo(), "TransitionId", whereClause);

                // Since we don't have the TransitionChromInfos to use for the indices, copy them from the temp table
                // into PrecursorChromInfo (but filter to only touch the rows where we have matches in the temp table)
                int updated = new SqlExecutor(TargetedMSSchema.getSchema()).execute("UPDATE " + getTableInfoPrecursorChromInfo() + " " +
                            "SET TransitionChromatogramIndices = (SELECT Indices FROM " + precursorChromInfoIndicesTempTableName +
                        " WHERE Id = PrecursorChromInfoId) WHERE Id IN (SELECT PrecursorChromInfoId FROM " + precursorChromInfoIndicesTempTableName + ")");
                _log.info("Updated " + updated + " PrecursorChromInfos with transition chromatogram index information");
            }

            // We're done with this table now, used or not
            new SqlExecutor(TargetedMSSchema.getSchema()).execute("DROP TABLE " + precursorChromInfoIndicesTempTableName);

            // Done parsing document
            _progressMonitor.getParserProgressTracker().complete("Done parsing Skyline document.");

            if (folderType == TargetedMSService.FolderType.QC)
            {
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

            if (run.isRepresentative())
            {
                // Persist the run so that the skydDataId is available when writing the updated chromatogram library.
                // skydDataId is required to get to the skyd file for reading chromatograms when they are not saved in the db.
                Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());
                RepresentativeStateManager.setRepresentativeState(_user, _container, _localDirectory, run, run.getRepresentativeDataState());
            }

            int calCurvesCount = quantifyRun(run, pepSettings, groupComparisons);

            SkylineAuditLogManager importer = new SkylineAuditLogManager(_container, _log);
            int auditLogEntriesCount = importer.importAuditLogFile(_auditLogFile, parser.getDocumentGUID(), run);

            run.setAuditLogEntriesCount(auditLogEntriesCount);
            run.setPeptideGroupCount(parser.getPeptideGroupCount());
            run.setPeptideCount(parser.getPeptideCount());
            run.setSmallMoleculeCount(parser.getSmallMoleculeCount());
            run.setPrecursorCount(parser.getPrecursorCount());
            run.setTransitionCount(parser.getTransitionCount());
            run.setReplicateCount(parser.getReplicateCount());
            run.setCalibrationCurveCount(calCurvesCount);
            run.setListCount(parser.getListCount());
            run.setDocumentGUID(parser.getDocumentGUID());

            Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

            if (folderType == TargetedMSService.FolderType.QC)
            {
                deleteOldSampleFiles(replicateInfo);

                TargetedMSManager.purgeUnreferencedReplicates(_container);
                List<String> msgs = TargetedMSManager.purgeUnreferencedFiles(replicateInfo.potentiallyUnusedFiles, _container, _user);
                for (String msg : msgs)
                {
                    _log.info(msg);
                }
            }
            else if (folderType == TargetedMSService.FolderType.Library)
            {
                // Add the Peptides and Molecules tabs if needed
                List<Portal.PortalPage> tabPages = Portal.getTabPages(_container);
                Portal.PortalPage peptidesTab = null;
                Portal.PortalPage moleculesTab = null;
                for (Portal.PortalPage tabPage : tabPages)
                {
                    if (TargetedMSModule.PEPTIDE_TAB_NAME.equals(tabPage.getPageId()))
                    {
                        peptidesTab = tabPage;
                    }
                    if (TargetedMSModule.MOLECULE_TAB_NAME.equals(tabPage.getPageId()))
                    {
                        moleculesTab = tabPage;
                    }
                }
                if (TargetedMSManager.containerHasSmallMolecules(_container) && moleculesTab == null)
                {
                    TargetedMSController.addDashboardTab(TargetedMSModule.MOLECULE_TAB_NAME, _container, TargetedMSModule.MOLECULE_TAB_WEB_PARTS);
                }
                if (TargetedMSManager.containerHasPeptides(_container) && peptidesTab == null)
                {
                    TargetedMSController.addDashboardTab(TargetedMSModule.PEPTIDE_TAB_NAME, _container, TargetedMSModule.PEPTIDE_TAB_WEB_PARTS);
                }
            }

            parser.logMissingChromatogramCounts();

            TargetedMSManager.updateModifiedAreaProportions(_log, run);

            if (_pipeRoot.isCloudRoot())
                copyExtractedFilesToCloud(run);
        }
        finally
        {
            if (_precursorChromInfoIndicesStmt != null) { try { _precursorChromInfoIndicesStmt.close(); } catch (SQLException ignored) {} }
            _precursorChromInfoIndicesStmt = null;
            if (_transitionChromInfoAnnotationStmt != null) { try { _transitionChromInfoAnnotationStmt.close(); } catch (SQLException ignored) {} }
            _transitionChromInfoAnnotationStmt = null;
            if (_transitionAnnotationStmt != null) { try { _transitionAnnotationStmt.close(); } catch (SQLException ignored) {} }
            _transitionAnnotationStmt = null;
            if (_precursorChromInfoAnnotationStmt != null) { try { _precursorChromInfoAnnotationStmt.close(); } catch (SQLException ignored) {} }
            _precursorChromInfoAnnotationStmt = null;
            if (_precursorAnnotationStmt != null) { try { _precursorAnnotationStmt.close(); } catch (SQLException ignored) {} }
            _precursorAnnotationStmt = null;
            if (_transitionChromInfoStmt != null) { try { _transitionChromInfoStmt.close(); } catch (SQLException ignored) {} }
            _transitionChromInfoStmt = null;
            if (_precursorChromInfoStmt != null) { try { _precursorChromInfoStmt.close(); } catch (SQLException ignored) {} }
            _precursorChromInfoStmt = null;
            if (_generalMoleculeAnnotationStmt != null) { try { _generalMoleculeAnnotationStmt.close(); } catch (SQLException ignored) {} }
            _generalMoleculeAnnotationStmt = null;
        }
    }

    private ReplicateInfo insertReplicates(TargetedMSRun run, SkylineDocumentParser parser, OptimizationInfo optimizationInfo, TargetedMSService.FolderType folderType)
    {
        ReplicateInfo replicateInfo = new ReplicateInfo();

        Map<Instrument, Long> instrumentIdMap = new HashMap<>();

        Map<String, SampleFile> pathToSampleFile = new HashMap<>();
        for(SkylineReplicate skyReplicate: parser.getReplicates())
        {
            Replicate replicate = new Replicate();
            replicate.setName(skyReplicate.getName());
            replicate.setSampleFileList(skyReplicate.getSampleFileList());
            replicate.setAnnotations(skyReplicate.getAnnotations());
            replicate.setSampleType(skyReplicate.getSampleType());
            replicate.setAnalyteConcentration(skyReplicate.getAnalyteConcentration());
            replicate.setSampleDilutionFactor(skyReplicate.getSampleDilutionFactor());
            replicate.setHasMidasSpectra(skyReplicate.hasMidasSpectra());
            replicate.setBatchName(skyReplicate.getBatchName());
            replicate.setRunId(_runId);

            if(optimizationInfo._cePredictor != null && skyReplicate.getCePredictor() != null && optimizationInfo._cePredictor.equals(skyReplicate.getCePredictor()))
            {
                replicate.setCePredictorId(optimizationInfo._cePredictor.getId());
            }
            if(optimizationInfo._dpPredictor != null && skyReplicate.getDpPredictor() != null && optimizationInfo._dpPredictor.equals(skyReplicate.getDpPredictor()))
            {
                replicate.setDpPredictorId(optimizationInfo._dpPredictor.getId());
            }

            if (folderType == TargetedMSService.FolderType.QC)
            {
                for (SampleFile sampleFile : replicate.getSampleFileList())
                {
                    // It's possible that a data file is referenced in multiple replicates, so handle that case
                    for (SampleFile existingSample : TargetedMSManager.getMatchingSampleFiles(sampleFile, run.getContainer()))
                    {
                        Replicate existingReplicate = TargetedMSManager.getReplicate(existingSample.getReplicateId(), run.getContainer());
                        if (existingReplicate != null && existingReplicate.getRunId() != run.getId())
                        {
                            replicateInfo.addSampleToDelete(sampleFile.getFilePath(), existingSample);
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

                if (annotation.isIgnoreInQC() && folderType == TargetedMSService.FolderType.QC)
                    ignoreInQcAnnot = annotation;
            }

            handleReplicateExclusions(replicate, ignoreInQcAnnot);

            insertSampleFiles(replicateInfo, instrumentIdMap, replicate, pathToSampleFile);
        }

        for (SampleFileChromInfo sampleFileChromInfo : parser.getSampleFileChromInfos(pathToSampleFile))
        {
            Table.insert(_user, TargetedMSManager.getTableInfoSampleFileChromInfo(), sampleFileChromInfo);
        }

        return replicateInfo;
    }

    private void deleteOldSampleFiles(ReplicateInfo replicateInfo)
    {
        int total = replicateInfo.oldSamplesToDelete.values().stream().mapToInt(Set::size).sum();
        int s = 0;
        IProgressStatus status = _progressMonitor.getQcCleanupProgressTracker();
        for(Map.Entry<String, Set<SampleFile>> entry: replicateInfo.oldSamplesToDelete.entrySet())
        {
            for (SampleFile existingSample : entry.getValue())
            {
                String srcFile = TargetedMSManager.deleteSampleFileAndDependencies(existingSample.getId());
                _log.debug(String.format("Updating previously imported data for sample file %s in QC folder. %d of %d", entry.getKey(), ++s, total));

                if (null != srcFile)
                {
                    try
                    {
                        replicateInfo.potentiallyUnusedFiles.add(new URI(srcFile));
                    }
                    catch (URISyntaxException e)
                    {
                        _log.error("Unable to delete file " + srcFile + ". May be an invalid path. This file is no longer needed on the server.");
                    }
                }
                status.updateProgress(s, total);
            }
        }
        status.complete(total > 0 ? "Done updating previously imported sample file data." : "Did not find any older sample file data to delete.");
    }

    @NotNull
    private List<GroupComparisonSettings> insertDataSettings(SkylineDocumentParser parser)
    {
        DataSettings dataSettings = parser.getDataSettings();
        for(AnnotationSetting annotSetting: dataSettings.getAnnotationSettings())
        {
            annotSetting.setRunId(_runId);
            Table.insert(_user, TargetedMSManager.getTableInfoAnnotationSettings(), annotSetting);
        }
        List<GroupComparisonSettings> groupComparisons = new ArrayList<>(dataSettings.getGroupComparisons());
        for (GroupComparisonSettings groupComparison : groupComparisons)
        {
            groupComparison.setRunId(_runId);
            Table.insert(_user, TargetedMSManager.getTableInfoGroupComparisonSettings(), groupComparison);
        }
        for (ListData listData : dataSettings.getListDatas()) {
            listData.getListDefinition().setRunId(_runId);
            SkylineListManager.saveListData(_user, listData);
        }
        return groupComparisons;
    }

    private static class ReplicateInfo
    {
        private final Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap = new HashMap<>();
        private final Set<URI> potentiallyUnusedFiles = new HashSet<>();
        // In QC folders any sample files from older documents that match a sample file in the current document
        // are deleted. We keep only the most current version of a sample file.
        // Key in the map below is the sample file path in the current document; Value is a Set of sample files
        // from older documents that match (same file name and acquisition time).
        // Issue 39401 - Keep unique sample files so that we don't try to delete the same file twice.
        private final Map<String, Set<SampleFile>> oldSamplesToDelete = new HashMap<>();

        public void addSampleToDelete(String currentSamplePath, SampleFile oldSampleFile)
        {
            oldSamplesToDelete.computeIfAbsent(currentSamplePath, k -> new HashSet<>());
            oldSamplesToDelete.get(currentSamplePath).add(oldSampleFile);
        }
    }

    private static class ModificationInfo
    {
        private final Map<String, Long> isotopeLabelIdMap = new HashMap<>();
        private final Set<Long> internalStandardLabelIds = new HashSet<>();
        private final Map<String, Long> structuralModNameIdMap = new HashMap<>();
        private final Map<Long, List<PeptideSettings.PotentialLoss>> structuralModLossesMap = new HashMap<>();
        private final Map<String, Long> isotopeModNameIdMap = new HashMap<>();
    }

    private ModificationInfo insertPeptideSettings(PeptideSettings pepSettings)
    {
        ModificationInfo modInfo = insertModifications(pepSettings.getModifications());

        insertDigestSettings(pepSettings);

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

        return modInfo;
    }

    private void insertDigestSettings(PeptideSettings pepSettings)
    {
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
            List<PeptideSettings.Enzyme> existingEnzymes = new TableSelector(TargetedMSManager.getTableInfoEnzyme(), filter, null).getArrayList(PeptideSettings.Enzyme.class);
            if (existingEnzymes.isEmpty())
            {
                enzyme = Table.insert(_user, TargetedMSManager.getTableInfoEnzyme(), enzyme);
            }
            else
            {
                enzyme = existingEnzymes.get(0);
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
    }

    @NotNull
    private Map<String, Long> insertSpectrumLibrarySettings(PeptideSettings pepSettings)
    {
        Map<String, Long> libraryNameIdMap = new HashMap<>();
        PeptideSettings.SpectrumLibrarySettings librarySettings = pepSettings.getLibrarySettings();
        if(librarySettings != null)
        {
            librarySettings.setRunId(_runId);
            Table.insert(_user, TargetedMSManager.getTableInfoLibrarySettings(), librarySettings);

            for(PeptideSettings.SpectrumLibrary library: librarySettings.getLibraries())
            {
                library.setRunId(_runId);
                library = Table.insert(_user, TargetedMSManager.getTableInfoSpectrumLibrary(), library);
                libraryNameIdMap.put(library.getName(), library.getId());
            }
        }
        return libraryNameIdMap;
    }

    private ModificationInfo insertModifications(PeptideSettings.PeptideModifications modifications)
    {
        ModificationInfo modInfo = new ModificationInfo();

        if (modifications == null)
        {
            return modInfo;
        }

        // Insert isotope labels
        List<PeptideSettings.IsotopeLabel> isotopeLabels = modifications.getIsotopeLabels();
        for(PeptideSettings.IsotopeLabel isotopeLabel: isotopeLabels)
        {
            isotopeLabel.setRunId(_runId);
            isotopeLabel = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeLabel(), isotopeLabel);
            modInfo.isotopeLabelIdMap.put(isotopeLabel.getName(), isotopeLabel.getId());

            if(isotopeLabel.isStandard())
            {
                modInfo.internalStandardLabelIds.add(isotopeLabel.getId());
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
                modInfo.structuralModNameIdMap.put(mod.getName(), existingMod.getId());
            }
            else
            {
                mod = Table.insert(_user, TargetedMSManager.getTableInfoStructuralModification(), mod);
                mod.setStructuralModId(mod.getId());
                modInfo.structuralModNameIdMap.put(mod.getName(), mod.getId());

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

            modInfo.isotopeModNameIdMap.put(mod.getName(), mod.getIsotopeModId());
            mod.setRunId(_runId);
            mod.setIsotopeLabelId(modInfo.isotopeLabelIdMap.get(mod.getIsotopeLabel()));
            Table.insert(_user, TargetedMSManager.getTableInfoRunIsotopeModification(), mod);
        }

        return modInfo;
    }

    private void insertFullScanSettings(@NotNull TransitionSettings.FullScanSettings fullScanSettings)
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

    private void handleReplicateExclusions(Replicate replicate, ReplicateAnnotation ignoreInQcAnnot)
    {
        // keep any existing exclusions for this replicate by name
        List<Integer> existingExclusions = ReplicateManager.getReplicateExclusions(replicate.getName(), _container);
        boolean hasExistingExcludeAllMetrics = false;
        for (Integer metricId : existingExclusions)
        {
            ReplicateManager.insertReplicateExclusion(_user, replicate.getId(), metricId);
            if (metricId == null)
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
            if (zipDir.exists())
            {
                FileUtil.deleteDirectoryContents(zipDir);
            }
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
                else if (SkylineFileUtils.EXT_SKY_LOG.equalsIgnoreCase(ext))
                {
                    _auditLogFile = file;   //prepare for the log file extraction
                }
            }

            if(skyFile == null)
            {
                throw new IOException("zip file " + f + " does not contain a .sky file");
            }
            f = skyFile;
        }
        else
        {
            ext = FileUtil.getExtension(f.getName());
            if (SkylineFileUtils.EXT_SKY.equalsIgnoreCase(ext))
            {
                File possibleAuditFile = new File(f.getParent(), FileUtil.getBaseName(f) + "." + SkylineFileUtils.EXT_SKY_LOG);
                if (possibleAuditFile.isFile())
                {
                    _auditLogFile = possibleAuditFile;
                }
            }
        }
        return f;
    }

    /**
     * Insert new iRT scale/peptides, or update existing scale for library folders
     *
     * @return The existing or new iRT Scale Id for the imported iRT Peptide set. null if no iRT Data for this run.
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
            if (scaleIds.isEmpty() || TargetedMSManager.getFolderType(_container) == TargetedMSService.FolderType.Experiment)
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

    private void insertPeptideGroup(ProteinService proteinService, Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                    ModificationInfo modInfo,
                                    Map<String, Long> libraryNameIdMap, PeptideGroup pepGroup, SkylineDocumentParser parser, Set<String> peptides, Set<String> smallMolecules,
                                    TransitionSettings transitionSettings)
            throws XMLStreamException, IOException
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
        int peptideCount = 0;
        int moleculeCount = 0;

        while((molType = parser.hasNextPeptideOrMolecule()) != null)
        {
            GeneralMolecule generalMolecule = null;
            switch(molType)
            {
                case PEPTIDE:
                    Peptide peptide = parser.nextPeptide(pepGroup.isDecoy());
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
                    peptideCount++;
                    if(peptideCount % 50 == 0)
                    {
                        _log.debug(String.format("Inserted %d peptides", peptideCount));
                    }
                    break;
                case MOLECULE:
                    Molecule molecule = parser.nextMolecule(pepGroup.isDecoy());
                    if (molecule.getIonFormula() != null)
                    {
                        // Some molecules only have a mass, no formula. Just omit them from the check.
                        smallMolecules.add(molecule.getIonFormula());
                    }
                    generalMolecule = molecule;
                    moleculeCount++;
                    if(moleculeCount % 50 == 0)
                    {
                        _log.debug(String.format("Inserted %d molecules", moleculeCount));
                    }
                    break;
            }

            insertPeptideOrSmallMolecule(skylineIdSampleFileIdMap, modInfo,
                    libraryNameIdMap, pepGroup, generalMolecule, transitionSettings, parser);
        }
        if(peptideCount > 0)
        {
            _log.debug(String.format("Total peptides inserted: %d", peptideCount));
        }
        if(moleculeCount > 0)
        {
            _log.debug(String.format("Total molecules inserted: %d", moleculeCount));
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

    private void insertPeptideOrSmallMolecule(Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                              ModificationInfo modInfo,
                                              Map<String, Long> libraryNameIdMap,
                                              PeptideGroup pepGroup,
                                              GeneralMolecule generalMolecule,
                                              TransitionSettings transitionSettings,
                                              SkylineDocumentParser parser)
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

            Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap = insertGeneralMoleculeChromInfos(generalMolecule.getId(),
                    molecule.getGeneralMoleculeChromInfoList(), skylineIdSampleFileIdMap);

            for (MoleculePrecursor moleculePrecursor : molecule.getMoleculePrecursorsList())
            {
                insertMoleculePrecursor(molecule, moleculePrecursor, skylineIdSampleFileIdMap, modInfo, sampleFileIdGeneralMolChromInfoIdMap, parser);
            }
        }

        for (GeneralMoleculeAnnotation annotation : generalMolecule.getAnnotations())
        {
            annotation.setGeneralMoleculeId(generalMolecule.getId());
            insertGeneralMoleculeAnnotation(annotation);
        }

        if (peptide != null)
        {
            for (Peptide.StructuralModification mod : peptide.getStructuralMods())
            {
                insertStructuralModification(modInfo, peptide, mod);
            }

            for (Peptide.IsotopeModification mod : peptide.getIsotopeMods())
            {
                insertIsotopeModification(modInfo, peptide, mod);
            }

            Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap = insertGeneralMoleculeChromInfos(generalMolecule.getId(),
                    peptide.getGeneralMoleculeChromInfoList(), skylineIdSampleFileIdMap);

            // 3. precursor
            for (Precursor precursor : peptide.getPrecursorList())
            {
                insertPrecursor(
                        skylineIdSampleFileIdMap,
                        modInfo,
                        libraryNameIdMap,
                        peptide,
                        sampleFileIdGeneralMolChromInfoIdMap,
                        precursor, parser);
            }

            // 4. Calculate and insert peak area ratios
            PeakAreaRatioCalculator areaRatioCalculator = new PeakAreaRatioCalculator(peptide, transitionSettings);
            areaRatioCalculator.init(skylineIdSampleFileIdMap);
            // Insert area ratios for each combination of 2 isotope labels
            for (Long numLabelId : modInfo.isotopeLabelIdMap.values())
            {
                for (Long denomLabelId : modInfo.isotopeLabelIdMap.values())
                {
                    if (!modInfo.internalStandardLabelIds.contains(denomLabelId))
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

    private void insertIsotopeModification(ModificationInfo modInfo, Peptide peptide, Peptide.IsotopeModification mod)
    {
        long modId = modInfo.isotopeModNameIdMap.get(mod.getModificationName());
        mod.setIsotopeModId(modId);
        mod.setPeptideId(peptide.getId());
        mod.setIsotopeLabelId(modInfo.isotopeLabelIdMap.get(mod.getIsotopeLabel()));
        Table.insert(_user, TargetedMSManager.getTableInfoPeptideIsotopeModification(), mod);
    }

    private void insertStructuralModification(ModificationInfo modInfo, Peptide peptide, Peptide.StructuralModification mod)
    {
        long modId = modInfo.structuralModNameIdMap.get(mod.getModificationName());
        mod.setStructuralModId(modId);
        mod.setPeptideId(peptide.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoPeptideStructuralModification(), mod);
    }

    private void insertMoleculePrecursor(Molecule molecule,
                                         MoleculePrecursor moleculePrecursor, Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                         ModificationInfo modInfo,
                                         Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap, SkylineDocumentParser parser)
    {
        GeneralPrecursor<?> gp = insertGeneralPrecursor(modInfo, molecule, moleculePrecursor);

        moleculePrecursor.setIsotopeLabelId(gp.getIsotopeLabelId());
        moleculePrecursor.setId(gp.getId());

        moleculePrecursor = Table.insert(_user, TargetedMSManager.getTableInfoMoleculePrecursor(), moleculePrecursor);

        //small molecule precursor annotations
        insertPrecursorAnnotation(moleculePrecursor.getAnnotations(), gp, moleculePrecursor.getId()); //adding small molecule precursor annotation in PrecursorAnnotation table. We might need to change this if we decide to have a separate MoleculePrecursorAnnotation table in the future.

        Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap = insertPrecursorChromInfos(gp.getId(),
                moleculePrecursor.getCustomIonName(), moleculePrecursor.getChromInfoList(), skylineIdSampleFileIdMap, sampleFileIdGeneralMolChromInfoIdMap);

        for(MoleculeTransition moleculeTransition: moleculePrecursor.getTransitionsList())
        {
            insertMoleculeTransition(molecule, moleculePrecursor, moleculeTransition, skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap, parser);
        }
    }

    private void insertPrecursorAnnotation(List<PrecursorAnnotation> precursorAnnotations, GeneralPrecursor<?> gp, long id)
    {
        for (PrecursorAnnotation annotation : precursorAnnotations)
        {
            annotation.setPrecursorId(id);
            annotation.setGeneralPrecursorId(gp.getId());
            insertPrecursorAnnotation(annotation);
        }
    }

    private void insertMoleculeTransition(Molecule molecule, MoleculePrecursor moleculePrecursor,
                                          MoleculeTransition moleculeTransition, Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                          Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap,
                                          SkylineDocumentParser parser)
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
        gt.setExplicitSLens(moleculeTransition.getExplicitSLens());
        gt.setExplicitConeVoltage(moleculeTransition.getExplicitConeVoltage());
        gt.setQuantitative(moleculeTransition.getQuantitative());
        gt.setExplicitIonMobilityHighEnergyOffset(moleculeTransition.getExplicitIonMobilityHighEnergyOffset());
        gt.setExplicitDeclusteringPotential(moleculeTransition.getExplicitDeclusteringPotential());
        gt.setCollisionEnergy(moleculeTransition.getCollisionEnergy());
        gt.setDeclusteringPotential(moleculeTransition.getDeclusteringPotential());
        gt = Table.insert(_user, TargetedMSManager.getTableInfoGeneralTransition(), gt);

        moleculeTransition.setTransitionId(gt.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoMoleculeTransition(), moleculeTransition);

        //small molecule transition annotations
        insertTransitionAnnotation(moleculeTransition.getAnnotations(), moleculeTransition.getId()); //adding small molecule transition annotation in TransitionAnnotation table. We might need to change this if we decide to have a separate MoleculeTransitionAnnotation table in the future.

        for (Iterator<OptimizationDBRow> i = parser.getOptimizationInfos().iterator(); i.hasNext();)
        {
            OptimizationDBRow info = i.next();
            if (info.matches(moleculeTransition, moleculePrecursor, molecule))
            {
                // Remove it from the list to make subsequent matches faster
                i.remove();
                insertOptimization(info, moleculeTransition);
            }
        }

        insertTransitionChromInfos(gt.getId(), moleculeTransition.getChromInfoList(), skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap, parser);
    }

    private void insertTransitionAnnotation(List<TransitionAnnotation> annotations, long id)
    {
        for (TransitionAnnotation annotation : annotations)
        {
            annotation.setTransitionId(id);
            insertTransitionAnnotation(annotation);
        }
    }

    private void insertPrecursor(Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                 ModificationInfo modInfo,
                                 Map<String, Long> libraryNameIdMap,
                                 Peptide peptide,
                                 Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap,
                                 Precursor precursor,
                                 SkylineDocumentParser parser)
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

        GeneralPrecursor<?> gp = insertGeneralPrecursor(modInfo, peptide, precursor);

        precursor.setIsotopeLabelId(gp.getIsotopeLabelId());
        precursor.setId(gp.getId());
        precursor = Table.insert(_user, TargetedMSManager.getTableInfoPrecursor(), precursor);

        insertPrecursorAnnotation(precursor.getAnnotations(), gp, precursor.getId());

        insertLibInfo(precursor.getBibliospecLibraryInfo(), precursor, gp, libraryNameIdMap, TargetedMSManager.getTableInfoBibliospec());
        insertLibInfo(precursor.getHunterLibraryInfo(), precursor, gp, libraryNameIdMap, TargetedMSManager.getTableInfoHunterLib());
        insertLibInfo(precursor.getNistLibraryInfo(), precursor, gp, libraryNameIdMap, TargetedMSManager.getTableInfoNistLib());
        insertLibInfo(precursor.getSpectrastLibraryInfo(), precursor, gp, libraryNameIdMap, TargetedMSManager.getTableInfoSpectrastLib());
        insertLibInfo(precursor.getChromatogramLibraryInfo(), precursor, gp, libraryNameIdMap, TargetedMSManager.getTableInfoChromatogramLib());

        Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap = insertPrecursorChromInfos(gp.getId(),
                precursor.getModifiedSequence(), precursor.getChromInfoList(), skylineIdSampleFileIdMap, sampleFileIdGeneralMolChromInfoIdMap);

        // 4. transition
        for(Transition transition: precursor.getTransitionsList())
        {
            insertTransition(skylineIdSampleFileIdMap, modInfo, precursor, sampleFilePrecursorChromInfoIdMap, transition, parser);
        }
    }

    private void insertLibInfo(Precursor.LibraryInfo libraryInfo, Precursor precursor, GeneralPrecursor<?> gp, Map<String, Long> libraryNameIdMap, TableInfo tableInfo)
    {
        if(libraryInfo != null)
        {
            Long specLibId = libraryNameIdMap.get(libraryInfo.getLibraryName());
            if(specLibId == null)
            {
                // Skyline documents can end up in a state where a library name is associated with a precursor but the
                // library was deselected in "Peptide Settings > Library tab" in Skyline and is no longer part of the
                // <peptide_libraries> element of the .sky file.  We will ignore such library infos.
                if (_missingLibraries.add(libraryInfo.getLibraryName()))
                {
                    // Only log the first time
                    _log.warn("'" + libraryInfo.getLibraryName() + "' library was not found in settings.");
                }
            }
            else
            {
                libraryInfo.setPrecursorId(precursor.getId());
                libraryInfo.setSpectrumLibraryId(specLibId);
                libraryInfo.setGeneralPrecursorId(gp.getId());
                Table.insert(_user, tableInfo, libraryInfo);
            }
        }
    }

    private GeneralPrecursor<?> insertGeneralPrecursor(ModificationInfo modInfo, GeneralMolecule peptide, GeneralPrecursor<?> precursor)
    {
        //setting values for GeneralPrecursor here seems odd - is there a better way?
        GeneralPrecursor<?> gp = new GeneralPrecursor<>();
        gp.setGeneralMoleculeId(peptide.getId());
        gp.setMz(precursor.getMz());
        gp.setCharge(precursor.getCharge());
        gp.setCollisionEnergy(precursor.getCollisionEnergy());
        gp.setDeclusteringPotential(precursor.getDeclusteringPotential());
        gp.setNote(precursor.getNote());
        gp.setExplicitIonMobility(precursor.getExplicitIonMobility());
        gp.setCcs(precursor.getCcs());
        gp.setExplicitCcsSqa(precursor.getExplicitCcsSqa());
        gp.setExplicitIonMobilityUnits(precursor.getExplicitIonMobilityUnits());
        gp.setExplicitCompensationVoltage(precursor.getExplicitCompensationVoltage());
        gp.setPrecursorConcentration(precursor.getPrecursorConcentration());
        gp.setIsotopeLabel(precursor.getIsotopeLabel());
        gp.setIsotopeLabelId(modInfo.isotopeLabelIdMap.get(precursor.getIsotopeLabel()));
        gp = Table.insert(_user, TargetedMSManager.getTableInfoGeneralPrecursor(), gp);
        return gp;
    }

    private void insertTransition(Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                  ModificationInfo modInfo,
                                  Precursor precursor,
                                  Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap,
                                  Transition transition, SkylineDocumentParser parser)
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
        gt.setExplicitSLens(transition.getExplicitSLens());
        gt.setExplicitConeVoltage(transition.getExplicitConeVoltage());
        gt.setQuantitative(transition.getQuantitative());
        gt.setExplicitIonMobilityHighEnergyOffset(transition.getExplicitIonMobilityHighEnergyOffset());
        gt.setExplicitDeclusteringPotential(transition.getExplicitDeclusteringPotential());
        gt.setCollisionEnergy(transition.getCollisionEnergy());
        gt.setDeclusteringPotential(transition.getDeclusteringPotential());
        gt.setRank(transition.getRank());
        gt.setIntensity(transition.getIntensity());
        gt = Table.insert(_user, TargetedMSManager.getTableInfoGeneralTransition(), gt);

        transition.setId(gt.getId());
        Table.insert(_user, TargetedMSManager.getTableInfoTransition(), transition);

        // transition annotations
        insertTransitionAnnotation(transition.getAnnotations(), transition.getId());

        for (Iterator<OptimizationDBRow> i = parser.getOptimizationInfos().iterator(); i.hasNext();)
        {
            OptimizationDBRow info = i.next();
            if (info.matches(transition, precursor))
            {
                // Remove it from the list to make subsequent matches faster
                i.remove();
                insertOptimization(info, transition);
            }
        }

        // transition results
        insertTransitionChromInfos(gt.getId(), transition.getChromInfoList(), skylineIdSampleFileIdMap, sampleFilePrecursorChromInfoIdMap, parser);

        // transition neutral losses
        for (TransitionLoss loss : transition.getNeutralLosses())
        {
            if(loss.getModificationName() != null)
            {
                Long modificationId = modInfo.structuralModNameIdMap.get(loss.getModificationName());
                if (modificationId == null)
                {
                    throw new PanoramaBadDataException("No such structural modification found: " + loss.getModificationName());
                }
                List<PeptideSettings.PotentialLoss> potentialLosses = modInfo.structuralModLossesMap.get(modificationId);
                if (potentialLosses == null)
                {
                    potentialLosses = new TableSelector(TargetedMSManager.getTableInfoStructuralModLoss(),
                                                        new SimpleFilter(FieldKey.fromString("structuralmodid"), modificationId),
                                                        new Sort("id")) // Sort by insertion id so that we can find the right match
                                                                        // if there were multiple potential losses defined for the modification
                                                        .getArrayList(PeptideSettings.PotentialLoss.class);
                    modInfo.structuralModLossesMap.put(modificationId, potentialLosses);
                }

                if(loss.getLossIndex() == null)
                {
                    throw new PanoramaBadDataException("No loss index found for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.toString()
                                                    +"; Precursor: "+precursor.getModifiedSequence());
                }
                if(loss.getLossIndex() < 0 || loss.getLossIndex() >= potentialLosses.size())
                {
                    throw new PanoramaBadDataException("Loss index out of bounds for transition loss."
                                                    +" Loss: "+loss.toString()
                                                    +"; Transition: "+transition.toString()
                                                    +"; Precursor: "+precursor.getModifiedSequence());
                }

                loss.setStructuralModLossId(potentialLosses.get(loss.getLossIndex()).getId());
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

    private void insertOptimization(OptimizationDBRow info, GeneralTransition transition)
    {
        TransitionOptimization opt = new TransitionOptimization();
        opt.setTransitionId(transition.getId());
        opt.setOptValue(info.getValue());
        opt.setOptimizationType(info.getType());
        Table.insert(_user, TargetedMSManager.getTableInfoTransitionOptimization(), opt);
    }

    private Map<Long, Long> insertGeneralMoleculeChromInfos(long gmId, List<GeneralMoleculeChromInfo> generalMoleculeChromInfos,
                                                                  Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap)
    {
        Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap = new HashMap<>();

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

    private Map<SampleFileOptStepKey, Long> insertPrecursorChromInfos(long gpId, String label,
                                                                         List<PrecursorChromInfo> precursorChromInfos,
                                                                         Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                                                         Map<Long, Long> sampleFileIdGeneralMolChromInfoIdMap)
    {
        Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap = new HashMap<>();

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

            insertPrecursorChromInfo(precursorChromInfo);

            sampleFilePrecursorChromInfoIdMap.put(sampleFileKey, precursorChromInfo.getId());

            for (PrecursorChromInfoAnnotation annotation : precursorChromInfo.getAnnotations())
            {
                annotation.setPrecursorChromInfoId(precursorChromInfo.getId());
                insertPrecursorChromInfoAnnotation(annotation);
            }
        }

        return sampleFilePrecursorChromInfoIdMap;
    }

    private void insertTransitionChromInfos(long gtId, List<TransitionChromInfo> transitionChromInfos,
                                            Map<SampleFileKey, SampleFile> skylineIdSampleFileIdMap,
                                            Map<SampleFileOptStepKey, Long> sampleFilePrecursorChromInfoIdMap,
                                            SkylineDocumentParser parser)
    {
        if (!parser.shouldSaveTransitionChromInfos())
        {
            // Bail out and don't persist to the DB
            return;
        }

        for (TransitionChromInfo transChromInfo : transitionChromInfos)
        {
            SampleFile sampleFile = skylineIdSampleFileIdMap.get(SampleFileKey.getKey(transChromInfo.getReplicateName(), transChromInfo.getSkylineSampleFileId()));
            if (sampleFile == null)
            {
                throw new PanoramaBadDataException("Database ID not found for Skyline samplefile id " + transChromInfo.getSkylineSampleFileId() + " in replicate " + transChromInfo.getReplicateName());
            }
            transChromInfo.setTransitionId(gtId);
            transChromInfo.setSampleFileId(sampleFile.getId());
            // Lookup a precursor chrom info measured in the same sample file with the same optimization step
            SampleFileOptStepKey sampleFileKey = SampleFileOptStepKey.getKey(transChromInfo);
            Long precursorChromInfoId = sampleFilePrecursorChromInfoIdMap.get(sampleFileKey);
            if (precursorChromInfoId == null)
            {
                throw new PanoramaBadDataException("Could not find precursor peak for " + sampleFileKey.toString());
            }

            transChromInfo.setPrecursorChromInfoId(precursorChromInfoId);

            insertTransitionChromInfo(transChromInfo);

            for (TransitionChromInfoAnnotation annotation : transChromInfo.getAnnotations())
            {
                annotation.setTransitionChromInfoId(transChromInfo.getId());
                insertTransitionChromInfoAnnotation(annotation);
            }
        }
    }

    private void insertTransitionChromInfoAnnotation(TransitionChromInfoAnnotation annotation)
    {
        try
        {
            _transitionChromInfoAnnotationStmt = ensureStatement(_transitionChromInfoAnnotationStmt,
                    "INSERT INTO targetedms.transitionchrominfoannotation(transitionchrominfoid, name, value) VALUES (?, ?, ?)",
                    false);

            insertAnnotation(_transitionChromInfoAnnotationStmt, annotation, annotation.getTransitionChromInfoId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertPrecursorAnnotation(PrecursorAnnotation annotation)
    {
        try
        {
            _precursorAnnotationStmt = ensureStatement(_precursorAnnotationStmt,
                    "INSERT INTO targetedms.precursorannotation(precursorid, name, value) VALUES (?, ?, ?)",
                    false);

            insertAnnotation(_precursorAnnotationStmt, annotation, annotation.getGeneralPrecursorId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertGeneralMoleculeAnnotation(GeneralMoleculeAnnotation annotation)
    {
        try
        {
            _generalMoleculeAnnotationStmt = ensureStatement(_generalMoleculeAnnotationStmt,
                    "INSERT INTO targetedms.generalmoleculeannotation(generalmoleculeid, name, value) VALUES (?, ?, ?)",
                    false);

            insertAnnotation(_generalMoleculeAnnotationStmt, annotation, annotation.getGeneralMoleculeId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertTransitionAnnotation(TransitionAnnotation annotation)
    {
        try
        {
            _transitionAnnotationStmt = ensureStatement(_transitionAnnotationStmt,
                    "INSERT INTO targetedms.transitionannotation(transitionid, name, value) VALUES (?, ?, ?)",
                    false);

            insertAnnotation(_transitionAnnotationStmt, annotation, annotation.getTransitionId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertPrecursorChromInfoAnnotation(PrecursorChromInfoAnnotation annotation)
    {
        try
        {
            _precursorChromInfoAnnotationStmt = ensureStatement(_precursorChromInfoAnnotationStmt,
                    "INSERT INTO targetedms.precursorchrominfoannotation(precursorchrominfoid, name, value) VALUES (?, ?, ?)",
                    false);

            insertAnnotation(_precursorChromInfoAnnotationStmt, annotation, annotation.getPrecursorChromInfoId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertAnnotation(PreparedStatement stmt, AbstractAnnotation annotation, long entityId) throws SQLException
    {
        int index = 1;
        stmt.setLong(index++, entityId);
        stmt.setString(index++, annotation.getName());
        stmt.setString(index++, annotation.getValue());
        stmt.execute();
    }

    /**
     * Prepares a statement for reuse during the import process. For tables that have a lot of rows, this is worth the
     * tradeoff between having more custom code and the perf hit from Table.insert() having to prep a statement for
     * every row
     */
    private PreparedStatement ensureStatement(PreparedStatement stmt, String sql, boolean reselect)
    {
        if (stmt == null)
        {
            try
            {
                assert TargetedMSManager.getSchema().getScope().isTransactionActive();
                Connection c = TargetedMSManager.getSchema().getScope().getConnection();
                if (reselect)
                {
                    SQLFragment reselectSQL = new SQLFragment(sql);
                    // All we really need is a ColumnInfo of the right name and type, so choose one of the TableInfos to supply it
                    TargetedMSManager.getSchema().getSqlDialect().addReselect(reselectSQL, getTableInfoTransitionChromInfo().getColumn("Id"), null);
                    sql = reselectSQL.getSQL();
                }
                stmt = c.prepareStatement(sql);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
        return stmt;
    }

    private void insertTransitionChromInfo(TransitionChromInfo transChromInfo)
    {
        try
        {
            _transitionChromInfoStmt = ensureStatement(_transitionChromInfoStmt,
                    "INSERT INTO targetedms.transitionchrominfo(transitionid, samplefileid, precursorchrominfoid, retentiontime, starttime, endtime, height, area, background, fwhm, fwhmdegenerate, truncated, peakrank, optimizationstep, note, chromatogramindex, masserrorppm, userset, identified, pointsacrosspeak, ccs, ionmobility, ionmobilitywindow, ionmobilitytype, rank, rankbylevel, forcedintegration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    true);

            int index = 1;
            _transitionChromInfoStmt.setLong(index++, transChromInfo.getTransitionId());
            _transitionChromInfoStmt.setLong(index++, transChromInfo.getSampleFileId());
            _transitionChromInfoStmt.setLong(index++, transChromInfo.getPrecursorChromInfoId());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getRetentionTime());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getStartTime());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getEndTime());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getHeight());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getArea());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getBackground());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getFwhm());
            setBoolean(_transitionChromInfoStmt, index++, transChromInfo.getFwhmDegenerate());
            setBoolean(_transitionChromInfoStmt, index++, transChromInfo.getTruncated());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getPeakRank());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getOptimizationStep());
            _transitionChromInfoStmt.setString(index++, transChromInfo.getNote());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getChromatogramIndex());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getMassErrorPPM());
            _transitionChromInfoStmt.setString(index++, transChromInfo.getUserSet());
            _transitionChromInfoStmt.setString(index++, transChromInfo.getIdentified());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getPointsAcrossPeak());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getCcs());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getIonMobility());
            setDouble(_transitionChromInfoStmt, index++, transChromInfo.getIonMobilityWindow());
            _transitionChromInfoStmt.setString(index++, transChromInfo.getIonMobilityType());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getRank());
            setInteger(_transitionChromInfoStmt, index++, transChromInfo.getRankByLevel());
            setBoolean(_transitionChromInfoStmt, index, transChromInfo.getForcedIntegration());

            try (ResultSet rs = TargetedMSManager.getSqlDialect().executeWithResults(_transitionChromInfoStmt))
            {
                rs.next();
                transChromInfo.setId(rs.getLong(1));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void insertPrecursorChromInfo(PrecursorChromInfo preChromInfo)
    {
        try
        {
            _precursorChromInfoStmt = ensureStatement(_precursorChromInfoStmt,
                    "INSERT INTO targetedms.precursorchrominfo( precursorid, samplefileid, generalmoleculechrominfoid, bestretentiontime, minstarttime, maxendtime, totalarea, totalbackground, maxfwhm, peakcountratio, numtruncated, librarydotp, optimizationstep, note, chromatogram, numtransitions, numpoints, maxheight, isotopedotp, averagemasserrorppm, bestmasserrorppm, userset, uncompressedsize, identified, container, chromatogramformat, chromatogramoffset, chromatogramlength, qvalue, zscore, ccs, ionmobilityms1, ionmobilityfragment, ionmobilitywindow, ionmobilitytype) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    true);

            int index = 1;
            _precursorChromInfoStmt.setLong(index++, preChromInfo.getPrecursorId());
            _precursorChromInfoStmt.setLong(index++, preChromInfo.getSampleFileId());
            _precursorChromInfoStmt.setLong(index++, preChromInfo.getGeneralMoleculeChromInfoId());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getBestRetentionTime());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getMinStartTime());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getMaxEndTime());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getTotalArea());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getTotalBackground());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getMaxFwhm());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getPeakCountRatio());
            setInteger(_precursorChromInfoStmt, index++, preChromInfo.getNumTruncated());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getLibraryDotP());
            setInteger(_precursorChromInfoStmt, index++, preChromInfo.getOptimizationStep());
            _precursorChromInfoStmt.setString(index++, preChromInfo.getNote());
            _precursorChromInfoStmt.setBytes(index++, preChromInfo.getChromatogram());
            _precursorChromInfoStmt.setInt(index++, preChromInfo.getNumTransitions());
            _precursorChromInfoStmt.setInt(index++, preChromInfo.getNumPoints());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getMaxHeight());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getIsotopeDotP());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getAverageMassErrorPPM());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getBestMassErrorPPM());
            _precursorChromInfoStmt.setString(index++, preChromInfo.getUserSet());
            setInteger(_precursorChromInfoStmt, index++, preChromInfo.getUncompressedSize());
            _precursorChromInfoStmt.setString(index++, preChromInfo.getIdentified());
            _precursorChromInfoStmt.setString(index++, preChromInfo.getContainer().getEntityId().toString());
            setInteger(_precursorChromInfoStmt, index++, preChromInfo.getChromatogramFormat());
            setLong(_precursorChromInfoStmt, index++, preChromInfo.getChromatogramOffset());
            setInteger(_precursorChromInfoStmt, index++, preChromInfo.getChromatogramLength());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getQvalue());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getZscore());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getCcs());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getIonMobilityMs1());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getIonMobilityFragment());
            setDouble(_precursorChromInfoStmt, index++, preChromInfo.getIonMobilityWindow());
            _precursorChromInfoStmt.setString(index++, preChromInfo.getIonMobilityType());

            try (ResultSet rs = TargetedMSManager.getSqlDialect().executeWithResults(_precursorChromInfoStmt))
            {
                rs.next();
                preChromInfo.setId(rs.getLong(1));
            }

            // Persist all of the indices into the chromatograms from the SKYD file separately. We need to retain it
            // at the PrecursorChromInfo level if we end up not storing the TransitionChromInfos. We will update the
            // PrecursorChromInfo to include the data if we end up needing it.
            byte[] indices = preChromInfo.getTransitionChromatogramIndices();
            if (indices != null)
            {
                _precursorChromInfoIndicesStmt.setLong(1, preChromInfo.getId());
                _precursorChromInfoIndicesStmt.setBinaryStream(2, new ByteArrayInputStream(indices), indices.length);
                _precursorChromInfoIndicesStmt.execute();
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private void setBoolean(PreparedStatement stmt, int index, Boolean b) throws SQLException
    {
        if (b != null)
        {
            stmt.setBoolean(index, b);
        }
        else
        {
            stmt.setNull(index, Types.BOOLEAN);
        }
    }

    private void setInteger(PreparedStatement stmt, int index, Integer i) throws SQLException
    {
        if (i != null)
        {
            stmt.setInt(index, i);
        }
        else
        {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    private void setLong(PreparedStatement stmt, int index, Long i) throws SQLException
    {
        if (i != null)
        {
            stmt.setLong(index, i);
        }
        else
        {
            stmt.setNull(index, Types.BIGINT);
        }
    }

    private void setDouble(PreparedStatement stmt, int index, Double d) throws SQLException
    {
        if (d != null)
        {
            stmt.setDouble(index, d);
        }
        else
        {
            stmt.setNull(index, Types.DECIMAL);
        }

    }


    private void insertSampleFiles(ReplicateInfo replicateInfo, Map<Instrument, Long> instrumentIdMap, Replicate replicate, Map<String, SampleFile> pathToSampleFile)
    {
        for(SampleFile sampleFile: replicate.getSampleFileList())
        {
            SampleFileKey sampleFileKey = SampleFileKey.getKey(replicate, sampleFile);
            if(replicateInfo.skylineIdSampleFileIdMap.containsKey(sampleFileKey))
            {
                throw new PanoramaBadDataException("Sample file id '" + sampleFile.getSkylineId() + "' for replicate '" + replicate.getName() + "' has already been seen in the document.");
            }

            sampleFile.setReplicateId(replicate.getId());

            List<Instrument> instrumentInfoList = sampleFile.getInstrumentInfoList();
            if (instrumentInfoList != null && instrumentInfoList.size() > 0)
            {
                Instrument instrument = combineInstrumentInfos(instrumentInfoList);

                Long instrumentId = instrumentIdMap.get(instrument);
                if (instrumentId == null)
                {
                    instrument.setRunId(_runId);
                    instrument = Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
                    instrumentIdMap.put(instrument, instrument.getId());
                    instrumentId = instrument.getId();
                }

                sampleFile.setInstrumentId(instrumentId);
            }



            sampleFile = Table.insert(_user, TargetedMSManager.getTableInfoSampleFile(), sampleFile);

            if (pathToSampleFile.containsKey(sampleFile.getFilePath()))
            {
                _log.warn("Duplicate entries found for file path " + sampleFile.getFilePath() + ", may not resolve sample file-scoped chromatograms correctly");
            }
            else
            {
                pathToSampleFile.put(sampleFile.getFilePath(), sampleFile);
            }

            // Remember the ids we inserted so we can reference them later
            replicateInfo.skylineIdSampleFileIdMap.put(sampleFileKey, sampleFile);
        }
    }

    private void copyExtractedFilesToCloud(TargetedMSRun run)
    {
        Integer skyDataId = run.getDataId();
        if (skyDataId != null)
        {
            ExpData skyData = ExperimentService.get().getExpData(skyDataId);
            if (skyData != null && skyData.getFilePath() != null)
            {
                Path skyParentPath = skyData.getFilePath().getParent();
                Path targetParentPath = skyParentPath.resolve(_blibSourceDir.getName());

                try
                {
                    if (!Files.exists(targetParentPath))
                        Files.createDirectory(targetParentPath);

                    ExpData skydData = run.getSkydDataId() == null ? null : ExperimentService.get().getExpData(run.getSkydDataId());
                    if (skydData != null)
                    {
                        // Copy .skyd file back to the cloud for on-demand usage later
                        Path skydLocalPath = skydData.getFilePath();
                        if (skydLocalPath != null && Files.exists(skydLocalPath))
                        {
                            Path skydTargetPath = copyFileToCloud(skydLocalPath, targetParentPath);
                            skydData.setDataFileURI(skydTargetPath.toUri());
                            skydData.save(_user);
                        }
                    }

                    for (Path blibPath : _blibSourcePaths)
                    {
                        copyFileToCloud(blibPath, targetParentPath);
                    }
                }
                catch (IOException e)
                {
                    logError("Copy files to cloud failed.", e);
                }
            }
        }
    }

    private Path copyFileToCloud(Path sourcePath, Path targetParentPath) throws IOException
    {
        Path dest = targetParentPath.resolve(FileUtil.getFileName(sourcePath));
        Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
        _log.info("Copied " + FileUtil.getFileName(sourcePath) + " to cloud storage.");
        return dest;
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
            return getKey(chromInfo.getReplicateName(), chromInfo.getSkylineSampleFileId());
        }

        public static SampleFileKey getKey(Replicate replicate, SampleFile sampleFile)
        {
            return getKey(replicate.getName(), sampleFile.getSkylineId());
        }

        public static SampleFileKey getKey(String replicateName, String sampleFileName)
        {
            return new SampleFileKey(replicateName, sampleFileName);
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
            return _skylineSampleFileId != null ? _skylineSampleFileId.equals(that._skylineSampleFileId) : that._skylineSampleFileId == null;
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

            return _optimizationStep != null ? _optimizationStep.equals(that._optimizationStep) : that._optimizationStep == null;
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

    private void saveRunDocumentSize(TargetedMSRun run)
    {
        new SqlExecutor(TargetedMSManager.getSchema()).execute("UPDATE " + TargetedMSManager.getTableInfoRuns() + " SET DocumentSize = ? WHERE Id = ?",
                run.getDocumentSize(), run.getId());
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

    protected static void updateRunStatus(long runId, String status, int statusId)
    {
        new SqlExecutor(TargetedMSManager.getSchema()).execute("UPDATE " + TargetedMSManager.getTableInfoRuns() + " SET Status = ?, StatusId = ? WHERE Id = ?",
                    status, statusId, runId);
    }

    private static final ReentrantLock _schemaLock = new ReentrantLock();

    public static class RunInfo implements Serializable
    {
        private final long _runId;
        private final boolean _alreadyImported;

        @JsonCreator
        private RunInfo(@JsonProperty("_runId") long runId, @JsonProperty("_alreadyImported") boolean alreadyImported)
        {
            _runId = runId;

            _alreadyImported = alreadyImported;
        }

        public long getRunId()
        {
            return _runId;
        }

        public boolean isAlreadyImported()
        {
            return _alreadyImported;
        }
    }

    public RunInfo prepareRun()
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

    protected long getRun()
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("DataId"), _expData.getRowId());
        filter.addCondition(FieldKey.fromParts("Container"), _container.getId());
        filter.addCondition(FieldKey.fromParts("Deleted"), Boolean.FALSE);
        TargetedMSRun run = new TableSelector(TargetedMSManager.getTableInfoRuns(), filter, null).getObject(TargetedMSRun.class);
        return run != null ? run.getId() : -1;
    }

    protected long createRun()
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

    protected void logError(String message, Exception e)
    {
        _systemLog.error(message, e);
        _log.error(message, e);
    }

    /** @return number of calibration curves in this run */
    int quantifyRun(TargetedMSRun run, PeptideSettings pepSettings, Collection<GroupComparisonSettings> groupComparisons)
    {
        QuantificationSettings quantificationSettings = pepSettings.getQuantificationSettings();
        if (quantificationSettings != null)
        {
            quantificationSettings.setRunId(_runId);
            Table.insert(_user, TargetedMSManager.getTableInfoQuantificationSettings(), quantificationSettings);
        }

        RegressionFit regressionFit = getRegressionFit(quantificationSettings);

        if (groupComparisons.isEmpty() && regressionFit == RegressionFit.NONE)
        {
            return 0;
        }

        RunQuantifier quantifier = new RunQuantifier(run, _user, _container);
        int i = 0;
        IProgressStatus _foldChangeStatus = _progressMonitor.getFoldChangeProgressTracker();
        if(groupComparisons.size() > 0)
        {
            _log.info("Calculating fold changes");
        }
        for (GroupComparisonSettings groupComparison : groupComparisons)
        {
            _log.debug(String.format("Calculating fold change for group comparison %s, %d of %d", groupComparison.getName(), ++i, groupComparisons.size()));
            for (FoldChange foldChange : quantifier.calculateFoldChanges(groupComparison))
            {
                Table.insert(_user, TargetedMSManager.getTableInfoFoldChange(), foldChange);
            }
            _foldChangeStatus.updateProgress(i, groupComparisons.size());
        }
        _foldChangeStatus.complete(groupComparisons.size() > 0 ? "Done calculating fold changes." : "No group comparisons found.");

        if (regressionFit != RegressionFit.NONE)
        {
            _log.info("Calculating calibration curves");
            List<GeneralMoleculeChromInfo> moleculeChromInfos = new ArrayList<>();
            IProgressStatus _calCurveStatus = _progressMonitor.getCalCurvesProgressTracker();
            List<CalibrationCurveEntity> calibrationCurves = quantifier.calculateCalibrationCurves(quantificationSettings, moleculeChromInfos, _calCurveStatus);
            for (CalibrationCurveEntity calibrationCurve : calibrationCurves)
            {
                Table.insert(_user, TargetedMSManager.getTableInfoCalibrationCurve(), calibrationCurve);
            }
            for (GeneralMoleculeChromInfo chromInfo : moleculeChromInfos)
            {
                Table.update(_user, TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), chromInfo, chromInfo.getId());
            }
            _calCurveStatus.complete(calibrationCurves.size() > 0 ? "Done calculating calibration curves." : "No calibration curves found.");

            return calibrationCurves.size();
        }
        return 0;
    }

    @Nullable
    private RegressionFit getRegressionFit(QuantificationSettings quantificationSettings)
    {
        RegressionFit regressionFit = RegressionFit.NONE;
        if (quantificationSettings != null)
        {
            regressionFit = RegressionFit.parse(quantificationSettings.getRegressionFit());
        }
        return regressionFit;
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

    private OptimizationInfo insertTransitionSettings(TransitionSettings settings)
    {
        TransitionSettings.InstrumentSettings instrumentSettings = settings.getInstrumentSettings();
        instrumentSettings.setRunId(_runId);
        Table.insert(_user, TargetedMSManager.getTableInfoTransInstrumentSettings(), instrumentSettings);

        boolean insertCEOptmizations = false;
        boolean insertDPOptmizations = false;

        TransitionSettings.PredictionSettings predictionSettings = settings.getPredictionSettings();
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
                s.setPredictorId(dpPredictor.getId());
                Table.insert(_user, TargetedMSManager.getTableInfoPredictorSettings(), s);
            }
        }
        Table.insert(_user, TargetedMSManager.getTableInfoTransitionPredictionSettings(), predictionSettings);

        TransitionSettings.FullScanSettings fullScanSettings = settings.getFullScanSettings();
        if (fullScanSettings != null)
        {
            insertFullScanSettings(fullScanSettings);
        }

        return new OptimizationInfo(insertCEOptmizations, insertDPOptmizations, cePredictor, dpPredictor);
    }

    private static class OptimizationInfo
    {
        private final boolean _insertCEOptimizations;
        private final boolean _insertDPOptimizations;
        private final TransitionSettings.Predictor _cePredictor;
        private final TransitionSettings.Predictor _dpPredictor;

        public OptimizationInfo(boolean insertCEOptimizations, boolean insertDPOptimizations, TransitionSettings.Predictor cePredictor, TransitionSettings.Predictor dpPredictor)
        {
            _insertCEOptimizations = insertCEOptimizations;
            _insertDPOptimizations = insertDPOptimizations;
            _cePredictor = cePredictor;
            _dpPredictor = dpPredictor;
        }
    }

    private void adjustProgressParts(ProgressMonitor progress, DataSettings dataSettings, QuantificationSettings quantificationSettings)
    {
        RegressionFit regressionFit = getRegressionFit(quantificationSettings);
        boolean hasCalCurves = regressionFit != RegressionFit.NONE;

        progress.updateProgressParts(dataSettings.getGroupComparisons().size() > 0, hasCalCurves);
    }

    private static class ProgressMonitor implements IProgressMonitor
    {
        private final ProgressPart _parserPart;     // Document parsing
        private final ProgressPart _qcCleanupPart;  // QC cleanup - remove redundant samples from older runs
        private final ProgressPart _foldChangePart; // Fold change calculations for group comparisons
        private final ProgressPart _calCurvesPart;  // Calibration curve calculations

        private final List<IProgressStatus> _progressParts;
        private final PipelineJob _job;
        private int _lastProgressPerc = 0;

        private final Logger _log;

        public ProgressMonitor(PipelineJob job, TargetedMSService.FolderType folderType, Logger log)
        {
            _job = job;
            _log = log;
            boolean isQc = (folderType == TargetedMSService.FolderType.QC);

            // Progress parts add up to 98%.  There is additional work that could happen after document import in
            // SkylineDocumentImportListener.onDocumentImport(). We will set progress to 100% after the listeners have done
            // their work.
            _parserPart = new ProgressPart(isQc ? 60 : 80, this);
            _qcCleanupPart = new ProgressPart(isQc ? 30 : 0, this);
            _foldChangePart = new ProgressPart(isQc ? 4 : 9, this);
            _calCurvesPart = new ProgressPart(isQc ? 4 : 9, this);

            _progressParts = Arrays.asList(_parserPart, _qcCleanupPart, _foldChangePart, _calCurvesPart);
        }
        public IProgressStatus getParserProgressTracker()
        {
            return _parserPart;
        }
        public IProgressStatus getQcCleanupProgressTracker()
        {
            return _qcCleanupPart;
        }
        public IProgressStatus getCalCurvesProgressTracker()
        {
            return _calCurvesPart;
        }
        public IProgressStatus getFoldChangeProgressTracker()
        {
            return _foldChangePart;
        }

        public void updateProgressParts(boolean hasGrpComparisons, boolean hasCalCurves)
        {
            if(hasGrpComparisons && hasCalCurves)
            {
                return;
            }

            int foldChangePartPerc = _foldChangePart.getPartPercent();
            int calCurvesPartPerc = _calCurvesPart.getPartPercent();

            if(!hasCalCurves && !hasGrpComparisons)
            {
                if(_qcCleanupPart.getPartPercent() > 0)
                {
                    _qcCleanupPart.setPartPercent(_qcCleanupPart.getPartPercent() + foldChangePartPerc + calCurvesPartPerc);
                }
                else
                {
                    _parserPart.setPartPercent(_parserPart.getPartPercent() + foldChangePartPerc + calCurvesPartPerc);
                }
                _foldChangePart.setPartPercent(0);
                _calCurvesPart.setPartPercent(0);
            }
            else if(!hasCalCurves)
            {
                _foldChangePart.setPartPercent(foldChangePartPerc + calCurvesPartPerc);
                _calCurvesPart.setPartPercent(0);
            }
            else if(!hasGrpComparisons)
            {
                _calCurvesPart.setPartPercent(foldChangePartPerc + calCurvesPartPerc);
                _foldChangePart.setPartPercent(0);
            }
        }
        public void complete()
        {
            _lastProgressPerc = 100;
            doUpdate(_lastProgressPerc);
        }
        @Override
        public void debug(String message)
        {
            if(!StringUtils.isBlank(message))
            {
                _log.debug(message);
            }
        }
        @Override
        public void info(String message)
        {
            if(!StringUtils.isBlank(message))
            {
                _log.info(message);
            }
        }
        @Override
        public void updateProgress()
        {
            int percDone = _progressParts.stream().mapToInt(IProgressStatus::getProgressPercent).sum();
            if(percDone > _lastProgressPerc)
            {
                doUpdate(percDone);
                _lastProgressPerc = percDone;
            }
        }

        private void doUpdate(int percDone)
        {
            if(_job != null)
            {
                _job.setStatus(PipelineJob.TaskStatus.running + ", " + percDone + "%"); // This will throw a CancelledException if the job was cancelled.
            }
            _log.info(percDone + "% Done");
        }


        private static class ProgressPart implements IProgressStatus
        {
            private int _partPercent; // percent share of total progress
            private int _percDone;
            private final IProgressMonitor _progressMonitor;

            public ProgressPart(int partPercent, IProgressMonitor progressMonitor)
            {
                _partPercent = partPercent;
                _progressMonitor = progressMonitor;
            }
            public int getPartPercent()
            {
                return _partPercent;
            }
            public void setPartPercent(int partPercent)
            {
                _partPercent = Math.min(100, partPercent);
            }
            @Override
            public void setStatus(String message)
            {
                _progressMonitor.debug(message);
            }
            @Override
            public void updateProgress(long done, long total)
            {
                if(done > total) done = total;
                int pd = total > 0 ? (int)(Math.round((done * 100.0) / total)) : 0;
                if(pd > _percDone)
                {
                    _percDone = pd;
                    _progressMonitor.updateProgress();
                }
            }
            @Override
            public int getProgressPercent()
            {
                return _percDone == 0 ? 0 : (int)(Math.round((_percDone * _partPercent) / 100.0));
            }
            @Override
            public void complete(String message)
            {
                updateProgress(100, 100);
                _progressMonitor.info(message);
            }
        }
    }

    public interface IProgressMonitor
    {
        void updateProgress();

        void debug(String message);

        void info(String message);
    }

    public interface IProgressStatus
    {
        void setStatus(String message);

        void updateProgress(long done, long total);

        void complete(String message);

        int getProgressPercent();
    }

    public void insertTraceCalculations(TargetedMSRun run)
    {
        List<QCMetricConfiguration> qcMetricConfigurations = TargetedMSManager.getTraceMetricConfigurations(_container, _user);

        if (!qcMetricConfigurations.isEmpty())
        {
            var qcTraceMetricValues = TargetedMSManager.calculateTraceMetricValues(qcMetricConfigurations, run);
            qcTraceMetricValues.forEach(qcTraceMetricValue -> Table.insert(_user, TargetedMSManager.getTableQCTraceMetricValues(), qcTraceMetricValue));
        }
    }
}
