/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.gwt.client.FacetingBehaviorType;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.DataSettings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Optionally adds annotation-valued columns as if there were "real" columns. Can be conditionalized via the omitAnnotations
 * column to optimize for scenarios where they will never be used, such as when populating a Java bean with a fixed set
 * of get/set methods.
 * User: jeckels
 * Date: Jul 6, 2012
 */
public class AnnotatedTargetedMSTable extends TargetedMSTable
{
    private static final String ANNOT_NAME_VALUE_SEPARATOR = ": ";
    private static final String ANNOT_DELIMITER = "\n";

    public AnnotatedTargetedMSTable(TableInfo table,
                                    TargetedMSSchema schema,
                                    ContainerFilter cf,
                                    TargetedMSSchema.ContainerJoinType joinType,
                                    TableInfo annotationTableInfo,
                                    String annotationFKName,
                                    String columnName,
                                    String annotationTarget, boolean omitAnnotations) // The target of an annotation that applies to this table.
    {
        this(table, schema, cf, joinType, annotationTableInfo, annotationFKName, columnName, "Id", annotationTarget, omitAnnotations);
    }

    public AnnotatedTargetedMSTable(TableInfo table,
                                    TargetedMSSchema schema,
                                    ContainerFilter cf,
                                    TargetedMSSchema.ContainerJoinType joinType,
                                    SQLFragment containerSQL,
                                    TableInfo annotationTableInfo,
                                    String annotationFKName,
                                    String columnName,
                                    String annotationTarget, boolean omitAnnotations) // The target of an annotation that applies to this table.
    {
        super(table, schema, cf, joinType, containerSQL);

        if (!omitAnnotations)
        {
            addAnnotationsColumns(annotationTableInfo, annotationFKName, columnName, "Id", annotationTarget);
        }
    }

    public AnnotatedTargetedMSTable(TableInfo table,
                                    TargetedMSSchema schema,
                                    ContainerFilter cf,
                                    TargetedMSSchema.ContainerJoinType joinType,
                                    TableInfo annotationTableInfo,
                                    String annotationFKName,
                                    String columnName,
                                    String pkColumnName,
                                    String annotationTarget, boolean omitAnnotations) // The target of an annotation that applies to this table.
    {
        super(table, schema, cf, joinType);

        if (!omitAnnotations)
        {
            addAnnotationsColumns(annotationTableInfo, annotationFKName, columnName, pkColumnName, annotationTarget);
        }
    }

    private void addAnnotationsColumns(TableInfo annotationTableInfo, String annotationFKName, String columnName, String pkColumnName, String annotationTarget)
    {
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

        //get list of annotations the relevant type in this container
        List<AnnotationSettingForTyping> annotationSettingForTypings = getAnnotationSettings(annotationTarget, getUserSchema(), getContainerFilter());
        //iterate over list of annotations settings
        for (AnnotationSettingForTyping annotationSettingForTyping : annotationSettingForTypings)
        {
            if (this.getColumn(annotationSettingForTyping.getName()) != null)
            {
                continue;
            }
            //build expr col sql to select value field from annotation table
            SQLFragment annotationSQL = new SQLFragment("(SELECT ",annotationSettingForTyping.getName());
            DataSettings.AnnotationType annotationType = appendValueWithCast(annotationSettingForTyping, annotationSQL);
            annotationSQL.append(" FROM ");
            annotationSQL.append(annotationTableInfo, "a");
            annotationSQL.append(" WHERE a.");
            annotationSQL.append(annotationFKName);
            annotationSQL.append(" = ");
            annotationSQL.append(ExprColumn.STR_TABLE_ALIAS);
            annotationSQL.append(".").append(pkColumnName).append(" AND a.name = ?)");

            //Create new Expression column representing annotation
            ExprColumn annotationColumn = new ExprColumn(this, annotationSettingForTyping.getName(), annotationSQL, annotationType.getDataType());
            annotationColumn.setLabel(annotationSettingForTyping.getName());
            annotationColumn.setTextAlign("left");
            annotationColumn.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
            annotationColumn.setMeasure(annotationType.isMeasure());
            annotationColumn.setDimension(annotationType.isDimension());

            addColumn(annotationColumn);
        }

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
     * Adds the Value field to the passed in SQLFragment.  If the AnnotationSettingForTyping two type properties are
     * of the same type it indicates all annotation settings having this name are of the same type so can be cast.
     *
     * @return The derived Annotation type.
     */
    protected DataSettings.AnnotationType appendValueWithCast(AnnotationSettingForTyping annotationSettingForTyping, SQLFragment annotationSQL)
    {
        if (annotationSettingForTyping.getMaxType().equals(annotationSettingForTyping.getMinType()))
        {
            DataSettings.AnnotationType annotationType =
                    DataSettings.AnnotationType.fromString(annotationSettingForTyping.getMaxType());
            if (annotationType != null && annotationType != DataSettings.AnnotationType.text)
            {
                annotationSQL.append("CAST(").append("a.value AS ")
                        .append(getSqlDialect().getSqlCastTypeName(annotationType.getDataType()));
                annotationSQL.append(")");
                return annotationType;
            }
        }

        annotationSQL.append("a.value");
        return DataSettings.AnnotationType.text;
    }

    public static List<AnnotationSettingForTyping> getAnnotationSettings(String annotationTarget, TargetedMSSchema schema, ContainerFilter containerFilter)
    {
        SQLFragment annoSettingsSql = new SQLFragment();
        TableInfo annotationSettingsTI = TargetedMSManager.getTableInfoAnnotationSettings();
        annoSettingsSql.append("SELECT name," +
                "max(Type) maxType," +
                "min(Type) minType" +
                "  FROM ");
        annoSettingsSql.append(annotationSettingsTI, " annoSettings ");
        annoSettingsSql.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), " runs ON runs.Id = annoSettings.RunId");
        annoSettingsSql.append(" WHERE ");
        annoSettingsSql.append(containerFilter.getSQLFragment(schema.getDbSchema(), new SQLFragment("runs.Container"), schema.getContainer()));
        // AnnotationSettings table has a "Targets" column that determines which targets
        // (protein, peptide, precursor, transition, precursor/transition results) an annotation applies to.
        // Filter annotations to the target that is relevant to this table.
        annoSettingsSql.append(" AND ");
        annoSettingsSql.append(" annoSettings.Targets=?").add(annotationTarget);
        annoSettingsSql.append(" GROUP BY name");
        annoSettingsSql.append(" ORDER BY name");

        SqlSelector annotationSettingsSelector = new SqlSelector(schema.getDbSchema(), annoSettingsSql);
        List<AnnotationSettingForTyping> annotationSettingForTypings = new ArrayList<>();
        try(TableResultSet rs = annotationSettingsSelector.getResultSet())
        {
            while (rs.next())
            {
                annotationSettingForTypings.add(new AnnotationSettingForTyping(
                        rs.getString("name"),
                        rs.getString("maxType"),
                        rs.getString("minType"))
                );
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        return annotationSettingForTypings;
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

    public static class AnnotationSettingForTyping
    {
        private String _name;
        private String _minType;
        private String _maxType;

        public AnnotationSettingForTyping(String name, String minType, String maxType)
        {
            _name = name;
            _minType = minType;
            _maxType = maxType;
        }

        public String getName()
        {
            return _name;
        }

        public String getMinType()
        {
            return _minType;
        }

        public String getMaxType()
        {
            return _maxType;
        }
    }
}
