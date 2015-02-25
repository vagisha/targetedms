/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.gwt.client.FacetingBehaviorType;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: Jul 6, 2012
 */
public class AnnotatedTargetedMSTable extends TargetedMSTable
{
    private static final String ANNOT_NAME_VALUE_SEPARATOR = ": ";
    private static final String ANNOT_DELIMITER = "\n";

    public AnnotatedTargetedMSTable(TableInfo table,
                                    TargetedMSSchema schema,
                                    SQLFragment containerSQL,
                                    TableInfo annotationTableInfo,
                                    String annotationFKName,
                                    String columnName)
    {
        this(table, schema, containerSQL, annotationTableInfo, annotationFKName, columnName, "Id");
    }

    public AnnotatedTargetedMSTable(TableInfo table,
                                    TargetedMSSchema schema,
                                    SQLFragment containerSQL,
                                    TableInfo annotationTableInfo,
                                    String annotationFKName,
                                    String columnName,
                                    String pkColumnName)
    {
        super(table, schema, containerSQL);

        SQLFragment annotationsSQL = new SQLFragment("(SELECT ");
        annotationsSQL.append(TargetedMSManager.getSqlDialect().getGroupConcat(
                new SQLFragment(TargetedMSManager.getSqlDialect().concatenate("a.Name", "\'"+ ANNOT_NAME_VALUE_SEPARATOR +"\' ", "a.Value")),
                false,
                true,
                "'" + ANNOT_DELIMITER + "'"));
        annotationsSQL.append(" FROM ");
        annotationsSQL.append(annotationTableInfo, "a");
        annotationsSQL.append(" WHERE a.");
        annotationsSQL.append(annotationFKName);
        annotationsSQL.append(" = ");
        annotationsSQL.append(ExprColumn.STR_TABLE_ALIAS);
        annotationsSQL.append(".").append(pkColumnName).append(")");
        ExprColumn annotationsColumn = new ExprColumn(this, "Annotations", annotationsSQL, JdbcType.VARCHAR);
        annotationsColumn.setLabel(columnName);
        annotationsColumn.setTextAlign("left");
        annotationsColumn.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
        addColumn(annotationsColumn);

        annotationsColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AnnotationsDisplayColumn(colInfo);
            }
        });
    }

    /**
     * Wraps an int column in the real SQL query.
     * Shows the name/value pairs for annotations if the underlying value is non-zero
     */
    public class AnnotationsDisplayColumn extends DataColumn
    {
        private final FieldKey _idFieldKey;

        public AnnotationsDisplayColumn(ColumnInfo col)
        {
            super(col);
            _idFieldKey = new FieldKey(getBoundColumn().getFieldKey().getParent(), "Id");
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            keys.add(_idFieldKey);
        }

        @Override
        public boolean isSortable()
        {
            return true;
        }

        @Override
        public boolean isFilterable()
        {
            return true;
        }

        private List<String> getAnnotations(RenderContext ctx)
        {
            String annotations = (String)super.getValue(ctx);
            if(!StringUtils.isBlank(annotations))
            {
                String[] annotationsArray = annotations.split(ANNOT_DELIMITER);
                return Arrays.asList(annotationsArray);
            }
            else
            {
                return Collections.emptyList();
            }
        }

        /** Build up the non-HTML encoded annotations for TSV/Excel export, etc */
        @Override
        public String getValue(RenderContext ctx)
        {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (String annotation : getAnnotations(ctx))
            {
                sb.append(separator);
                separator = "\n";
                sb.append(annotation);
            }
            return sb.toString();
        }

        @Override
        public Class getValueClass()
        {
            return String.class;
        }

        @Override
        public Class getDisplayValueClass()
        {
            return String.class;
        }


        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            return getValue(ctx);
        }

        /** The HTML encoded annotation name/value pairs */
        @Override @NotNull
        public String getFormattedValue(RenderContext ctx)
        {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (String annotation : getAnnotations(ctx))
            {
                sb.append(separator);
                separator = "<br/>";
                sb.append("<nobr>");
                sb.append(PageFlowUtil.filter(annotation));
                sb.append("</nobr>");
            }
            return sb.toString();
        }
    }
}
