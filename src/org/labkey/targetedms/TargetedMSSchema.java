/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.AJAXDetailsDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
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
import org.labkey.api.module.ModuleLoader;
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
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.query.AnnotatedTargetedMSTable;
import org.labkey.targetedms.query.DocTransitionsTableInfo;
import org.labkey.targetedms.query.ModifiedPeptideDisplayColumn;
import org.labkey.targetedms.query.PrecursorTableInfo;
import org.labkey.targetedms.query.RepresentativeStateDisplayColumn;
import org.labkey.targetedms.query.TargetedMSTable;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

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
    public static final String TABLE_TRANSITION_FULL_SCAN_SETTINGS = "TransitionFullScanSettings";
    public static final String TABLE_TRANSITION_PREDICITION_SETTINGS = "TransitionPredictionSettings";
    public static final String TABLE_SAMPLE_FILE = "SampleFile";
    public static final String TABLE_PEPTIDE_GROUP = "PeptideGroup";
    public static final String TABLE_PEPTIDE_GROUP_ANNOTATION = "PeptideGroupAnnotation";
    public static final String TABLE_INSTRUMENT = "Instrument";
    public static final String TABLE_ISOTOPE_ENRICHMENT = "IsotopeEnrichment";
    public static final String TABLE_PROTEIN = "Protein";
    public static final String TABLE_PEPTIDE = "Peptide";
    public static final String TABLE_PEPTIDE_ANNOTATION = "PeptideAnnotation";
    public static final String TABLE_PRECURSOR = "Precursor";
    public static final String TABLE_EXPERIMENT_PRECURSOR = "ExperimentPrecursor";
    public static final String TABLE_LIBRARY_PRECURSOR = "LibraryPrecursor";
    public static final String TABLE_LIBRARY_DOC_PRECURSOR = "LibraryDocPrecursor";
    public static final String TABLE_PRECURSOR_ANNOTATION = "PrecursorAnnotation";
    public static final String TABLE_TRANSITION = "Transition";
    public static final String TABLE_TRANSITION_LOSS = "TransitionLoss";
    public static final String TABLE_TRANSITION_ANNOTATION = "TransitionAnnotation";
    public static final String TABLE_TRANSITION_OPTIMIZATION = "TransitionOptimization";
    public static final String TABLE_TRANSITION_CHROM_INFO = "TransitionChromInfo";
    public static final String TABLE_TRANSITION_CHROM_INFO_ANNOTATION = "TransitionChromInfoAnnotation";
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

    public static final String TABLE_RESPRESENTATIVE_DATA_STATE_RUN = "RepresentativeDataState_Run";
    public static final String TABLE_RESPRESENTATIVE_DATA_STATE = "RepresentativeDataState";
    public static final String TABLE_IRT_PEPTIDE = "iRTPeptide";
    public static final String TABLE_IRT_SCALE = "iRTScale";

    private static final String PROTOCOL_PATTERN_PREFIX = "urn:lsid:%:Protocol.%:";

    private ExpSchema _expSchema;
    private boolean _restrictContainer = true;
    private final ContainerFilter _containerFilter;


    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider()
        {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                if (schema.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.NAME)))
                    return new TargetedMSSchema(schema.getUser(), schema.getContainer());
                return null;
            }
        });
    }

    public TargetedMSSchema(User user, Container container)
    {
         this(user, container, true);
    }

    public TargetedMSSchema(User user, Container container, boolean restrictContainer)
    {
        super(SCHEMA_NAME, SCHEMA_DESCR, user, container, TargetedMSManager.getSchema());
        _restrictContainer = restrictContainer;

        _expSchema = new ExpSchema(user, container);

        if (_restrictContainer)
        {
            _containerFilter = ContainerFilter.CURRENT;
        }
        else
        {
            _containerFilter = new ContainerFilter.CurrentAndSubfolders(user);
            _expSchema.setContainerFilter(_containerFilter);
        }
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public enum ContainerJoinType
    {
        PeptideFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = PeptideId)");
                return sql;
            }
        },
        PeptideChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideChromInfo(), "pci");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = pci.PeptideId AND pci.Id = PeptideChromInfoId)");
                return sql;
            }
        },
        TransitionFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoTransition(), "t");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = pre.PeptideId AND pre.Id = t.PrecursorId AND TransitionId = t.Id)");
                return sql;
            }
        },
        PrecursorChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = pre.PeptideId AND pre.Id = pci.PrecursorId AND pci.Id = PrecursorChromInfoId)");
                return sql;
            }
        },
        RunFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(" WHERE r.Id = RunId)");
                return sql;
            }
        },
        PeptideGroupFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = PeptideGroupId)");
                return sql;
            }
        },
        ReplicateFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoReplicate(), "rep");
                sql.append(" WHERE r.Id = rep.RunId AND rep.Id = ReplicateId)");
                return sql;
            }
        },
        PrecursorFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = pre.PeptideId AND pre.Id = PrecursorId)");
                return sql;

            }
        },
        TransitionChromInfoFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT r.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoTransition(), "t");
                sql.append(", ");
                sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
                sql.append(" WHERE r.Id = pg.RunId AND pg.Id = pep.PeptideGroupId AND pep.Id = pre.PeptideId AND ");
                sql.append("pre.Id = t.PrecursorId AND tci.TransitionId = t.Id AND tci.Id = TransitionChromInfoId)");
                return sql;
            }
        },
        iRTScaleFK
        {
            @Override
            public SQLFragment getSQL()
            {
                SQLFragment sql = new SQLFragment("(SELECT s.Container FROM ");
                sql.append(TargetedMSManager.getTableInfoiRTScale(), "s");
                sql.append(" WHERE s.Id = iRTScaleId)");
                return sql;
            }
        };

        public abstract SQLFragment getSQL();
    }

    public ExpRunTable getTargetedMSRunsTable()
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.getRunsTable();
        result.setDescription("Contains a row per Skyline document loaded in this folder.");

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
        skyDocDetailColumn.setFk(new LookupForeignKey(url, "id", "Id", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable<>(TargetedMSManager.getTableInfoRuns(), TargetedMSSchema.this);
                result.addWrapColumn(result.getRealTable().getColumn("Id"));
                result.addWrapColumn(result.getRealTable().getColumn("Description"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideGroupCount"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideCount"));
                result.addWrapColumn(result.getRealTable().getColumn("PrecursorCount"));
                result.addWrapColumn(result.getRealTable().getColumn("TransitionCount"));
                result.addWrapColumn(result.getRealTable().getColumn("Status"));
                ColumnInfo stateColumn = result.addWrapColumn(result.getRealTable().getColumn("RepresentativeDataState"));
                stateColumn.setFk(new LookupForeignKey("RowId")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return getTable(TABLE_RESPRESENTATIVE_DATA_STATE_RUN);
                    }
                });

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
        columns.add(FieldKey.fromParts("File", "PeptideGroupCount"));
        columns.add(FieldKey.fromParts("File", "PeptideCount"));
        columns.add(FieldKey.fromParts("File", "PrecursorCount"));
        columns.add(FieldKey.fromParts("File", "TransitionCount"));

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
        if (TABLE_RESPRESENTATIVE_DATA_STATE_RUN.equalsIgnoreCase(name))
        {
            return new EnumTableInfo<>(
                    TargetedMSRun.RepresentativeDataState.class,
                    getSchema(),
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
        if (TABLE_RESPRESENTATIVE_DATA_STATE.equalsIgnoreCase(name))
        {
            EnumTableInfo tableInfo = new EnumTableInfo<>(
                    RepresentativeDataState.class,
                    getSchema(),
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
            result.getColumn("RunId").setFk(new QueryForeignKey(this, TABLE_TARGETED_MS_RUNS, "File", null));
            result.getColumn("RepresentativeDataState").setFk(new QueryForeignKey(this, TargetedMSSchema.TABLE_RESPRESENTATIVE_DATA_STATE, "RowId", null));
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
            noteAnnotation.setLabel("Note/Annotations");
            result.addColumn(noteAnnotation);

            return result;
        }

        // Tables that have a FK to targetedms.Runs
        if (TABLE_TRANSITION_INSTRUMENT_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) ||
            TABLE_INSTRUMENT.equalsIgnoreCase(name) ||
            TABLE_ISOTOPE_ENRICHMENT.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name) ||
            TABLE_REPLICATE.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_FULL_SCAN_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_PREDICITION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_RETENTION_TIME_PREDICTION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_RUN_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_RUN_ISOTOPE_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_ISOTOPE_LABEL.equalsIgnoreCase(name) ||
            TABLE_MODIFICATION_SETTINGS.equalsIgnoreCase(name) ||
            TABLE_LIBRARY_SETTINGS.equals(name) ||
            TABLE_RUN_ENZYME.equals(name) ||
            TABLE_SPECTRUM_LIBRARY.equalsIgnoreCase(name) ||
            TABLE_ANNOTATION_SETTINGS.equalsIgnoreCase(name) )
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.RunFK.getSQL());
        }

        // Tables that have a FK to targetedms.peptidegroup
        if (TABLE_PEPTIDE.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name),
                                                                  this,
                                                                  ContainerJoinType.PeptideGroupFK.getSQL(),
                                                                  TargetedMSManager.getTableInfoPeptideAnnotation(),
                                                                  "PeptideId",
                                                                  "Peptide Annotations")
            {
                @Override
                public FieldKey getContainerFieldKey()
                {
                    return FieldKey.fromParts("PeptideGroupId", "RunId", "Folder");
                }
            };
            result.getColumn("PeptideGroupId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getTable(TABLE_PEPTIDE_GROUP);
                }
            });
            final DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()),
                                                                  Collections.singletonMap("id", "Id"));
            result.setDetailsURL(detailsURL);
            List<FieldKey> defaultCols = new ArrayList<>(result.getDefaultVisibleColumns());
            defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
            defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
            defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
            defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
            result.setDefaultVisibleColumns(defaultCols);

            // Add a WrappedColumn for Note & Annotations
            WrappedColumn noteAnnotation = new WrappedColumn(result.getColumn("Annotations"), "NoteAnnotations");
            noteAnnotation.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new AnnotationUIDisplayColumn(colInfo);
                }
            });
            noteAnnotation.setLabel("Note/Annotations");
            result.addColumn(noteAnnotation);

            ColumnInfo sequenceColumn = result.getColumn("Sequence");
            sequenceColumn.setURL(detailsURL);

            ColumnInfo modifiedSequenceColumn = result.getColumn("PeptideModifiedSequence");
            modifiedSequenceColumn.setDisplayColumnFactory( new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new ModifiedPeptideDisplayColumn(colInfo, detailsURL.getActionURL(), true);
                }
            });

            SQLFragment currentLibPrecursorCountSQL = new SQLFragment("(SELECT COUNT(p.Id) FROM ");
            currentLibPrecursorCountSQL.append(TargetedMSManager.getTableInfoPrecursor(), "p");
            currentLibPrecursorCountSQL.append(" WHERE p.PeptideId = ");
            currentLibPrecursorCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
            currentLibPrecursorCountSQL.append(".Id");
            currentLibPrecursorCountSQL.append(" AND p.RepresentativeDataState = ?");
            currentLibPrecursorCountSQL.add(RepresentativeDataState.Representative.ordinal());
            currentLibPrecursorCountSQL.append(")");
            ExprColumn currentLibPrecursorCountCol = new ExprColumn(result, "RepresentivePrecursorCount", currentLibPrecursorCountSQL, JdbcType.INTEGER);
            currentLibPrecursorCountCol.setLabel("Library Precursor Count");
            result.addColumn(currentLibPrecursorCountCol);

            return result;
        }

        if (TABLE_PROTEIN.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideGroupFK.getSQL());
        }

        // Tables that have a FK to targetedms.replicate
        if (TABLE_SAMPLE_FILE.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.ReplicateFK.getSQL());
        }

        if (TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideFK.getSQL());
            // Add a link to view the chromatogram an individual transition
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PeptideChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }

        // Tables that have a FK to targetedms.peptidechrominfo
        if (TABLE_PEPTIDE_AREA_RATIO.equals(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideChromInfoFK.getSQL());
        }

        // Tables that have a FK to targetedms.peptide
        if (TABLE_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo(this);
        }

        if(TABLE_EXPERIMENT_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.ExperimentPrecursorTableInfo(this);
        }

        if(TABLE_LIBRARY_PRECURSOR.equalsIgnoreCase(name) || TABLE_LIBRARY_DOC_PRECURSOR.equalsIgnoreCase(name))
        {
            return new PrecursorTableInfo.LibraryPrecursorTableInfo(this);
        }
        if (TABLE_PEPTIDE_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_ISOTOPE_MODIFICATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PeptideFK.getSQL());
        }

        // Tables that have a FK to targetedms.precursor
        if (TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PrecursorFK.getSQL());
            // Add a link to view the chromatogram for all of the precursor's transitions
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }
        if (TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            return new DocTransitionsTableInfo(this);
        }
        if (TABLE_PRECURSOR_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_LIB_INFO.equalsIgnoreCase(name)
            )
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PrecursorFK.getSQL());
        }

        // Tables that have a FK to targetedms.precursorchrominfo
        if (TABLE_PRECURSOR_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_AREA_RATIO.equals(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.PrecursorChromInfoFK.getSQL());
        }

        // Tables that have a FK to targetedms.transition
        if (TABLE_TRANSITION_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.TransitionFK.getSQL());
            // Add a link to view the chromatogram an individual transition
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.TransitionChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }

        if (TABLE_TRANSITION_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.TransitionFK.getSQL());
        }

        // Tables that have a FK to targetedms.transitionchrominfo
        if (TABLE_TRANSITION_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_TRANSITION_AREA_RATIO.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), this, ContainerJoinType.TransitionChromInfoFK.getSQL());
        }

        // TODO - handle filtering for annotation, predictor

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
        if(TABLE_PRECURSOR.equalsIgnoreCase(queryName))
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
        hs.add(TABLE_PRECURSOR);
        hs.add(TABLE_TRANSITION);
        hs.add(TABLE_REPLICATE);
        hs.add(TABLE_INSTRUMENT);
        hs.add(TABLE_ISOTOPE_ENRICHMENT);
        hs.add(TABLE_PEPTIDE_CHROM_INFO);
        hs.add(TABLE_PEPTIDE_ANNOTATION);
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
        hs.add(TABLE_RESPRESENTATIVE_DATA_STATE_RUN);
        hs.add(TABLE_RESPRESENTATIVE_DATA_STATE);
        hs.add(TABLE_IRT_PEPTIDE);
        hs.add(TABLE_IRT_SCALE);
        return hs;
    }
}
