/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.AJAXDetailsDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.EnumTableInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
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
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.query.*;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static final String TABLE_REPRESENTATIVE_DATA_STATE_RUN = "RepresentativeDataState_Run";
    public static final String TABLE_REPRESENTATIVE_DATA_STATE = "RepresentativeDataState";
    public static final String TABLE_IRT_PEPTIDE = "iRTPeptide";
    public static final String TABLE_IRT_SCALE = "iRTScale";

    public static final String TABLE_AUTOQC_PING = "AutoQCPing";

    public static final String TABLE_EXPERIMENT_ANNOTATIONS = "ExperimentAnnotations";

    public static final String TABLE_QC_ANNOTATION_TYPE = "QCAnnotationType";
    public static final String TABLE_QC_ANNOTATION = "QCAnnotation";

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
        return DbSchema.get(SCHEMA_NAME);
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
                result.addWrapColumn(result.getRealTable().getColumn("Status"));
                ColumnInfo stateColumn = result.addWrapColumn(result.getRealTable().getColumn("RepresentativeDataState"));
                stateColumn.setFk(new QueryForeignKey(TargetedMSSchema.this, null, TABLE_REPRESENTATIVE_DATA_STATE_RUN, "RowId", null));

                ColumnInfo downloadLinkColumn = result.addWrapColumn("Download", result.getRealTable().getColumn("Id"));
                downloadLinkColumn.setLabel("");
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
                                String runId = String.valueOf(ctx.get(this.getColumnInfo().getFieldKey()));
                                downloadUrl.replaceParameter("runId", runId);

                                File skyDocFile = SkylineFileUtils.getSkylineFile(Integer.parseInt(runId));
                                String size = h((skyDocFile != null && skyDocFile.isFile()) ? " (" + FileUtils.byteCountToDisplaySize(skyDocFile.length()) + ")" : "");
                                out.write(PageFlowUtil.textLink("Download", downloadUrl));
                                out.write("<span class=\"labkey-text-link\">" + size + "</span>");
                            }
                        };
                    }
                });

                // add Peptide Group count column
                SQLFragment sql = new SQLFragment("(").append(TargetedMSManager.getRunPeptideGroupCountSQL(ExprColumn.STR_TABLE_ALIAS + ".Id")).append(")");
                ColumnInfo countCol = new ExprColumn(result, "Proteins", sql, JdbcType.INTEGER);
                countCol.setFormat("#,###");
                result.addColumn(countCol);

                // add Peptide count column
                sql = new SQLFragment("(").append(TargetedMSManager.getRunPeptideCountSQL(ExprColumn.STR_TABLE_ALIAS + ".Id")).append(")");
                countCol = new ExprColumn(result, "Peptides", sql, JdbcType.INTEGER);
                countCol.setFormat("#,###");
                result.addColumn(countCol);

                // add Small Molecule count column
                sql = new SQLFragment("(").append(TargetedMSManager.getRunSmallMoleculeCountSQL(ExprColumn.STR_TABLE_ALIAS + ".Id")).append(")");
                countCol = new ExprColumn(result, "SmallMolecules", sql, JdbcType.INTEGER);
                countCol.setFormat("#,###");
                result.addColumn(countCol);

                // add Precursor count column
                sql = new SQLFragment("(").append(TargetedMSManager.getRunPrecursorCountSQL(ExprColumn.STR_TABLE_ALIAS + ".Id")).append(")");
                countCol = new ExprColumn(result, "Precursors", sql, JdbcType.INTEGER);
                countCol.setFormat("#,###");
                result.addColumn(countCol);

                // add Transition count column
                sql = new SQLFragment("(").append(TargetedMSManager.getRunTransitionCountSQL(ExprColumn.STR_TABLE_ALIAS + ".Id")).append(")");
                countCol = new ExprColumn(result, "Transitions", sql, JdbcType.INTEGER);
                countCol.setFormat("#,###");
                result.addColumn(countCol);

                return result;
            }
        });
        skyDocDetailColumn.setHidden(false);
        result.addColumn(skyDocDetailColumn);

        //adjust the default visible columns
        List<FieldKey> columns = new ArrayList<>(result.getDefaultVisibleColumns());
        columns.remove(FieldKey.fromParts("File"));
        columns.remove(FieldKey.fromParts("Protocol"));
        columns.remove(FieldKey.fromParts("CreatedBy"));
        columns.remove(FieldKey.fromParts("RunGroups"));
        columns.remove(FieldKey.fromParts("Name"));

        columns.add(2, FieldKey.fromParts("File"));
        columns.add(FieldKey.fromParts("File", "Proteins"));
        columns.add(FieldKey.fromParts("File", "Peptides"));
        columns.add(FieldKey.fromParts("File", "SmallMolecules"));
        columns.add(FieldKey.fromParts("File", "Precursors"));
        columns.add(FieldKey.fromParts("File", "Transitions"));
        columns.add(FieldKey.fromParts("File", "Download"));

        result.setDefaultVisibleColumns(columns);

        return result;
    }

    @Override
    protected TableInfo createTable(String name)
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
        if (TABLE_GUIDE_SET.equalsIgnoreCase(name))
        {
            return new GuideSetTable(this);
        }
        if ( TABLE_AUTOQC_PING.equalsIgnoreCase(name))
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
        if (TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name),
                                                                  this,
                                                                  ContainerJoinType.RunFK.getSQL(),
                                                                  TargetedMSManager.getTableInfoPeptideGroupAnnotation(),
                                                                  "PeptideGroupId",
                                                                  "Protein Annotations")
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
            noteAnnotation.setLabel("Protein Note/Annotations");
            result.addColumn(noteAnnotation);

            return result;
        }

        // Tables that have a FK to targetedms.Runs
        if (TABLE_TRANSITION_INSTRUMENT_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) ||
            TABLE_INSTRUMENT.equalsIgnoreCase(name) ||
            TABLE_ISOTOPE_ENRICHMENT.equalsIgnoreCase(name) ||
            TABLE_ISOLATION_SCHEME.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) ||
            TABLE_REPLICATE.equalsIgnoreCase(name) ||
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
            TABLE_DRIFT_TIME_PREDICTION_SETTINGS.equalsIgnoreCase(name) )
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.RunFK.getSQL());
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
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.ReplicateFK.getSQL());
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
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.SampleFileFK.getSQL());
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
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_TARGETED_MS_RUNS);
        hs.add(TABLE_ENZYME);
        hs.add(TABLE_RUN_ENZYME);
        hs.add(TABLE_RUNS);
        hs.add(TABLE_TRANSITION_INSTRUMENT_SETTINGS);
        hs.add(TABLE_PEPTIDE_GROUP);
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
        return hs;
    }

}
