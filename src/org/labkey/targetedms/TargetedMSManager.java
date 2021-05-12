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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.QCTraceMetricValues;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.model.passport.IKeyword;
import org.labkey.targetedms.outliers.OutlierGenerator;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.SampleFileChromInfo;
import org.labkey.targetedms.parser.TransitionSettings;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;
import org.labkey.targetedms.pipeline.TargetedMSImportPipelineJob;
import org.labkey.targetedms.query.GuideSetTable;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.labkey.api.targetedms.TargetedMSService.FOLDER_TYPE_PROP_NAME;
import static org.labkey.api.targetedms.TargetedMSService.MODULE_NAME;

public class TargetedMSManager
{
    private static final TargetedMSManager _instance = new TargetedMSManager();

    private static final Logger _log = LogManager.getLogger(TargetedMSManager.class);

    private TargetedMSManager()
    {
        // prevent external construction with a private default constructor
    }

    public static TargetedMSManager get()
    {
        return _instance;
    }

    public static List<SampleFileChromInfo> getSampleFileChromInfos(SampleFile sampleFile)
    {
        return new TableSelector(getTableInfoSampleFileChromInfo(), new SimpleFilter(FieldKey.fromParts("SampleFileId"), sampleFile.getId()), new Sort("TextId")).getArrayList(SampleFileChromInfo.class);    }

    public static SampleFileChromInfo getSampleFileChromInfo(int id, Container c)
    {
        return new TableSelector(getTableInfoSampleFileChromInfo(), new SimpleFilter(FieldKey.fromParts("Id"), id).addCondition(FieldKey.fromParts("Container"), c), null).getObject(SampleFileChromInfo.class);
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

    public static TableInfo getTableInfoSampleFileChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE_CHROM_INFO);
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

    public static TableInfo getTableQCTraceMetricValues()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_QC_TRACE_METRIC_VALUES);
    }

    public static TableInfo getTableInfoSkylineAuditLogEntry()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG_ENTRY);
    }

    /** View that's a CTE to pull in the RunId */
    public static TableInfo getTableInfoSkylineAuditLog()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG);
    }

    public static TableInfo getTableInfoSkylineAuditLogMessage()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SKYLINE_AUDITLOG_MESSAGE);
    }

    public static TableInfo getTableInfoListDefinition() {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIST_DEFINITION);
    }

    public static TableInfo getTableInfoListColumnDefinition() {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIST_COLUMN_DEFINITION);
    }

    public static TableInfo getTableInfoListItem() {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIST_ITEM);
    }

    public static TableInfo getTableInfoListItemValue() {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIST_ITEM_VALUE);
    }

    public static TableInfo getTableInfoBibliospec()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_BIBLIOSPEC_LIB_INFO);
    }

    public static TableInfo getTableInfoHunterLib()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_HUNTER_LIB_INFO);
    }

    public static TableInfo getTableInfoNistLib()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_NIST_LIB_INFO);
    }

    public static TableInfo getTableInfoSpectrastLib()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SPECTRAST_LIB_INFO);
    }

    public static TableInfo getTableInfoChromatogramLib()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_CHROMATOGRAM_LIB_INFO);
    }

    /** @return rowId for pipeline job that will perform the import asynchronously */
    public static Integer addRunToQueue(ViewBackgroundInfo info,
                                        final Path path) throws XarFormatException, PipelineValidationException
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
                @Override
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

        TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(container);
        // Default folder type or Experiment is not representative
        TargetedMSRun.RepresentativeDataState representative = TargetedMSRun.RepresentativeDataState.NotRepresentative;
        if (folderType == TargetedMSService.FolderType.Library)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Peptide;
        else if (folderType == TargetedMSService.FolderType.LibraryProtein)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Protein;

        SkylineDocImporter importer = new SkylineDocImporter(user, container, FileUtil.getFileName(path), expData, null, xarContext, representative, null, null);
        SkylineDocImporter.RunInfo runInfo = importer.prepareRun();
        PipeRoot root = PipelineService.get().findPipelineRoot(info.getContainer());
        if (root == null)
        {
            throw new IllegalStateException("Could not resolve PipeRoot for " + info.getContainer().getPath());
        }
        TargetedMSImportPipelineJob job = new TargetedMSImportPipelineJob(info, expData, runInfo, root, representative);
        PipelineService.get().queueJob(job);
        return PipelineService.get().getJobId(user, container, job.getJobGUID());
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
        ContainerFilter f = ContainerFilter.Type.CurrentAndSubfolders.create(c, user);
        sql.append(" WHERE ");
        sql.append(f.getSQLFragment(getSchema(), new SQLFragment("Container")));
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

    public static List<Long> getRunIdsByInstrument(String serialNumber)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT r.Id FROM ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append(", ");
        sql.append(getTableInfoSampleFile(), "sf");
        sql.append(", ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(" WHERE rep.Id = sf.ReplicateId AND rep.RunId = r.Id AND sf.instrumentSerialNumber = ? ");
        sql.add(serialNumber);
        return new SqlSelector(getSchema(), sql).getArrayList(Long.class);
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
    public static TargetedMSRun getRun(long runId)
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
        return getRuns("Container=? AND StatusId=? AND deleted=?",
                container.getId(), SkylineDocImporter.STATUS_SUCCESS, Boolean.FALSE);
    }

    @Nullable
    public static TargetedMSRun getRunByFileName(String fileName, Container container)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Container"), container.getId());
        filter.addCondition(FieldKey.fromParts("FileName"), fileName);
        List<TargetedMSRun> matches = new TableSelector(TargetedMSManager.getTableInfoRuns(), filter, null).getArrayList(TargetedMSRun.class);
        if (matches.size() == 1)
        {
            return matches.get(0);
        }
        return null;
    }

    public static void markRunsNotRepresentative(Container container, TargetedMSRun.RepresentativeDataState representativeState)
    {
        Collection<Long> representativeRunIds = null;

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
        // Issue 40407. Change the representative state only for documents that have already successfully imported.
        // If multiple documents were queued for import in this folder, there will be rows in targetedms.runs
        // for the documents not yet imported.
        updateSql.append(" AND StatusId = ? ");
        updateSql.add(SkylineDocImporter.STATUS_SUCCESS);
        updateSql.append(" AND Id NOT IN ("+StringUtils.join(representativeRunIds, ",")+")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(updateSql);
    }

    public static List<Long> getCurrentRepresentativeRunIds(Container container)
    {
        List<Long> representativeRunIds = null;
        if(getFolderType(container) == TargetedMSService.FolderType.LibraryProtein)
        {
            representativeRunIds = getCurrentProteinRepresentativeRunIds(container);
        }
        else if(getFolderType(container) == TargetedMSService.FolderType.Library)
        {
            representativeRunIds = getCurrentPeptideRepresentativeRunIds(container);
        }

        return representativeRunIds != null ? representativeRunIds : Collections.emptyList();
    }

    private static List<Long> getCurrentProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal());
    }

    private static List<Long> getProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal(),
                                                         RepresentativeDataState.Deprecated.ordinal(),
                                                         RepresentativeDataState.Conflicted.ordinal());
    }

    private static List<Long> getProteinRepresentativeRunIds(Container container, int... stateArray)
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

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getArrayList(Long.class);
    }

    private static List<Long> getCurrentPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal());
    }

    private static List<Long> getPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, RepresentativeDataState.Representative.ordinal(),
                                                         RepresentativeDataState.Deprecated.ordinal(),
                                                         RepresentativeDataState.Conflicted.ordinal());
    }

    private static List<Long> getPeptideRepresentativeRunIds(Container container, int... stateArray)
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

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getArrayList(Long.class);
    }

    public static void updateRun(TargetedMSRun run, User user)
    {
        Table.update(user, getTableInfoRuns(), run, run.getRunId());
    }

    /** Delete all of the TargetedMS runs in the container, including their experiment run wrappers */
    public static void deleteIncludingExperimentWrapper(Container c, User user)
    {
        List<Long> runIds = new SqlSelector(getSchema(), "SELECT Id FROM " + getTableInfoRuns() + " WHERE Container = ?", c).getArrayList(Long.class);

        if (runIds.isEmpty())
            return;

        // Save these to delete after we've deleted the runs
        List<ExpRun> experimentRunsToDelete = new ArrayList<>();

        for (Long runId : runIds)
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
    public static void deleteRuns(List<Long> runIds, Container c, User user)
    {
        List<Long> cantDelete = new ArrayList<>();
        List<TargetedMSRun> runsToDelete = new ArrayList<>();
        for(long runId: runIds)
        {
            TargetedMSRun run = getRun(runId);
            if (run == null || run.isDeleted())
            {
                continue;
            }
            if(!run.getContainer().equals(c) && !run.getContainer().hasPermission(user, DeletePermission.class))
            {
               cantDelete.add(runId);
            }
            runsToDelete.add(run);
        }

        if(cantDelete.size() > 0)
        {
            throw new UnauthorizedException("User does not have permissions to delete run " + (cantDelete.size() > 1 ? "Ids" : "Id")
                    + " " + StringUtils.join(cantDelete, ',')
                    + ". " + (cantDelete.size() > 1 ? "They are" : "It is") + " not in container " + c.getName());
        }

        for (TargetedMSRun run : runsToDelete)
        {
            // Revert the representative state if any of the runs are representative at the protein or peptide level.
            if(run.isRepresentative())
            {
                PipeRoot root = PipelineService.get().getPipelineRootSetting(run.getContainer());
                if (null != root)
                {
                    LocalDirectory localDirectory = LocalDirectory.create(root, MODULE_NAME);
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

        try
        {   //deleting audit log data for these runs.
            SkylineAuditLogManager auditMgr = new SkylineAuditLogManager(c, null);
            for(Long runId : runIds)
                auditMgr.deleteDocumentVersionLog(runId);
        }
        catch (AuditLogException e)
        {
            throw new RuntimeException("Error while deleting document's audit log", e);
        }


        // and then delete them
        purgeDeletedRuns();
    }

    public static TargetedMSRun getRunForPrecursor(long precursorId)
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

    public static TargetedMSRun getRunForGeneralMolecule(long id)
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
    public static Integer getPeptideGroupPeptideCount(@Nullable TargetedMSRun run, long peptideGroupId)
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
    public static Integer getPeptideGroupMoleculeCount(@Nullable TargetedMSRun run, long peptideGroupId)
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

        boolean success;
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

    public static List<GuideSet> getGuideSets(Container c, User u)
    {
        TargetedMSSchema schema = new TargetedMSSchema(u, c);
        TableInfo table = schema.getTable("GuideSetForOutliers", null);
        return new TableSelector(table, null, new Sort("TrainingStart")).getArrayList(GuideSet.class);
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
    public static String getSampleFileUploadFile(long sampleFileId)
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

        var map = new SqlSelector(getSchema(), sql).getMap();
        String filePath = null;
        if(map != null)
        {
            filePath = (String) map.get("dataFileUrl");
        }
        return filePath != null && !filePath.isEmpty() ? filePath : null;
    }

    /**
     * @return the file path of the import file containing the sample
     */
    @Nullable
    public static String deleteSampleFileAndDependencies(long sampleFileId)
    {
        purgeDeletedSampleFiles(sampleFileId);

        String file = getSampleFileUploadFile(sampleFileId);

        execute("DELETE FROM " + getTableInfoSampleFile() + " WHERE Id = " + sampleFileId);

        return file;
    }

    public static void purgeDeletedSampleFiles(long sampleFileId)
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

        // Delete from QCTraceMetricValues
        execute("DELETE FROM " + getTableQCTraceMetricValues() + " WHERE SampleFileId = ?", sampleFileId);

        // Delete from SampleFileChromInfo
        execute("DELETE FROM " + getTableInfoSampleFileChromInfo() + " WHERE SampleFileId = ?", sampleFileId);
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

        SQLFragment whereClause = new SQLFragment("WHERE r.Deleted = ?", true);

        // Delete from TransitionChromInfoAnnotation
        deleteTransitionChromInfoDependent(getTableInfoTransitionChromInfoAnnotation(), whereClause);
        // Delete from TransitionAreaRatio
        deleteTransitionChromInfoDependent(getTableInfoTransitionAreaRatio(), whereClause);

        // Delete from PrecursorChromInfoAnnotation
        deletePrecursorChromInfoDependent(getTableInfoPrecursorChromInfoAnnotation());
        // Delete from PrecursorAreaRatio
        deletePrecursorChromInfoDependent(getTableInfoPrecursorAreaRatio());

        // Delete from PeptideAreaRatio
        deleteGeneralMoleculeChromInfoDependent(getTableInfoPeptideAreaRatio());

        // Delete from TransitionChromInfo
        deleteSampleFileDependent(getTableInfoTransitionChromInfo());
        // Delete from TransitionAnnotation
        deleteGeneralTransitionDependent(getTableInfoTransitionAnnotation(), "TransitionId", whereClause);
        // Delete from TransitionLoss
        deleteGeneralTransitionDependent(getTableInfoTransitionLoss(), "TransitionId", whereClause);
        // Delete from TransitionOptimization
        deleteGeneralTransitionDependent(getTableInfoTransitionOptimization(), "TransitionId", whereClause);
        // Delete from MoleculeTransition
        deleteGeneralTransitionDependent(getTableInfoMoleculeTransition(), "TransitionId", whereClause);
        // Delete from Transition
        deleteGeneralTransitionDependent(getTableInfoTransition(), "Id", whereClause);

        //Delete GeneralTransition
        deleteGeneralPrecursorDependent(getTableInfoGeneralTransition(), "GeneralPrecursorId");
        // Delete from PrecursorChromInfo
        deleteSampleFileDependent(getTableInfoPrecursorChromInfo());
        // Delete from PrecursorAnnotation
        deleteGeneralPrecursorDependent(getTableInfoPrecursorAnnotation(), "PrecursorId");
        // Delete from BiblioSpecLibInfo
        deleteGeneralPrecursorDependent(getTableInfoBibliospec(), "PrecursorId");
        // Delete from HunterLibInfo
        deleteGeneralPrecursorDependent(getTableInfoHunterLib(), "PrecursorId");
        // Delete from NistLibInfo
        deleteGeneralPrecursorDependent(getTableInfoNistLib(), "PrecursorId");
        // Delete from SpectrastLibInfo
        deleteGeneralPrecursorDependent(getTableInfoSpectrastLib(), "PrecursorId");
        // Delete from ChromatogramLibInfo
        deleteGeneralPrecursorDependent(getTableInfoChromatogramLib(), "PrecursorId");
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

        // Delete from QCTraceMetricValues
        deleteSampleFileDependent(getTableQCTraceMetricValues());

        // Delete from SampleFileChromInfo
        deleteSampleFileDependent(getTableInfoSampleFileChromInfo());

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

        // Delete from all list-related tables
        deleteListDependent();

        // Delete from runs
        execute("DELETE FROM " + getTableInfoRuns() + " WHERE Deleted = ?", true);

        // Remove any cached results for the deleted runs
        removeCachedResults();
    }

    private static void removeCachedResults()
    {
        // Get a list of deleted runs
        SQLFragment sql = new SQLFragment("SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted =  ?", true);
        List<Long> deletedRunIds = new SqlSelector(getSchema(), sql).getArrayList(Long.class);
        if(deletedRunIds.size() > 0)
        {
            ModificationManager.removeRunCachedResults(deletedRunIds);
            PeptideManager.removeRunCachedResults(deletedRunIds);
            PrecursorManager.removeRunCachedResults(deletedRunIds);
        }
    }

    public static void deleteTransitionChromInfoDependent(TableInfo tableInfo, SQLFragment whereClause)
    {
        execute(new SQLFragment(" DELETE FROM " + tableInfo +
                " WHERE TransitionChromInfoId IN (SELECT tci.Id FROM " + getTableInfoTransitionChromInfo() + " tci " +
                " INNER JOIN " + getTableInfoSampleFile() + " s ON tci.SampleFileId = s.Id " +
                " INNER JOIN " + getTableInfoReplicate() + " rep ON s.ReplicateId = rep.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON rep.RunId = r.Id ").
                append(whereClause).
                append(")"));
    }

    public static void deletePrecursorChromInfoDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE PrecursorChromInfoId IN (SELECT pci.Id FROM " + getTableInfoPrecursorChromInfo() + " pci "+
                " INNER JOIN " + getTableInfoSampleFile() + " s ON pci.SampleFileId = s.Id " +
                " INNER JOIN " + getTableInfoReplicate() + " rep ON s.ReplicateId = rep.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON rep.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    public static void deleteGeneralMoleculeChromInfoDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE PeptideChromInfoId IN (SELECT mci.Id FROM " + getTableInfoGeneralMoleculeChromInfo() + " mci "+
                " INNER JOIN " + getTableInfoSampleFile() + " s ON mci.SampleFileId = s.Id " +
                " INNER JOIN " + getTableInfoReplicate() + " rep ON s.ReplicateId = rep.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON rep.RunId = r.Id " +
                " WHERE r.Deleted = ?)", true);
    }

    public static void deleteGeneralTransitionDependent(TableInfo tableInfo, String colName, SQLFragment whereClause)
    {
        execute(new SQLFragment(" DELETE FROM " + tableInfo +
                " WHERE " + colName + " IN (SELECT gt.Id FROM " + getTableInfoGeneralTransition() + " gt "+
                " INNER JOIN " + getTableInfoGeneralPrecursor() + " gp ON gt.GeneralPrecursorId = gp.Id "+
                " INNER JOIN " + getTableInfoGeneralMolecule() + " gm ON gp.GeneralMoleculeId = gm.Id " +
                " INNER JOIN " + getTableInfoPeptideGroup() + " pg ON gm.PeptideGroupId = pg.Id " +
                " INNER JOIN " + getTableInfoRuns() + " r ON pg.RunId = r.Id ").append(whereClause).append(")"));
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

    private static void deleteSampleFileDependent(TableInfo tableInfo)
    {
        execute(" DELETE FROM " + tableInfo +
                " WHERE SampleFileId IN (SELECT sf.Id FROM " + getTableInfoSampleFile() + " sf " +
                " INNER JOIN " + getTableInfoReplicate() + " rep ON rep.Id = sf.ReplicateId "+
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

        List<Long> predictorsToDelete = getPredictorsToDelete();

        // Delete from TransitionPredictionSettings
        deleteRunDependent(getTableInfoTransitionPredictionSettings());

        if (!predictorsToDelete.isEmpty())
        {
            // Delete predictors
            execute("DELETE FROM " + getTableInfoPredictor() + " WHERE " +
                    "Id IN (" + predictorsToDelete.stream().map(String::valueOf).collect(Collectors.joining(",")) + " )"
            );
        }
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

    private static void deleteListDefinitionIdDependent(TableInfo tableInfo)
    {
        execute("DELETE FROM " + tableInfo + " WHERE ListDefinitionId IN (SELECT Id FROM " + getTableInfoListDefinition() + " WHERE RunId IN (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteListDependent()
    {
        execute("DELETE FROM " + getTableInfoListItemValue() + " WHERE ListItemId IN" +
                " (SELECT Id FROM " + getTableInfoListItem() + " WHERE ListDefinitionId IN" +
                " (SELECT Id FROM " + getTableInfoListDefinition() + " WHERE RunId IN" +
                " (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?)))", true);
        deleteListDefinitionIdDependent(getTableInfoListItem());
        deleteListDefinitionIdDependent(getTableInfoListColumnDefinition());
        deleteRunDependent(getTableInfoListDefinition());
    }

    private static void execute(String sql, @NotNull Object... parameters)
    {
        new SqlExecutor(getSchema()).execute(sql, parameters);
    }

    private static void execute(SQLFragment sql)
    {
        new SqlExecutor(getSchema()).execute(sql);
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
    public static TargetedMSService.FolderType getFolderType(Container c)
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
        ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(FOLDER_TYPE_PROP_NAME);
        String svalue = moduleProperty.getValueContainerSpecific(c);
        try
        {
            return TargetedMSService.FolderType.valueOf(svalue);
        }
        catch (IllegalArgumentException e)
        {
            // return undefined if the string does not match any type
            return TargetedMSService.FolderType.Undefined;
        }
    }

    public static void renameRun(long runId, String newDescription, User user) throws BatchValidationException
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
    public static Replicate getReplicate(long replicateId, Container container)
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
    public static List<SampleFile> getSampleFile(String filePath, Date acquiredTime, Container container)
    {
        return getSampleFile(filePath, acquiredTime, container, true);
    }

    /** @return the sample file if it has already been imported in the container */
    @Nullable
    public static SampleFile getSampleFile(long id, Container container)
    {
        List<SampleFile> matches = getSampleFiles(container, new SQLFragment("sf.Id = ?", id));
        if (matches.size() > 1)
        {
            throw new IllegalStateException("More than one SampleFile for Id " + id);
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    public static List<SampleFile> getSampleFiles(Container container, @Nullable SQLFragment whereClause)
    {
        SQLFragment sql = new SQLFragment("SELECT sf.*, CASE WHEN x.Id IS NOT NULL THEN ? ELSE ? END AS IgnoreForAllMetric, COALESCE(gs.RowId, 0) AS GuideSetId  FROM ");
        sql.add(true);
        sql.add(false);
        sql.append(getTableInfoSampleFile(), "sf");
        sql.append("\n INNER JOIN  ");
        sql.append(getTableInfoReplicate(), "rep");
        sql.append("\n ON sf.ReplicateId = rep.Id INNER JOIN ");
        sql.append(getTableInfoRuns(), "r");
        sql.append("\n ON rep.RunId = r.Id AND r.Container = ? LEFT JOIN ");
        sql.add(container);
        sql.append(getTableInfoQCMetricExclusion(), "x");
        sql.append("\n ON x.MetricId IS NULL AND x.ReplicateId = rep.Id LEFT JOIN (SELECT g.*, ");
        sql.append(GuideSetTable.getReferenceEndSql("g"));
        sql.append(" AS ReferenceEnd FROM ");
        sql.append(getTableInfoGuideSet(), "g");
        sql.append(" WHERE g.Container = ?) gs ");
        sql.add(container);
        sql.append("\n ON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))");

        if (whereClause != null)
        {
            sql.append("\nWHERE ");
            sql.append(whereClause);
        }
        return new SqlSelector(getSchema(), sql).getArrayList(SampleFile.class);
    }

    /**
     * @return a list of sample files that have the same name and acquired time as the given sample file.
     * The full file path is used instead of just the file name if the acquired time of the given sample file is null
     */
    public static List<SampleFile> getMatchingSampleFiles(@NotNull SampleFile sampleFile, Container container)
    {
        if(sampleFile.getAcquiredTime() != null)
        {
            // Issue 38270. A file may have been imported from a different path in a previous document.  
            // If SampleFile has an acquired time check for a file with same name and acquired time.
            String filePath = sampleFile.getFilePath();
            String fileName = FilenameUtils.getName(filePath);
            if(!StringUtils.isBlank(fileName) && fileName.length() < filePath.length())
            {
                fileName = filePath.substring(filePath.indexOf(fileName) - 1); // Include the separator char
            }
            return getSampleFile(fileName, sampleFile.getAcquiredTime(), container, false);
        }
        else
        {
            return getSampleFile(sampleFile.getFilePath(), sampleFile.getAcquiredTime(), container);
        }
    }

    private static List<SampleFile> getSampleFile(String filePath, Date acquiredTime, Container container, boolean fullPath)
    {
        SQLFragment sql = new SQLFragment();
        if(fullPath)
        {
            sql.append(" sf.FilePath = ? ");
            sql.add(filePath);
        }
        else
        {
            sql.append(" sf.FilePath LIKE ? ");
            sql.add("%" + getSqlDialect().encodeLikeOpSearchString(filePath));
        }
        if(acquiredTime == null)
            sql.append(" AND sf.AcquiredTime IS NULL");
        else
        {
            sql.append(" AND sf.AcquiredTime = ?");
            sql.add(acquiredTime);
        }
        return getSampleFiles(container, sql);
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

    public static Map<String, Map<String, Double>> getClustergrammerQuery(User user, Container container, Long[] rowIds)
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

            String proteinName = (String)rowMap.get(rowHeadingColumnName);
            Map<String, Double> intensityMap = new TreeMap<>();
            boolean hasValue = false;
            for(ColumnInfo column : columns)
            {
                String colName = column.getName();
                if (colName.compareToIgnoreCase(intensityColumnName) == 0 || colName.compareToIgnoreCase(rowHeadingColumnName) == 0)
                    continue;

                String sampleName = column.getLabel();

                Double value = getValue(column.getValue(rowMap));
                if (value != null)
                {
                    value = (value.doubleValue() - stats.getMean()) / stats.getStdDev();
                }

                if (value == null || value.isNaN())
                {
                    // Always store a value so that we have consistent rows
                    intensityMap.put(sampleName, null);
                }
                else
                {
                    hasValue = true;
                    intensityMap.put(sampleName, value);
                }
            }

            if (hasValue)
            {
                // Only stash proteins where we were able to calculate at least one value
                intensities.put(proteinName, intensityMap);
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

    public static List<QCMetricConfiguration> getEnabledQCMetricConfigurations(Container container, User user)
    {
        QuerySchema targetedMSSchema = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        if (targetedMSSchema == null)
        {
            // Module must not be enabled in this folder, so bail out
            return Collections.emptyList();
        }
        TableInfo metricsTable = targetedMSSchema.getTable("qcMetricsConfig", null);
        List<QCMetricConfiguration> metrics = new TableSelector(metricsTable, new SimpleFilter(FieldKey.fromParts("Enabled"), false, CompareType.NEQ_OR_NULL), new Sort(FieldKey.fromParts("Name"))).getArrayList(QCMetricConfiguration.class);
        List<QCMetricConfiguration> result = new ArrayList<>();
        for (QCMetricConfiguration metric : metrics)
        {
            if (metric.getEnabled() == null)
            {
                if (metric.getEnabledQueryName() == null || metric.getEnabledSchemaName() == null)
                {
                    // Metrics without a query to define their default enabled status are on by default
                    result.add(metric);
                }
                else
                {
                    QuerySchema enabledSchema = TargetedMSSchema.SCHEMA_NAME.equalsIgnoreCase(metric.getEnabledSchemaName()) ? targetedMSSchema : DefaultSchema.get(user, container).getSchema(metric.getEnabledSchemaName());
                    if (enabledSchema != null)
                    {
                        TableInfo enabledQuery = enabledSchema.getTable(metric.getEnabledQueryName(), null);
                        if (enabledQuery != null)
                        {
                            if (new TableSelector(enabledQuery).exists())
                            {
                                result.add(metric);
                            }
                        }
                        else
                        {
                            _log.warn("Could not find query " + metric.getEnabledSchemaName() + "." + metric.getEnabledQueryName() + " to determine if metric " + metric.getName() + " should be enabled in container " + container.getPath());
                        }
                    }
                    else
                    {
                        _log.warn("Could not find schema " + metric.getEnabledSchemaName() + " to determine if metric " + metric.getName() + " should be enabled in container " + container.getPath());
                    }
                }
            }
            else
            {
                result.add(metric);
            }
        }
        return result;
    }

    public List<SampleFileInfo> getSampleFileInfos(Container container, User user, Integer sampleFileLimit)
    {
        List<QCMetricConfiguration> enabledQCMetricConfigurations = getEnabledQCMetricConfigurations(container, user);
        if(!enabledQCMetricConfigurations.isEmpty())
        {
            List<GuideSet> guideSets = TargetedMSManager.getGuideSets(container, user);
            Map<Integer, QCMetricConfiguration> metricMap = enabledQCMetricConfigurations.stream().collect(Collectors.toMap(QCMetricConfiguration::getId, Function.identity()));

            List<RawMetricDataSet> rawMetricDataSets = OutlierGenerator.get().getRawMetricDataSets(container, user, enabledQCMetricConfigurations, null, null, Collections.emptyList(), true);

            Map<GuideSetKey, GuideSetStats> stats = OutlierGenerator.get().getAllProcessedMetricGuideSets(rawMetricDataSets, guideSets.stream().collect(Collectors.toMap(GuideSet::getRowId, Function.identity())));

            return OutlierGenerator.get().getSampleFiles(rawMetricDataSets, stats, metricMap, container, sampleFileLimit);
        }
        return Collections.emptyList();
    }

    public static int getMaxTransitionCount(long moleculeId)
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

    public static int getMaxTransitionCountForPrecursor(long precursorId)
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

    public List<String> getReplicateSubgroupNames(User user, Container container, @NotNull GeneralMolecule molecule)
    {
        UserSchema userSchema = QueryService.get().getUserSchema(user, container, "targetedms");
        TableInfo tableInfo = userSchema.getTable("pharmacokinetics");

        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(COALESCE(p.SubGroup, 'NA')) FROM ");
        sqlFragment.append(tableInfo, "p");
        sqlFragment.append(" WHERE p.MoleculeId = ? ");
        sqlFragment.add(Long.toString(molecule.getId()));

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

    public static boolean containerHasLists(Container container)
    {
        return new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", container, false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE ListCount > 0 AND Container = ? AND Deleted = ?")).exists();
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

    public static void updateModifiedAreaProportions(Logger log, @NotNull TargetedMSRun run)
    {
        final String suffix = StringUtilsLabKey.getPaddedUniquifier(9);
        final String precursorGroupingsTableName = getSqlDialect().getTempTablePrefix() + "PrecursorGroupings" + suffix;
        final String moleculeGroupingsTableName = getSqlDialect().getTempTablePrefix() + "MoleculeGroupings" + suffix;
        final String areasTableName = getSqlDialect().getTempTablePrefix() +  "Areas" + suffix;

        log.info("Creating and populating temp tables for Proportion values");

        // Create temp tables to help make the rollups efficient
        SqlExecutor executor = new SqlExecutor(getSchema());
        executor.execute("CREATE " + getSqlDialect().getTempTableKeyword() + " TABLE " +
                precursorGroupingsTableName + "(Grouping " + (getSqlDialect().isSqlServer() ? "NVARCHAR" : "VARCHAR") + "(300), PrecursorId BIGINT)");
        executor.execute("CREATE " + getSqlDialect().getTempTableKeyword() + " TABLE " +
                moleculeGroupingsTableName + "(Grouping " + (getSqlDialect().isSqlServer() ? "NVARCHAR" : "VARCHAR") + "(300), GeneralMoleculeId BIGINT)");
        executor.execute("CREATE " + getSqlDialect().getTempTableKeyword() + " TABLE " +
                areasTableName + "(Grouping " + (getSqlDialect().isSqlServer() ? "NVARCHAR" : "VARCHAR") + "(300), SampleFileId BIGINT, Area REAL);");

        // Populate the temp tables
        // TODO - filter based on run
        SQLFragment precursorGroupingsSQL = new SQLFragment("INSERT INTO ").append(precursorGroupingsTableName).append("(Grouping, PrecursorId)\n")
                .append("SELECT DISTINCT COALESCE(gm.AttributeGroupId, p.Sequence, m.CustomIonName, m.IonFormula) AS Grouping, pci.PrecursorId \n")
                .append(" FROM ").append(getTableInfoPrecursorChromInfo(), "pci").append(" INNER JOIN \n")
                .append(getTableInfoGeneralPrecursor(), "gp").append(" ON gp.Id = pci.PrecursorId INNER JOIN \n")
                .append(getTableInfoGeneralMolecule(), "gm").append(" ON gp.GeneralMoleculeId = gm.Id LEFT OUTER JOIN \n")
                .append(getTableInfoMolecule(), "m").append(" ON gm.Id = m.Id LEFT OUTER JOIN \n").append(getTableInfoPeptide(), "p")
                .append(" ON p.id = gp.generalmoleculeid");
        precursorGroupingsSQL.append(" INNER JOIN \n")
                .append(getTableInfoSampleFile(), "sf").append(" ON sf.Id = pci.SampleFileId INNER JOIN \n")
                .append(getTableInfoReplicate(), "r").append(" ON sf.ReplicateId = r.Id AND r.RunId = ?");
        precursorGroupingsSQL.add(run.getId());
        executor.execute(precursorGroupingsSQL);

        executor.execute(new SQLFragment("INSERT INTO ").append(moleculeGroupingsTableName).append("(Grouping, GeneralMoleculeId)\n")
                .append("SELECT DISTINCT g.grouping, gp.GeneralMoleculeId FROM \n")
                .append(precursorGroupingsTableName).append(" g INNER JOIN \n")
                .append(getTableInfoGeneralPrecursor(), "gp").append(" ON gp.Id = g.PrecursorId"));

        executor.execute(new SQLFragment("INSERT INTO ").append(areasTableName).append("(Grouping, SampleFileId, Area)\n")
                .append("SELECT g.Grouping, pci.SampleFileId, SUM(pci.TotalArea) AS Area FROM \n")
                .append(getTableInfoPrecursorChromInfo(), "pci").append(" INNER JOIN \n")
                .append(precursorGroupingsTableName).append(" g ON pci.PrecursorId = g.PrecursorId \n")
                .append("GROUP BY g.grouping, pci.SampleFileId"));

        // Add indices to temp tables to make them faster to use
        executor.execute("CREATE INDEX IDX_PrecursorGroupings  ON " + precursorGroupingsTableName + "(PrecursorId, Grouping)");
        executor.execute("CREATE INDEX IDX_MoleculeGroupings ON " + moleculeGroupingsTableName + "(GeneralMoleculeId, Grouping)");
        executor.execute("CREATE INDEX IDX_Areas ON " + areasTableName + "(Grouping, SampleFileId)");

        // Populate the permanent tables
        SQLFragment updatePrecursorSQL = new SQLFragment("UPDATE targetedms.precursorchrominfo SET PrecursorModifiedAreaProportion = \n")
                .append("(SELECT CASE WHEN X.PrecursorAreaInReplicate = 0 THEN NULL ELSE TotalArea / X.PrecursorAreaInReplicate END \n")
                .append("FROM (SELECT Area AS PrecursorAreaInReplicate FROM ").append(areasTableName).append(" a INNER JOIN \n")
                .append(precursorGroupingsTableName).append(" g ON a.grouping = g.grouping \n")
                .append("WHERE g.PrecursorId = targetedms.precursorchrominfo.PrecursorId AND \n")
                .append("a.SampleFileId = targetedms.precursorchrominfo.SampleFileId) X) ");
        updatePrecursorSQL.append(" WHERE PrecursorId IN \n")
            .append("(SELECT PrecursorId FROM ").append(precursorGroupingsTableName).append(")");

        log.info("Setting PrecursorModifiedAreaProportion values on precursorchrominfo");
        executor.execute(updatePrecursorSQL);

        SQLFragment updateMoleculeSQL = new SQLFragment("UPDATE targetedms.generalmoleculechrominfo SET ModifiedAreaProportion = \n")
                .append("(SELECT CASE WHEN X.MoleculeAreaInReplicate = 0 THEN NULL ELSE \n")
                .append("(SELECT SUM(TotalArea) FROM ").append(getTableInfoPrecursorChromInfo(), "pci")
                .append(" WHERE pci.generalmoleculechrominfoid = targetedms.generalmoleculechrominfo.Id) / X.MoleculeAreaInReplicate END \n")
                .append(" FROM (SELECT SUM(a.Area) AS MoleculeAreaInReplicate FROM ").append(areasTableName)
                .append("\n a INNER JOIN ").append(moleculeGroupingsTableName).append(" g ON a.grouping = g.grouping \n")
                .append("WHERE g.GeneralMoleculeId = targetedms.generalmoleculechrominfo.GeneralMoleculeId AND \n")
                .append("a.SampleFileId = targetedms.generalmoleculechrominfo.SampleFileId) X)");
        updateMoleculeSQL.append(" WHERE GeneralMoleculeId IN \n")
            .append("(SELECT GeneralMoleculeId FROM ").append(moleculeGroupingsTableName).append(")");

        log.info("Setting ModifiedAreaProportion values on generalmoleculechrominfo");
        executor.execute(updateMoleculeSQL);

        // Drop the temp tables
        log.info("Cleaning up temp tables");
        executor.execute("DROP TABLE " + precursorGroupingsTableName);
        executor.execute("DROP TABLE " + moleculeGroupingsTableName);
        executor.execute("DROP TABLE " + areasTableName);
    }

    /**
     * Returns the Transition Full Scan setttings for the given run.  This may be null if the settings were not included
     * in the Skyline document.
     * @param runId
     * @return
     */
    @Nullable
    public static TransitionSettings.FullScanSettings getTransitionFullScanSettings(long runId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransitionFullScanSettings(),
                new SimpleFilter(FieldKey.fromParts("runId"), runId), null)
                .getObject(TransitionSettings.FullScanSettings.class);
    }

    public static TransitionSettings.Predictor getReplicatePredictor(long predictorId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransitionFullScanSettings(),
                new SimpleFilter(FieldKey.fromParts("Id"), predictorId), null)
                .getObject(TransitionSettings.Predictor.class);
    }

    public static List<TransitionSettings.Predictor> getPredictors(long runId)
    {
        List<TransitionSettings.Predictor> predictors = new ArrayList<>();
        List<Replicate> replicates = ReplicateManager.getReplicatesForRun(runId);
        replicates.forEach(replicate -> {
            if (null != replicate.getCePredictorId())
            {
                predictors.add(getReplicatePredictor(replicate.getCePredictorId()));
            }

            if (null != replicate.getDpPredictorId())
            {
                predictors.add(getReplicatePredictor(replicate.getDpPredictorId()));
            }
        });
        return predictors;
    }

    private static List<Long> getPredictorsToDelete()
    {
        SQLFragment sql = new SQLFragment("Select Id FROM " + getTableInfoPredictor() + " WHERE " +
                "Id IN (SELECT tps.CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?) " +
                " OR Id IN (SELECT tps.DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)").add(true).add(true);

        return new SqlSelector(getSchema(), sql).getArrayList(Long.class);
    }

    private static List<SampleFileChromInfo> getSampleFileChromInfosByName(String traceName, long runId)
    {
        var sql = new SQLFragment(" SELECT sfi.Id, sfi.SampleFileId, sfi.StartTime, sfi.EndTime, sfi.NumPoints," +
                " sfi.UncompressedSize, sfi.ChromatogramFormat, sfi.ChromatogramOffset, sfi.ChromatogramLength, sfi.TextId" +
                " FROM " + getTableInfoSampleFileChromInfo() + " sfi" +
                " INNER JOIN " + getTableInfoSampleFile() + " sf ON sf.Id = sfi.SampleFileId" +
                " INNER JOIN " + getTableInfoReplicate() + " r ON r.Id = sf.ReplicateId" +
                " INNER JOIN " + getTableInfoRuns() + " rn ON rn.Id = r.RunId" +
                " WHERE TextId = ?" +
                " AND rn.ID = ? ").add(traceName).add(runId);
        return new SqlSelector(getSchema(), sql).getArrayList(SampleFileChromInfo.class);
    }

    public static List<QCTraceMetricValues> calculateTraceMetricValues(List<QCMetricConfiguration> qcMetricConfigurations, TargetedMSRun run)
    {
        List<QCTraceMetricValues> qcTraceMetricValuesList = new ArrayList<>();
        if (!qcMetricConfigurations.isEmpty())
        {
            for (QCMetricConfiguration qcMetricConfiguration : qcMetricConfigurations)
            {
                List<SampleFileChromInfo> sampleFileChromInfos = getSampleFileChromInfosByName(qcMetricConfiguration.getTraceName(), run.getRunId());
                Map<SampleFileChromInfo, Float> valuesToStore = new HashMap<>();

                for (SampleFileChromInfo sampleFileChromInfo : sampleFileChromInfos)
                {
                    Chromatogram chromatogram = sampleFileChromInfo.createChromatogram(run);
                    if (null != chromatogram)
                    {
                        float[] times = chromatogram.getTimes();
                        float[] values = chromatogram.getIntensities(0);

                        if (times.length != values.length)
                        {
                            throw new IllegalStateException("Incorrect values in skyd file for time and intensities for trace - " + sampleFileChromInfo.getTextId());
                        }

                        for (int i = 0; i < times.length; i++)
                        {
                            Double timeValue = qcMetricConfiguration.getTimeValue();
                            Double traceValue = qcMetricConfiguration.getTraceValue();

                            if (timeValue != null && times[i] >= timeValue)
                            {
                                valuesToStore.put(sampleFileChromInfo, values[i]);
                                break;
                            }
                            else if (traceValue != null && values[i] >= traceValue)
                            {
                                valuesToStore.put(sampleFileChromInfo, times[i]);
                                break;
                            }
                        }
                    }
                }

                if (!valuesToStore.isEmpty())
                {
                    valuesToStore.forEach((key,val) -> {
                        QCTraceMetricValues qcTraceMetricValues = new QCTraceMetricValues();
                        qcTraceMetricValues.setMetric(qcMetricConfiguration.getId());
                        qcTraceMetricValues.setSampleFileId(key.getSampleFileId());
                        qcTraceMetricValues.setValue(val);
                        qcTraceMetricValuesList.add(qcTraceMetricValues);
                    });
                }
            }
        }
        return qcTraceMetricValuesList;
    }

    public static List<QCMetricConfiguration> getTraceMetricConfigurations(Container container, User user)
    {
        return getEnabledQCMetricConfigurations(container, user)
                .stream()
                .filter(qcMetricConfiguration -> qcMetricConfiguration.getTraceName() != null)
                .collect(Collectors.toList());
    }

    public static List<IKeyword> getKeywords(long sequenceId)
    {
        String qs = "SELECT kw.keywordid, kw.keyword, kw.category, kc.label " +
                "FROM prot.sequences p, prot.annotations a, prot.identifiers pi, targetedms.keywords kw, targetedms.keywordcategories kc " +
                "WHERE p.seqid = ? AND a.seqid = p.seqid AND pi.identid = a.annotident AND kw.keywordid = pi.identifier AND kc.categoryid = kw.category";
        SQLFragment keywordQuery = new SQLFragment();
        keywordQuery.append(qs);
        keywordQuery.add(sequenceId);

        SqlSelector sqlSelector = new SqlSelector(getSchema(), keywordQuery);
        List<IKeyword> keywords = new ArrayList<>();
        sqlSelector.forEach(prot -> keywords.add(new IKeyword(prot.getString("keywordid"),
                prot.getString("category"),
                prot.getString("keyword"),
                prot.getString("label"))));
        return keywords;
    }

    public static Map<String,Object> getQCFolderDateRange(Container container)
    {
        var sql = new SQLFragment("SELECT MIN(sf.AcquiredTime) AS startDate, MAX(sf.AcquiredTime) AS endDate ");
        sql.append(" FROM ").append(getTableInfoSampleFile(), "sf");
        sql.append(" INNER JOIN ").append(getTableInfoReplicate(), "rep").append(" ON sf.ReplicateId = rep.Id ");
        sql.append(" INNER JOIN ").append(getTableInfoRuns(), "r").append(" ON rep.RunId = r.Id ");
        sql.append(" WHERE r.Container = ").append(container);

        return new SqlSelector(getSchema(), sql).getMap();
    }
}
