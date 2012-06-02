package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;

/**
 * User: jeckels
 * Date: Apr 19, 2012
 */
public class TargetedMSTable extends FilteredTable
{
    private final SQLFragment _containerSQL;
    private static final String CONTAINER_FAKE_COLUMN_NAME = "Container";

    public TargetedMSTable(TableInfo table, Container container, SQLFragment containerSQL)
    {
        super(table, container);
        _containerSQL = containerSQL;
        wrapAllColumns(true);
        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        clearConditions(CONTAINER_FAKE_COLUMN_NAME);
        if (_containerSQL != null)
        {
            addCondition(filter.getSQLFragment(getSchema(), _containerSQL, getContainer()), CONTAINER_FAKE_COLUMN_NAME);
        }
    }
}
