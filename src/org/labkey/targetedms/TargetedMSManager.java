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

import com.google.common.base.Joiner;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.data.statistics.MathStat;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.AbstractFileXarSource;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.XarSource;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.SchemaKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.pipeline.TargetedMSImportPipelineJob;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.RepresentativeStateManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Math.toIntExact;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_FOLDER_TYPE;

public class TargetedMSManager
{
    private static final TargetedMSManager _instance = new TargetedMSManager();

    private static Logger _log = Logger.getLogger(TargetedMSManager.class);

    private TargetedMSManager()
    {
        // prevent external construction with a private default constructor
    }

    public static TargetedMSManager get()
    {
        return _instance;
    }

    public String getSchemaName()
    {
        return TargetedMSSchema.SCHEMA_NAME;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(TargetedMSSchema.SCHEMA_NAME);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoExperimentAnnotations()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_EXPERIMENT_ANNOTATIONS);
    }

    public static TableInfo getTableInfoRuns()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUNS);
    }

    public static TableInfo getTableInfoTransInstrumentSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_INSTRUMENT_SETTINGS);
    }

    public static TableInfo getTableInfoPredictor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR);
    }

    public static TableInfo getTableInfoPredictorSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR_SETTINGS);
    }

    public static TableInfo getTableInfoReplicate()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE);
    }

    public static TableInfo getTableInfoSampleFile()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE);
    }

    public static TableInfo getTableInfoReplicateAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO);
    }

    public static TableInfo getTableInfoTransitionAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_AREA_RATIO);
    }

    public static TableInfo getTableInfoPeptideGroup()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
    }

    public static TableInfo getTableInfoPeptideGroupAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP_ANNOTATION);
    }

    public static TableInfo getTableInfoPeptide()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE);
    }

    public static TableInfo getTableInfoMolecule()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MOLECULE);
    }

    public static TableInfo getTableInfoGeneralMoleculeAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_MOLECULE_ANNOTATION);
    }

    public static TableInfo getTableInfoProtein()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PROTEIN);
    }

    public static TableInfo getTableInfoPrecursor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR);
    }

    public static TableInfo getTableInfoPrecursorAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_ANNOTATION);
    }

    public static TableInfo getTableInfoPrecursorChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO);
    }

    public static TableInfo getTableInfoPrecursorAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_AREA_RATIO);
    }

    public static TableInfo getTableInfoPrecursorChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoGeneralMoleculeChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_MOLECULE_CHROM_INFO);
    }

    public static TableInfo getTableInfoPeptideAreaRatio()
    {
       return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_AREA_RATIO);
    }

    public static TableInfo getTableInfoInstrument()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_INSTRUMENT);
    }

    public static TableInfo getTableInfoIsotopeEnrichment()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_ENRICHMENT);
    }

    public static TableInfo getTableInfoIsolationScheme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOLATION_SCHEME);
    }

    public static TableInfo getTableInfoIsolationWindow()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOLATION_WINDOW);
    }

    public static TableInfo getTableInfoRetentionTimePredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RETENTION_TIME_PREDICTION_SETTINGS);
    }

    public static TableInfo getTableInfoDriftTimePredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_DRIFT_TIME_PREDICTION_SETTINGS);
    }

    public static TableInfo getTableInfoMeasuredDriftTime()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MEASURED_DRIFT_TIME);
    }

    public static TableInfo getTableInfoTransitionPredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_PREDICITION_SETTINGS);
    }

    public static TableInfo getTableInfoTransitionFullScanSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_FULL_SCAN_SETTINGS);
    }

    public static TableInfo getTableInfoTransition()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION);
    }

    public static TableInfo getTableInfoMoleculeTransition()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MOLECULE_TRANSITION);
    }

    public static TableInfo getTableInfoTransitionLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_LOSS);
    }

    public static TableInfo getTableInfoModificationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MODIFICATION_SETTINGS);
    }

    public static TableInfo getTableInfoPeptideStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoPeptideIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoIsotopeLabel()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_LABEL);
    }

    public static TableInfo getTableInfoIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MOD_LOSS);
    }

    public static TableInfo getTableInfoRunIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoRunEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ENZYME);
    }

    public static TableInfo getTableInfoRunStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoTransitionAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionOptimization()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_OPTIMIZATION);
    }

    public static TableInfo getTableInfoLibrarySettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SETTINGS);
    }

    public static TableInfo getTableInfoSpectrumLibrary()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SPECTRUM_LIBRARY);
    }

    public static TableInfo getTableInfoEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ENZYME);
    }

    public static TableInfo getTableInfoLibrarySource()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SOURCE);
    }

    public static TableInfo getTableInfoPrecursorLibInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_LIB_INFO);
    }

    public static TableInfo getTableInfoAnnotationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ANNOTATION_SETTINGS);
    }

    public static TableInfo getTableInfoGroupComparisonSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GROUP_COMPARISON_SETTINGS);
    }

    public static TableInfo getTableInfoFoldChange()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_FOLD_CHANGE);
    }

    public static TableInfo getTableInfoiRTPeptide()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_IRT_PEPTIDE);
    }

    public static TableInfo getTableInfoiRTScale()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_IRT_SCALE);
    }

    public static TableInfo getTableInfoAutoQCPing()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_AUTOQC_PING);
    }

    public static TableInfo getTableInfoJournal()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_JOURNAL);
    }

    public static TableInfo getTableInfoJournalExperiment()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_JOURNAL_EXPERIMENT);
    }

    public static TableInfo getTableInfoQCAnnotationType()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE);
    }

    public static TableInfo getTableInfoQCAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_ANNOTATION);
    }
    public static TableInfo getTableInfoQuantificationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QUANTIIFICATION_SETTINGS);
    }

    public static TableInfo getTableInfoCalibrationCurve() {
        return getSchema().getTable(TargetedMSSchema.TABLE_CALIBRATION_CURVE);
    }

    public static TableInfo getTableInfoQCMetricConfiguration()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION);
    }

    public static TableInfo getTableInfoQCMetricExclusion()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION);
    }

    public static TableInfo getTableInfoGuideSet()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GUIDE_SET);
    }

    public static TableInfo getTableInfoGeneralMolecule()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_MOLECULE);
    }

    public static TableInfo getTableInfoGeneralMoleculeChomInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_MOLECULE_CHROM_INFO);
    }

    public static TableInfo getTableInfoGeneralTransition()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_TRANSITION);
    }

    public static TableInfo getTableInfoGeneralPrecursor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_GENERAL_PRECURSOR);
    }

    public static TableInfo getTableInfoMoleculePrecursor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MOLECULE_PRECURSOR);
    }

    public static TableInfo getTableInfoQCEnabledMetrics()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_ENABLED_METRICS);
    }

    public static TableInfo getTableInfoQCEmailNotifications()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_EMAIL_NOTIFICATIONS);
    }

    public static TableInfo getTableInfoSkylineAuditLogEntry()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG_ENTRY);
    }

    public static TableInfo getTableInfoSkylineAuditLogMessage()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG_MESSAGE);
    }

    public static Integer addRunToQueue(ViewBackgroundInfo info,
                                     final Path path,
                                     PipeRoot root) throws IOException, XarFormatException
    {
        String description = "Skyline document import - " + FileUtil.getFileName(path);
        XarContext xarContext = new XarContext(description, info.getContainer(), info.getUser());
        User user =  info.getUser();
        Container container = info.getContainer();

        // If an entry does not already exist for this data file in exp.data create it now.
		// This should happen only if a file was copied to the pipeline directory instead
		// of being uploaded via the files browser.
        ExpData expData = ExperimentService.get().getExpDataByURL(path, container);
        if(expData == null)
        {
            XarSource source = new AbstractFileXarSource("Wrap Targeted MS Run", container, user)
            {
                public File getLogFile()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public File getRoot()
                {
                    if (!FileUtil.hasCloudScheme(path))
                        return path.toFile().getParentFile();

                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getRootPath()
                {
                    return path.getParent();
                }

                @Override
                public ExperimentArchiveDocument getDocument()
                {
                    throw new UnsupportedOperationException();
                }
            };

            expData = ExperimentService.get().createData(path.toUri(), source);
        }

        TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(container);
        // Default folder type or Experiment is not representative
        TargetedMSRun.RepresentativeDataState representative = TargetedMSRun.RepresentativeDataState.NotRepresentative;
        if (folderType == TargetedMSModule.FolderType.Library)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Peptide;
        else if (folderType == TargetedMSModule.FolderType.LibraryProtein)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Protein;

        SkylineDocImporter importer = new SkylineDocImporter(user, container, FileUtil.getFileName(path), expData, null, xarContext, representative, null, null);
        SkylineDocImporter.RunInfo runInfo = importer.prepareRun();
        TargetedMSImportPipelineJob job = new TargetedMSImportPipelineJob(info, expData, runInfo, root, representative);
        try
        {
            PipelineService.get().queueJob(job);
            return PipelineService.get().getJobId(user, container, job.getJobGUID());
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
    }

    public static ExpRun ensureWrapped(TargetedMSRun run, User user, PipeRoot pipeRoot, Integer jobId) throws ExperimentException
    {
        ExpRun expRun;
        if (run.getExperimentRunLSID() != null)
        {
            expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun != null && expRun.getContainer().equals(run.getContainer()))
            {
                return expRun;
            }
        }
        return wrapRun(run, user, pipeRoot, jobId);
    }

    private static ExpRun wrapRun(TargetedMSRun run, User user, PipeRoot pipeRoot, Integer jobId) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            // Make sure that we have a protocol in this folder
            String protocolPrefix = run.isZipFile() ? TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX :
                                                      TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX;

            Lsid lsid = new Lsid("Protocol.Folder-" + container.getRowId(), protocolPrefix);
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(lsid.toString());
            if (protocol == null)
            {
                protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ProtocolApplication, "Skyline Document Import", lsid.toString());
                protocol.setMaxInputMaterialPerInstance(0);
                protocol = ExperimentService.get().insertSimpleProtocol(protocol, user);
            }

            ExpData expData = ExperimentService.get().getExpData(run.getDataId());
            Path skylineFile = pipeRoot.resolveToNioPathFromUrl(expData.getDataFileUrl());

            ExpRun expRun = ExperimentService.get().createExperimentRun(container, run.getDescription());
            expRun.setProtocol(protocol);
            expRun.setJobId(jobId);
            expRun.setFilePathRootPath(null != skylineFile ? skylineFile.getParent() : null);
            ViewBackgroundInfo info = new ViewBackgroundInfo(container, user, null);

            Map<ExpData, String> inputDatas = new HashMap<>();
            Map<ExpData, String> outputDatas = new HashMap<>();

            outputDatas.put(expData, "sky");

            ExpData expSkydData = null;
            if(run.getSkydDataId() != null)
            {
                expSkydData = ExperimentService.get().getExpData(run.getSkydDataId());
            }
            if(expSkydData != null)
            {
                outputDatas.put(expSkydData, "skyd");
            }

            ExpRun mostRecentExpRun = null;

            if (run.getDocumentGUID() != null)
            {
                TargetedMSRun mostRecentRun = getMostRecentRunRevision(run);
                if (mostRecentRun != null && mostRecentRun.getExperimentRunLSID() != null)
                {
                    mostRecentExpRun = ExperimentService.get().getExpRun(mostRecentRun.getExperimentRunLSID());
                }
            }

            expRun = ExperimentService.get().saveSimpleExperimentRun(expRun,
                                                                     Collections.emptyMap(),
                                                                     inputDatas,
                                                                     Collections.emptyMap(),
                                                                     outputDatas,
                                                                     Collections.emptyMap(),
                                                                     info, _log, false);

            run.setExperimentRunLSID(expRun.getLSID());
            TargetedMSManager.updateRun(run, user);

            if (mostRecentExpRun != null && mostRecentExpRun.getReplacedByRun() == null)
            {
                mostRecentExpRun.setReplacedByRun(expRun);
                try
                {
                    mostRecentExpRun.save(user);
                }
                catch (BatchValidationException e)
                {
                    throw new ExperimentException(e);
                }
            }

            transaction.commit();
            return expRun;
        }
    }

    @NotNull
    public static Container getMostRecentPingChild(@NotNull User user, @NotNull Container c)
    {
        SQLFragment sql = new SQLFragment("SELECT Container FROM ");
        sql.append(getTableInfoAutoQCPing(), "p");
        ContainerFilter f = new ContainerFilter.CurrentAndSubfolders(user);
        sql.append(" WHERE ");
        sql.append(f.getSQLFragment(getSchema(), new SQLFragment("Container"), c));
        sql.append(" ORDER BY Modified DESC");

        String containerId = new SqlSelector(getSchema(), getSqlDialect().limitRows(sql, 1)).getObject(String.class);
        Container result = null;
        if (containerId != null)
        {
            result = ContainerManager.getForId(containerId);
        }
        return result == null ? c : result;
    }

    private static TargetedMSRun getMostRecentRunRevision(TargetedMSRun run)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(" WHERE DocumentGUID = ? AND Container = ? AND Id != ? ORDER BY Id DESC");
        sql.add(run.getDocumentGUID());
        sql.add(run.getContainer());
        sql.add(run.getId());
        SQLFragment limitedSQL = getSchema().getSqlDialect().limitRows(sql, 1);
        return new SqlSelector(getSchema(), limitedSQL).getObject(TargetedMSRun.class);
    }

    public static void deleteRun(Container container, User user, TargetedMSRun run)
    {
        deleteRuns(Arrays.asList(run.getRunId()), container, user);
        deleteiRTscales(container);
    }

    public static TargetedMSRun getRunByDataId(int dataId)
    {
        return getRunByDataId(dataId, null);
    }

    public static TargetedMSRun getRunByDataId(int dataId, Container c)
    {
        TargetedMSRun[] runs;
        if(c != null)
        {
            runs = getRuns("DataId = ? AND Deleted = ? AND Container = ?", dataId, Boolean.FALSE, c.getId());
        }
        else
        {
            runs = getRuns("DataId = ? AND Deleted = ?", dataId, Boolean.FALSE);
        }
        if(runs.length == 0)
        {
            return null;
        }
        if(runs.length == 1)
        {
            return runs[0];
        }
        throw new IllegalStateException("There is more than one non-deleted Targeted MS Run for dataId " + dataId);
    }

    public static TargetedMSRun getRunBySkydDataId(int skydDataId)
    {
        return getRunBySkydDataId(skydDataId, null);
    }

    public static TargetedMSRun getRunBySkydDataId(int skydDataId, Container c)
    {
        TargetedMSRun[] runs;
        if(c != null)
        {
            runs = getRuns("SkydDataId = ? AND Deleted = ? AND Container = ?", skydDataId, Boolean.FALSE, c.getId());
        }
        else
        {
            runs = getRuns("SkydDataId = ? AND Deleted = ?", skydDataId, Boolean.FALSE);
        }
        if(runs.length == 0)
        {
            return null;
        }
        if(runs.length == 1)
        {
            return runs[0];
        }
        throw new IllegalStateException("There is more than one non-deleted Targeted MS Run for skydDataId " + skydDataId);
    }


    public static TargetedMSRun getRunByLsid(String lsid, Container c)
    {
        TargetedMSRun[] runs = getRuns("experimentrunlsid = ? AND container = ?", lsid, c.getId());
        if(runs.length == 0)
        {
            return null;
        }
        if(runs.length == 1)
        {
            return runs[0];
        }
        throw new IllegalArgumentException("More than one TargetedMS runs found for LSID "+lsid);
    }

    @NotNull
    private static TargetedMSRun[] getRuns(String whereClause, Object... params)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append(whereClause);
        sql.addAll(params);
        return new SqlSelector(getSchema(), sql).getArray(TargetedMSRun.class);
    }

    @Nullable
    public static TargetedMSRun getRun(int runId)
    {
        TargetedMSRun[] runs = getRuns("Id = ? AND deleted = ?", runId, false);

        if (runs.length == 1)
        {
            return runs[0];
        }

        return null;
    }

    public static TargetedMSRun[] getRunsInContainer(Container container)
    {
        return getRuns("Container=? AND StatusId=? AND deleted=?", container.getId(), 1, Boolean.FALSE);
    }

    public static TargetedMSRun getRunByFileName(String fileName, Container container)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Container"), container.getId());
        filter.addCondition(FieldKey.fromParts("FileName"), fileName);
        return new TableSelector(TargetedMSManager.getTableInfoRuns(), filter, null).getObject(TargetedMSRun.class);
    }

    public static boolean isRunConflicted(TargetedMSRun run)
    {
        if(run == null)
            return false;

        TargetedMSRun.RepresentativeDataState representativeState = run.getRepresentativeDataState();
        switch (representativeState)
        {
            case NotRepresentative:
                return false;
            case Representative_Protein:
                return getConflictedProteinCount(run.getId()) > 0;
            case Representative_Peptide:
                return getConflictedPeptideCount(run.getId()) > 0;
        }
        return false;
    }

    private static int getConflictedProteinCount(int runId)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(" WHERE runid = ?");
        sql.add(runId);
        sql.append(" AND representativedatastate = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count : 0;
    }

    private static int getConflictedPeptideCount(int runId)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(" WHERE pepgrp.runid = ?");
        sql.add(runId);
        sql.append(" AND prec.representativedatastate = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        sql.append(" AND gp.GeneralMoleculeId = gm.Id");
        sql.append(" AND gm.PeptideGroupId = pepgrp.Id");
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count : 0;
    }

    public static void markRunsNotRepresentative(Container container, TargetedMSRun.RepresentativeDataState representativeState)
    {
        Collection<Integer> representativeRunIds = null;

        if(representativeState == TargetedMSRun.RepresentativeDataState.Representative_Protein)
        {
            representativeRunIds = getProteinRepresentativeRunIds(container);
        }
        else if(representativeState == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
        {
            representativeRunIds = getPeptideRepresentativeRunIds(container);
        }

        if(representativeRunIds == null || representativeRunIds.size() == 0)
        {
            return;
        }

        SQLFragment updateSql = new SQLFragment();
        updateSql.append("UPDATE "+TargetedMSManager.getTableInfoRuns());
        updateSql.append(" SET RepresentativeDataState = ?");
        updateSql.add(TargetedMSRun.RepresentativeDataState.NotRepresentative.ordinal());
        updateSql.append(" WHERE Container = ?");
        updateSql.add(container);
        updateSql.append(" AND RepresentativeDataState = ? ");
        updateSql.add(representativeState.ordinal());
        updateSql.append(" AND Id NOT IN ("+StringUtils.join(representativeRunIds, ",")+")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(updateSql);
    }

    public static List<Integer> getCurrentRepresentativeRunIds(Container container)
    {
        List<Integer> representativeRunIds = null;
        if(getFolderType(container) == TargetedMSModule.FolderType.LibraryProtein)
        {
            representativeRunIds = getCurrentProteinRepresentativeRunIds(container);
        }
        else if(getFolderType(container) == TargetedMSModule.FolderType.Library)
        {
            representativeRunIds = getCurrentPeptideRepresentativeRunIds(container);
        }

        return representativeRunIds != null ? representativeRunIds : Collections.emptyList();
    }

    private static List<Integer> getCurrentProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal());
    }

    private static List<Integer> getProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal(),
                                                         RepresentativeDataState.Deprecated.ordinal(),
                                                         RepresentativeDataState.Conflicted.ordinal());
    }

    private static List<Integer> getProteinRepresentativeRunIds(Container container, int... stateArray)
    {
        SQLFragment reprRunIdSql = new SQLFragment();
        reprRunIdSql.append("SELECT DISTINCT (RunId) FROM ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoRuns(), "runs");
        String states = StringUtils.join(stateArray, ',');

        reprRunIdSql.append(" WHERE pepgrp.RepresentativeDataState IN (" + states + ")");
        reprRunIdSql.append(" AND runs.Container = ?");
        reprRunIdSql.add(container);
        reprRunIdSql.append(" AND runs.Id = pepgrp.RunId");

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getArrayList(Integer.class);
    }

    private static List<Integer> getCurrentPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal());
    }

    private static List<Integer> getPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal(),
                                                         RepresentativeDataState.Deprecated.ordinal(),
                                                         RepresentativeDataState.Conflicted.ordinal());
    }

    private static List<Integer> getPeptideRepresentativeRunIds(Container container, int... stateArray)
    {
        // Get a list of runIds that have proteins that are either representative, deprecated or conflicted.
        SQLFragment reprRunIdSql = new SQLFragment();
        reprRunIdSql.append("SELECT DISTINCT (pepgrp.RunId) FROM ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoRuns(), "runs");

        String states = StringUtils.join(stateArray, ',');

        reprRunIdSql.append(" WHERE gp.RepresentativeDataState IN (" + states + ")");
        reprRunIdSql.append(" AND gp.GeneralMoleculeId = gm.Id");
        reprRunIdSql.append(" AND gm.PeptideGroupId = pepgrp.Id");
        reprRunIdSql.append(" AND Container = ?");
        reprRunIdSql.add(container);
        reprRunIdSql.append(" AND runs.Id = pepgrp.RunId");

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getArrayList(Integer.class);
    }

    public static void updateRun(TargetedMSRun run, User user)
    {
        Table.update(user, getTableInfoRuns(), run, run.getRunId());
    }

    /** Delete all of the TargetedMS runs in the container, including their experiment run wrappers */
    public static void deleteIncludingExperimentWrapper(Container c, User user)
    {
        List<Integer> runIds = new SqlSelector(getSchema(), "SELECT Id FROM " + getTableInfoRuns() + " WHERE Container = ?", c).getArrayList(Integer.class);

        if (runIds.isEmpty())
            return;

        // Save these to delete after we've deleted the runs
        List<ExpRun> experimentRunsToDelete = new ArrayList<>();

        for (Integer runId : runIds)
        {
            TargetedMSRun run = getRun(runId);
            if (run != null && run.getDataId() != null)
            {
                ExpData data = ExperimentService.get().getExpData(run.getDataId());
                if (data != null)
                {
                    ExpRun expRun = data.getRun();
                    if (expRun != null)
                    {
                        experimentRunsToDelete.add(expRun);
                    }
                }
            }
        }

        deleteRuns(runIds, c, user);

        for (ExpRun run : experimentRunsToDelete)
        {
            run.delete(user);
        }
    }

    /**
     * Delete just the targetedms run and its child tables
     * Pulled out into separate method so could be called by itself from data handlers
     * @param runIds targetedms.run.id values
     */
    public static void deleteRuns(List<Integer> runIds, Container c, User user)
    {
        List<Integer> cantDelete = new ArrayList<>();
        for(int runId: runIds)
        {
            TargetedMSRun run = getRun(runId);
            if(!run.getContainer().equals(c) && !run.getContainer().hasPermission(user, DeletePermission.class))
            {
               cantDelete.add(runId);
            }
        }

        if(cantDelete.size() > 0)
        {
            throw new UnauthorizedException("User does not have permissions to delete run " + (cantDelete.size() > 1 ? "Ids" : "Id")
                    + " " + StringUtils.join(cantDelete, ',')
                    + ". " + (cantDelete.size() > 1 ? "They are" : "It is") + " not in container " + c.getName());
        }

        for(int runId: runIds)
        {
            TargetedMSRun run = getRun(runId);
            if(run.isDeleted())
            {
                continue;
            }
            // Revert the representative state if any of the runs are representative at the protein or peptide level.
            if(run.isRepresentative())
            {
                PipeRoot root = PipelineService.get().getPipelineRootSetting(run.getContainer());
                if (null != root)
                {
                    LocalDirectory localDirectory = LocalDirectory.create(root, TargetedMSModule.NAME);
                    try
                    {
                        RepresentativeStateManager.setRepresentativeState(user, run.getContainer(), localDirectory, run, TargetedMSRun.RepresentativeDataState.NotRepresentative);
                    }
                    finally
                    {
                        localDirectory.cleanUpLocalDirectory();
                    }
                }
                else
                {
                    throw new RuntimeException("Pipeline root not found.");
                }
            }
        }

        // Mark all of the runs for deletion
        SQLFragment markDeleted = new SQLFragment("UPDATE " + getTableInfoRuns() + " SET ExperimentRunLSID = NULL, DataId = NULL, SkydDataId = NULL, Deleted=?, Modified=? ", Boolean.TRUE, new Date());
        SimpleFilter where = new SimpleFilter();
        where.addInClause(FieldKey.fromParts("Id"), runIds);
        markDeleted.append(where.getSQLFragment(getSqlDialect()));
        new SqlExecutor(getSchema()).execute(markDeleted);

        // and then delete them
        purgeDeletedRuns();
    }

    public static TargetedMSRun getRunForPrecursor(int precursorId)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoGeneralMolecule()+" AS gm, "+
                     getTableInfoGeneralPrecursor()+" AS gp "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=gm.PeptideGroupId "+
                     "AND gm.Id=gp.GeneralMoleculeId "+
                     "AND gp.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(precursorId);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for precursor: "+precursorId);
        }
        return run;
    }

    public static TargetedMSRun getRunForGeneralMolecule(int id)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoGeneralMolecule()+" AS gm "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=gm.PeptideGroupId "+
                     "AND gm.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(id);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for general molecule: " + id);
        }
        return run;
    }

    public static Set<String> getDistinctPeptides(Container c)
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT DISTINCT(p.Sequence) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptide(), "p").append(" INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(" ON p.Id = gm.Id INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(" ON gm.PeptideGroupId = pg.Id INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r").append(" ON r.Id = pg.RunId AND r.Container = ?");
        sqlFragment.add(c);

        return new TreeSet<>(new SqlSelector(getSchema(), sqlFragment).getCollection(String.class));
    }

    public static Set<String> getDistinctMolecules(Container c)
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT DISTINCT(m.IonFormula) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoMolecule(), "m").append(" INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(" ON m.Id = gm.Id INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(" ON gm.PeptideGroupId = pg.Id INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r").append(" ON r.Id = pg.RunId AND r.Container = ? AND m.IonFormula IS NOT NULL");
        sqlFragment.add(c);

        return new TreeSet<>(new SqlSelector(getSchema(), sqlFragment).getCollection(String.class));
    }

    @Nullable
    public static Integer getPeptideGroupPeptideCount(@Nullable TargetedMSRun run, int peptideGroupId)
    {
        if (run != null && peptideGroupId > 0)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(p.id) FROM ").append(TargetedMSManager.getTableInfoPeptide(), "p")
                    .append(" INNER JOIN ")
                    .append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(" ON p.Id = gm.Id ")
                    .append(" INNER JOIN ")
                    .append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(" ON gm.PeptideGroupId = pg.Id ")
                    .append(" AND pg.RunId = ? AND pg.Id = ?");
            sql.add(run.getId());
            sql.add(peptideGroupId);
            return new SqlSelector(TargetedMSSchema.getSchema(), sql).getObject(Integer.class);
        }
        return null;
    }

    @Nullable
    public static Integer getPeptideGroupMoleculeCount(@Nullable TargetedMSRun run, int peptideGroupId)
    {
        if (run != null && peptideGroupId > 0)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(m.id) FROM ").append(TargetedMSManager.getTableInfoMolecule(), "m")
                    .append(" INNER JOIN ")
                    .append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(" ON m.Id = gm.Id ")
                    .append(" INNER JOIN ")
                    .append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(" ON gm.PeptideGroupId = pg.Id ")
                    .append(" AND pg.RunId = ? AND pg.Id = ?");
            sql.add(run.getId());
            sql.add(peptideGroupId);
            return new SqlSelector(TargetedMSSchema.getSchema(), sql).getObject(Integer.class);
        }
        return null;
    }

    private static String getDependentSampleFileReplicateDeleteSql(TableInfo fromTable, String replicateId, Container container)
    {
        SQLFragment sql = new SQLFragment("DELETE FROM " + fromTable + " WHERE " + replicateId + " IN (").append(getEmptyReplicateIdsSql(container, "Id")).append(")");
        return sql.getSQL();
    }

    private static void deletePredictorsForUnusedReplicates(Container container)
    {
        execute("DELETE FROM " + getTableInfoPredictor() + " WHERE " +
                "Id IN (" + getEmptyReplicateIdsSql(container, "CePredictorId") + ") OR " +
                "Id IN (" + getEmptyReplicateIdsSql(container, "DpPredictorId") + ")");
    }

    private static String getEmptyReplicateIdsSql(Container container, String selectColumn)
    {
        // Get all the replicates in this container which no longer have any sample files. 
        SQLFragment sql = new SQLFragment("( SELECT rep.").append(selectColumn).append(" FROM targetedms.replicate rep ");
        sql.append(" LEFT OUTER JOIN targetedms.samplefile sf ON rep.Id = sf.ReplicateId ");
        sql.append(" INNER JOIN targetedms.runs r on rep.runId = r.Id ");
        sql.append(" WHERE r.container = ").append(container);
        sql.append(" AND sf.ReplicateId IS NULL )"); // No sample files for this replicate
        return sql.getSQL();
    }

    private static boolean fileIsReferenced(@NotNull URI uri)
    {
        SQLFragment sql = new SQLFragment("SELECT sf.Id FROM ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append(", ");
        sql.append(getTableInfoSampleFile(), "sf");
        sql.append(", ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(", ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append( " WHERE rep.Id = sf.ReplicateId AND rep.RunId = r.Id AND d.RowId = r.DataId AND d.DataFileUrl = ?");
        sql.add(FileUtil.uriToString(uri));

        return new SqlSelector(getSchema(), sql).exists();
    }

    @NotNull
    private static List<String> deleteFileWithLogging(@NotNull Path file, @NotNull List<String> logs)
    {
        String logMsg = "Deleting " + file.toString();
        logs.add(logMsg);
        _log.info(logMsg);

        boolean success = true;
        try
        {
            success = Files.deleteIfExists(file);
        }
        catch (IOException e)
        {
            success = false;
        }

        if (!success)
        {
            logMsg = "Unable to delete " + file.toString();
            logs.add(logMsg);
            _log.warn(logMsg);
        }

        return logs;
    }

    private static void deleteRunForFile(URI uri, Container c, User u)
    {
        // Fix issue 33828 by handing legacy URI; TODO: possibly refactor with FileQueryUpdateService.getQueryFilter; also, should we filter by container?
        TableInfo expDataTable = ExperimentService.get().getTinfoData();
        String dataFileUrl = FileUtil.uriToString(uri).replaceFirst("^file:/+", "/");
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.OrClause(
                new CompareType.EqualsCompareClause(FieldKey.fromParts("DataFileUrl"), CompareType.EQUAL, dataFileUrl),
                new CompareType.EqualsCompareClause(FieldKey.fromParts("DataFileUrl"), CompareType.EQUAL, "file:" + dataFileUrl),
                new CompareType.EqualsCompareClause(FieldKey.fromParts("DataFileUrl"), CompareType.EQUAL, "file://" + dataFileUrl)
        ));

        Set<Integer> runIds = new HashSet<>();
        for (Map<String, Object> map : new TableSelector(expDataTable, Collections.singleton("RunId"), filter, null).getMapCollection())
            runIds.add((Integer) map.get("runId"));

        ExperimentService expService = ExperimentService.get();
        for (Integer runId : runIds)
            expService.deleteExperimentRunsByRowIds(c, u, runId);
    }

    @NotNull
    public static List<String> purgeUnreferencedFiles(@NotNull Set<URI> fileURIs, Container c, User u)
    {
        List<String> logMsgs = new ArrayList<>();
        String logMsg;

        for (URI uri : fileURIs)
        {
            if (!fileIsReferenced(uri))
            {
                try
                {
                    deleteRunForFile(uri, c, u);

                    // Remove .sky.zip from file name to get directory name
                    Path file = FileUtil.getPath(c, uri);
                    String dirName = FilenameUtils.removeExtension(FilenameUtils.removeExtension(FileUtil.getFileName(file)));

                    Path dir = file.getParent().resolve(dirName);
                    Path viewFile = file.getParent().resolve(dirName + ".sky.view");
                    Path skydFile = file.getParent().resolve(dirName + ".skyd");

                    logMsg = "All the related sampleFiles for " + file.toString() + " have been updated with newer data.";
                    logMsgs.add(logMsg);
                    _log.info(logMsg);

                    logMsgs = deleteFileWithLogging(file, logMsgs);

                    if (Files.exists(viewFile))
                        logMsgs = deleteFileWithLogging(viewFile, logMsgs);

                    if (Files.exists(skydFile))
                        logMsgs = deleteFileWithLogging(skydFile, logMsgs);

                    if (Files.isDirectory(dir))
                    {
                        logMsg = "Deleting directory " + dir.toString();
                        logMsgs.add(logMsg);
                        _log.info(logMsg);
                        FileUtil.deleteDir(dir);
                    }
                }
                catch (IOException e)
                {
                    logMsg = "Unable to delete unzipped directory from file "; // + file;        TODO
                    _log.warn(logMsg);
                    logMsgs.add(logMsg);
                }
            }
        }
        return logMsgs;
    }

    public static void purgeUnreferencedReplicates(Container container)
    {
        execute(getDependentSampleFileReplicateDeleteSql(getTableInfoReplicateAnnotation(), "ReplicateId", container));
        execute(getDependentSampleFileReplicateDeleteSql(getTableInfoQCMetricExclusion(), "ReplicateId", container));
        execute(getDependentSampleFileReplicateDeleteSql(getTableInfoReplicate(), "Id", container));

        deletePredictorsForUnusedReplicates(container);
    }

    /**
     * @return the source file path for a sampleFile
     */
    @Nullable
    public static SampleFile getSampleFileUploadFile(int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT d.DataFileUrl FROM ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append(", ");
        sql.append(getTableInfoSampleFile(), "sf");
        sql.append(", ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(", ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE rep.Id = sf.ReplicateId AND rep.RunId = r.Id AND d.RowId = r.DataId AND sf.Id = ? ");
        sql.add(sampleFileId);

        String filePath = (String) new SqlSelector(getSchema(), sql).getMap().get("dataFileUrl");
        if(null != filePath && !filePath.isEmpty())
        {
            SampleFile sampleFile = new SampleFile();
            sampleFile.setFilePath(filePath);
            return sampleFile;
        }
        return null;
    }

    /**
     * @return a SampleFile that contains the file path of the import file containing the sample
     */
    @Nullable
    public static SampleFile deleteSampleFileAndDependencies(int sampleFileId)
    {
        purgeDeletedSampleFiles(sampleFileId);

        SampleFile file = getSampleFileUploadFile(sampleFileId);

        execute("DELETE FROM " + getTableInfoSampleFile() + " WHERE Id = " + sampleFileId);

        return file;
    }

    public static void purgeDeletedSampleFiles(int sampleFileId)
    {
        // Delete from TransitionChromInfoAnnotation (dependent of TransitionChromInfo)
        execute(getDependentSampleFileDeleteSql(getTableInfoTransitionChromInfoAnnotation(), "TransitionChromInfoId", getTableInfoTransitionChromInfo()), sampleFileId);

        // Delete from TransitionAreaRatio (dependent of TransitionChromInfo)
        execute(getDependentSampleFileDeleteSql(getTableInfoTransitionAreaRatio(), "TransitionChromInfoId", getTableInfoTransitionChromInfo()), sampleFileId);

        // Delete from TransitionChromInfo
        execute("DELETE FROM " + getTableInfoTransitionChromInfo() + " WHERE SampleFileId = ?", sampleFileId);

        // Delete from PrecursorChromInfoAnnotation (dependent of PrecursorChromInfo)
        execute(getDependentSampleFileDeleteSql(getTableInfoPrecursorChromInfoAnnotation(), "PrecursorChromInfoId", getTableInfoPrecursorChromInfo()), sampleFileId);

        // Delete from PrecursorAreaRatio (dependent of PrecursorChromInfo)
        execute(getDependentSampleFileDeleteSql(getTableInfoPrecursorAreaRatio(), "PrecursorChromInfoId", getTableInfoPrecursorChromInfo()), sampleFileId);

        // Delete from PrecursorChromInfo
        execute("DELETE FROM " + getTableInfoPrecursorChromInfo() + " WHERE SampleFileId = ?", sampleFileId);

        // Delete from PeptideAreaRatio (dependent of PeptideChromInfo)
        execute(getDependentSampleFileDeleteSql(getTableInfoPeptideAreaRatio(), "PeptideChromInfoId", getTableInfoGeneralMoleculeChromInfo()), sampleFileId);

        // Delete from PeptideChromInfo
        execute("DELETE FROM " + getTableInfoGeneralMoleculeChromInfo() + " WHERE SampleFileId = ?", sampleFileId);
    }

    private static String getDependentSampleFileDeleteSql(TableInfo fromTable, String fromFk, TableInfo dependentTable)
    {
        return "DELETE FROM " + fromTable + " WHERE " + fromFk + " IN (SELECT Id FROM " + dependentTable + " WHERE SampleFileId = ?)";
    }

    /** Actually delete runs that have been marked as deleted from the database */
    private static void purgeDeletedRuns()
    {
        // Delete from FoldChange
        deleteRunDependent(getTableInfoFoldChange());
        // Delete from CalibrationCurve
        deleteRunDependent(getTableInfoCalibrationCurve());

        // Delete from TransitionChromInfoAnnotation
        deleteTransitionChromInfoDependent(getTableInfoTransitionChromInfoAnnotation());
        // Delete from TransitionAreaRatio
        deleteTransitionChromInfoDependent(getTableInfoTransitionAreaRatio());

        // Delete from PrecursorChromInfoAnnotation
        deletePrecursorChromInfoDependent(getTableInfoPrecursorChromInfoAnnotation());
        // Delete from PrecursorAreaRatio
        deletePrecursorChromInfoDependent(getTableInfoPrecursorAreaRatio());

        // Delete from PeptideAreaRatio
        deleteGeneralMoleculeChromInfoDependent(getTableInfoPeptideAreaRatio());

        // Delete from TransitionChromInfo
        deleteGeneralTransitionDependent(getTableInfoTransitionChromInfo(), "TransitionId");
        // Delete from TransitionAnnotation
        deleteGeneralTransitionDependent(getTableInfoTransitionAnnotation(), "TransitionId");
        // Delete from TransitionLoss
        deleteGeneralTransitionDependent(getTableInfoTransitionLoss(), "TransitionId");
        // Delete from TransitionOptimization
        deleteGeneralTransitionDependent(getTableInfoTransitionOptimization(), "TransitionId");
        // Delete from MoleculeTransition
        deleteGeneralTransitionDependent(getTableInfoMoleculeTransition(), "TransitionId");
        // Delete from Transition
        deleteGeneralTransitionDependent(getTableInfoTransition(), "Id");

        //Delete GeneralTransition
        deleteGeneralPrecursorDependent(getTableInfoGeneralTransition(), "GeneralPrecursorId");
        // Delete from PrecursorChromInfo
        deleteGeneralPrecursorDependent(getTableInfoPrecursorChromInfo(), "PrecursorId");
        // Delete from PrecursorAnnotation
        deleteGeneralPrecursorDependent(getTableInfoPrecursorAnnotation(), "PrecursorId");
        // Delete from PrecursorLibInfo
        deleteGeneralPrecursorDependent(getTableInfoPrecursorLibInfo(), "PrecursorId");
        // Delete from Precursor
        deleteGeneralPrecursorDependent(getTableInfoPrecursor(), "Id");
        //Delete from MoleculePrecursor
        deleteGeneralPrecursorDependent(getTableInfoMoleculePrecursor(), "Id");

        //Delete GeneralPrecursor
        deleteGeneralMoleculeDependent(getTableInfoGeneralPrecursor(), "GeneralMoleculeId");
        // Delete from GeneralMoleculeAnnotation
        deleteGeneralMoleculeDependent(getTableInfoGeneralMoleculeAnnotation(), "GeneralMoleculeId");
        // Delete from GeneralMoleculeChromInfo
        deleteGeneralMoleculeDependent(getTableInfoGeneralMoleculeChromInfo(), "GeneralMoleculeId");

        // Delete from PeptideStructuralModification
        deleteGeneralMoleculeDependent(getTableInfoPeptideStructuralModification(), "PeptideId");
        // Delete from PeptideIsotopeModification
        deleteGeneralMoleculeDependent(getTableInfoPeptideIsotopeModification(), "PeptideId");
        // Delete from Molecule
        deleteGeneralMoleculeDependent(getTableInfoMolecule(), "Id");
        // Delete from Peptide
        deleteGeneralMoleculeDependent(getTableInfoPeptide(), "Id");

        //Delete from GeneralMolecule
        deletePeptideGroupDependent(getTableInfoGeneralMolecule());
        // Delete from Protein
        deletePeptideGroupDependent(getTableInfoProtein());
        // Delete from PeptideGroupAnnotation
        deletePeptideGroupDependent(getTableInfoPeptideGroupAnnotation());

        // Delete from sampleFile
        deleteReplicateDependent(getTableInfoSampleFile());
        // Delete from ReplicateAnnotation
        deleteReplicateDependent(getTableInfoReplicateAnnotation());
        // Delete from QCMetricExclusion
        deleteReplicateDependent(getTableInfoQCMetricExclusion());

        // Delete from IsolationWindow
        deleteIsolationSchemeDependent(getTableInfoIsolationWindow());

        // Delete from MeasuredDriftTime
        deleteDriftTimePredictionSettingsDependent(getTableInfoMeasuredDriftTime());

        // Delete from PeptideGroup
        deleteRunDependent(getTableInfoPeptideGroup());
        // Delete from Replicate
        deleteRunDependent(getTableInfoReplicate());
        // Delete from TransitionInstrumentSettings
        deleteRunDependent(getTableInfoTransInstrumentSettings());
        // Delete from Instrument
        deleteRunDependent(getTableInfoInstrument());
        // Delete from RetentionTimePredictionSettings
        deleteRunDependent(getTableInfoRetentionTimePredictionSettings());
        // Delete from DriftTimePredictionSettings
        deleteRunDependent(getTableInfoDriftTimePredictionSettings());

        // Delete from PredictorSettings and Predictor
        // This has to be done BEFORE deleting from TransitionPredictionSettings and
        // AFTER deleting from Replicate (
        deleteTransitionPredictionSettingsDependent();
        // Delete from TransitionPredictionSettings
        deleteRunDependent(getTableInfoTransitionPredictionSettings());

        // Delete from TransitionFullScanSettings
        deleteRunDependent(getTableInfoTransitionFullScanSettings());
        // Delete from IsotopeEnrichment (part of Full Scan settings)
        deleteRunDependent(getTableInfoIsotopeEnrichment());
        // Delete from ModificationSettings
        deleteRunDependent(getTableInfoModificationSettings());
        // Delete from RunStructuralModification
        deleteRunDependent(getTableInfoRunStructuralModification());
        // Delete from RunIsotopeModification
        deleteRunDependent(getTableInfoRunIsotopeModification());
        // Delete from IsotopeLabel
        deleteRunDependent(getTableInfoIsotopeLabel());
        // Delete from LibrarySettings
        deleteRunDependent(getTableInfoLibrarySettings());
        // Delete from SpectrumLibrary
        deleteRunDependent(getTableInfoSpectrumLibrary());
        // Delete from RunEnzyme
        deleteRunDependent(getTableInfoRunEnzyme());
        // Delete from AnnotationSettings
        deleteRunDependent(getTableInfoAnnotationSettings());
        // Delete from GroupComparisons
        deleteRunDependent(getTableInfoGroupComparisonSettings());
        // Delete from CalibrationCurve
        deleteRunDependent(getTableInfoQuantificationSettings());
        // Delete from IsolationScheme
        deleteRunDependent(getTableInfoIsolationScheme());

        // Delete from runs
        execute("DELETE FROM " + getTableInfoRuns() + " WHERE Deleted = ?", true);

        // Remove any cached results for the deleted runs
        removeCachedResults();
    }

    private static void removeCachedResults()
    {
        // Get a list of deleted runs
        SQLFragment sql = new SQLFragment("SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted =  ?", true);
        List<Integer> deletedRunIds = new SqlSelector(getSchema(), sql).getArrayList(Integer.class);
        if(deletedRunIds.size() > 0)
        {
            ModificationManager.removeRunCachedResults(deletedRunIds);
            PeptideManager.removeRunCachedResults(deletedRunIds);
            PrecursorManager.removeRunCachedResults(deletedRunIds);
        }
    }

    public static void deleteTransitionChromInfoDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE TransitionChromInfoId IN (SELECT tci.Id FROM " + getTableInfoTransitionChromInfo() + " tci "+
                " INNER JOIN " + getTableInfoGeneralTransition() + " gt ON tci.TransitionId = gt.Id " +
                " INNER JOIN " + getTableInfoGeneralPrecursor() + " gp ON gt.GeneralPrecursorId = gp.Id "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON gp.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    public static void deletePrecursorChromInfoDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE PrecursorChromInfoId IN (SELECT pci.Id FROM " + getTableInfoPrecursorChromInfo() + " pci "+
                " INNER JOIN " + getTableInfoGeneralPrecursor() + " gp ON pci.PrecursorId = gp.Id "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON gp.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    public static void deleteGeneralMoleculeChromInfoDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE PeptideChromInfoId IN (SELECT mci.Id FROM " + getTableInfoGeneralMoleculeChromInfo() + " mci "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON mci.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    public static void deleteGeneralTransitionDependent(TableInfo tableInfo, String colName)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE " + colName + " IN (SELECT gt.Id FROM " + getTableInfoGeneralTransition() + " gt "+
                " INNER JOIN " + getTableInfoGeneralPrecursor() + " gp ON gt.GeneralPrecursorId = gp.Id "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON gp.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    private static void deleteGeneralPrecursorDependent(TableInfo tableInfo, String colName)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE " + colName + " IN (SELECT gp.Id FROM " + getTableInfoGeneralPrecursor() + " gp "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON gp.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    private static void deleteGeneralMoleculeDependent(TableInfo tableInfo, String colName)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE " + colName + " IN (SELECT gm.Id FROM " + getTableInfoGeneralMolecule() + " gm "+
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    private static void deletePeptideGroupDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE PeptideGroupId IN (SELECT pg.Id FROM " + getTableInfoPeptideGroup() + " pg "+
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    private static void deleteRunDependent(TableInfo tableInfo)
    {
        execute("DELETE FROM " + tableInfo + " WHERE RunId IN (SELECT Id FROM " +
                getTableInfoRuns() + " WHERE Deleted = ?)", true);
    }

    private static void deleteReplicateDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE ReplicateId IN (SELECT rep.Id FROM " + getTableInfoReplicate() + " rep "+
                " INNER JOIN " + getTableInfoRuns() + " r ON rep.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    private static void deleteTransitionPredictionSettingsDependent()
    {
        execute("DELETE FROM " + getTableInfoPredictorSettings() + " WHERE PredictorId IN (SELECT Id FROM " +
                getTableInfoPredictor() + " WHERE " +
                "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?))"
                , true, true);

        execute("DELETE FROM " + getTableInfoPredictor() + " WHERE " +
                "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)"
                , true, true);
    }

    private static void deleteIsolationSchemeDependent(TableInfo tableInfo)
    {
        execute("DELETE FROM " + tableInfo + " WHERE IsolationSchemeId IN (SELECT Id FROM " +
                getTableInfoIsolationScheme() + " WHERE RunId IN (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteDriftTimePredictionSettingsDependent(TableInfo tableInfo)
    {
        execute("DELETE FROM " + tableInfo + " WHERE DriftTimePredictionSettingsId IN (SELECT Id FROM " +
                getTableInfoDriftTimePredictionSettings() + " WHERE RunId IN (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void execute(String sql, @NotNull Object... parameters)
    {
        new SqlExecutor(getSchema()).execute(sql, parameters);
    }

    public static void deleteiRTscales(Container c)
    {
        // Check every iRT scale in the container to see if we can delete that scale. This is brute force, but
        // it avoids a situation seen during development in which a Work In Progress bug caused a broken reference to iRTScale from a run,
        // which resulted in an undeleteable container.
        for (Integer scaleId : getIrtScaleIds(c))
        {
            // Only delete if no runs still reference this iRT scale
            if (!(new TableSelector(getTableInfoRuns(), new SimpleFilter(FieldKey.fromParts("iRTScaleId"), scaleId), null).exists()))
            {
                Table.delete(getTableInfoiRTPeptide(), new SimpleFilter(FieldKey.fromParts("iRTScaleId"), scaleId));
                Table.delete(getTableInfoiRTScale(), new SimpleFilter(FieldKey.fromParts("id"), scaleId));
            }
        }
    }

    public static ArrayList<Integer> getIrtScaleIds(Container c)
    {
        SimpleFilter conFil = SimpleFilter.createContainerFilter(c);
        return new TableSelector(TargetedMSManager.getTableInfoiRTScale().getColumn(FieldKey.fromParts("id")), conFil , null).getArrayList(Integer.class);
    }

    // return the ModuleProperty value for "TARGETED_MS_FOLDER_TYPE"
    public static TargetedMSModule.FolderType getFolderType(Container c)
    {
        TargetedMSModule targetedMSModule = null;
        for (Module m : c.getActiveModules())
        {
            if (m instanceof TargetedMSModule)
            {
                targetedMSModule = (TargetedMSModule) m;
            }
        }
        if (targetedMSModule == null)
        {
            return null; // no TargetedMS module found - do nothing
        }
        ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(TARGETED_MS_FOLDER_TYPE);
        String svalue = moduleProperty.getValueContainerSpecific(c);
        try
        {
            return TargetedMSModule.FolderType.valueOf(svalue);
        }
        catch (IllegalArgumentException e)
        {
            // return undefined if the string does not match any type
            return TargetedMSModule.FolderType.Undefined;
        }
    }

    public static void renameRun(int runId, String newDescription, User user) throws BatchValidationException
    {
        if (newDescription == null || newDescription.length() == 0)
            return;

        new SqlExecutor(getSchema()).execute("UPDATE " + getTableInfoRuns() + " SET Description=? WHERE Id = ?",
                            newDescription, runId);
        TargetedMSRun run = getRun(runId);
        if (run != null)
        {
            // Keep the experiment run wrapper in sync
            ExpRun expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun != null)
            {
                expRun.setName(newDescription);
                expRun.save(user);
            }
        }
    }

    /** @return the sample file if it has already been imported in the container */
    @Nullable
    public static Replicate getReplicate(int replicateId, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT rep.* FROM ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append(", ");
        sql.append(getTableInfoRuns(), "r");
        sql.append( " WHERE r.Id = rep.RunId AND rep.Id = ? AND r.Container = ? ");
        sql.add(replicateId);
        sql.add(container);
        return new SqlSelector(getSchema(), sql).getObject(Replicate.class);
    }

    /** @return the sample file if it has already been imported in the container */
    @Nullable
    public static List<SampleFile> getSampleFile(String filePath, Date acquiredTime, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT sf.* FROM ");
        sql.append(getTableInfoSampleFile(), "sf");
        sql.append(", ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append(", ");
        sql.append(getTableInfoRuns(), "r");
        sql.append( " WHERE r.Id = rep.RunId AND rep.Id = sf.ReplicateId AND r.Container = ? AND sf.FilePath = ? ");
        sql.add(container);
        sql.add(filePath);
        if(acquiredTime == null)
            sql.append("AND sf.AcquiredTime IS NULL");
        else
        {
            sql.append("AND sf.AcquiredTime = ?");
            sql.add(acquiredTime);
        }
        return new SqlSelector(getSchema(), sql).getArrayList(SampleFile.class);
    }

    public Map<String, Object> getAutoQCPingMap(Container container)
    {
        TableInfo table = TargetedMSManager.getTableInfoAutoQCPing();
        return new TableSelector(table, SimpleFilter.createContainerFilter(container), null).getMap();
    }

    // return the ModuleProperty value for "AUTO_QC_PING_TIMEOUT"
    public int getAutoQCPingTimeout(Container container)
    {
        TargetedMSModule targetedMSModule = null;
        int timeoutValue = 15;

        for (Module m : container.getActiveModules())
        {
            if (m instanceof TargetedMSModule)
            {
                targetedMSModule = (TargetedMSModule) m;
                break;
            }
        }

        if (targetedMSModule != null)
        {
            ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(TargetedMSModule.AUTO_QC_PING_TIMEOUT);
            try
            {
                timeoutValue = Integer.parseInt(moduleProperty.getEffectiveValue(container));
            }
            catch (NumberFormatException e)
            {
                // noop, stick with the default value for the timeout setting
            }
        }

        return timeoutValue;
    }

    public static Map<String, Map<String, Double>> getClustergrammerQuery(User user, Container container, Integer[] rowIds)
    {
        //Set column names
        String intensityColumnName = "Area";
        String rowHeadingColumnName = "ProteinName";

        //TODO: This query will be replaced once Skyline Updates are ready
        String sql = "SELECT\n" +
                "    SUM(TotalArea) AS " + intensityColumnName + ",\n" +
                "    SampleFileId.SampleName AS SampleName,\n" +
                "    PrecursorId.PeptideId.PeptideGroupId.Label AS " + rowHeadingColumnName + "\n" +
                "FROM \n" +
                "    PrecursorChromInfo pci, targetedms.targetedmsruns r\n" +
                "WHERE\n" +
                "    PrecursorId.PeptideId.PeptideGroupId.RunId = r.File.Id AND r.RowId IN (" + Joiner.on(", ").skipNulls().join(rowIds) + ")\n" +
                "GROUP BY\n" +
                "    SampleFileId.SampleName,\n" +
                "    PrecursorId.PeptideId.PeptideGroupId.Label\n" +
                "PIVOT " + intensityColumnName + " By SampleName";

        QueryDefinition qdef = QueryService.get().createQueryDef(user, container, SchemaKey.fromString(getSchema().getQuerySchemaName()), "ClustergrammerHeatMap");
        qdef.setSql(sql);
        qdef.setIsHidden(true);
        qdef.setIsTemporary(true);

        List<QueryException> errors = new ArrayList<>();
        TableInfo table = qdef.getTable(errors, true);

        Map<String, Map<String, Double>> intensities = new HashMap<>();

        for (Map<String, Object> rowMap:new TableSelector(table).getMapArray())
        {
            List <ColumnInfo> columns = table.getColumns();

            List<Double> values = new ArrayList<>();

            for(ColumnInfo column : columns)
            {
                //Skip pivot column and row name column
                String colName = column.getName();
                if (colName.compareToIgnoreCase(intensityColumnName) == 0 || colName.compareToIgnoreCase(rowHeadingColumnName) == 0)
                    continue;

                Double value = getValue(column.getValue(rowMap));
                if (value != null)
                {
                    values.add(value);
                }
            }

            double[] primitiveValues = new double[values.size()];
            int index = 0;
            for (Double value : values)
            {
                primitiveValues[index++] = value.doubleValue();
            }

            MathStat stats = StatsService.get().getStats(primitiveValues);

            for(ColumnInfo column : columns)
            {
                String colName = column.getName();
                if (colName.compareToIgnoreCase(intensityColumnName) == 0 || colName.compareToIgnoreCase(rowHeadingColumnName) == 0)
                    continue;

                Map<String, Double> intensityMap = intensities.get(column.getLabel());
                if (intensityMap == null)
                {
                    intensityMap = new HashMap<>();
                    intensities.put(column.getLabel(), intensityMap);
                }

                Double value = getValue(column.getValue(rowMap));
                if (value != null)
                {
                    value = (value.doubleValue() - stats.getMean()) / stats.getStdDev();
                }
                intensityMap.put((String)rowMap.get(rowHeadingColumnName), value);
            }
        }

        return intensities;
    }

    private static Double getValue(Object o)
    {
        Double value = (Double)JdbcType.DOUBLE.convert(o);
        if (value == null || value == 0.0)
        {
            return null;
        }

        return Math.log(value.doubleValue()) / Math.log(2);
    }

    public List<QCMetricConfiguration> getEnabledQCMetricConfigurations(Container container, User user)
    {
        String sql = "SELECT Id, " +
                        "Name, " +
                        "Series1Label, " +
                        "Series1SchemaName, " +
                        "Series1QueryName, " +
                        "Series2Label, " +
                        "Series2SchemaName, " +
                        "Series2QueryName " +
                      "FROM qcMetricsConfig WHERE Enabled = TRUE";

        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        return QueryService.get().selector(query, sql).getArrayList(QCMetricConfiguration.class);
    }

    public static int getMaxTransitionCount(int moleculeId)
    {
        SQLFragment maxTransitionSQL = new SQLFragment("select MAX(c) FROM\n" +
                "(\n" +
                "select pci.id, COUNT(DISTINCT tci.Id) AS C FROM \n");
        maxTransitionSQL.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        maxTransitionSQL.append(" INNER JOIN \n");
        maxTransitionSQL.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        maxTransitionSQL.append(" ON tci.precursorchrominfoid = pci.id INNER JOIN \n");
        maxTransitionSQL.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        maxTransitionSQL.append(" ON pci.generalmoleculechrominfoid = gmci.id \n" +
                "WHERE\n" +
                "gmci.generalmoleculeid = ?\n" +
                "GROUP BY pci.id\n" +
                ") x\n");
        maxTransitionSQL.add(moleculeId);

        Integer maxCount = new SqlSelector(getSchema(), maxTransitionSQL).getObject(Integer.class);
        return maxCount != null ? maxCount.intValue() : 0;
    }

    public static int getMaxTransitionCountForPrecursor(int precursorId)
    {
        SQLFragment maxTransitionSQL = new SQLFragment("select MAX(c) FROM\n" +
                "(\n" +
                "select pci.id, COUNT(DISTINCT tci.Id) AS C FROM \n");
        maxTransitionSQL.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        maxTransitionSQL.append(" INNER JOIN \n");
        maxTransitionSQL.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        maxTransitionSQL.append(" ON tci.precursorchrominfoid = pci.id \n");
        maxTransitionSQL.append(" WHERE pci.precursorid = ?\n" +
                "GROUP BY pci.id\n" +
                ") x\n");
        maxTransitionSQL.add(precursorId);

        Integer maxCount = new SqlSelector(getSchema(), maxTransitionSQL).getObject(Integer.class);
        return maxCount != null ? maxCount.intValue() : 0;
    }

    static void moveRun(TargetedMSRun run, Container newContainer, String newRunLSID, int newDataRowId, User user)
    {
        // MoveRunsTask.moveRun ensures a transaction
        SQLFragment updatePrecChromInfoSql = new SQLFragment("UPDATE ");
        updatePrecChromInfoSql.append(getTableInfoPrecursorChromInfo(), "");
        updatePrecChromInfoSql.append(" SET container = ?").add(newContainer);
        updatePrecChromInfoSql.append(" WHERE sampleFileId IN (");
        updatePrecChromInfoSql.append(" SELECT sf.Id FROM ").append(getTableInfoSampleFile(), "sf");
        updatePrecChromInfoSql.append(" INNER JOIN ").append(getTableInfoReplicate(), "rep").append(" ON rep.Id = sf.ReplicateId");
        updatePrecChromInfoSql.append(" WHERE rep.runId = ?").add(run.getId());
        updatePrecChromInfoSql.append(" )");

        new SqlExecutor(getSchema()).execute(updatePrecChromInfoSql);

        run.setExperimentRunLSID(newRunLSID);
        run.setDataId(newDataRowId);
        run.setContainer(newContainer);
        updateRun(run, user);
    }

    private static void addParentRunsToChain(ArrayDeque<Integer> chainRowIds, Map<Integer, Integer> replacedByMap, Integer rowId)
    {
        // add all runs rowIds up the chain to the end of the list, recursively
        Integer replacedBy = replacedByMap.get(rowId);
        int originalSize = chainRowIds.size();
        if (replacedBy != null)
        {
            if (!chainRowIds.contains(rowId))
                chainRowIds.addLast(rowId);
            if (!chainRowIds.contains(replacedBy))
                chainRowIds.addLast(replacedBy);

            if (chainRowIds.size() != originalSize)
            {
                addParentRunsToChain(chainRowIds, replacedByMap, replacedBy);
            }
        }
    }

    private static void addChildRunsToChain(ArrayDeque<Integer> chainRowIds, Map<Integer, Integer> replacesMap, Integer rowId)
    {
        // add all runs rowIds down the chain to the front of the list, recursively
        Integer replaces = replacesMap.get(rowId);
        int originalSize = chainRowIds.size();
        if (replaces != null)
        {
            if (!chainRowIds.contains(rowId))
                chainRowIds.addFirst(rowId);
            if (!chainRowIds.contains(replaces))
                chainRowIds.addFirst(replaces);

            if (chainRowIds.size() != originalSize)
            {
                addChildRunsToChain(chainRowIds, replacesMap, replaces);
            }
        }
    }

    public static Collection<Integer> getLinkedVersions(User u, Container c, Collection<Integer> selectedRowIds, Collection<Integer> linkedRowIds)
    {
        Set<Integer> result = new HashSet<>(linkedRowIds);
        //get related/linked RowIds from TargetedMSRuns table
        QuerySchema targetedMSRunsQuerySchema = DefaultSchema.get(u, c).getSchema(TargetedMSSchema.getSchema().getName());
        if (targetedMSRunsQuerySchema != null)
        {
            //create a filter for non-null ReplacedByRun value
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("ReplacedByRun"), null, CompareType.NONBLANK);

            //create a set of column names with RowId and ReplacedByRun, a pair representing a parent child relationship between any two documents
            Set<String> idColumnNames = new LinkedHashSet<>();
            idColumnNames.add("RowId"); //RowId is parent or original document's rowid
            idColumnNames.add("ReplacedByRun");//ReplacedByRun is child or modified document's rowid

            TableInfo runsTable = targetedMSRunsQuerySchema.getTable(TargetedMSSchema.TABLE_TARGETED_MS_RUNS);
            TableSelector selector = new TableSelector(runsTable, idColumnNames, filter, null);

            //get RowId -> ReplacedByRun key value pairs and also populate the opposite direction to get ReplacedByRun -> RowId
            Map<Integer, Integer> replacedByMap = selector.getValueMap();
            Map<Integer, Integer> replacesMap = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : replacedByMap.entrySet())
                replacesMap.put(entry.getValue(), entry.getKey());

            //get full chain for the selected runs to be added to the linkedRowIds list
            for (Integer rowId : selectedRowIds)
            {
                ArrayDeque<Integer> chainRowIds = new ArrayDeque<>();
                addParentRunsToChain(chainRowIds, replacedByMap, rowId);
                addChildRunsToChain(chainRowIds, replacesMap, rowId);

                result.addAll(chainRowIds);
            }
        }

        return result;
    }

    private static int getCountForRunFKTable(int runId, TableInfo table)
    {
        SimpleFilter runFilter = new SimpleFilter(FieldKey.fromParts("RunId"), runId, CompareType.EQUAL);
        TableSelector selector = new TableSelector(table, runFilter, null);
        return toIntExact(selector.getRowCount());
    }

    public List<String> getReplicateSubgroupNames(User user, Container container, @NotNull GeneralMolecule molecule)
    {
        UserSchema userSchema = QueryService.get().getUserSchema(user, container, "targetedms");
        TableInfo tableInfo = userSchema.getTable("pharmacokinetics");

        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(COALESCE(p.SubGroup, 'NA')) FROM ");
        sqlFragment.append(tableInfo, "p");
        sqlFragment.append(" WHERE p.MoleculeId = ? ");
        sqlFragment.add(Integer.toString(molecule.getId()));

        String[] sampleGroupNames = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment).getArray(String.class);
        Arrays.sort(sampleGroupNames);
        return Arrays.asList(sampleGroupNames);
    }

    public static boolean containerHasSmallMolecules(Container container)
    {
        return new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", container, false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE SmallMoleculeCount > 0 AND Container = ? AND Deleted = ?")).exists();
    }

    public static boolean containerHasPeptides(Container container)
    {
        return new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", container, false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE PeptideCount > 0 AND Container = ? AND Deleted = ?")).exists();
    }

    public static boolean containerHasCalibrationCurves(Container container)
    {
        return new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", container, false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE CalibrationCurveCount > 0 AND Container = ? AND Deleted = ?")).exists();
    }

    public static boolean containerHasDocVersions(Container container)
    {
        ExperimentService svc = ExperimentService.get();
        return new SqlSelector(svc.getSchema(), new SQLFragment("SELECT r.rowId FROM ", Boolean.FALSE, container )
                .append(svc.getTinfoExperimentRun(), "r")
                .append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), "tRuns")
                .append(" ON (tRuns.ExperimentRunLSID = r.lsid AND tRuns.Deleted = ?)")
                .append(" WHERE r.Container = ? AND r.ReplacedByRunId IS NOT NULL")).exists();
    }
}