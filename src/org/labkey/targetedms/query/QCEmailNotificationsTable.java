package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.UserIdQueryForeignKey;
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

public class QCEmailNotificationsTable extends FilteredTable<TargetedMSSchema>
{
    public QCEmailNotificationsTable(@NotNull TargetedMSSchema userSchema, @Nullable ContainerFilter containerFilter)
    {
        super(TargetedMSManager.getTableInfoQCEmailNotifications(), userSchema, containerFilter);
        var userColumn = wrapColumn(getRealTable().getColumn("userId"));
        var outlierColumn = wrapColumn( getRealTable().getColumn("outliers"));
        var sampleFilesColumn = wrapColumn( getRealTable().getColumn("samples"));
        var enabled = wrapColumn(getRealTable().getColumn("enabled"));

        addColumn(userColumn);
        userColumn.setFk(new UserIdQueryForeignKey(_userSchema));
        addColumn(outlierColumn);
        addColumn(sampleFilesColumn);
        addColumn(enabled);

        // Only admins can see subscriptions for other users
        if (!userSchema.getContainer().hasPermission(userSchema.getUser(), AdminPermission.class))
        {
            addCondition(getRealTable().getColumn("userId"), userSchema.getUser().getUserId());
        }
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
        return new QCEmailNotificationsQueryUpdateService(this, getRealTable());
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return (getContainer().hasPermission(user, ReadPermission.class));
    }

    private class QCEmailNotificationsQueryUpdateService extends DefaultQueryUpdateService
    {
        public QCEmailNotificationsQueryUpdateService(@NotNull TableInfo queryTable, TableInfo dbTable)
        {
            super(queryTable, dbTable);
        }

        @Override
        protected Map<String, Object> insertRow(User user, Container container, Map<String, Object> row) throws DuplicateKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
             if(user.getUserId() == (int) row.get("userId"))
             {
                 return super.insertRow(user, container, row);
             }
             else
             {
                 throw new ValidationException("Can't subscribe to notifications for other user.");
             }
        }

        @Override
        protected Map<String, Object> updateRow(User user, Container container, Map<String, Object> row, @NotNull Map<String, Object> oldRow) throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            if(user.getUserId() == (int) row.get("userId"))
            {
                return super.updateRow(user, container, row, oldRow);

            }
            else
            {
                throw new ValidationException("Can't modify subscriptions for other user.");
            }
        }

        @Override
        protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap) throws QueryUpdateServiceException, SQLException, InvalidKeyException
        {
            if(user.getUserId() == (int) oldRowMap.get("userId"))
            {
                return super.deleteRow(user, container, oldRowMap);
            }
            else
            {
                throw new QueryUpdateServiceException("Can't delete subscriptions of other user."); //super method does not throw ValidationException
            }
        }
    }
}
