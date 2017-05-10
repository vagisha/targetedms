package org.labkey.targetedms.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class QCMetricConfigurationTable extends FilteredTable<TargetedMSSchema>
{
    public QCMetricConfigurationTable(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoQCMetricConfiguration(), schema);
        wrapAllColumns(true);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        if (filter.equals(ContainerFilter.CURRENT))
            filter = getDefaultMetricContainerFilter(getUserSchema().getUser());

        super.applyContainerFilter(filter);
    }

    public static ContainerFilter getDefaultMetricContainerFilter(User user)
    {
        // the base set of configuration live at the root container
        return new ContainerFilter.CurrentPlusExtras(user, ContainerManager.getRoot());
    }
}
