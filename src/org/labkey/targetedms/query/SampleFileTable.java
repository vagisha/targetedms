package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;
import java.util.Map;

public class SampleFileTable extends TargetedMSTable
{
    public SampleFileTable(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL)
    {
        super(table, schema, joinSQL);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // only allow delete of targetedms.SampleFile table for QC folder type
        TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
        boolean allowDelete = folderType == TargetedMSModule.FolderType.QC && DeletePermission.class.equals(perm);

        return (ReadPermission.class.equals(perm) || allowDelete) && getContainer().hasPermission(user, perm);
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
                    Integer convertedId = Integer.parseInt(id.toString());

                    Table.delete(TargetedMSManager.getTableInfoTransitionChromInfo(), new SimpleFilter(FieldKey.fromParts("SampleFileId"), convertedId));
                    Table.delete(TargetedMSManager.getTableInfoPrecursorChromInfo(), new SimpleFilter(FieldKey.fromParts("SampleFileId"), convertedId));
                    Table.delete(TargetedMSManager.getTableInfoPeptideChromInfo(), new SimpleFilter(FieldKey.fromParts("SampleFileId"), convertedId));
                }
                return super.deleteRow(user, container, oldRowMap);
            }
        };
    }
}
