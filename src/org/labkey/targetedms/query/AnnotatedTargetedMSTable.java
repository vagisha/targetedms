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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
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
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.DataSettings;
import org.labkey.targetedms.parser.list.ListDefinition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Optionally adds annotation-valued columns as if there were "real" columns. Can be conditionalized via the omitAnnotations
 * column to optimize for scenarios where they will never be used, such as when populating a Java bean with a fixed set
 * of get/set methods.
 *
 * Wires up lookups to Skyline lists when possible. {@link SkylineListUnionTable}
 *
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
        getAnnotationJoinSQL(annotationTableInfo, annotationFKName, annotationsSQL);
        annotationsSQL.append(".").append(pkColumnName).append(")");
        ExprColumn annotationsColumn = new ExprColumn(this, "Annotations", annotationsSQL, JdbcType.VARCHAR);
        annotationsColumn.setLabel(columnName);
        annotationsColumn.setTextAlign("left");
        annotationsColumn.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
        addColumn(annotationsColumn);

        //get list of annotations the relevant type in this container
        List<AnnotationSettingForTyping> annotationSettingForTypings = getAnnotationSettings(annotationTarget, getUserSchema(), getContainerFilter());
        //iterate over list of annotations settings
        for (AnnotationSettingForTyping annotationSetting : annotationSettingForTypings)
        {
            if (this.getColumn(annotationSetting.getName()) != null)
            {
                continue;
            }
            // build expr col sql to select value field from annotation table
            SQLFragment annotationSQL = new SQLFragment("(SELECT ",annotationSetting.getName());
            DataSettings.AnnotationType annotationType = appendValueWithCast(annotationSetting, annotationSQL);
            getAnnotationJoinSQL(annotationTableInfo, annotationFKName, annotationSQL);
            annotationSQL.append(".").append(pkColumnName).append(" AND a.name = ?)");

            // Create new column representing the annotation
            ExprColumn annotationColumn = new AnnotationColumn(this, annotationSetting.getName(), annotationSQL, annotationType.getDataType());
            annotationColumn.setLabel(annotationSetting.getName());
            annotationColumn.setTextAlign("left");
            annotationColumn.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
            annotationColumn.setMeasure(annotationType.isMeasure());
            annotationColumn.setDimension(annotationType.isDimension());
            
            // Check if the annotation is a lookup and all of the definitions agree on what its target list should be
            if (annotationSetting.getMaxLookup() != null && Objects.equals(annotationSetting.getMaxLookup(), annotationSetting.getMinLookup()))
            {
                String lookup = annotationSetting.getMaxLookup();
                // Look at all the lists with the same name in this scope
                List<ListDefinition> listDefs = SkylineListManager.getListDefinitions(getContainer(), getContainerFilter());
                listDefs = listDefs.stream().filter((l) -> lookup.equals(l.getName())).collect(Collectors.toList());
                if (!listDefs.isEmpty())
                {
                    ListDefinition listDef = listDefs.get(0);
                    // Use the first one (the most recent import) as our FK to a UNION version of the table
                    LookupForeignKey fk = new LookupForeignKey()
                    {
                        @Override
                        public @Nullable TableInfo getLookupTableInfo()
                        {
                            return new SkylineListSchema(getUserSchema().getUser(), _userSchema.getContainer()).getTable(listDef.getUnionUserSchemaTableName(), getContainerFilter());
                        }
                    };
                    fk.addJoin(_joinType.getRunIdFieldKey(), "RunId", false);
                    annotationColumn.setFk(fk);
                }
            }

            addColumn(annotationColumn);
        }

        annotationsColumn.setDisplayColumnFactory(AnnotationsDisplayColumn::new);
    }

    private void getAnnotationJoinSQL(TableInfo annotationTableInfo, String annotationFKName, SQLFragment annotationSQL)
    {
        annotationSQL.append(" FROM ");
        annotationSQL.append(annotationTableInfo, "a");
        annotationSQL.append(" WHERE a.");
        annotationSQL.append(annotationFKName);
        annotationSQL.append(" = ");
        annotationSQL.append(ExprColumn.STR_TABLE_ALIAS);
    }

    public static class AnnotationColumn extends ExprColumn
    {
        public AnnotationColumn(AnnotatedTargetedMSTable table, String name, SQLFragment annotationSQL, JdbcType dataType)
        {
            super(table, name, annotationSQL, dataType);
        }
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
                // Issue 39003 - It's up to DB to decide if it applies the WHERE filter first to get to just the annotation
                // values we expect based on replicate and name, so on SQL Server be permissive on the conversion
                // in case we encounter other annotation values first that are of different types
                annotationSQL.append(getSqlDialect().isSqlServer() ? "TRY_CAST" : "CAST");

                annotationSQL.append("(").append("a.value AS ")
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
        // We query for the min and max values to determine both what they're set to, and if they're all the same.
        // If we have at least one value that's different in the column, the min and max will be different.
        annoSettingsSql.append("SELECT name," +
                "max(Type) maxType, " +
                "min(Type) minType, " +
                "max(Lookup) maxLookup, " +
                "min(Lookup) minLookup" +
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
                        rs.getString("minType"),
                        rs.getString("maxLookup"),
                        rs.getString("minLookup"))
                );
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        // Do a case-insensitive sort since different DBs have different default collations
        annotationSettingForTypings.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

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
        private final String _name;
        private final String _minType;
        private final String _maxType;
        private final String _maxLookup;
        private final String _minLookup;

        public AnnotationSettingForTyping(String name, String maxType, String minType, String maxLookup, String minLookup)
        {
            _name = name;
            _minType = minType;
            _maxType = maxType;
            _maxLookup = maxLookup;
            _minLookup = minLookup;
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

        public String getMaxLookup()
        {
            return _maxLookup;
        }

        public String getMinLookup()
        {
            return _minLookup;
        }
    }
}
