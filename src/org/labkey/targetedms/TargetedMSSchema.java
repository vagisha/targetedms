/*
 * Copyright (c) 2012-2018 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.AJAXDetailsDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.EnumTableInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpdateColumn;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.exp.api.ExperimentService;
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
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringExpression;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.PopupMenu;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.parser.Chromatogram;
import org.labkey.targetedms.parser.ChromatogramBinaryFormat;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.SkylineBinaryParser;
import org.labkey.targetedms.query.*;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.Controller;

import java.io.IOException;
import java.io.Writer;
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
    public static final String TABLE_PHARMACOKINETICS = "Pharmacokinetics";

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
    public static final String TABLE_QC_ENABLED_METRICS = "QCEnabledMetrics";
    public static final String TABLE_QC_EMAIL_NOTIFICATIONS = "QCEmailNotifications";

    public static final String TABLE_GUIDE_SET = "GuideSet";

    public static final String TABLE_JOURNAL = "Journal";
    public static final String TABLE_JOURNAL_EXPERIMENT = "JournalExperiment";

    private static final String PROTOCOL_PATTERN_PREFIX = "urn:lsid:%:Protocol.%:";

    public static final String TABLE_GENERAL_MOLECULE = "GeneralMolecule";
    public static final String TABLE_GENERAL_PRECURSOR = "GeneralPrecursor";
    public static final String TABLE_GENERAL_TRANSITION = "GeneralTransition";
    public static final String TABLE_MOLECULE_PRECURSOR = "MoleculePrecursor";
    public static final String TABLE_SKYLINE_AUDITLOG_ENTRY = "AuditLogEntry";
    public static final String TABLE_SKYLINE_AUDITLOG_MESSAGE = "AuditLogMessage";


    public static final String COL_PROTEIN = "Protein";
    public static final String COL_LIST = "List";

    /** Prefix for a run-specific table name, customized based on the data present within that run */
    public static final String SAMPLE_FILE_RUN_PREFIX = "samplefile_run";

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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("GeneralMoleculeChromInfoId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("GeneralTransitionId", "GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("PrecursorChromInfoId", "SampleFileId", "ReplicateId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("PrecursorId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("Id", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("PeptideChromInfoId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("ReplicateId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("TransitionId", "GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("TransitionChromInfoId", "SampleFileId", "ReplicateId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("SampleFileId", "ReplicateId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("iRTScaleId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("IsolationSchemeId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("DriftTimePredictionSettingsId", "RunId", "Container");
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
            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("ExperimentAnnotationsId", "Container");
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

            @Override
            public FieldKey getContainerFieldKey()
            {
                return FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Container");
            }
        };

        public abstract SQLFragment getSQL();
        public abstract FieldKey getContainerFieldKey();
    }

    public ExpRunTable getTargetedMSRunsTable(ContainerFilter cf)
    {
        // Start with a standard experiment run table (forWrite==true because we hack on it)
        ExpRunTable result = _expSchema.getRunsTable(true);
        if (null != cf)
            result.setContainerFilter(cf);
        result.setDescription(TargetedMSManager.getTableInfoRuns().getDescription());

        // Filter to just the runs with the Targeted MS protocol
        result.setProtocolPatterns(PROTOCOL_PATTERN_PREFIX + TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX + "%",
                                   PROTOCOL_PATTERN_PREFIX + TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX + "%");

        // Add a lookup column to the Runs table in targetedms schema
        SQLFragment sql = new SQLFragment("(SELECT MIN(tmsRuns.Id)\n" +
                "\nFROM " + TargetedMSManager.getTableInfoRuns() + " tmsRuns " +
                "\nWHERE tmsRuns.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID AND tmsRuns.Deleted = ?)");
        sql.add(Boolean.FALSE);
        var skyDocDetailColumn = new ExprColumn(result, "File", sql, JdbcType.INTEGER);

        ActionURL url = TargetedMSController.getShowRunURL(getContainer());

        boolean hasSmallMolecules = TargetedMSManager.containerHasSmallMolecules(getContainer());
        boolean hasPeptides = TargetedMSManager.containerHasPeptides(getContainer());
        boolean hasCalibrationCurves = TargetedMSManager.containerHasCalibrationCurves(getContainer());
        String peptideGroupColName = (hasSmallMolecules ? COL_LIST : COL_PROTEIN) + "s";
        boolean hasDocVersions = TargetedMSManager.containerHasDocVersions(getContainer());

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
                var stateColumn = result.addWrapColumn(result.getRealTable().getColumn("RepresentativeDataState"));
                stateColumn.setFk(new QueryForeignKey(TargetedMSSchema.this, null, TABLE_REPRESENTATIVE_DATA_STATE_RUN, "RowId", null));

                var downloadLinkColumn = result.addWrapColumn("Download", result.getRealTable().getColumn("Id"));
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
                                Integer runId = ctx.get(this.getColumnInfo().getFieldKey(), Integer.class);
                                if (runId != null)
                                {
                                    TargetedMSRun run = TargetedMSManager.getRun(runId);

                                    if(run != null)
                                    {
                                        PopupMenu menu = TargetedMSController.createDownloadMenu(run);
                                        menu.render(out);
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
                                keys.add(FieldKey.fromParts("Folder"));
                            }
                        };
                    }
                });

                result.addWrapColumn(peptideGroupColName, result.getRealTable().getColumn("PeptideGroupCount"));
                result.addWrapColumn("Peptides", result.getRealTable().getColumn("PeptideCount"));
                result.addWrapColumn("SmallMolecules", result.getRealTable().getColumn("SmallMoleculeCount"));
                result.addWrapColumn("Precursors", result.getRealTable().getColumn("PrecursorCount"));
                result.addWrapColumn("Transitions", result.getRealTable().getColumn("TransitionCount"));
                result.addWrapColumn("Replicates", result.getRealTable().getColumn("ReplicateCount"));
                result.addWrapColumn("CalibrationCurves", result.getRealTable().getColumn("CalibrationCurveCount"));

                addVersionsColumn(result);

                return result;
            }

            private void addVersionsColumn(FilteredTable result)
            {
                var versionsCol = result.addWrapColumn("Versions", result.getRealTable().getColumn("ExperimentRunLSID"));
                versionsCol.setTextAlign("right");
                versionsCol.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        return new DataColumn(colInfo){
                            @Override
                            public Object getValue(RenderContext ctx)
                            {
                                String runLsid = ctx.get(getColumnInfo().getFieldKey(), String.class);
                                if(runLsid != null)
                                {
                                    Set<Integer> rowIds = Collections.singleton(ExperimentService.get().getExpRun(runLsid).getRowId());
                                    return TargetedMSManager.getLinkedVersions(getUser(), getContainer(), rowIds, rowIds).size();
                                }
                                return super.getValue(ctx);
                            }
                            @Override
                            public Object getDisplayValue(RenderContext ctx)
                            {
                                return getValue(ctx);
                            }
                            @Override
                            public @NotNull String getFormattedValue(RenderContext ctx)
                            {
                                return PageFlowUtil.filter(getValue(ctx));
                            }
                            @Override
                            public boolean isFilterable()
                            {
                                return false;
                            }
                            @Override
                            public boolean isSortable()
                            {
                                return false;
                            }
                            @Override
                            public void addQueryFieldKeys(Set<FieldKey> keys)
                            {
                                keys.add(FieldKey.fromParts("File", "Id"));
                            }
                            @Override
                            public String renderURL(RenderContext ctx)
                            {
                                Integer runId = ctx.get(FieldKey.fromParts("File", "Id"), Integer.class);
                                if (runId == null)
                                    return null;

                                ActionURL url = new ActionURL(TargetedMSController.ShowVersionsAction.class, getContainer());
                                url.addParameter("id", runId);
                                return url.toString();
                            }
                        };
                    }
                });
            }
        });
        skyDocDetailColumn.setHidden(false);
        result.addColumn(skyDocDetailColumn);

        var replacedByCol = new WrappedColumn(result.getColumn("ReplacedByRun"), "ReplacedBy"){
            @Override
            public SQLFragment getValueSql(String tableAlias)
            {
                SQLFragment sql = new SQLFragment(" (SELECT tr.id FROM ")
                        .append(ExperimentService.get().getTinfoExperimentRun(), "er")
                        .append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), "tr")
                        .append(" ON er.LSID = tr.experimentRunLSID")
                        .append(" WHERE er.rowId=").append(tableAlias).append(".replacedByRunId")
                        .append(") ");
                return sql;
            }
        };
        replacedByCol.setFk(new LookupForeignKey(url, "id", "Id", "Description")
        {
            @Override
            public @Nullable TableInfo getLookupTableInfo()
            {
                return TargetedMSManager.getTableInfoRuns();
            }
        });
        result.addColumn(replacedByCol);

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
                    _fieldKeys.add(FieldKey.fromParts("File", peptideGroupColName));

                    // Omit peptides or small molecules if we don't have any in this container
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
                    if (hasCalibrationCurves)
                        _fieldKeys.add(FieldKey.fromParts("File", "CalibrationCurves"));
                    if(hasDocVersions)
                    {
                        _fieldKeys.add(FieldKey.fromParts("File", "Versions"));
                        _fieldKeys.add(FieldKey.fromParts("ReplacedBy"));
                    }
                }
                return _fieldKeys;
            }

            @NotNull
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
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (TABLE_TARGETED_MS_RUNS.equalsIgnoreCase(name))
        {
            return getTargetedMSRunsTable(cf);
        }
        if (TABLE_IRT_PEPTIDE.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.iRTScaleFK);
        }
        if (TABLE_QC_ANNOTATION_TYPE.equalsIgnoreCase(name))
        {
            return new QCAnnotationTypeTable(this, cf);
        }
        if (TABLE_QC_ANNOTATION.equalsIgnoreCase(name))
        {
            return new QCAnnotationTable(this, cf);
        }
        if (TABLE_QC_METRIC_CONFIGURATION.equalsIgnoreCase(name))
        {
            return new QCMetricConfigurationTable(this, cf);
        }
        if(TABLE_QC_ENABLED_METRICS.equalsIgnoreCase(name))
        {
            return new QCEnabledMetricsTable(this, cf);
        }
        if(TABLE_QC_EMAIL_NOTIFICATIONS.equalsIgnoreCase(name))
        {
            return new QCEmailNotificationsTable(this, cf);
        }
        if (TABLE_GUIDE_SET.equalsIgnoreCase(name))
        {
            return new GuideSetTable(this, cf);
        }
        if (TABLE_AUTOQC_PING.equalsIgnoreCase(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<>(getSchema().getTable(TABLE_AUTOQC_PING), this, cf);
            result.wrapAllColumns(true);
            result.getMutableColumn("CreatedBy").setFk(new UserIdQueryForeignKey(this));
            result.getMutableColumn("ModifiedBy").setFk(new UserIdQueryForeignKey(this));
            result.getMutableColumn("Container").setFk(new ContainerForeignKey(this));
            return result;
        }
        if(TABLE_EXPERIMENT_ANNOTATIONS.equalsIgnoreCase(name))
        {
            return new ExperimentAnnotationsTableInfo(this, cf);
        }

        if (TABLE_REPRESENTATIVE_DATA_STATE_RUN.equalsIgnoreCase(name))
        {
            return new EnumTableInfo<>(
                    TargetedMSRun.RepresentativeDataState.class,
                    this,
                    TargetedMSRun.RepresentativeDataState::getLabel,
                    true,
                    "Possible states a run might be in for resolving representative data after upload"
                    );
        }
        if (TABLE_REPRESENTATIVE_DATA_STATE.equalsIgnoreCase(name))
        {
            EnumTableInfo tableInfo = new EnumTableInfo<>(
                    RepresentativeDataState.class,
                    this,
                    RepresentativeDataState::getLabel,
                    true,
                    "Possible representative states for a peptide group or precursor");

            var viewColumn = tableInfo.getMutableColumn("Value");
            viewColumn.setLabel("Library State");
            viewColumn.setDisplayColumnFactory(RepresentativeStateDisplayColumn::new);

            return tableInfo;
        }

        // Tables that have a FK directly to targetedms.Runs
        if (TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) || TABLE_MOLECULE_GROUP.equalsIgnoreCase(name))
        {
            boolean proteomics = TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name);
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(TABLE_PEPTIDE_GROUP),
                                                                  this, cf,
                                                                  ContainerJoinType.RunFK,
                                                                  TargetedMSManager.getTableInfoPeptideGroupAnnotation(),
                                                                  "PeptideGroupId",
                                                                  "Protein Annotations",
                                                                  "protein", false) // This may change as more small molecule work is done in Skyline.
            {
                @Override
                protected Class<? extends Controller> getDetailsActionClass()
                {
                    return TargetedMSController.ShowProteinAction.class;
                }
            };
            result.getMutableColumn("SequenceId").setFk(new LookupForeignKey("SeqId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createSequencesTableInfo(getUser(), getContainer());
                }
            });
            var labelColumn = result.getMutableColumn("Label");
            labelColumn.setURL(result.getDetailsURL(null, null));
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
                        FieldKey containerFieldKey = result.getContainerFieldKey();
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
                labelColumn.setLabel(TargetedMSSchema.COL_LIST);
                result.getMutableColumn("SequenceId").setHidden(true);
            }
            result.getMutableColumn("RunId").setFk(new QueryForeignKey(this, null, TABLE_TARGETED_MS_RUNS, "File", null));
            result.getMutableColumn("RepresentativeDataState").setFk(new QueryForeignKey(this, null, TargetedMSSchema.TABLE_REPRESENTATIVE_DATA_STATE, "RowId", null));
            result.getMutableColumn("RepresentativeDataState").setHidden(true);

            // Create a WrappedColumn for Note & Annotations
            WrappedColumn noteAnnotation = new WrappedColumn(result.getColumn("Annotations"), "NoteAnnotations");
            noteAnnotation.setDisplayColumnFactory(AnnotationUIDisplayColumn::new);
            if (proteomics)
            {
                noteAnnotation.setLabel(TargetedMSSchema.COL_PROTEIN + " Note/Annotations");
            }
            else
            {
                noteAnnotation.setLabel(TargetedMSSchema.COL_LIST + " Note/Annotations");
            }
            result.addColumn(noteAnnotation);

            return result;
        }

        if (TABLE_REPLICATE.equalsIgnoreCase(name))
        {
            return new AnnotatedTargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.RunFK, TargetedMSManager.getTableInfoReplicateAnnotation(), "ReplicateId", "Replicate Annotations", "replicate", false);
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
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.RunFK);
        }

        if (TABLE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable(this, cf);
        }
        if (TABLE_PEPTIDE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable.PeptideCalibrationCurveTable(this, cf);
        }
        if (TABLE_MOLECULE_CALIBRATION_CURVE.equalsIgnoreCase(name))
        {
            return new CalibrationCurveTable.MoleculeCalibrationCurveTable(this, cf);
        }

        if (TABLE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable(this, cf);
        }

        if (TABLE_PEPTIDE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable.PeptideFoldChangeTable(this, cf);
        }

        if (TABLE_MOLECULE_FOLD_CHANGE.equalsIgnoreCase(name))
        {
            return new FoldChangeTable.MoleculeFoldChangeTable(this, cf);
        }

        // Tables that have a FK to targetedms.peptidegroup
        if (TABLE_PEPTIDE.equalsIgnoreCase(name))
        {
            return new PeptideTableInfo(this, cf, false);
        }
        if (TABLE_MOLECULE.equalsIgnoreCase(name))
        {
            return new MoleculeTableInfo(this, cf, false);
        }

        if (TABLE_PROTEIN.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.PeptideGroupFK);
        }

        // Tables that have a FK to targetedms.replicate
        if (TABLE_SAMPLE_FILE.equalsIgnoreCase(name))
        {
            return new SampleFileTable(this, cf);
        }
        if (TABLE_REPLICATE_ANNOTATION.equalsIgnoreCase(name))
        {
            return new ReplicateAnnotationTable(this, cf);
        }
        if (TABLE_QC_METRIC_EXCLUSION.equalsIgnoreCase(name))
        {
            return new QCMetricExclusionTable(this, cf);
        }

        if (TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name) || TABLE_GENERAL_MOLECULE_CHROM_INFO.equalsIgnoreCase(name))
        {
            return new GeneralMoleculeChromInfoTableInfo(getSchema().getTable(TABLE_GENERAL_MOLECULE_CHROM_INFO), this, cf, ContainerJoinType.GeneralMoleculeFK, name);
        }

        // Tables that have a FK to targetedms.peptidechrominfo
        if (TABLE_PEPTIDE_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.PeptideChromInfoFK);
        }

        // Tables that have a FK to targetedms.peptide
        if (TABLE_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo(this, cf, false);
        }

        if (TABLE_MOLECULE_PRECURSOR.equalsIgnoreCase(name))
        {
            return new MoleculePrecursorTableInfo(this, cf, false);
        }

        if (TABLE_EXPERIMENT_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.ExperimentPrecursorTableInfo(this, cf);
        }

        if (TABLE_LIBRARY_PRECURSOR.equalsIgnoreCase(name) || TABLE_LIBRARY_DOC_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.LibraryPrecursorTableInfo(this, cf);
        }
        if (TABLE_GENERAL_MOLECULE_ANNOTATION.equalsIgnoreCase(name) || TABLE_PEPTIDE_ANNOTATION.equalsIgnoreCase(name))
        {
            GeneralMoleculeAnnotationTableInfo result = new GeneralMoleculeAnnotationTableInfo(getSchema().getTable(TABLE_GENERAL_MOLECULE_ANNOTATION), this, cf, ContainerJoinType.GeneralMoleculeFK);
            result.setName(name);
            return result;
        }
        if (TABLE_PEPTIDE_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name))
        {
            // TODO ContainerFilter: who is responsible for CF here?
            return new TargetedMSTable(new PeptideStructuralModificationTableInfo(this, cf), this, cf, ContainerJoinType.ModPeptideFK);
        }
        if (TABLE_PEPTIDE_ISOTOPE_MODIFICATION.equalsIgnoreCase(name))
        {
            // TODO ContainerFilter: who is responsible for CF here?
            return new TargetedMSTable(new PeptideIsotopeModificationTableInfo(this, cf), this, cf, ContainerJoinType.ModPeptideFK);
        }
        // Tables that have a FK to targetedms.precursor
        if (TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name))
        {
            return new PrecursorChromInfoTable(getSchema().getTable(name), this, cf);
        }
        if (TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            return new DocTransitionsTableInfo(this, cf);
        }
        if(TABLE_MOLECULE_TRANSITION.equalsIgnoreCase(name))
        {
            return new MoleculeTransitionsTableInfo(this, cf, false);
        }

        // Tables that have a FK to targetedms.precursorchrominfo
        if (TABLE_PRECURSOR_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.PrecursorChromInfoFK);
        }

        // Tables that have a FK to targetedms.transition
        if (TABLE_TRANSITION_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.SampleFileFK,
                    TargetedMSManager.getTableInfoTransitionChromInfoAnnotation(), "TransitionChromInfoId", "Transition Result Annotations", "transition_result", false);
            TargetedMSSchema targetedMSSchema = this;

            var transitionId = result.getMutableColumn("TransitionId");
            transitionId.setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new DocTransitionsTableInfo(targetedMSSchema, cf);
                }
            });

            var moleculeTransitionId = result.wrapColumn("MoleculeTransitionId", result.getRealTable().getColumn(transitionId.getFieldKey()));
            moleculeTransitionId.setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new MoleculeTransitionsTableInfo(targetedMSSchema, cf, false);
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
            var timesColInfo = result.addWrapColumn("Times", result.getRealTable().getColumn("identified"));
            timesColInfo.setDisplayColumnFactory(colInfo -> new TargetedMSSchema.ChromatogramDisplayColumn(colInfo, true));
            var intensitiesColInfo = result.addWrapColumn("Intensities", result.getRealTable().getColumn("identified"));
            intensitiesColInfo.setDisplayColumnFactory(colInfo -> new TargetedMSSchema.ChromatogramDisplayColumn(colInfo, false));

            return result;
        }

        if (TABLE_TRANSITION_ANNOTATION.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.TransitionFK);
            TargetedMSSchema targetedMSSchema = this;
            result.getMutableColumn("TransitionId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new DocTransitionsTableInfo(targetedMSSchema, cf);
                }
            });

            return result;
        }

        // Tables that have a FK to targetedms.transitionchrominfo
        if (TABLE_TRANSITION_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.TransitionChromInfoFK);
        }

        // TODO - handle filtering for annotation, predictor

        // Tables that have a FK to targetedms.isolationScheme
        if (TABLE_ISOLATION_WINDOW.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.IsolationSchemeFK);
        }

        // Tables that have a FT to targetesms.DriftTimePredictionSettings
        if(TABLE_MEASURED_DRIFT_TIME.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, cf, ContainerJoinType.DriftTimePredictionSettingsFK);
        }

        if(TABLE_JOURNAL_EXPERIMENT.equalsIgnoreCase(name))
        {
            return new JournalExperimentTableInfo(this, cf, getContainer());
        }

        if(TABLE_JOURNAL.equalsIgnoreCase(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<>(getSchema().getTable(name), this, cf);
            result.wrapAllColumns(true);
            var projectCol = result.getMutableColumn(FieldKey.fromParts("Project"));
            ContainerForeignKey.initColumn(projectCol, this);
            return result;
        }

        if (getTableNames().contains(name))
        {
            FilteredTable<TargetedMSSchema> result = new FilteredTable<>(getSchema().getTable(name), this, cf);
            result.wrapAllColumns(true);
            if (name.equalsIgnoreCase(TABLE_RUNS))
            {
                result.getMutableColumn("DataId").setFk(new QueryForeignKey(_expSchema, null, ExpSchema.TableType.Data.name(), null, null));
                result.getMutableColumn("SkydDataId").setFk(new QueryForeignKey(_expSchema, null, ExpSchema.TableType.Data.name(), null, null));
            }
            return result;
        }

        // Issue 35966 - Show a custom set of columns by default for a run-scoped replicate view
        if (name.toLowerCase().startsWith(SAMPLE_FILE_RUN_PREFIX))
        {
            try
            {
                TargetedMSRun run = TargetedMSManager.getRun(Integer.parseInt(name.substring(SAMPLE_FILE_RUN_PREFIX.length())));
                // If we can't find the run or it's not in the current container, just act like the table doesn't exist
                if (run != null && run.getContainer().equals(getContainer()))
                {
                    SampleFileTable result = new SampleFileTable(this, cf, run);
                    result.setName(name);
                    return result;
                }
            }
            catch (NumberFormatException ignored) {}
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
            return new QueryView(TargetedMSSchema.this, settings, errors)
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
        hs.add(TABLE_QC_ENABLED_METRICS);
        hs.add(TABLE_QC_EMAIL_NOTIFICATIONS);
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
