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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.analytics.AnalyticsService;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.*;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.module.Module;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.stats.AnalyticsProvider;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringExpression;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.ChromatogramBinaryFormat;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.SkylineBinaryParser;
import org.labkey.targetedms.query.*;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public class TargetedMSSchema extends UserSchema
{
    public static final String SCHEMA_NAME = "targetedms";
    public static final String SCHEMA_DESCR = "Contains data about Targeted MS experiments.";

    // Tables
    public static final String TABLE_TARGETED_MS_RUNS = "TargetedMSRuns";
    public static final String TABLE_RUNS = "Runs";
    public static final String TABLE_PREDICTOR = "Predictor";
    public static final String TABLE_PREDICTOR_SETTINGS = "PredictorSettings";
    public static final String TABLE_TRANSITION_INSTRUMENT_SETTINGS = "TransitionInstrumentSettings";
    public static final String TABLE_REPLICATE = "Replicate";
    public static final String TABLE_REPLICATE_ANNOTATION = "ReplicateAnnotation";
    public static final String TABLE_RETENTION_TIME_PREDICTION_SETTINGS = "RetentionTimePredictionSettings";
    public static final String TABLE_DRIFT_TIME_PREDICTION_SETTINGS = "DriftTimePredictionSettings";
    public static final String TABLE_MEASURED_DRIFT_TIME = "MeasuredDriftTime";
    public static final String TABLE_TRANSITION_FULL_SCAN_SETTINGS = "TransitionFullScanSettings";
    public static final String TABLE_TRANSITION_PREDICITION_SETTINGS = "TransitionPredictionSettings";
    public static final String TABLE_SAMPLE_FILE = "SampleFile";
    public static final String TABLE_PEPTIDE_GROUP = "PeptideGroup";
    public static final String TABLE_MOLECULE_GROUP = "MoleculeGroup";
    public static final String TABLE_PEPTIDE_GROUP_ANNOTATION = "PeptideGroupAnnotation";
    public static final String TABLE_INSTRUMENT = "Instrument";
    public static final String TABLE_ISOTOPE_ENRICHMENT = "IsotopeEnrichment";
    public static final String TABLE_ISOLATION_SCHEME = "IsolationScheme";
    public static final String TABLE_ISOLATION_WINDOW = "IsolationWindow";
    public static final String TABLE_PROTEIN = "Protein";
    public static final String TABLE_PEPTIDE = "Peptide";
    public static final String TABLE_MOLECULE = "Molecule";
    public static final String TABLE_GENERAL_MOLECULE_ANNOTATION = "GeneralMoleculeAnnotation";
    public static final String TABLE_PEPTIDE_ANNOTATION = "PeptideAnnotation";
    public static final String TABLE_PRECURSOR = "Precursor";
    public static final String TABLE_EXPERIMENT_PRECURSOR = "ExperimentPrecursor";
    public static final String TABLE_LIBRARY_PRECURSOR = "LibraryPrecursor";
    public static final String TABLE_LIBRARY_DOC_PRECURSOR = "LibraryDocPrecursor";
    public static final String TABLE_PRECURSOR_ANNOTATION = "PrecursorAnnotation";
    public static final String TABLE_TRANSITION = "Transition";
    public static final String TABLE_MOLECULE_TRANSITION = "MoleculeTransition";
    public static final String TABLE_TRANSITION_LOSS = "TransitionLoss";
    public static final String TABLE_TRANSITION_ANNOTATION = "TransitionAnnotation";
    public static final String TABLE_TRANSITION_OPTIMIZATION = "TransitionOptimization";
    public static final String TABLE_TRANSITION_CHROM_INFO = "TransitionChromInfo";
    public static final String TABLE_TRANSITION_CHROM_INFO_ANNOTATION = "TransitionChromInfoAnnotation";
    public static final String TABLE_GENERAL_MOLECULE_CHROM_INFO = "GeneralMoleculeChromInfo";
    public static final String TABLE_PEPTIDE_CHROM_INFO = "PeptideChromInfo";
    public static final String TABLE_PRECURSOR_CHROM_INFO = "PrecursorChromInfo";
    public static final String TABLE_PRECURSOR_CHROM_INFO_ANNOTATION = "PrecursorChromInfoAnnotation";
    public static final String TABLE_PEPTIDE_AREA_RATIO = "PeptideAreaRatio";
    public static final String TABLE_PRECURSOR_AREA_RATIO = "PrecursorAreaRatio";
    public static final String TABLE_TRANSITION_AREA_RATIO = "TransitionAreaRatio";
    public static final String TABLE_MODIFICATION_SETTINGS = "ModificationSettings";
    public static final String TABLE_ISOTOPE_LABEL = "IsotopeLabel";
    public static final String TABLE_STRUCTURAL_MODIFICATION = "StructuralModification";
    public static final String TABLE_STRUCTURAL_MOD_LOSS = "StructuralModLoss";
    public static final String TABLE_ISOTOPE_MODIFICATION = "IsotopeModification";
    public static final String TABLE_RUN_STRUCTURAL_MODIFICATION = "RunStructuralModification";
    public static final String TABLE_RUN_ISOTOPE_MODIFICATION = "RunIsotopeModification";
    public static final String TABLE_PEPTIDE_STRUCTURAL_MODIFICATION = "PeptideStructuralModification";
    public static final String TABLE_PEPTIDE_ISOTOPE_MODIFICATION = "PeptideIsotopeModification";
    public static final String TABLE_SPECTRUM_LIBRARY = "SpectrumLibrary";
    public static final String TABLE_RUN_ENZYME = "RunEnzyme";
    public static final String TABLE_ENZYME = "Enzyme";
    public static final String TABLE_LIBRARY_SETTINGS = "LibrarySettings";
    public static final String TABLE_LIBRARY_SOURCE = "LibrarySource";
    public static final String TABLE_PRECURSOR_LIB_INFO = "PrecursorLibInfo";
    public static final String TABLE_ANNOTATION_SETTINGS = "AnnotationSettings";
    public static final String TABLE_GROUP_COMPARISON_SETTINGS = "GroupComparisonSettings";
    public static final String TABLE_FOLD_CHANGE = "FoldChange";
    public static final String TABLE_PEPTIDE_FOLD_CHANGE = "PeptideFoldChange";
    public static final String TABLE_MOLECULE_FOLD_CHANGE = "MoleculeFoldChange";
    public static final String TABLE_QUANTIIFICATION_SETTINGS = "QuantificationSettings";
    public static final String TABLE_CALIBRATION_CURVE = "CalibrationCurve";
    public static final String TABLE_PEPTIDE_CALIBRATION_CURVE = "PeptideCalibrationCurve";
    public static final String TABLE_MOLECULE_CALIBRATION_CURVE = "MoleculeCalibrationCurve";
    public static final String TABLE_MOLECULE_INFO = "MoleculeInfo";

    public static final String TABLE_REPRESENTATIVE_DATA_STATE_RUN = "RepresentativeDataState_Run";
    public static final String TABLE_REPRESENTATIVE_DATA_STATE = "RepresentativeDataState";
    public static final String TABLE_IRT_PEPTIDE = "iRTPeptide";
    public static final String TABLE_IRT_SCALE = "iRTScale";

    public static final String TABLE_AUTOQC_PING = "AutoQCPing";

    public static final String TABLE_EXPERIMENT_ANNOTATIONS = "ExperimentAnnotations";

    public static final String TABLE_QC_ANNOTATION_TYPE = "QCAnnotationType";
    public static final String TABLE_QC_ANNOTATION = "QCAnnotation";
    public static final String TABLE_QC_METRIC_CONFIGURATION = "QCMetricConfiguration";
    public static final String TABLE_QC_METRIC_EXCLUSION = "QCMetricExclusion";

    public static final String TABLE_GUIDE_SET = "GuideSet";

    public static final String TABLE_JOURNAL = "Journal";
    public static final String TABLE_JOURNAL_EXPERIMENT = "JournalExperiment";

    private static final String PROTOCOL_PATTERN_PREFIX = "urn:lsid:%:Protocol.%:";

    public static final String TABLE_GENERAL_MOLECULE = "GeneralMolecule";
    public static final String TABLE_GENERAL_PRECURSOR = "GeneralPrecursor";
    public static final String TABLE_GENERAL_TRANSITION = "GeneralTransition";
    public static final String TABLE_MOLECULE_PRECURSOR = "MoleculePrecursor";

    private ExpSchema _expSchema;

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new TargetedMSSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public TargetedMSSchema(User user, Container container)
    {
        super(SCHEMA_NAME, SCHEMA_DESCR, user, container, TargetedMSManager.getSchema());
        _expSchema = new ExpSchema(user, container);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }


    private static SQLFragment getJoinToRunsTable(String tableAlias)
    {
        tableAlias = tableAlias == null ? "" : tableAlias + ".";
        return makeInnerJoin(TargetedMSManager.getTableInfoRuns(),
                TargetedMSTable.CONTAINER_COL_TABLE_ALIAS, tableAlias + "RunId");
    }

    private static SQLFragment makeInnerJoin(TableInfo table, String alias, String colRight)
    {
        SQLFragment sql = new SQLFragment("INNER JOIN ");
        sql.append(table, alias);
        sql.append(" ON ( ");
        sql.append(alias).append(".id");
        sql.append(" = ");
        sql.append(colRight);
        sql.append(" ) ");
        return sql;
    }

    private static SQLFragment makeLeftJoin(TableInfo table, String alias, String colRight)
    {
        SQLFragment sql = new SQLFragment("LEFT JOIN ");
        sql.append(table, alias);
        sql.append(" ON ( ");
        sql.append(alias).append(".id");
        sql.append(" = ");
        sql.append(colRight);
        sql.append(" ) ");
        return sql;
    }

    public enum ContainerJoinType
    {
        GeneralMoleculeFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        GeneralMoleculeChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci", "GeneralMoleculeChromInfoId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gmid", "gmci.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gmid.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        GeneralPrecursorFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre", "GeneralPrecursorId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "pre.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;

            }
        },
        GeneralTransitionFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralTransition(), "gtr", "GeneralTransitionId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "gpi", "gtr.GeneralPrecursorId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gmid", "gpi.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gmid.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        PrecursorChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci", "PrecursorChromInfoId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoSampleFile(), "sfile", "pci.SampleFileId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoReplicate(), "rep", "sfile.ReplicateId"));
                sql.append(getJoinToRunsTable("rep"));
                return sql;
            }
        },
        PrecursorFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp", "PrecursorId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "gp.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        PrecursorTableFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp", "Id"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "gp.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        RunFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(getJoinToRunsTable(null));
                return sql;
            }
        },
        PeptideGroupFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        PeptideGroupForMoleculeFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        PeptideChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci", "PeptideChromInfoId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gmid", "gmci.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gmid.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        ReplicateFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoReplicate(), "rep", "ReplicateId"));
                sql.append(getJoinToRunsTable("rep"));
                return sql;
            }
        },

        PrecursorForMoleculeFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralTransition(), "gt", "Id"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp", "gt.GeneralPrecursorId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "gp.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;

            }
        },
        TransitionFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralTransition(), "pre", "TransitionId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralPrecursor(), "gpi", "pre.GeneralPrecursorId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gmid", "gpi.GeneralMoleculeId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gmid.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        },
        TransitionChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci", "TransitionChromInfoId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoSampleFile(), "sfile", "tci.SampleFileId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoReplicate(), "rep", "sfile.ReplicateId"));
                sql.append(getJoinToRunsTable("rep"));
                return sql;
            }
        },
        SampleFileFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoSampleFile(), "sfile", "SampleFileId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoReplicate(), "rep", "sfile.ReplicateId"));
                sql.append(getJoinToRunsTable("rep"));
                return sql;
            }
        },
        iRTScaleFK
        {
            @Override
            public SQLFragment getSQL()
            {
                return makeInnerJoin(TargetedMSManager.getTableInfoiRTScale(),
                        TargetedMSTable.CONTAINER_COL_TABLE_ALIAS, "iRTScaleId");
            }
        },
        QCAnnotationTypeFK
        {
            @Override
            public SQLFragment getSQL()
            {
                return makeInnerJoin(TargetedMSManager.getTableInfoQCAnnotationType(),
                        TargetedMSTable.CONTAINER_COL_TABLE_ALIAS, "QCAnnotationTypeId");
            }
        },
        IsolationSchemeFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoIsolationScheme(), "ischeme", "IsolationSchemeId"));
                sql.append(getJoinToRunsTable("ischeme"));
                return sql;
            }
        },

        DriftTimePredictionSettingsFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoDriftTimePredictionSettings(), "driftTimeSettings", "DriftTimePredictionSettingsId"));
                sql.append(getJoinToRunsTable("driftTimeSettings"));
                return sql;
            }
        },
        ExperimentAnnotationsFK
        {
            @Override
            public SQLFragment getSQL()
            {
                return makeInnerJoin(TargetedMSManager.getTableInfoExperimentAnnotations(),
                        TargetedMSTable.CONTAINER_COL_TABLE_ALIAS, "ExperimentAnnotationsId");
            }
        },
        ModPeptideFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment();
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoGeneralMolecule(), "gm", "PeptideId"));
                sql.append(makeInnerJoin(TargetedMSManager.getTableInfoPeptideGroup(), "pg", "gm.PeptideGroupId"));
                sql.append(getJoinToRunsTable("pg"));
                return sql;
            }
        };

        public abstract SQLFragment getSQL();
    }

    public ExpRunTable getTargetedMSRunsTable()
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.getRunsTable();
        result.setDescription(TargetedMSManager.getTableInfoRuns().getDescription());

        // Filter to just the runs with the Targeted MS protocol
        result.setProtocolPatterns(PROTOCOL_PATTERN_PREFIX + TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX + "%",
                                   PROTOCOL_PATTERN_PREFIX + TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX + "%");

        // Add a lookup column to the Runs table in targetedms schema
        SQLFragment sql = new SQLFragment("(SELECT MIN(tmsRuns.Id)\n" +
                "\nFROM " + TargetedMSManager.getTableInfoRuns() + " tmsRuns " +
                "\nWHERE tmsRuns.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID AND tmsRuns.Deleted = ?)");
        sql.add(Boolean.FALSE);
        ColumnInfo skyDocDetailColumn = new ExprColumn(result, "File", sql, JdbcType.INTEGER);

        ActionURL url = TargetedMSController.getShowRunURL(getContainer());
        final ActionURL downloadUrl = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getContainer());
        skyDocDetailColumn.setFk(new LookupForeignKey(url, "id", "Id", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable<>(TargetedMSManager.getTableInfoRuns(), TargetedMSSchema.this);
                result.addWrapColumn(result.getRealTable().getColumn("Id"));
                result.addWrapColumn(result.getRealTable().getColumn("Description"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("ExperimentRunLSID"));
                result.addWrapColumn(result.getRealTable().getColumn("Status"));
                ColumnInfo stateColumn = result.addWrapColumn(result.getRealTable().getColumn("RepresentativeDataState"));
                stateColumn.setFk(new QueryForeignKey(TargetedMSSchema.this, null, TABLE_REPRESENTATIVE_DATA_STATE_RUN, "RowId", null));

                ColumnInfo downloadLinkColumn = result.addWrapColumn("Download", result.getRealTable().getColumn("Id"));
                downloadLinkColumn.setKeyField(false);
                downloadLinkColumn.setTextAlign("left");
                downloadLinkColumn.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        return new DataColumn(colInfo)
                        {
                            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                            {
                                FieldKey parentFK = this.getColumnInfo().getFieldKey().getParent();
                                String runLSID = ctx.get(new FieldKey(parentFK, "ExperimentRunLSID"), String.class);
                                Integer runId = ctx.get(this.getColumnInfo().getFieldKey(), Integer.class);
                                if (runId != null && runLSID != null)
                                {
                                    downloadUrl.replaceParameter("runId", runId.toString());

                                    Path skyDocFile = SkylineFileUtils.getSkylineFile(runLSID);
                                    if (skyDocFile != null && !Files.isDirectory(skyDocFile))
                                    {

                                        String onClickScript = null;
                                        if(!StringUtils.isBlank(AnalyticsService.getTrackingScript()))
                                        {
                                            // http://www.blastam.com/blog/how-to-track-downloads-in-google-analytics
                                            // Tell the browser to wait 400ms before going to the download.  This is to ensure
                                            // that the GA tracking request goes through. Some browsers will interrupt the tracking
                                            // request if the download opens on the same page.
                                            String timeout = "setTimeout(function(){location.href=that.href;},400);return false;";

                                            onClickScript = "if(_gaq) {that=this; _gaq.push(['_trackEvent', 'SkyDocDownload', '" + ctx.getContainerPath() + "', '" + FileUtil.getFileName(skyDocFile) + "']); " + timeout + "}";
                                        }
                                        out.write(PageFlowUtil.iconLink("fa fa-download", "Download File", PageFlowUtil.filter(downloadUrl),
                                                                        onClickScript, null, null));
                                        String size = h(" (" + FileUtils.byteCountToDisplaySize(Files.size(skyDocFile)) + ")");
                                        out.write(size);
                                    }
                                    else
                                    {
                                        out.write("<em>Not available</em>");
                                    }
                                }
                            }

                            @Override
                            public void addQueryFieldKeys(Set<FieldKey> keys)
                            {
                                FieldKey parentFK = this.getColumnInfo().getFieldKey().getParent();
                                keys.add(new FieldKey(parentFK, "ExperimentRunLSID"));
                            }
                        };
                    }
                });

                result.addWrapColumn("Proteins", result.getRealTable().getColumn("PeptideGroupCount"));
                result.addWrapColumn("Peptides", result.getRealTable().getColumn("PeptideCount"));
                result.addWrapColumn("SmallMolecules", result.getRealTable().getColumn("SmallMoleculeCount"));
                result.addWrapColumn("Precursors", result.getRealTable().getColumn("PrecursorCount"));
                result.addWrapColumn("Transitions", result.getRealTable().getColumn("TransitionCount"));
                result.addWrapColumn("Replicates", result.getRealTable().getColumn("ReplicateCount"));

                return result;
            }
        });
        skyDocDetailColumn.setHidden(false);
        result.addColumn(skyDocDetailColumn);

        List<FieldKey> defaultVisibleColumns = result.getDefaultVisibleColumns();

        // Create the list lazily to avoid extra DB queries when we don't need any
        result.setDefaultVisibleColumns(new Iterable<FieldKey>()
        {
            private List<FieldKey> _fieldKeys;

            private List<FieldKey> init()
            {
                // Cache so we don't have to requery for a single request
                if (_fieldKeys == null)
                {
                    //adjust the default visible columns
                    _fieldKeys = new ArrayList<>(defaultVisibleColumns);
                    _fieldKeys.remove(FieldKey.fromParts("File"));
                    _fieldKeys.remove(FieldKey.fromParts("Protocol"));
                    _fieldKeys.remove(FieldKey.fromParts("CreatedBy"));
                    _fieldKeys.remove(FieldKey.fromParts("RunGroups"));
                    _fieldKeys.remove(FieldKey.fromParts("Name"));

                    _fieldKeys.add(2, FieldKey.fromParts("File"));
                    _fieldKeys.add(3, FieldKey.fromParts("File", "Download"));
                    _fieldKeys.add(FieldKey.fromParts("File", "Proteins"));

                    // Omit peptides or small molecules if we don't have any in this container
                    boolean hasSmallMolecules = new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", getContainer(), false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE SmallMoleculeCount > 0 AND Container = ? AND Deleted = ?")).exists();
                    boolean hasPeptides = new SqlSelector(TargetedMSManager.getSchema(), new SQLFragment("SELECT Id FROM ", getContainer(), false).append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE PeptideCount > 0 AND Container = ? AND Deleted = ?")).exists();

                    if (hasPeptides || !hasSmallMolecules)
                    {
                        _fieldKeys.add(FieldKey.fromParts("File", "Peptides"));
                    }
                    if (hasSmallMolecules)
                    {
                        _fieldKeys.add(FieldKey.fromParts("File", "SmallMolecules"));
                    }
                    _fieldKeys.add(FieldKey.fromParts("File", "Precursors"));
                    _fieldKeys.add(FieldKey.fromParts("File", "Transitions"));
                    _fieldKeys.add(FieldKey.fromParts("File", "Replicates"));
                }
                return _fieldKeys;
            }

            @Override
            public Iterator<FieldKey> iterator()
            {
                return init().iterator();
            }

            @Override
            public void forEach(Consumer<? super FieldKey> action)
            {
                init().forEach(action);
            }

            @Override
            public Spliterator<FieldKey> spliterator()
            {
                return init().spliterator();
            }
        });

        return result;
    }

    @Override
    public TableInfo createTable(String name)
    {
        if (TABLE_TARGETED_MS_RUNS.equalsIgnoreCase(name))
        {
            return getTargetedMSRunsTable();
        }
        if (TABLE_IRT_PEPTIDE.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.iRTScaleFK.getSQL());
        }
        if (TABLE_QC_ANNOTATION_TYPE.equalsIgnoreCase(name))
        {
            return new QCAnnotationTypeTable(this);
        }
        if (TABLE_QC_ANNOTATION.equalsIgnoreCase(name))
        {
            return new QCAnnotationTable(this);
        }
        if (TABLE_QC_METRIC_CONFIGURATION.equalsIgnoreCase(name))
        {
            return new QCMetricConfigurationTable(this);
        }
        if (TABLE_GUIDE_SET.equalsIgnoreCase(name))
        {
            return new GuideSetTable(this);
        }
        if (TABLE_AUTOQC_PING.equalsIgnoreCase(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<>(getSchema().getTable(TABLE_AUTOQC_PING), this);
            result.wrapAllColumns(true);
            result.getColumn("CreatedBy").setFk(new UserIdQueryForeignKey(getUser(), getContainer()));
            result.getColumn("ModifiedBy").setFk(new UserIdQueryForeignKey(getUser(), getContainer()));
            result.getColumn("Container").setFk(new ContainerForeignKey(this));
            return result;
        }
        if(TABLE_EXPERIMENT_ANNOTATIONS.equalsIgnoreCase(name))
        {
            return new ExperimentAnnotationsTableInfo(this, getUser());
        }

        if (TABLE_REPRESENTATIVE_DATA_STATE_RUN.equalsIgnoreCase(name))
        {
            return new EnumTableInfo<>(
                    TargetedMSRun.RepresentativeDataState.class,
                    this,
                    new EnumTableInfo.EnumValueGetter<TargetedMSRun.RepresentativeDataState>() {

                        @Override
                        public String getValue(TargetedMSRun.RepresentativeDataState e)
                        {
                            return e.getLabel();
                        }
                    },
                    true,
                    "Possible states a run might be in for resolving representative data after upload"
                    );
        }
        if (TABLE_REPRESENTATIVE_DATA_STATE.equalsIgnoreCase(name))
        {
            EnumTableInfo tableInfo = new EnumTableInfo<>(
                    RepresentativeDataState.class,
                    this,
                    new EnumTableInfo.EnumValueGetter<RepresentativeDataState>() {

                        @Override
                        public String getValue(RepresentativeDataState e)
                        {
                            return e.getLabel();
                        }
                    },
                    true,
                    "Possible representative states for a peptide group or precursor");

            ColumnInfo viewColumn = tableInfo.getColumn("Value");
            viewColumn.setLabel("Library State");
            viewColumn.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new RepresentativeStateDisplayColumn(colInfo);
                }
            });

            return tableInfo;
        }

        // Tables that have a FK directly to targetedms.Runs
        if (TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) || TABLE_MOLECULE_GROUP.equalsIgnoreCase(name))
        {
            boolean proteomics = TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name);
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(TABLE_PEPTIDE_GROUP),
                                                                  this,
                                                                  ContainerJoinType.RunFK.getSQL(),
                                                                  TargetedMSManager.getTableInfoPeptideGroupAnnotation(),
                                                                  "PeptideGroupId",
                                                                  "Protein Annotations",
                                                                  "protein") // This may change as more small molecule work is done in Skyline.
            {
                @Override
                public FieldKey getContainerFieldKey()
                {
                    return FieldKey.fromParts("RunId", "Folder");
                }
            };
            DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.ShowProteinAction.class, getContainer()), Collections.singletonMap("id", "Id"));
            result.setDetailsURL(detailsURLs);
            result.getColumn("SequenceId").setFk(new LookupForeignKey("SeqId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createSequencesTableInfo(getUser(), getContainer());
                }
            });
            ColumnInfo labelColumn = result.getColumn("Label");
            labelColumn.setURL(detailsURLs);
            if (proteomics)
            {
                labelColumn.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        FieldKey seqIdFK = new FieldKey(colInfo.getFieldKey().getParent(), "Id");
                        Map<String, FieldKey> params = new HashMap<>();
                        params.put("id", seqIdFK);
                        JSONObject props = new JSONObject();
                        props.put("width", 450);
                        props.put("title", "Protein Details");
                        FieldKey containerFieldKey = new FieldKey(new FieldKey(colInfo.getFieldKey().getParent(), "RunId"), "Folder");
                        return new AJAXDetailsDisplayColumn(colInfo, new ActionURL(TargetedMSController.ShowProteinAJAXAction.class, getContainer()), params, props, containerFieldKey)
                        {
                            private boolean _renderedCSS = false;

                            @Override
                            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                            {
                                if (!_renderedCSS)
                                {
                                    out.write("<script type=\"text/javascript\">\n" +
                                            "LABKEY.requiresCss(\"ProteinCoverageMap.css\");\n" +
                                            "LABKEY.requiresScript(\"util.js\");\n" +
                                            "</script>");
                                    _renderedCSS = true;
                                }
                                super.renderGridCellContents(ctx, out);
                            }
                        };
                    }
                });
            }
            else
            {
                labelColumn.setLabel("Molecule / Label");
                result.getColumn("SequenceId").setHidden(true);
            }
            result.getColumn("RunId").setFk(new QueryForeignKey(this, null, TABLE_TARGETED_MS_RUNS, "File", null));
            result.getColumn("RepresentativeDataState").setFk(new QueryForeignKey(this, null, TargetedMSSchema.TABLE_REPRESENTATIVE_DATA_STATE, "RowId", null));
            result.getColumn("RepresentativeDataState").setHidden(true);

            // Create a WrappedColumn for Note & Annotations
            WrappedColumn noteAnnotation = new WrappedColumn(result.getColumn("Annotations"), "NoteAnnotations");
            noteAnnotation.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new AnnotationUIDisplayColumn(colInfo);
                }
            });
            if (proteomics)
            {
                noteAnnotation.setLabel("Protein Note/Annotations");
            }
            else
            {
                noteAnnotation.setLabel("Molecule Note/Annotations");
            }
            result.addColumn(noteAnnotation);

            return result;
        }

        if (TABLE_REPLICATE.equalsIgnoreCase(name))
        {
            return new AnnotatedTargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.RunFK.getSQL(), TargetedMSManager.getTableInfoReplicateAnnotation(), "ReplicateId", "Replicate Annotations", "replicate");
        }

        // Tables that have a FK to targetedms.Runs
        if (TABLE_TRANSITION_INSTRUMENT_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_INSTRUMENT.equalsIgnoreCase(name) ||
            TABLE_ISOTOPE_ENRICHMENT.equalsIgnoreCase(name) ||
            TABLE_ISOLATION_SCHEME.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_FULL_SCAN_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_PREDICITION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_RETENTION_TIME_PREDICTION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_RUN_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_RUN_ISOTOPE_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_ISOTOPE_LABEL.equalsIgnoreCase(name) ||
            TABLE_MODIFICATION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_LIBRARY_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_RUN_ENZYME.equalsIgnoreCase(name) ||
            TABLE_SPECTRUM_LIBRARY.equalsIgnoreCase(name) ||
            TABLE_ANNOTATION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_QUANTIIFICATION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_DRIFT_TIME_PREDICTION_SETTINGS.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.RunFK.getSQL());
        }

        if (TABLE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable(this);
        }
        if (TABLE_PEPTIDE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable.PeptideCalibrationCurveTable(this);
        }
        if (TABLE_MOLECULE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable.MoleculeCalibrationCurveTable(this);
        }

        if (TABLE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable(this);
        }

        if (TABLE_PEPTIDE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable.PeptideFoldChangeTable(this);
        }

        if (TABLE_MOLECULE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable.MoleculeFoldChangeTable(this);
        }

        // Tables that have a FK to targetedms.peptidegroup
        if (TABLE_PEPTIDE.equalsIgnoreCase(name))
        {
            return new PeptideTableInfo(this);
        }
        if (TABLE_MOLECULE.equalsIgnoreCase(name))
        {
            return new MoleculeTableInfo(this);
        }

        if (TABLE_PROTEIN.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideGroupFK.getSQL());
        }

        // Tables that have a FK to targetedms.replicate
        if (TABLE_SAMPLE_FILE.equalsIgnoreCase(name))
        {
            return new SampleFileTable(getSchema().getTable(name), this);
        }
        if (TABLE_REPLICATE_ANNOTATION.equalsIgnoreCase(name))
        {
            return new ReplicateAnnotationTable(this);
        }
        if (TABLE_QC_METRIC_EXCLUSION.equalsIgnoreCase(name))
        {
            return new QCMetricExclusionTable(this);
        }

        if (TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name) || TABLE_GENERAL_MOLECULE_CHROM_INFO.equalsIgnoreCase(name))
        {
            return new GeneralMoleculeChromInfoTableInfo(getSchema().getTable(TABLE_GENERAL_MOLECULE_CHROM_INFO), this, ContainerJoinType.GeneralMoleculeFK.getSQL(), name);
        }

        // Tables that have a FK to targetedms.peptidechrominfo
        if (TABLE_PEPTIDE_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideChromInfoFK.getSQL());
        }

        // Tables that have a FK to targetedms.peptide
        if (TABLE_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo(this);
        }

        if (TABLE_MOLECULE_PRECURSOR.equalsIgnoreCase(name))
        {
            return new MoleculePrecursorTableInfo(this);
        }

        if(TABLE_EXPERIMENT_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.ExperimentPrecursorTableInfo(this);
        }

        if(TABLE_LIBRARY_PRECURSOR.equalsIgnoreCase(name) || TABLE_LIBRARY_DOC_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.LibraryPrecursorTableInfo(this);
        }
        if (TABLE_GENERAL_MOLECULE_ANNOTATION.equalsIgnoreCase(name) || TABLE_PEPTIDE_ANNOTATION.equalsIgnoreCase(name))
        {
            GeneralMoleculeAnnotationTableInfo result = new GeneralMoleculeAnnotationTableInfo(getSchema().getTable(TABLE_GENERAL_MOLECULE_ANNOTATION), this, ContainerJoinType.GeneralMoleculeFK.getSQL());
            result.setName(name);
            return result;
        }
        if(TABLE_PEPTIDE_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(new PeptideStructuralModificationTableInfo(this), this, ContainerJoinType.ModPeptideFK.getSQL());
        }
        if(TABLE_PEPTIDE_ISOTOPE_MODIFICATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(new PeptideIsotopeModificationTableInfo(this), this, ContainerJoinType.ModPeptideFK.getSQL());
        }
        // Tables that have a FK to targetedms.precursor
        if (TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name))
        {
            return new PrecursorChromInfoTable(getSchema().getTable(name), this);
        }
        if (TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            return new DocTransitionsTableInfo(this);
        }
        if(TABLE_MOLECULE_TRANSITION.equalsIgnoreCase(name))
        {
            return new MoleculeTransitionsTableInfo(this);
        }

        // Tables that have a FK to targetedms.precursorchrominfo
        if (TABLE_PRECURSOR_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PrecursorChromInfoFK.getSQL());
        }

        // Tables that have a FK to targetedms.transition
        if (TABLE_TRANSITION_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.SampleFileFK.getSQL(),
                    TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), "TransitionChromInfoId", "Transition Result Annotations", "transition_result");
            TargetedMSSchema targetedMSSchema = this;

            ColumnInfo transitionId = result.getColumn("TransitionId");
            transitionId.setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new DocTransitionsTableInfo(targetedMSSchema);
                }
            });

            ColumnInfo moleculeTransitionId = result.wrapColumn("MoleculeTransitionId", result.getRealTable().getColumn(transitionId.getFieldKey()));
            moleculeTransitionId.setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new MoleculeTransitionsTableInfo(targetedMSSchema);
                }
            });
            result.addColumn(moleculeTransitionId);

            // Add a link to view the chromatogram an individual transition
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.TransitionChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));

            // AreaNormalized  = (area of transition in the replicate) / (total area of all transitions in the replicate/sample file)
            SQLFragment areaNormalizedSQL = new SQLFragment("(SELECT Area / X.TotalTransitionAreaInReplicate FROM ");
            areaNormalizedSQL.append(" ( ");
            areaNormalizedSQL.append(" SELECT SUM(Area) AS TotalTransitionAreaInReplicate FROM ");
            areaNormalizedSQL.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
            areaNormalizedSQL.append(" WHERE tci.SampleFileId = ").append(ExprColumn.STR_TABLE_ALIAS).append(".SampleFileId");
            areaNormalizedSQL.append(") X ");
            areaNormalizedSQL.append(" ) ");
            ExprColumn areaNormalizedCol = new ExprColumn(result, "AreaNormalized", areaNormalizedSQL, JdbcType.DOUBLE);
            areaNormalizedCol.setFormat("##0.####%");
            result.addColumn(areaNormalizedCol);

            // Include all but the chromatogram columns which are about to be added
            List<FieldKey> defaultCols = new ArrayList<>(result.getDefaultVisibleColumns());
            result.setDefaultVisibleColumns(defaultCols);

            // Doesn't really matter what we wrap, so do a VARCHAR column so the type of the output matches
            ColumnInfo timesColInfo = result.addWrapColumn("Times", result.getRealTable().getColumn("identified"));
            timesColInfo.setDisplayColumnFactory(colInfo -> new TargetedMSSchema.ChromatogramDisplayColumn(colInfo, true));
            ColumnInfo intensitiesColInfo = result.addWrapColumn("Intensities", result.getRealTable().getColumn("identified"));
            intensitiesColInfo.setDisplayColumnFactory(colInfo -> new TargetedMSSchema.ChromatogramDisplayColumn(colInfo, false));

            return result;
        }

        if (TABLE_TRANSITION_ANNOTATION.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.TransitionFK.getSQL());
            TargetedMSSchema targetedMSSchema = this;
            result.getColumn("TransitionId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new DocTransitionsTableInfo(targetedMSSchema);
                }
            });

            return result;
        }

        // Tables that have a FK to targetedms.transitionchrominfo
        if (TABLE_TRANSITION_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.TransitionChromInfoFK.getSQL());
        }

        // TODO - handle filtering for annotation, predictor

        // Tables that have a FK to targetedms.isolationScheme
        if (TABLE_ISOLATION_WINDOW.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.IsolationSchemeFK.getSQL());
        }

        // Tables that have a FT to targetesms.DriftTimePredictionSettings
        if(TABLE_MEASURED_DRIFT_TIME.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.DriftTimePredictionSettingsFK.getSQL());
        }

        if(TABLE_JOURNAL_EXPERIMENT.equalsIgnoreCase(name))
        {
            return new JournalExperimentTableInfo(this, getContainer(), ContainerJoinType.ExperimentAnnotationsFK.getSQL());
        }

        if(TABLE_JOURNAL.equalsIgnoreCase(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<TargetedMSSchema>(getSchema().getTable(name), this);
            result.wrapAllColumns(true);
            ColumnInfo projectCol = result.getColumn(FieldKey.fromParts("Project"));
            ContainerForeignKey.initColumn(projectCol, this);
            return result;
        }

        if (getTableNames().contains(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<>(getSchema().getTable(name), this);
            result.wrapAllColumns(true);
            return result;
        }

        return null;
    }

    protected QuerySettings createQuerySettings(String dataRegionName, String queryName, String viewName)
    {
        if(TABLE_PRECURSOR.equalsIgnoreCase(queryName)
           || TABLE_EXPERIMENT_PRECURSOR.equalsIgnoreCase(queryName)
           || TABLE_LIBRARY_DOC_PRECURSOR.equalsIgnoreCase(queryName)
           || TABLE_TRANSITION.equalsIgnoreCase(queryName))
        {
            return new QuerySettings(dataRegionName)
            {
                {
                    setMaxRows(10);
                }
            };
        }
        return super.createQuerySettings(dataRegionName, queryName, viewName);
    }

    @Override
    public QueryView createView(ViewContext context, @NotNull QuerySettings settings, BindException errors)
    {
        if(TABLE_REPLICATE_ANNOTATION.equalsIgnoreCase(settings.getQueryName()))
        {
            QueryView view = new QueryView(this, settings, errors)
            {
                @Override
                protected void addDetailsAndUpdateColumns(List<DisplayColumn> ret, TableInfo table)
                {
                    StringExpression urlUpdate = urlExpr(QueryAction.updateQueryRow);
                    if (urlUpdate != null)
                    {
                        UpdateColumn update = new UpdateColumn(urlUpdate) {
                            @Override
                            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                            {
                                Object id = ctx.get("Id");
                                boolean display = false;
                                try
                                {
                                    int annotationId = Integer.parseInt(id.toString());
                                    ReplicateAnnotation annotation = ReplicateManager.getReplicateAnnotation(annotationId);
                                    if (!ReplicateAnnotation.isSourceSkyline(annotation.getSource()))
                                    {
                                        display = true;
                                    }
                                }
                                catch (NumberFormatException ignored){}
                                if (display)
                                {
                                    super.renderGridCellContents(ctx, out);
                                }
                            }
                        };
                        ret.add(0, update);
                    }
                }
            };
            return view;
        }

        return super.createView(context, settings, errors);
    }

    @Override
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_TARGETED_MS_RUNS);
        hs.add(TABLE_ENZYME);
        hs.add(TABLE_RUN_ENZYME);
        hs.add(TABLE_RUNS);
        hs.add(TABLE_TRANSITION_INSTRUMENT_SETTINGS);
        hs.add(TABLE_PEPTIDE_GROUP);
        hs.add(TABLE_MOLECULE_GROUP);
        hs.add(TABLE_PROTEIN);
        hs.add(TABLE_PEPTIDE);
        hs.add(TABLE_MOLECULE);
        hs.add(TABLE_PRECURSOR);
        hs.add(TABLE_MOLECULE_PRECURSOR);
        hs.add(TABLE_TRANSITION);
        hs.add(TABLE_MOLECULE_TRANSITION);
        hs.add(TABLE_REPLICATE);
        hs.add(TABLE_REPLICATE_ANNOTATION);
        hs.add(TABLE_INSTRUMENT);
        hs.add(TABLE_ISOTOPE_ENRICHMENT);
        hs.add(TABLE_GENERAL_MOLECULE_CHROM_INFO);
        hs.add(TABLE_GENERAL_MOLECULE_ANNOTATION);
        hs.add(TABLE_PEPTIDE_AREA_RATIO);
        hs.add(TABLE_TRANSITION_ANNOTATION);
        hs.add(TABLE_TRANSITION_CHROM_INFO);
        hs.add(TABLE_TRANSITION_AREA_RATIO);
        hs.add(TABLE_TRANSITION_FULL_SCAN_SETTINGS);
        hs.add(TABLE_TRANSITION_PREDICITION_SETTINGS);
        hs.add(TABLE_PRECURSOR_CHROM_INFO);
        hs.add(TABLE_PRECURSOR_ANNOTATION);
        hs.add(TABLE_PRECURSOR_AREA_RATIO);
        hs.add(TABLE_PRECURSOR_CHROM_INFO_ANNOTATION);
        hs.add(TABLE_TRANSITION_CHROM_INFO_ANNOTATION);
        hs.add(TABLE_SAMPLE_FILE);
        hs.add(TABLE_MODIFICATION_SETTINGS);
        hs.add(TABLE_ISOTOPE_LABEL);
        hs.add(TABLE_STRUCTURAL_MODIFICATION);
        hs.add(TABLE_STRUCTURAL_MOD_LOSS);
        hs.add(TABLE_ISOTOPE_MODIFICATION);
        hs.add(TABLE_RUN_STRUCTURAL_MODIFICATION);
        hs.add(TABLE_RUN_ISOTOPE_MODIFICATION);
        hs.add(TABLE_PEPTIDE_STRUCTURAL_MODIFICATION);
        hs.add(TABLE_PEPTIDE_ISOTOPE_MODIFICATION);
        hs.add(TABLE_LIBRARY_SETTINGS);
        hs.add(TABLE_SPECTRUM_LIBRARY);
        hs.add(TABLE_LIBRARY_SOURCE);
        hs.add(TABLE_PRECURSOR_LIB_INFO);
        hs.add(TABLE_ANNOTATION_SETTINGS);
        hs.add(TABLE_QUANTIIFICATION_SETTINGS);
        hs.add(TABLE_REPRESENTATIVE_DATA_STATE_RUN);
        hs.add(TABLE_REPRESENTATIVE_DATA_STATE);
        hs.add(TABLE_IRT_PEPTIDE);
        hs.add(TABLE_IRT_SCALE);
        hs.add(TABLE_RETENTION_TIME_PREDICTION_SETTINGS);
        hs.add(TABLE_EXPERIMENT_PRECURSOR);
        hs.add(TABLE_LIBRARY_PRECURSOR);
        hs.add(TABLE_LIBRARY_DOC_PRECURSOR);
        hs.add(TABLE_ISOLATION_SCHEME);
        hs.add(TABLE_ISOLATION_WINDOW);
        hs.add(TABLE_PREDICTOR);
        hs.add(TABLE_PREDICTOR_SETTINGS);
        hs.add(TABLE_DRIFT_TIME_PREDICTION_SETTINGS);
        hs.add(TABLE_MEASURED_DRIFT_TIME);
        hs.add(TABLE_JOURNAL);
        hs.add(TABLE_JOURNAL_EXPERIMENT);
        hs.add(TABLE_EXPERIMENT_ANNOTATIONS);
        hs.add(TABLE_QC_ANNOTATION_TYPE);
        hs.add(TABLE_QC_ANNOTATION);
        hs.add(TABLE_GUIDE_SET);
        hs.add(TABLE_AUTOQC_PING);
        hs.add(TABLE_FOLD_CHANGE);
        hs.add(TABLE_CALIBRATION_CURVE);
        hs.add(TABLE_PEPTIDE_CALIBRATION_CURVE);
        hs.add(TABLE_MOLECULE_CALIBRATION_CURVE);
        hs.add(TABLE_QC_METRIC_CONFIGURATION);
        hs.add(TABLE_QC_METRIC_EXCLUSION);
        return hs;
    }

    public static class ChromatogramDisplayColumn extends DataColumn
    {
        private final boolean _times;

        public ChromatogramDisplayColumn(ColumnInfo col, boolean times)
        {
            super(col);
            setTextAlign("left");
            _times = times;
        }

        @Override
        public boolean isSortable()
        {
            return false;
        }

        @Override
        public boolean isFilterable()
        {
            return false;
        }

        @Override
        public Object getValue(RenderContext ctx)
        {
            try
            {
                byte[] bytes = ctx.getResults().getBytes(getChromatogramFieldKey());
                Integer numPoints = ctx.get(getNumPointsFieldKey(), Integer.class);
                Integer numTransitions = ctx.get(getNumTransitionsFieldKey(), Integer.class);
                Integer uncompressedSize = ctx.get(getUncompressedSizeFieldKey(), Integer.class);
                Integer chromatogramIndex = ctx.get(getChromatogramIndexFieldKey(), Integer.class);

                if (bytes != null && numPoints != null && numTransitions != null && chromatogramIndex != null)
                {
                    Integer formatOrdinal = ctx.get(getChromatogramFormatKey(), Integer.class);
                    ChromatogramBinaryFormat format = formatOrdinal == null
                            ? ChromatogramBinaryFormat.Arrays : ChromatogramBinaryFormat.values()[formatOrdinal];
                    byte[] uncompressedBytes = SkylineBinaryParser.uncompressStoredBytes(bytes, uncompressedSize, numPoints, numTransitions);
                    Chromatogram chromatogram = format.readChromatogram(uncompressedBytes, numPoints, numTransitions);
                    return StringUtils.join(_times ? chromatogram.getTimes() : chromatogram.getIntensities(chromatogramIndex.intValue()), ',');
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            catch (Exception exception) {
                throw new UnexpectedException(exception);
            }


            return null;
        }

        @NotNull
        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            return PageFlowUtil.filter(getValue(ctx));
        }

        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            return getValue(ctx);
        }

        @Nullable
        @Override
        public String getFormattedText(RenderContext ctx)
        {
            Object o = getValue(ctx);
            return o == null ? null : o.toString();
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            keys.add(getNumPointsFieldKey());
            keys.add(getNumTransitionsFieldKey());
            keys.add(getUncompressedSizeFieldKey());
            keys.add(getChromatogramIndexFieldKey());
            keys.add(getChromatogramFieldKey());
            keys.add(getChromatogramFormatKey());
        }

        private FieldKey getNumPointsFieldKey()
        {
            return new FieldKey(getPrecursorChromInfoFK(), "NumPoints");
        }

        private FieldKey getPrecursorChromInfoFK()
        {
            return new FieldKey(getColumnInfo().getFieldKey().getParent(), "PrecursorChromInfoId");
        }

        private FieldKey getNumTransitionsFieldKey()
        {
            return new FieldKey(getPrecursorChromInfoFK(), "NumTransitions");
        }

        private FieldKey getUncompressedSizeFieldKey()
        {
            return new FieldKey(getPrecursorChromInfoFK(), "UncompressedSize");
        }

        private FieldKey getChromatogramFieldKey()
        {
            return new FieldKey(getPrecursorChromInfoFK(), "Chromatogram");
        }

        private FieldKey getChromatogramIndexFieldKey()
        {
            return new FieldKey(getColumnInfo().getFieldKey().getParent(), "ChromatogramIndex");
        }

        private FieldKey getChromatogramFormatKey() { return new FieldKey(getPrecursorChromInfoFK(), "ChromatogramFormat");}
    }
}
