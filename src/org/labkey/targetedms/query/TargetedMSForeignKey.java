/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.GUID;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Collection;

/**
 * Created by Josh on 9/20/2016.
 */
public class TargetedMSForeignKey extends LookupForeignKey
{
    private final TargetedMSSchema _schema;
    private final String _tableName;

    public TargetedMSForeignKey(TargetedMSSchema schema, String tableName, ContainerFilter cf)
    {
        super(cf,"Id", null);
        _schema = schema;
        _tableName = tableName;
    }

    @Override
    public TableInfo getLookupTableInfo()
    {
        // Avoid applying a container filter on lookups. The import process should be only creating FKs to data
        // in the same container. Thus, we can rely on the outer query doing the proper filtering and avoid
        // what can be expensive multi-table joins to get to a table that has the Container column we need
        return _schema.getTable(_tableName, new AnnotationsContainerFilter(_schema.getContainer(), getLookupContainerFilter()));
    }

    /** Special wrapper over the standard container filter. Used to identify lookups that don't need to do container
     * filtering over their data (see note in getLookupTableInfo() above), but still respect the right filtering
     * when loading the annotations (in AnnotatedTargetedMSTableInfo) that are in scope and should be added as columns.
     * See issue 38134 */
    public static class AnnotationsContainerFilter extends ContainerFilter
    {
        @NotNull
        private final ContainerFilter _annotationContainerFilter;

        public AnnotationsContainerFilter(Container currentContainer, ContainerFilter annotationContainerFilter)
        {
            super(null,null);
            _annotationContainerFilter = annotationContainerFilter == null ? ContainerFilter.current(currentContainer) : annotationContainerFilter;
        }

        @Override
        public @Nullable Collection<GUID> getIds()
        {
            return _annotationContainerFilter.getIds();
        }

        @Override
        public @Nullable Type getType()
        {
            return _annotationContainerFilter.getType();
        }

        @Override
        public String getCacheKey()
        {
            return "TargetedMSEverything" + _annotationContainerFilter.getCacheKey();
        }
    }
}
