/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;

public class QCMetricExclusionTable extends TargetedMSTable
{
    public QCMetricExclusionTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION),
                schema, cf, TargetedMSSchema.ContainerJoinType.ReplicateFK);

        getMutableColumn(FieldKey.fromParts("MetricId")).setFk(new QueryForeignKey(schema, cf, schema, getUserSchema().getContainer(), TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, "Id", "Name")
        {
            @Override
            protected ContainerFilter getLookupContainerFilter()
            {
                return QCMetricConfigurationTable.getDefaultMetricContainerFilter(getUserSchema().getContainer());
            }
        });
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        if(TargetedMSManager.getFolderType(getContainer()) == TargetedMSModule.FolderType.QC)
        {
            // Allow edits, deletes and inserts only in QC folder types
            return getContainer().hasPermission(user, perm);
        }
        return super.hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
