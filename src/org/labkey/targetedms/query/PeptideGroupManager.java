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
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        return new TableSelector(TargetedMSManager.getTableInfoPeptideGroup(), new SimpleFilter(FieldKey.fromParts("Id"), peptideGroupId), null).getObject(PeptideGroup.class);
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

    public static void updateRepresentativeStatus(int[] peptideGroupIds, RepresentativeDataState representativeState)
    {
        if(peptideGroupIds == null || peptideGroupIds.length == 0)
            return;

        StringBuilder peptideGroupIdsString = new StringBuilder();
        for(int id: peptideGroupIds)
        {
            peptideGroupIdsString.append(",").append(id);
        }
        if(peptideGroupIdsString.length() > 0)
            peptideGroupIdsString.deleteCharAt(0);

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ? ");
        sql.add(representativeState.ordinal());
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPeptideGroup()+".Id IN (");
        sql.append(peptideGroupIdsString.toString());
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema(), sql).execute();
    }

    // Set to either NotRepresentative or Representative_Deprecated.
    // If the original status was Representative it will be updated to Representative_Deprecated.
    // If the original status was Conflicted it will be update to NotRepresentative.
    public static void updateStatusToDeprecatedOrNotRepresentative(int[] peptideGroupIds)
    {
        if(peptideGroupIds == null || peptideGroupIds.length == 0)
            return;

        StringBuilder peptideGroupIdsString = new StringBuilder();
        for(int id: peptideGroupIds)
        {
            peptideGroupIdsString.append(",").append(id);
        }
        if(peptideGroupIdsString.length() > 0)
            peptideGroupIdsString.deleteCharAt(0);

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ");
        sql.append(" CASE WHEN RepresentativeDataState = "+RepresentativeDataState.Conflicted.ordinal());
        sql.append(" THEN "+RepresentativeDataState.NotRepresentative.ordinal());
        sql.append(" ELSE "+RepresentativeDataState.Deprecated.ordinal());
        sql.append(" END");
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPeptideGroup()+".Id IN (");
        sql.append(peptideGroupIdsString.toString());
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema(), sql).execute();
    }

    public static List<PeptideGroup> getRepresentativePeptideGroups(int runId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), runId);
        filter.addCondition(FieldKey.fromParts("RepresentativeDataState"), RepresentativeDataState.Representative.ordinal());

        Collection<PeptideGroup> groups = new TableSelector(TargetedMSManager.getTableInfoPeptideGroup(), filter, null).getCollection(PeptideGroup.class);
        return new ArrayList<PeptideGroup>(groups);
    }

    public static PeptideGroup getLastDeprecatedPeptideGroup(PeptideGroup pepGrp, Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT pepgrp.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.container = ?");
        sql.add(container);
        sql.append(" AND pepgrp.runId = run.Id");
        sql.append(" AND pepgrp.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Deprecated.ordinal());
        sql.append(" AND (");
        if(pepGrp.getSequenceId() != null)
        {
            sql.append("pepgrp.SequenceId = ?");
            sql.add(pepGrp.getSequenceId());
            sql.append(" OR ");
        }
        sql.append(" pepgrp.Label = ?");
        sql.add(pepGrp.getLabel());
        sql.append(")");
        sql.append(" ORDER BY pepgrp.Modified DESC ");

        PeptideGroup[] deprecatedGroups = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(PeptideGroup.class);
        if(deprecatedGroups == null || deprecatedGroups.length == 0)
        {
            return null;
        }
        return deprecatedGroups[0];
    }

    public static int setRepresentativeState(int runId, RepresentativeDataState state)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ?");
        sql.add(state.ordinal());
        sql.append(" WHERE runId = ?");
        sql.add(runId);
        return new SqlExecutor(TargetedMSManager.getSchema(), sql).execute();
    }

    public static boolean ensureContainerMembership(int[] peptideGroupIds, Container container)
    {
        if(peptideGroupIds == null || peptideGroupIds.length == 0)
            return false;

        StringBuilder pepGrpIds = new StringBuilder();
        for(int id: peptideGroupIds)
        {
            pepGrpIds.append(",").append(id);
        }
        if(pepGrpIds.length() > 0)
            pepGrpIds.deleteCharAt(0);
        SQLFragment sql = new SQLFragment("SELECT COUNT(pg.Id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("pg.RunId = r.Id AND r.Container = ? AND pg.Id IN (" + pepGrpIds + ")");
        sql.add(container.getId());

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count == peptideGroupIds.length;
    }
}
