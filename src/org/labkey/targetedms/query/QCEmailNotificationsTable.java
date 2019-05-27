package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class QCEmailNotificationsTable extends FilteredTable<TargetedMSSchema>
{
    public QCEmailNotificationsTable(@NotNull TargetedMSSchema userSchema, @Nullable ContainerFilter containerFilter)
    {
        super(TargetedMSManager.getTableInfoQCEmailNotifications(), userSchema, containerFilter);
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

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return (getContainer().hasPermission(user, AdminPermission.class));
    }
}
