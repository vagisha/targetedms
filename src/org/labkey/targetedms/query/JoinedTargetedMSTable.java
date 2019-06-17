/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flattens a "general" and a "specific" table into a single query. For example, can be used to present a single
 * Peptides table to the user, which is backed by the combined set of columns from the GeneralMolecule and Peptide
 * tables.
 * Created by Josh on 1/22/2016.
 */
public class JoinedTargetedMSTable extends AnnotatedTargetedMSTable
{
    private final TableInfo _specializedTable;

    public JoinedTargetedMSTable(TableInfo generalTable, TableInfo specializedTable, TargetedMSSchema schema, ContainerFilter cf, TargetedMSSchema.ContainerJoinType joinType, TableInfo annotationTableInfo, String annotationFKName, String columnName, String annotationTarget, boolean omitAnnotations)
    {
        super(generalTable, schema, cf, joinType, annotationTableInfo, annotationFKName, columnName, annotationTarget, omitAnnotations);

        _specializedTable = specializedTable;
        setName(_specializedTable.getName());

        Set<String> currentColumnNames = getColumnNameSet();

        for (ColumnInfo col: _specializedTable.getColumns())
        {
            if (!currentColumnNames.contains(col.getName()))
            {
                var ret = new AliasedColumn(this, col.getName(), col);
                if (col.isHidden())
                {
                    ret.setHidden(true);
                }
                propagateKeyField(col, ret);
                addColumn(ret);
            }
        }
    }

    @Override
    @NotNull
    public SQLFragment getFromSQL(String alias)
    {
        // Hack for issue 26146 - the filter may include columns that aren't on the "real" table (that is, the more
        // generalized table), which can cause bad SQL generation by trying to use a WHERE clause on a column
        // that doesn't exist. Thus, remove filters prior to calling super.getFromSQL() and apply them directly here.
        SimpleFilter realFilter = getFilter();
        List<SimpleFilter.FilterClause> clauses = new ArrayList<>(realFilter.getClauses());
        for (FieldKey fieldKey : realFilter.getAllFieldKeys())
        {
            realFilter.deleteConditions(fieldKey);
        }

        SQLFragment result = new SQLFragment("(SELECT * FROM (SELECT ");
        String separator = "";
        for (ColumnInfo columnInfo : getRealTable().getColumns())
        {
            result.append(separator);
            result.append("G.");
            result.append(columnInfo.getName());
            separator = ", ";
        }
        for (ColumnInfo columnInfo : _specializedTable.getColumns())
        {
            // Avoid duplicate column names
            if (getRealTable().getColumn(columnInfo.getName()) == null)
            {
                result.append(separator);
                result.append("S.");
                result.append(columnInfo.getName());
                separator = ", ";
            }
        }
        result.append("\n FROM ");
        result.append(super.getFromSQL("G"));
        result.append(" INNER JOIN ");
        result.append(_specializedTable.getFromSQL("S"));

        // Join based on the PKs on both tables
        result.append(" ON G.");
        result.append(getRealTable().getPkColumnNames().get(0));
        result.append(" = S.");
        result.append(_specializedTable.getPkColumnNames().get(0));
        result.append(") i ");

        // Re-add the clauses, but only the ones associated with a FieldKey because the others wouldn't have been removed above
        for (SimpleFilter.FilterClause clause : clauses)
        {
            if (!clause.getFieldKeys().isEmpty())
            {
                realFilter.addClause(clause);
            }
        }

        // Append them to the generated SQL
        Map<FieldKey, ColumnInfo> columnMap = Table.createColumnMap(getFromTable(), getFromTable().getColumns());
        SQLFragment filterFrag = realFilter.getSQLFragment(_rootTable.getSqlDialect(), columnMap);
        result.append("\n").append(filterFrag).append(") ").append(alias);


        return result;
    }

    @Override
    public String getSelectName()
    {
        return null; //returning null so that the 'tableinfo' tables are selected and not the 'general' tables in the queries.
    }
}
