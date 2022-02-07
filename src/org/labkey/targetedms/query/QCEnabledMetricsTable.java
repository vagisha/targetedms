/*
 * Copyright (c) 2019 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.Map;

public class QCEnabledMetricsTable extends FilteredTable<TargetedMSSchema>
{
    public QCEnabledMetricsTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableInfoQCEnabledMetrics(), schema, cf);
        TargetedMSTable.fixupLookups(this);
        wrapAllColumns(true);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        if (filter.getType() == ContainerFilter.Type.Current)
            filter = getDefaultMetricContainerFilter(getUserSchema().getContainer(), getUserSchema().getUser());

        super.applyContainerFilter(filter);
    }

    public static ContainerFilter getDefaultMetricContainerFilter(Container c, User user)
    {
        // the base set of configuration live at the root container
        return new ContainerFilter.CurrentPlusExtras(c, user, ContainerManager.getRoot());
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        Class<? extends Permission> permissionToCheck = perm == ReadPermission.class ? ReadPermission.class : AdminPermission.class;
        return getContainer().hasPermission(user, permissionToCheck);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> _insert(User user, Container c, Map<String, Object> row) throws SQLException, ValidationException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                return super._insert(user, c, row);
            }

            @Override
            protected Map<String, Object> _update(User user, Container c, Map<String, Object> row, Map<String, Object> oldRow, Object[] keys) throws SQLException, ValidationException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                return super._update(user, c, row, oldRow, keys);
            }

            @Override
            protected void _delete(Container c, Map<String, Object> row) throws InvalidKeyException
            {
                TargetedMSManager.get().clearCachedEnabledQCMetrics(c);
                super._delete(c, row);
            }
        };
    }
}