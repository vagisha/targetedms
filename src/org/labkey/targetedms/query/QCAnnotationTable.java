package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

/**
* Created by: jeckels
* Date: 12/7/14
*/
public class QCAnnotationTable extends FilteredTable<TargetedMSSchema>
{
    public QCAnnotationTable(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoQCAnnotation(), schema);

        wrapAllColumns(true);
        getColumn("Container").setFk(new ContainerForeignKey(schema));
        getColumn("QCAnnotationTypeId").setFk(new QueryForeignKey(schema, null, TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE, "Id", "Name")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                // Tweak the container filter based on the scoping rules for annotation types
                FilteredTable result = (FilteredTable) super.getLookupTableInfo();
                result.setContainerFilter(new ContainerFilter.CurrentPlusProjectAndShared(getUserSchema().getUser()));
                return result;
            }
        });
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
