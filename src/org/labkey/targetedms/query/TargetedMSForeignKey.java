package org.labkey.targetedms.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * Created by Josh on 9/20/2016.
 */
public class TargetedMSForeignKey extends LookupForeignKey
{
    private final TargetedMSSchema _schema;
    private final String _tableName;

    public TargetedMSForeignKey(TargetedMSSchema schema, String tableName)
    {
        super("Id");
        _schema = schema;
        _tableName = tableName;
    }

    @Override
    public TableInfo getLookupTableInfo()
    {
        TableInfo table = _schema.getTable(_tableName);
        ((TargetedMSTable)table).setNeedsContainerWhereClause(false);
        return table;
    }
}
