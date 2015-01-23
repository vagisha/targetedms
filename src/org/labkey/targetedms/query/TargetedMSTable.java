/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * User: jeckels
 * Date: Apr 19, 2012
 */
public class TargetedMSTable extends FilteredTable<TargetedMSSchema>
{
    public static final String CONTAINER_COL_TABLE_ALIAS = "r";
    private static final SQLFragment _containerSQL = new SQLFragment(CONTAINER_COL_TABLE_ALIAS).append(".Container");

    private final SQLFragment _joinSQL;

    public TargetedMSTable(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL)
    {
        super(table, schema);
        _joinSQL = joinSQL;
        wrapAllColumns(true);
        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // Don't apply the container filter normally, let us apply it in our wrapper around the normally generated SQL
    }

    @NotNull
    public SQLFragment getFromSQL(String alias)
    {
        SQLFragment sql = new SQLFragment("(SELECT X.* FROM ");
        sql.append(super.getFromSQL("X"));
        sql.append(" ");
        sql.append(_joinSQL);
        sql.append(" WHERE ");
        sql.append(getContainerFilter().getSQLFragment(getSchema(), _containerSQL, getContainer()));
        sql.append(") ");
        sql.append(alias);

        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return ReadPermission.class.equals(perm) && getContainer().hasPermission(user, perm);
    }
}
