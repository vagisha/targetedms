package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.list.ListDefinition;

import java.util.Set;
import java.util.stream.Collectors;

public class SkylineListSchema extends UserSchema
{
    public static final String SCHEMA_NAME = "targetedmslists";

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new SkylineListSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public SkylineListSchema(User user, Container container) {
        super(SCHEMA_NAME, null, user, container, TargetedMSSchema.getSchema());
    }

    @Override
    public @Nullable TableInfo createTable(String name, ContainerFilter cf)
    {
        int ichHyphen = name.indexOf('-');
        if (ichHyphen < 0) {
            return null;
        }
        int runId = Integer.parseInt(name.substring(0, ichHyphen));
        String listName = name.substring(ichHyphen + 1);
        ListDefinition listDefinition  = SkylineListManager.getListDefinition(cf, runId, listName);
        return new SkylineListTable(this, listDefinition, SkylineListManager.getListColumns(listDefinition));
    }

    @Override
    public Set<String> getTableNames()
    {
        return SkylineListManager.getListDefinitions(getContainer()).stream()
                .map(ListDefinition::getUserSchemaTableName).collect(Collectors.toSet());
    }
}
