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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.targetedms.parser.list.ListColumn;
import org.labkey.targetedms.parser.list.ListDefinition;

import java.util.List;

public class SkylineListTable extends AbstractTableInfo
{
    UserSchema _userSchema;
    ListDefinition _listDefinition;
    List<ListColumn> _listColumns;
    public SkylineListTable(UserSchema schema, ListDefinition listDefinition, List<ListColumn> columns) {
        super(schema.getDbSchema(), listDefinition.getName());
        _userSchema = schema;
        _listDefinition = listDefinition;
        _listColumns = columns;
        for (ListColumn listColumn : _listColumns) {
            addColumn(new ListColumnInfo(this, listColumn));
        }
    }

    @Override
    public @Nullable UserSchema getUserSchema()
    {
        return _userSchema;
    }

    @Override
    protected SQLFragment getFromSQL()
    {
        SQLFragment fragment = new SQLFragment("SELECT Id FROM targetedms.ListItem WHERE ListDefinitionId = ?", _listDefinition.getId());
        return fragment;
    }

    class ListColumnInfo extends BaseColumnInfo
    {
        ListColumn _listColumn;
        public ListColumnInfo(SkylineListTable listTable, ListColumn listColumn) {
            super(new FieldKey(null, listColumn.getName()), listTable);
            _listColumn = listColumn;
            setJdbcType(listColumn.getAnnotationTypeEnum().getDataType());
        }

        @Override
        public SQLFragment getValueSql(String tableAliasName)
        {
            SQLFragment sqlFragment = new SQLFragment();
            sqlFragment.append("(SELECT ");
            switch (_listColumn.getAnnotationTypeEnum())
            {
                default:
                    sqlFragment.append("TextValue");
                    break;
                case number:
                    sqlFragment.append("NumericValue");
                    break;
                case true_false:
                    sqlFragment.append("NumericValue = 1");
                    break;
            }
            sqlFragment.append(new SQLFragment(" FROM targetedms.ListItemValue WHERE ListItemId = " + tableAliasName +".Id AND ColumnIndex = ?",
                    _listColumn.getColumnIndex()));
            sqlFragment.append(")");
            return sqlFragment;
        }
    }
}
