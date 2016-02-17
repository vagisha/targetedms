/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.ArrayList;
import java.util.Collections;

/**
 * User: vsharma
 * Date: 8/4/13
 * Time: 9:56 PM
 */
public class PrecursorTableInfo extends AnnotatedTargetedMSTable
{
    public PrecursorTableInfo(final TargetedMSSchema schema)
    {
        this(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_PRECURSOR, schema);
    }

    public PrecursorTableInfo(final TableInfo tableInfo, String tableName, final TargetedMSSchema schema)
    {
        super(tableInfo,
                schema,
                TargetedMSSchema.ContainerJoinType.PeptideFK.getSQL(),
                TargetedMSManager.getTableInfoPrecursorAnnotation(),
                "PrecursorId",
                "Precursor Annotations");

        setName(tableName);

        final DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class,
                                                                    getContainer()),
                                                      Collections.singletonMap("id", "Id"));
        detailsURLs.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Folder")));
        setDetailsURL(detailsURLs);

        ColumnInfo peptideCol = getColumn("PeptideId");
        peptideCol.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE);
            }
        });

        getColumn("RepresentativeDataState").setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return getUserSchema().getTable(TargetedMSSchema.TABLE_RESPRESENTATIVE_DATA_STATE);
            }
        });

        getColumn("IsotopeLabelId").setFk(new QueryForeignKey(getUserSchema(), null, TargetedMSSchema.TABLE_ISOTOPE_LABEL, "Id", null));


        SQLFragment transitionCountSQL = new SQLFragment("(SELECT COUNT(t.Id) FROM ");
        transitionCountSQL.append(TargetedMSManager.getTableInfoTransition(), "t");
        transitionCountSQL.append(" WHERE t.PrecursorId = ");
        transitionCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        transitionCountSQL.append(".Id)");
        ExprColumn transitionCountCol = new ExprColumn(this, "TransitionCount", transitionCountSQL, JdbcType.INTEGER);
        addColumn(transitionCountCol);

        WrappedColumn modSeqCol = new WrappedColumn(getColumn("ModifiedSequence"), ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME);
        modSeqCol.setLabel("Precursor");
        modSeqCol.setDescription("Modified precursor sequence");
        modSeqCol.setDisplayColumnFactory( new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ModifiedSequenceDisplayColumn.PrecursorCol(colInfo, detailsURLs.getActionURL());
            }
        });
        addColumn(modSeqCol);


        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();

        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "NoteAnnotations"));

        visibleColumns.add(FieldKey.fromParts("PeptideId", ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "NumMissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "CalcNeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Rank"));


        visibleColumns.add(FieldKey.fromParts(ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("IsotopeLabelId", "Name"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("NeutralMass"));
        visibleColumns.add(FieldKey.fromParts("TransitionCount"));
        visibleColumns.add(FieldKey.fromParts("CollisionEnergy"));
        visibleColumns.add(FieldKey.fromParts("DeclusteringPotential"));

        setDefaultVisibleColumns(visibleColumns);

        // Create a WrappedColumn for Note & Annotations
        WrappedColumn noteAnnotation = new WrappedColumn(getColumn("Annotations"), "NoteAnnotations");
        noteAnnotation.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AnnotationUIDisplayColumn(colInfo);
            }
        });
        noteAnnotation.setLabel("Precursor Note/Annotations");
        addColumn(noteAnnotation);
    }

    public void setRunId(int runId)
    {
        addRunFilter(runId);
    }

    private void addRunFilter(int runId)
    {
        getFilter().deleteConditions(FieldKey.fromParts("Run"));
        SQLFragment sql = new SQLFragment();
        sql.append("Id IN ");
        sql.append("(SELECT prec.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(" ON (prec.PeptideId=pep.Id) ");
        sql.append("INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (pep.PeptideGroupId=pg.Id) ");
        sql.append("WHERE pg.RunId=? ");
        sql.append(")");

        sql.add(runId);

        addCondition(sql, FieldKey.fromParts("Run"));
    }

    public static class ExperimentPrecursorTableInfo extends PrecursorTableInfo
    {
        public ExperimentPrecursorTableInfo(final TargetedMSSchema schema)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR, schema);
        }

        @Override
        public String getName()
        {
            return TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR;
        }
    }

    public static class LibraryPrecursorTableInfo extends PrecursorTableInfo
    {
        public LibraryPrecursorTableInfo(final TargetedMSSchema schema)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_LIBRARY_PRECURSOR, schema);
        }

        public void selectRepresentative()
        {
            SQLFragment sql = new SQLFragment();
            sql.append("RepresentativeDataState = ? ");
            sql.add(RepresentativeDataState.Representative.ordinal());
            addCondition(sql);
        }

        @Override
        public String getName()
        {
            return TargetedMSSchema.TABLE_LIBRARY_PRECURSOR;
        }
    }
}
