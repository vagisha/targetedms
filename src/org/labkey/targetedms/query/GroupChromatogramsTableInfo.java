/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.PeptideGroup;

/**
 * User: vsharma
 * Date: 4/29/12
 * Time: 3:31 PM
 */
public class GroupChromatogramsTableInfo extends FilteredTable<TargetedMSSchema>
{
    private final TargetedMSController.ChromatogramForm _form;
    private PeptideGroup _group;

    public GroupChromatogramsTableInfo(TargetedMSSchema schema, TargetedMSController.ChromatogramForm form)
    {
        super(TargetedMSManager.getTableInfoSampleFile(), schema);
        _form = form;

        setName(TargetedMSSchema.TABLE_GROUP_CHROM_INFO);

        //wrap all the columns
        wrapAllColumns(true);

        var col = getMutableColumn("Id");
        col.setLabel("");

        ChromatogramDisplayColumnFactory colFactory = new ChromatogramDisplayColumnFactory(
                getContainer(),
                ChromatogramDisplayColumnFactory.Type.Group,
                form.getChartWidth(),
                form.getChartHeight(),
                (x) -> x.addParameter("groupId", _group.getId()),
                form.isSyncY(),
                form.isSyncX(),
                false,
                form.isShowOptimizationPeaks(),
                form.getAnnotationsFilter(),
                form.getReplicatesFilter());
        col.setDisplayColumnFactory(colFactory);
    }

    public void addGroupFilter(PeptideGroup group)
    {
        _group = group;
        SQLFragment runIdSQL = new SQLFragment("ReplicateId IN (SELECT r.Id FROM ");
        runIdSQL.append(TargetedMSManager.getTableInfoReplicate(), "r");
        runIdSQL.append(" WHERE r.RunId = ?)");
        runIdSQL.add(group.getRunId());

        _form.appendReplicateFilters(runIdSQL, "ReplicateId");

        addCondition(runIdSQL, FieldKey.fromParts("ReplicateId"));
    }
}
