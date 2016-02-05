/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class PrecursorChromInfoTable extends TargetedMSTable
{
    public PrecursorChromInfoTable(TableInfo table, TargetedMSSchema schema)
    {
        super(table, schema, TargetedMSSchema.ContainerJoinType.PrecursorFK.getSQL());

        // Add a calculated column for Full Width at Base (FWB)
        SQLFragment sql = new SQLFragment("(MaxEndTime - MinStartTime)");
        ExprColumn col = new ExprColumn(this, "MaxFWB", sql, JdbcType.DOUBLE);
        col.setDescription("Full Width at Base (FWB)");
        addColumn(col);

        // Add a calculated column for sum of area for fragment type equal 'precursor'
        sql = new SQLFragment("(SELECT SUM(PrecursorArea) FROM ");
        sql.append("(SELECT CASE WHEN FragmentType = 'precursor' THEN Area ELSE 0 END AS PrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TotalPrecursorArea", sql, JdbcType.DOUBLE);
        addColumn(col);

        // Add a calculated column for sum of area for fragment type not equal 'precursor'
        sql = new SQLFragment("(SELECT SUM(NonPrecursorArea) FROM ");
        sql.append("(SELECT CASE WHEN FragmentType != 'precursor' THEN Area ELSE 0 END AS NonPrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TotalNonPrecursorArea", sql, JdbcType.DOUBLE);
        addColumn(col);

        // Add a calculated column for the ratio of the transitions' areas (TotalNonPrecursorArea) to the precursor's area (TotalPrecursorArea)
        sql = new SQLFragment("(SELECT CASE WHEN SUM(PrecursorArea) = 0 THEN NULL ELSE SUM(NonPrecursorArea) / SUM(PrecursorArea) END FROM ");
        sql.append("(SELECT CASE WHEN FragmentType = 'precursor' THEN Area ELSE 0 END AS PrecursorArea, ");
        sql.append("CASE WHEN FragmentType != 'precursor' THEN Area ELSE 0 END AS NonPrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TransitionPrecursorRatio", sql, JdbcType.DOUBLE);
        col.setDescription("Transition/Precursor Area Ratio");
        addColumn(col);

        // Add a link to view the chromatogram for all of the precursor's transitions
        setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));
    }

    private SQLFragment getTransitionJoinSQL()
    {
        SQLFragment sql = new SQLFragment();
        sql.append(TargetedMSManager.getTableInfoTransition(), "t");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE tci.TransitionId = t.Id AND tci.PrecursorChromInfoId = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".Id) X)");
        return sql;
    }
}
