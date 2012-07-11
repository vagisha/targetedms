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

import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.AJAXDetailsDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
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
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.query.AnnotatedTargetedMSTable;
import org.labkey.targetedms.query.DocPrecursorTableInfo;
import org.labkey.targetedms.query.DocTransitionsTableInfo;
import org.labkey.targetedms.query.ModifiedPeptideDisplayColumn;
import org.labkey.targetedms.query.TargetedMSTable;

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
    public static final String TABLE_PRECURSOR_ANNOTATION = "PrecursorAnnotation";
    public static final String TABLE_TRANSITION = "Transition";
    public static final String TABLE_TRANSITION_ANNOTATION = "TransitionAnnotation";
    public static final String TABLE_TRANSITION_CHROM_INFO = "TransitionChromInfo";
    public static final String TABLE_TRANSITION_CHROM_INFO_ANNOTATION = "TransitionChromInfoAnnotation";
    public static final String TABLE_PEPTIDE_CHROM_INFO = "PeptideChromInfo";
    public static final String TABLE_PRECURSOR_CHROM_INFO = "PrecursorChromInfo";
    public static final String TABLE_PRECURSOR_CHROM_INFO_ANNOTATION = "PrecursorChromInfoAnnotation";
    public static final String TABLE_MODIFICATION_SETTINGS = "ModificationSettings";
    public static final String TABLE_ISOTOPE_LABEL = "IsotopeLabel";
    public static final String TABLE_STRUCTURAL_MODIFICATION = "StructuralModification";
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

    public static final String TABLE_DOC_TRANSITIONS = "DocumentTransitions";
    public static final String TABLE_DOC_PRECURSORS = "DocumentPrecursors";

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

        if (_restrictContainer)
        {
            _containerFilter = ContainerFilter.CURRENT;
        }
        else
        {
            _containerFilter = new ContainerFilter.CurrentAndSubfolders(user);
        }

        _expSchema = new ExpSchema(user, container);
        _expSchema.setContainerFilter(_containerFilter);
    }

    public DbSchema getSchema()
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

        if(_restrictContainer)
        {
            result.setContainerFilter(_containerFilter);
        }

        // Add a lookup column to the Runs table in targetedms schema
        SQLFragment sql = new SQLFragment("(SELECT MIN(tmsRuns.Id)\n" +
                "\nFROM " + TargetedMSManager.getTableInfoRuns() + " tmsRuns " +
                "\nWHERE tmsRuns.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID AND tmsRuns.Deleted = ?)");
        sql.add(Boolean.FALSE);
        ColumnInfo skyDocDetailColumn = new ExprColumn(result, "File", sql, JdbcType.INTEGER);

        ActionURL url = TargetedMSController.getShowRunURL(getContainer());
        skyDocDetailColumn.setFk(new LookupForeignKey(url, "id", "Id", "FileName")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable(TargetedMSManager.getTableInfoRuns());
                result.addWrapColumn(result.getRealTable().getColumn("Id"));
                result.addWrapColumn(result.getRealTable().getColumn("Description"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("Path"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideGroupCount"));
                result.addWrapColumn(result.getRealTable().getColumn("PeptideCount"));
                result.addWrapColumn(result.getRealTable().getColumn("PrecursorCount"));
                result.addWrapColumn(result.getRealTable().getColumn("TransitionCount"));
                result.addWrapColumn(result.getRealTable().getColumn("Status"));

                return result;
            }
        });
        skyDocDetailColumn.setHidden(false);
        result.addColumn(skyDocDetailColumn);


        //adjust the default visible columns
        List<FieldKey> columns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
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

        // Tables that have a FK directly to targetedms.Runs
        if (TABLE_PEPTIDE_GROUP.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.RunFK.getSQL(), TargetedMSManager.getTableInfoPeptideGroupAnnotation(), "PeptideGroupId");
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
                    Map<String, FieldKey> params = new HashMap<String, FieldKey>();
                    params.put("id", seqIdFK);
                    JSONObject props = new JSONObject();
                    props.put("width", 450);
                    props.put("title", "Protein Details");
                    return new AJAXDetailsDisplayColumn(colInfo, new ActionURL(TargetedMSController.ShowProteinAJAXAction.class, getContainer()), params, props)
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
            result.getColumn("RunId").setFk(new LookupForeignKey("File")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getTable(TABLE_TARGETED_MS_RUNS);
                }
            });
            return result;
        }

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
            TABLE_SPECTRUM_LIBRARY.equalsIgnoreCase(name)
            )
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.RunFK.getSQL());
        }

        // Tables that have a FK to targetedms.peptidegroup
        if (TABLE_PEPTIDE.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PeptideGroupFK.getSQL(), TargetedMSManager.getTableInfoPeptideAnnotation(), "PeptideId");
            result.getColumn("PeptideGroupId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getTable(TABLE_PEPTIDE_GROUP);
                }
            });
            DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()),
                                                                  Collections.singletonMap("id", "Id"));
            result.setDetailsURL(detailsURLs);
            List<FieldKey> defaultCols = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
            defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
            defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "Label"));
            defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
            result.setDefaultVisibleColumns(defaultCols);
            ColumnInfo labelColumn = result.getColumn("Sequence");
            labelColumn.setURL(detailsURLs);
            return result;
        }

        if (TABLE_PEPTIDE.equalsIgnoreCase(name) ||
            TABLE_PROTEIN.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_GROUP_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PeptideGroupFK.getSQL());
        }

        // Tables that have a FK to targetedms.replicate
        if (TABLE_SAMPLE_FILE.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.ReplicateFK.getSQL());
        }

        if (TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PeptideFK.getSQL());
            // Add a link to view the chromatogram an individual transition
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PeptideChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }

        // Tables that have a FK to targetedms.peptide
        if (TABLE_PRECURSOR.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PeptideFK.getSQL(), TargetedMSManager.getTableInfoPrecursorAnnotation(), "PrecursorId");
            result.getColumn("PeptideId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getTable(TABLE_PEPTIDE);
                }
            });
            final DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class,
                                                                        getContainer()),
                                                          Collections.singletonMap("id", "Id"));
            result.setDetailsURL(detailsURLs);
            ColumnInfo modPepCol = result.wrapColumn("ModifiedPeptideHtml", result.getRealTable().getColumn("Id"));
            DisplayColumnFactory modPepDisplayFactory = new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new ModifiedPeptideDisplayColumn(colInfo, detailsURLs.getActionURL());
                }
            };
            modPepCol.setDisplayColumnFactory(modPepDisplayFactory);
            result.addColumn(modPepCol);
            return result;
        }

        if (TABLE_PRECURSOR.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_CHROM_INFO.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_STRUCTURAL_MODIFICATION.equalsIgnoreCase(name) ||
            TABLE_PEPTIDE_ISOTOPE_MODIFICATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PeptideFK.getSQL());
        }

        if (TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new AnnotatedTargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PrecursorFK.getSQL(), TargetedMSManager.getTableInfoTransitionAnnotation(), "TransitionId");
            result.getColumn("PrecursorId").setFk(new LookupForeignKey("Id")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getTable(TABLE_PRECURSOR);
                }
            });
            return result;
        }

        if (TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PrecursorFK.getSQL());
            // Add a link to view the chromatogram for all of the precursor's transitions
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }

        // Tables that have a FK to targetedms.precursor
        if (TABLE_TRANSITION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_CHROM_INFO.equalsIgnoreCase(name) ||
            TABLE_PRECURSOR_LIB_INFO.equalsIgnoreCase(name) ||
            TABLE_TRANSITION.equalsIgnoreCase(name)
            )
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PrecursorFK.getSQL());
        }

        // Tables that have a FK to targetedms.precursorchrominfo
        if (TABLE_PRECURSOR_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.PrecursorChromInfoFK.getSQL());
        }

        // Tables that have a FK to targetedms.transition
        if (TABLE_TRANSITION_CHROM_INFO.equalsIgnoreCase(name))
        {
            TargetedMSTable result = new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.TransitionFK.getSQL());
            // Add a link to view the chromatogram an individual transition
            result.setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.TransitionChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
            return result;
        }

        if (TABLE_TRANSITION_ANNOTATION.equalsIgnoreCase(name) ||
            TABLE_TRANSITION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.TransitionFK.getSQL());
        }

        // Tables that have a FK to targetedms.transitionchrominfo
        if (TABLE_TRANSITION_CHROM_INFO_ANNOTATION.equalsIgnoreCase(name))
        {
            return new TargetedMSTable(getSchema().getTable(name), getContainer(), ContainerJoinType.TransitionChromInfoFK.getSQL());
        }

        // TODO - handle filtering for annotation, predictor

        if(TABLE_DOC_TRANSITIONS.equalsIgnoreCase(name))
        {
            return new DocTransitionsTableInfo(this);
        }

        if(TABLE_DOC_PRECURSORS.equalsIgnoreCase(name))
        {
            return new DocPrecursorTableInfo(this);
        }

        if (getTableNames().contains(name))
        {
            FilteredTable result = new FilteredTable(getSchema().getTable(name), getContainer());
            result.wrapAllColumns(true);
            return result;
        }

        return null;
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
        hs.add(TABLE_TRANSITION_ANNOTATION);
        hs.add(TABLE_TRANSITION_CHROM_INFO);
        hs.add(TABLE_TRANSITION_FULL_SCAN_SETTINGS);
        hs.add(TABLE_TRANSITION_PREDICITION_SETTINGS);
        hs.add(TABLE_PRECURSOR_CHROM_INFO);
        hs.add(TABLE_PRECURSOR_ANNOTATION);
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
        return hs;
    }
}
