/*
 * Copyright (c) 2019 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.SkylineDocImporter;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.list.ListColumn;
import org.labkey.targetedms.parser.list.ListData;
import org.labkey.targetedms.parser.list.ListDefinition;
import org.labkey.targetedms.parser.list.ListItem;
import org.labkey.targetedms.parser.list.ListItemValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SkylineListManager
{
    /**
     * Returns true if the long text columns such as ListDefinition.Name support the equals operator.
     */
    public static boolean canFilterNameColumn()
    {
        return !TargetedMSSchema.getSchema().getSqlDialect().isSqlServer();
    }

    public static ListDefinition saveListData(User user, ListData listData)
    {
        ListDefinition listDefinition = Table.insert(user, TargetedMSManager.getTableInfoListDefinition(), listData.getListDefinition());
        List<ListColumn> columns = new ArrayList<>();
        for (int icol = 0; icol < listData.getColumnCount(); icol++)
        {
            ListColumn column = listData.getColumnDef(icol);
            column.setListDefinitionId(listDefinition.getId());
            column = Table.insert(user, TargetedMSManager.getTableInfoListColumnDefinition(), column);
            columns.add(column);
        }
        for (int irow = 0; irow < listData.getItemCount(); irow++)
        {
            ListItem listItem = new ListItem();
            listItem.setListDefinitionId(listDefinition.getId());
            listItem = Table.insert(user, TargetedMSManager.getTableInfoListItem(), listItem);
            for (int icol = 0; icol < listData.getColumnCount(); icol++)
            {
                Object value = listData.getColumnValue(irow, icol);
                if (value == null)
                {
                    continue;
                }
                ListItemValue listValue = new ListItemValue();
                listValue.setListItemId(listItem.getId());
                listValue.setColumnIndex(icol);
                listValue.setValue(value);
                Table.insert(user, TargetedMSManager.getTableInfoListItemValue(), listValue);
            }
        }
        return listDefinition;
    }

    public static TableInfo getListDefTable()
    {
        return TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_LIST_DEFINITION);
    }

    public static TableInfo getListColumnTable()
    {
        return TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_LIST_COLUMN_DEFINITION);
    }

    public static List<ListDefinition> getListDefinitions(Container container)
    {
        SQLFragment fragment = new SQLFragment("SELECT * FROM ");
        fragment.append(getListDefTable(), "t");
        fragment.append(" WHERE t.RunId IN (SELECT Id FROM ");
        fragment.append(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_RUNS), "r");
        fragment.append(new SQLFragment(" WHERE Container = ? AND StatusId = ? AND deleted = ?)",
                container.getId(), SkylineDocImporter.STATUS_SUCCESS, Boolean.FALSE));
        return new SqlSelector(TargetedMSSchema.getSchema(), fragment).getArrayList(ListDefinition.class);
    }

    public static ListDefinition getListDefinition(ContainerFilter containerFilter, int runId, String listName)
    {
        SQLFragment fragment = new SQLFragment("SELECT * FROM ");
        fragment.append(getListDefTable(), "t");
        fragment.append(new SQLFragment(" WHERE t.RunId = ?", runId));
        if (canFilterNameColumn())
        {
            fragment.append(new SQLFragment(" and t.Name = ?", listName));
        }
        if (containerFilter != null)
        {
            SQLFragment sqlFragmentContainer = new SQLFragment("(SELECT CONTAINER FROM " + TargetedMSManager.getTableInfoRuns() + " WHERE RunId = ?)", runId);
            fragment.append(" AND ");
            fragment.append(containerFilter.getSQLFragment(TargetedMSSchema.getSchema(), sqlFragmentContainer, null));
        }
        for (ListDefinition listDefinition : new SqlSelector(TargetedMSSchema.getSchema(), fragment).getCollection(ListDefinition.class))
        {
            if (Objects.equals(listName, listDefinition.getName()))
            {
                return listDefinition;
            }
        }
        return null;
    }

    public static List<ListColumn> getListColumns(ListDefinition listDefinition)
    {
        return new TableSelector(getListColumnTable(), new SimpleFilter(FieldKey.fromParts("ListDefinitionId"), listDefinition.getId()), new Sort("ColumnIndex")).getArrayList(ListColumn.class);
    }
}
