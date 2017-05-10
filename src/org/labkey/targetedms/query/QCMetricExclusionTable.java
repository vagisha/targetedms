package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;

public class QCMetricExclusionTable extends TargetedMSTable
{
    public QCMetricExclusionTable(TargetedMSSchema schema)
    {
        super(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION),
                schema, TargetedMSSchema.ContainerJoinType.ReplicateFK.getSQL());

        getColumn(FieldKey.fromParts("MetricId")).setFk(new QueryForeignKey(schema, getUserSchema().getContainer(), TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION, "Id", "Name")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                // tweak the container filter to get the right set of QC metric configurations
                FilteredTable result = (FilteredTable) super.getLookupTableInfo();
                result.setContainerFilter(QCMetricConfigurationTable.getDefaultMetricContainerFilter(getUserSchema().getUser()));
                return result;
            }
        });
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        if(TargetedMSManager.getFolderType(getContainer()) == TargetedMSModule.FolderType.QC)
        {
            // Allow edits, deletes and inserts only in QC folder types
            return getContainer().hasPermission(user, perm);
        }
        return super.hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
