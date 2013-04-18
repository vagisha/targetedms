/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.labkey.api.data.IconDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.Collections;

/**
 * User: vsharma
 * Date: 5/10/12
 * Time: 3:54 PM
 */
public class DocPrecursorTableInfo extends AnnotatedTargetedMSTable
{
    public DocPrecursorTableInfo(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoPrecursor(),
                schema,
                TargetedMSSchema.ContainerJoinType.PeptideFK.getSQL(),
                TargetedMSManager.getTableInfoPrecursorAnnotation(),
                "PrecursorId",
                "Precursor Annotations");

        setName(TargetedMSSchema.TABLE_PRECURSOR);

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


        SQLFragment transitionCountSQL = new SQLFragment("(SELECT COUNT(t.Id) FROM ");
        transitionCountSQL.append(TargetedMSManager.getTableInfoTransition(), "t");
        transitionCountSQL.append(" WHERE t.PrecursorId = ");
        transitionCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        transitionCountSQL.append(".Id)");
        ExprColumn transitionCountCol = new ExprColumn(this, "TransitionCount", transitionCountSQL, JdbcType.INTEGER);
        addColumn(transitionCountCol);


        ColumnInfo modSeqCol = getColumn("ModifiedSequence");
        modSeqCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ModifiedPeptideDisplayColumn(colInfo, getRealTable().getColumn("Id"), detailsURLs.getActionURL());
            }
        });

        ColumnInfo chromatogramsLinkCol = wrapColumn("Chromatograms", getRealTable().getColumn("Id"));
        chromatogramsLinkCol.setIsUnselectable(true);
        chromatogramsLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ActionURL url = new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class, getContainer());
                return new IconDisplayColumn(colInfo, 18, 18, url, "id",
                                             AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroup.gif");
            }
        });
        addColumn(chromatogramsLinkCol);

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>();

        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RepresentativeDataState"));


        visibleColumns.add(FieldKey.fromParts("PeptideId", "Sequence"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "NumMissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "CalcNeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Rank"));


        visibleColumns.add(FieldKey.fromParts("ModifiedSequence"));
        visibleColumns.add(FieldKey.fromParts("Annotations"));
        visibleColumns.add(FieldKey.fromParts("IsotopeLabelId", "Name"));
        visibleColumns.add(FieldKey.fromParts("NeutralMass"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        visibleColumns.add(FieldKey.fromParts("RepresentativeDataState"));
        visibleColumns.add(FieldKey.fromParts("TransitionCount"));

        visibleColumns.add(FieldKey.fromParts("Chromatograms"));

        setDefaultVisibleColumns(visibleColumns);
    }

    public void setRunId(int runId)
    {
        addRunFilter(runId);
    }

    private void addRunFilter(int runId)
    {
        getFilter().deleteConditions("Run");
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
}
