package org.labkey.targetedms.query;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.Map;

/**
* Created by: jeckels
* Date: 12/7/14
*/
public class QCAnnotationTypeTable extends FilteredTable<TargetedMSSchema>
{
    public QCAnnotationTypeTable(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoQCAnnotationType(), schema);
        wrapAllColumns(true);
        getColumn("Container").setFk(new ContainerForeignKey(schema));
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap) throws QueryUpdateServiceException, SQLException, InvalidKeyException
            {
                // Need to cascade the delete
                Object id = oldRowMap.get("id");
                if (id != null)
                {
                    try
                    {
                        // Rely on the superclass implemenation to do the container ownership check. Since
                        // we're transacted, it's OK to do the DELETE first
                        Integer convertedId = (Integer) ConvertUtils.convert(id.toString(), Integer.class);
                        new SqlExecutor(getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCAnnotation() + " WHERE QCAnnotationTypeId = ?", convertedId);
                    }
                    catch (ConversionException ignored) {}
                }
                return super.deleteRow(user, container, oldRowMap);
            }
        };
    }
}
