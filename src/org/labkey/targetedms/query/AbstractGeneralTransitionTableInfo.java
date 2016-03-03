/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * User: binalpatel
 * Date: 02/25/2016
 */

public class AbstractGeneralTransitionTableInfo extends JoinedTargetedMSTable
{
    public AbstractGeneralTransitionTableInfo(final TargetedMSSchema schema, TableInfo tableInfo)
    {
        super(TargetedMSManager.getTableInfoGeneralTransition(), tableInfo,
                schema, TargetedMSSchema.ContainerJoinType.GeneralPrecursorFK.getSQL(),
                TargetedMSManager.getTableInfoTransitionAnnotation(), "TransitionId", "Annotations");
    }

    public void setRunId(int runId)
    {
        addRunFilter(runId);
    }

    public void addRunFilter(int runId)
    {
        getFilter().deleteConditions(FieldKey.fromParts("Run"));
        SQLFragment sql = new SQLFragment();
        sql.append("Id IN ");

        sql.append("(SELECT trans.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralTransition(), "trans");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "prec");
        sql.append(" ON (trans.GeneralPrecursorId=prec.Id) ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" ON (prec.GeneralMoleculeId=gm.Id) ");
        sql.append("INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (gm.PeptideGroupId=pg.Id) ");
        sql.append("WHERE pg.RunId=? ");
        sql.append(")");

        sql.add(runId);

        addCondition(sql, FieldKey.fromParts("Run"));
    }
}
