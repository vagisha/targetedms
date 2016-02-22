package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasedColumn;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * Flattens a "general" and a "specific" table into a single query. For example, can be used to present a single
 * Peptides table to the user, which is backed by the combined set of columns from the GeneralMolecule and Peptide
 * tables.
 * Created by Josh on 1/22/2016.
 */
public class JoinedTargetedMSTable extends AnnotatedTargetedMSTable
{
    private final TableInfo _specializedTable;

    public JoinedTargetedMSTable(TableInfo generalTable, TableInfo specializedTable, TargetedMSSchema schema, SQLFragment containerSQL, TableInfo annotationTableInfo, String annotationFKName, String columnName)
    {
        super(generalTable, schema, containerSQL, annotationTableInfo, annotationFKName, columnName);

        _specializedTable = specializedTable;
        setName(_specializedTable.getName());

        for (ColumnInfo col: _specializedTable.getColumns())
        {
            if (getColumn(col.getName()) == null)
            {
                ColumnInfo ret = new AliasedColumn(this, col.getName(), col);
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
        SQLFragment result = new SQLFragment("(SELECT ");
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
        result.append(") ");

        result.append(alias);
        return result;
    }

    @Override
    public String getSelectName()
    {
        return null; //returning null so that the 'tableinfo' tables are selected and not the 'general' tables in the queries.
    }
}
