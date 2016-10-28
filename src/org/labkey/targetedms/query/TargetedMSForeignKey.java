/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * Created by Josh on 9/20/2016.
 */
public class TargetedMSForeignKey extends LookupForeignKey
{
    private final TargetedMSSchema _schema;
    private final String _tableName;

    public TargetedMSForeignKey(TargetedMSSchema schema, String tableName)
    {
        super("Id");
        _schema = schema;
        _tableName = tableName;
    }

    @Override
    public TableInfo getLookupTableInfo()
    {
        TableInfo table = _schema.getTable(_tableName);
        ((TargetedMSTable)table).setNeedsContainerWhereClause(false);
        return table;
    }
}
