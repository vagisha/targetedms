/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.parser.AbstractAnnotation;

import java.util.Collection;
import java.util.Set;

/**
 * User: jeckels
 * Date: Jul 6, 2012
 */
public class AnnotatedTargetedMSTable extends TargetedMSTable
{
    private final TableInfo _annotationTableInfo;
    private final String _annotationFKName;

    public AnnotatedTargetedMSTable(TableInfo table, Container container, SQLFragment containerSQL, TableInfo annotationTableInfo, String annotationFKName)
    {
        super(table, container, containerSQL);
        _annotationTableInfo = annotationTableInfo;
        _annotationFKName = annotationFKName;

        SQLFragment annotationsSQL = new SQLFragment("(SELECT COUNT(Id) FROM ");
        annotationsSQL.append(annotationTableInfo, "a");
        annotationsSQL.append(" WHERE a.");
        annotationsSQL.append(annotationFKName);
        annotationsSQL.append(" = ");
        annotationsSQL.append(ExprColumn.STR_TABLE_ALIAS);
        annotationsSQL.append(".Id)");
        ExprColumn annotationsColumn = new ExprColumn(this, "Annotations", annotationsSQL, JdbcType.INTEGER);
        annotationsColumn.setTextAlign("left");
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
            // This is somewhat questionable since the database doesn't what we're going to actually display, but
            // at least it lets the user group the annotatated vs non-annotated rows
            return true;
        }

        @Override
        public boolean isFilterable()
        {
            // The database is only returning the number of annotations, so it's not useful to filter on them
            // because they don't match with what we're going to show in the UI
            return false;
        }

        public int getAnnotationCount(RenderContext ctx)
        {
            Number value = (Number)super.getValue(ctx);
            return value == null ? 0 : value.intValue();
        }

        /** Do a separate query to get the annotations for this row */
        private Collection<AbstractAnnotation> getAnnotations(RenderContext ctx)
        {
            int id = ctx.get(_idFieldKey, Integer.class);
            TableSelector selector = new TableSelector(_annotationTableInfo, Table.ALL_COLUMNS, new SimpleFilter(_annotationFKName, id), new Sort("Name"));
            return selector.getCollection(AbstractAnnotation.class);
        }

        /** Build up the non-HTML encoded annotations for TSV/Excel export, etc */
        @Override
        public String getValue(RenderContext ctx)
        {
            StringBuilder sb = new StringBuilder();
            if (getAnnotationCount(ctx) > 0)
            {
                String separator = "";
                for (AbstractAnnotation annotation : getAnnotations(ctx))
                {
                    sb.append(separator);
                    separator = "\n";
                    sb.append(annotation.getName());
                    sb.append(": ");
                    sb.append(annotation.getValue());
                }
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
        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            StringBuilder sb = new StringBuilder();
            if (getAnnotationCount(ctx) > 0)
            {
                String separator = "";
                for (AbstractAnnotation annotation : getAnnotations(ctx))
                {
                    sb.append(separator);
                    separator = "<br/>";
                    sb.append(PageFlowUtil.filter(annotation.getName()));
                    sb.append(": ");
                    sb.append(PageFlowUtil.filter(annotation.getValue()));
                }
            }
            return sb.toString();
        }
    }
}
