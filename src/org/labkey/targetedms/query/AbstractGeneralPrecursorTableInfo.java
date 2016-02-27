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
import org.labkey.api.util.ContainerContext;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.Collections;

/**
 * User: binalpatel
 * Date: 02/24/2016
 */

public class AbstractGeneralPrecursorTableInfo extends JoinedTargetedMSTable
{
    protected DetailsURL _detailsURL;

    public AbstractGeneralPrecursorTableInfo(final TableInfo tableInfo, String tableName, final TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoGeneralPrecursor(), tableInfo, schema,
        TargetedMSSchema.ContainerJoinType.GeneralMoleculeFK.getSQL(),
        TargetedMSManager.getTableInfoPrecursorAnnotation(),
        "Id", "Precursor Annotations");

        setName(tableName);
        // use the description and title column from the specialized TableInfo
        setDescription(tableInfo.getDescription());
        setTitleColumn(tableInfo.getTitleColumn());

        _detailsURL = new DetailsURL(new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class,
                                                                    getContainer()),
                                                      Collections.singletonMap("id", "Id"));
        _detailsURL.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("GeneralMoleculeId", "PeptideGroupId", "RunId", "Folder")));
        setDetailsURL(_detailsURL);

        getColumn("RepresentativeDataState").setFk(new LookupForeignKey()
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return getUserSchema().getTable(TargetedMSSchema.TABLE_REPRESENTATIVE_DATA_STATE);
            }
        });

        SQLFragment transitionCountSQL = new SQLFragment("(SELECT COUNT(gt.Id) FROM ");
        transitionCountSQL.append(TargetedMSManager.getTableInfoGeneralTransition(), "gt");
        transitionCountSQL.append(" WHERE gt.GeneralPrecursorId = ");
        transitionCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        transitionCountSQL.append(".Id)");
        ExprColumn transitionCountCol = new ExprColumn(this, "TransitionCount", transitionCountSQL, JdbcType.INTEGER);
        addColumn(transitionCountCol);

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
        sql.append("(SELECT gp.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" ON (gp.GeneralMoleculeId=gm.Id) ");
        sql.append("INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (gm.PeptideGroupId=pg.Id) ");
        sql.append("WHERE pg.RunId=? ");
        sql.append(")");

        sql.add(runId);

        addCondition(sql, FieldKey.fromParts("Run"));
    }

}
