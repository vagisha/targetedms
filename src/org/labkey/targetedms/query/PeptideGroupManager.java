/*
 * Copyright (c) 2012 LabKey Corporation
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


import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.PeptideGroup;

import java.sql.ResultSet;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 9:12 PM
 */
public class PeptideGroupManager
{
    private PeptideGroupManager() {}

    public static PeptideGroup get(int peptideGroupId)
    {
        return Table.selectObject(TargetedMSManager.getTableInfoPeptideGroup(), peptideGroupId, PeptideGroup.class);
    }

    public static PeptideGroup getPeptideGroup(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pg.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pg.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(PeptideGroup.class);
    }

    public static void updateRepresentativeStatus(int[] peptideGroupIds, boolean status)
    {
        if(peptideGroupIds == null || peptideGroupIds.length == 0)
            return;

        StringBuilder selectedIdsString = new StringBuilder();
            for(int peptideGroupId: peptideGroupIds)
            {
                selectedIdsString.append(",").append(peptideGroupId);
            }
            if(selectedIdsString.length() > 0)
            {
                selectedIdsString.deleteCharAt(0);
                SQLFragment sql = new SQLFragment("UPDATE ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(" SET ActiveRepresentativeData = ? ");
                sql.add(status);
                sql.append(" WHERE pg.Id IN (");
                sql.append(selectedIdsString);
                sql.append(")");

                new SqlExecutor(TargetedMSManager.getSchema(), sql).execute();
            }
    }

    public static boolean hasRepresentativeData(int runId)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE pg.runID=?");
        sql.add(runId);
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count > 0;
    }
}
