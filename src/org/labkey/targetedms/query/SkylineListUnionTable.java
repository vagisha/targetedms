package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Unions together the contents of lists that have identical designs. This is important because a user may import
 * multiple versions of a Skyline document in a single folder, and each will have its own copy of any lists defined
 * therein. Those lists may be identical, or may differ. Since custom annotations on other data elements like
 * transitions and precursors can be defined as lookups to lists, we do our best to resolve them against the right
 * version of the list.
 *
 * We use the most recently imported copy of a list of a given name as the canonical design of the list. For any
 * other lists with the same name and identical design, we union their results into a single query using this class.
 * Annotations can then do a two-column join based on the RunId (unique to the Skyline file that was imported) and
 * the PK of the list. Thus, they resolve to the row in the union that came from the same file.
 *
 * If an older copy of the list doesn't match the design exactly, it's still available through the schema browser
 * as its own discrete query.
 */
public class SkylineListUnionTable extends VirtualTable<SkylineListSchema>
{
    private final List<SkylineListTable> _tables = new ArrayList<>();

    public SkylineListUnionTable(SkylineListSchema schema, SkylineListTable listTable)
    {
        super(schema.getDbSchema(), listTable._listDefinition.getUnionUserSchemaTableName(), schema);
        _tables.add(listTable);

        for (ColumnInfo childColumn : listTable.getColumns())
        {
            BaseColumnInfo column = new ExprColumn(this, childColumn.getFieldKey(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + " ." + childColumn.getAlias()), childColumn.getJdbcType());
            column.setKeyField(childColumn.isKeyField());
            addColumn(column);
        }
        setTitleColumn(listTable.getTitleColumn());

        // Use RunId so we can differentiate rows coming from different copies of the list
        if (getColumn("RunId") != null)
        {
            removeColumn(getColumn("RunId"));
        }
        BaseColumnInfo runIdColumn = new BaseColumnInfo(FieldKey.fromParts("RunId"), this, JdbcType.INTEGER);
        runIdColumn.setKeyField(true);
        runIdColumn.setFk(new QueryForeignKey.Builder(schema, schema.getDefaultContainerFilter()).schema(TargetedMSSchema.SCHEMA_NAME).table(TargetedMSSchema.TABLE_RUNS));
        addColumn(runIdColumn);
    }

    @Override
    public @NotNull SQLFragment getFromSQL()
    {
        String separator = "";
        SQLFragment result = new SQLFragment();
        for (SkylineListTable table : _tables)
        {
            result.append(separator);
            separator = "\nUNION\n";
            String innerAlias = "list" + table._listDefinition.getId();
            result.append(" SELECT ");
            result.append(table._listDefinition.getRunId()).append(" AS RunId ");
            for (ColumnInfo colInfo : table.getColumns())
            {
                result.append(",\n ");
                result.append(colInfo.getValueSql(innerAlias));
                result.append(" AS ");
                result.append(getSqlDialect().makeLegalIdentifier(colInfo.getAlias()));
            }
            result.append(" FROM ");
            result.append(table.getFromSQL(innerAlias));
        }
        return result;
    }

    public void addUnionTable(SkylineListTable skylineListTable)
    {
        _tables.add(skylineListTable);
    }
}
