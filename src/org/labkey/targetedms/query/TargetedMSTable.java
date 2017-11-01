/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Collections;

/**
 * User: jeckels
 * Date: Apr 19, 2012
 */
public class TargetedMSTable extends FilteredTable<TargetedMSSchema>
{
    public static final String CONTAINER_COL_TABLE_ALIAS = "r";
    private static final SQLFragment _defaultContainerSQL = new SQLFragment(CONTAINER_COL_TABLE_ALIAS).append(".Container");

    private final SQLFragment _joinSQL;
    private final SQLFragment _containerSQL;

    private boolean _needsContainerWhereClause = true;

    private CompareType.EqualsCompareClause _containerTableFilter;

    /** Assumes that the table has its own container column, instead of needing to join to another table for container info */
    public TargetedMSTable(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL)
    {
        this(table, schema, joinSQL, _defaultContainerSQL);
    }

    public TargetedMSTable(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL, SQLFragment containerSQL)
    {
        super(table, schema);
        _joinSQL = joinSQL;
        _containerSQL = containerSQL;
        wrapAllColumns(true);

        // Swap out DbSchema FKs with Query FKs so that we get all the extra calculated columns and such
        for (ColumnInfo columnInfo : getColumns())
        {
            ForeignKey fk = columnInfo.getFk();
            if (fk != null && TargetedMSSchema.SCHEMA_NAME.equalsIgnoreCase(fk.getLookupSchemaName()))
            {
                columnInfo.setFk(new QueryForeignKey(schema, null, fk.getLookupTableName(), fk.getLookupColumnName(), fk.getLookupDisplayName()));
            }
        }
        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // Don't apply the container filter normally, let us apply it in our wrapper around the normally generated SQL
    }

    public void setNeedsContainerWhereClause(boolean needsContainerWhereClause)
    {
        _needsContainerWhereClause = needsContainerWhereClause;
    }

    @NotNull
    public SQLFragment getFromSQL(String alias)
    {
        SQLFragment sql = new SQLFragment("(SELECT X.* FROM ");
        sql.append(super.getFromSQL("X"));
        sql.append(" ");

        if (_needsContainerWhereClause || _containerTableFilter != null)
        {
            sql.append(_joinSQL);

            sql.append(" WHERE ");
            sql.append(getContainerFilter().getSQLFragment(getSchema(), _containerSQL, getContainer()));

            if(_containerTableFilter != null)
            {
                // Add another filter on the table that has the container column
                sql.append(" AND ");
                SQLFragment fragment = new SQLFragment(CONTAINER_COL_TABLE_ALIAS).append(".")
                                      .append(_containerTableFilter.toSQLFragment(Collections.emptyMap(),getSqlDialect()));
                sql.append(fragment);
            }
        }
        sql.append(") ");
        sql.append(alias);

        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return ReadPermission.class.equals(perm) && getContainer().hasPermission(user, perm);
    }

    /*
    This is an additional filter that is applied to the table that has the container column. This can be used, for example,
    to filter the results of the Precursor table to a single run in a container (Id column in the targetedms.runs table).
    Tables in the targetedms schema that have a container column are:
     - runs, autoqcping, irtscale, experimentannotations, guideset, qcannotation, qcannotationtype
     */
    public void addContainerTableFilter(CompareType.EqualsCompareClause filterClause)
    {
       _containerTableFilter = filterClause;
    }
}
