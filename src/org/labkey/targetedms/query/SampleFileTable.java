/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.LazyForeignKey;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.api.SampleTypeService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.SamplesSchema;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.datasource.MsDataSourceUtil;
import org.labkey.targetedms.parser.SampleFile;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleFileTable extends TargetedMSTable
{
    @Nullable
    private final TargetedMSRun _run;

    private final String SAMPLE_FIELD_KEY = "SampleName";

    public SampleFileTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        this(schema, cf, null);
    }

    public SampleFileTable(TargetedMSSchema schema, ContainerFilter cf, @Nullable TargetedMSRun run)
    {
        super(TargetedMSManager.getTableInfoSampleFile(), schema, cf, TargetedMSSchema.ContainerJoinType.ReplicateFK);

        _run = run;
        SQLFragment runIdSQL = new SQLFragment("(SELECT r.RunId FROM ").
                append(TargetedMSManager.getTableInfoReplicate(), "r").
                append(" WHERE r.Id = ").append(ExprColumn.STR_TABLE_ALIAS).append(".ReplicateId)");
        addColumn(new ExprColumn(this, "RunId", runIdSQL, JdbcType.INTEGER));

        if (_run != null)
        {
            addCondition(new SQLFragment("ReplicateId IN (SELECT Id FROM ").
                    append(TargetedMSManager.getTableInfoReplicate(), "r").
                    append(" WHERE RunId = ?)").add(_run.getId()), FieldKey.fromParts("ReplicateId"));
        }

        SQLFragment excludedSQL = new SQLFragment("CASE WHEN ReplicateId IN (SELECT ReplicateId FROM ");
        excludedSQL.append(TargetedMSManager.getTableInfoQCMetricExclusion(), "x");
        excludedSQL.append(" WHERE x.MetricId IS NULL) THEN ? ELSE ? END");
        excludedSQL.add(true);
        excludedSQL.add(false);
        ExprColumn excludedColumn = new ExprColumn(this, "Excluded", excludedSQL, JdbcType.BOOLEAN);
        addColumn(excludedColumn);

        getMutableColumn(SAMPLE_FIELD_KEY).setFk(new LazyForeignKey(() ->
        {
            // Do a query to look across the entire server for samples where the name matches with names from the
            // targetedms.SampleFiles table, as currently filtered by the SampleFileTable (container and/or run)
            SQLFragment sql = new SQLFragment("SELECT DISTINCT Container, CpasType FROM ");
            sql.append(ExperimentService.get().getTinfoMaterial(), "m");
            sql.append(" WHERE CpasType IS NOT NULL AND Name IN (SELECT SampleName FROM (");
            Set<ColumnInfo> selectCols = Set.of(getColumn(SAMPLE_FIELD_KEY));
            sql.append(QueryService.get().getSelectSQL(SampleFileTable.this, selectCols, null, null, Table.ALL_ROWS, 0, false));
            sql.append(") X)");

            Set<Container> matchingContainers = new HashSet<>();
            Set<String> matchingSampleTypeLSIDs = new HashSet<>();

            // Iterate and make sure the user has permission to the target container
            Collection<Map<String, Object>> matches = new SqlSelector(getSchema().getScope(), sql).getMapCollection();
            for (Map<String, Object> match : matches)
            {
                Container c = ContainerManager.getForId((String)match.get("Container"));
                if (c != null && c.hasPermission(getUserSchema().getUser(), ReadPermission.class))
                {
                    matchingContainers.add(c);
                    String sampleTypeLSID = (String)match.get("CpasType");
                    matchingSampleTypeLSIDs.add(sampleTypeLSID);
                }
            }

            if (matchingContainers.size() == 1 && matchingSampleTypeLSIDs.size() == 1)
            {
                ExpSampleType sampleType = SampleTypeService.get().getSampleType(matchingSampleTypeLSIDs.iterator().next());
                if (sampleType != null)
                {
                    ActionURL sampleUrl = PageFlowUtil.urlProvider(ExperimentUrls.class, true).getMaterialDetailsBaseURL(sampleType.getContainer(), SAMPLE_FIELD_KEY + "/RowId");
                    DetailsURL url = new DetailsURL(sampleUrl, "rowId", FieldKey.fromParts(SAMPLE_FIELD_KEY, "RowId"));
                    url.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts(SAMPLE_FIELD_KEY, "Folder"), true), false);
                    url.setStrictContainerContextEval(true);

                    return new QueryForeignKey.Builder(new SamplesSchema(getUserSchema().getUser(), matchingContainers.iterator().next()), null).
                            table(sampleType.getName()).
                            key("Name").
                            url(url).
                            raw(true).
                            build();
                }
            }
            return null;
        }));

        ActionURL url = new ActionURL(TargetedMSController.ShowSampleFileAction.class, getContainer());
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("id", "Id");
        DetailsURL detailsURL = new DetailsURL(url, urlParams);
        setDetailsURL(detailsURL);
        getMutableColumn("ReplicateId").setURL(detailsURL);


        // Add a column to display the file name extracted from the value in the sampleFile.FilePath column
        ExprColumn fileNameCol = new ExprColumn(this, "File", getFileNameSql(), JdbcType.VARCHAR);
        addColumn(fileNameCol);

        var downloadCol = addWrapColumn("Download", getRealTable().getColumn("Id"));
        downloadCol.setKeyField(false);
        downloadCol.setTextAlign("left");
        downloadCol.setDisplayColumnFactory(DownloadLinkColumn::new);

        DetailsURL instrumentURL = new DetailsURL(new ActionURL(TargetedMSController.ShowInstrumentAction.class, getContainer()), Collections.singletonMap("serialNumber", "InstrumentSerialNumber"));
        getMutableColumn("InstrumentSerialNumber").setURL(instrumentURL);
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        if (_defaultVisibleColumns == null && _run != null)
        {
            List<FieldKey> defaultCols = new ArrayList<>(Arrays.asList(
                    FieldKey.fromParts("ReplicateId"),
                    FieldKey.fromParts(SAMPLE_FIELD_KEY)
            ));

            // call to the SampleName FK to resolve the ExpSampleType so we can add any custom user defined fields to the default view
            TableInfo sampleLookupTableInfo = getColumn(SAMPLE_FIELD_KEY).getFk().getLookupTableInfo();
            if (sampleLookupTableInfo != null)
            {
                for (DomainProperty property : sampleLookupTableInfo.getDomain().getProperties())
                    defaultCols.add(FieldKey.fromParts(SAMPLE_FIELD_KEY, property.getName()));
            }

            defaultCols.addAll(Arrays.asList(
                    FieldKey.fromParts("File"),
                    FieldKey.fromParts("Download"),
                    FieldKey.fromParts("AcquiredTime"),
                    FieldKey.fromParts("InstrumentSerialNumber")
            ));

            // Find the columns that have values for the run of interest, and include them in the set of columns in the default
            // view. We don't really care what the value is for the columns, just that it exists, so we arbitrarily use
            // MAX as the aggregate.
            List<Aggregate> aggregates = new ArrayList<>();
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "CePredictorId"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "DpPredictorId"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "SampleType"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "AnalyteConcentration"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", "SampleDilutionFactor"), Aggregate.BaseType.MAX));
            aggregates.add(new Aggregate(FieldKey.fromParts("InstrumentId"), Aggregate.BaseType.MAX));

            // Also search for values for any replicate annotations being used in this container
            for (AnnotatedTargetedMSTable.AnnotationSettingForTyping annotation : AnnotatedTargetedMSTable.getAnnotationSettings("replicate", getUserSchema(), ContainerFilter.current(getUserSchema().getContainer())))
            {
                aggregates.add(new Aggregate(FieldKey.fromParts("ReplicateId", annotation.getName()), Aggregate.BaseType.MAX));
            }

            TableSelector ts = new TableSelector(this);
            Map<String, List<Aggregate.Result>> aggValues = ts.getAggregates(aggregates);

            for (Aggregate aggregate: aggregates)
            {
                List<Aggregate.Result> result = aggValues.get(aggregate.getFieldKey().toString());
                if(result != null)
                {
                    Aggregate.Result aggResult = result.get(0);
                    if (aggResult.getValue() != null)
                    {
                        defaultCols.add(aggResult.getAggregate().getFieldKey());
                    }
                }
            }
            // setDefaultVisibleColumns() will call checkLocked(), however this is just being done lazily for perf (I think) so should be legal
            _defaultVisibleColumns = defaultCols;
        }

        return super.getDefaultVisibleColumns();
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // only allow delete of targetedms.SampleFile table for QC folder type
        TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
        boolean allowDelete = folderType == TargetedMSService.FolderType.QC && DeletePermission.class.equals(perm);

        return (ReadPermission.class.equals(perm) || allowDelete) && getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable())
        {
            @Override
            protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRowMap) throws QueryUpdateServiceException, SQLException, InvalidKeyException
            {
                // Need to cascade the delete
                Object id = oldRowMap.get("id");
                if (id != null)
                {
                    int convertedId = Integer.parseInt(id.toString());
                    TargetedMSManager.purgeDeletedSampleFiles(convertedId);
                }
                return super.deleteRow(user, container, oldRowMap);
            }
        };
    }

    private SQLFragment getFileNameSql()
    {
        SqlDialect dialect = TargetedMSManager.getSqlDialect();

        // If FilePath is C:\Project1\RawData\sample001.raw
        // we want sample001.raw
        // Example SQL on Postgres (sf is alias for SampleFile):
        // CASE WHEN sf.FilePath IS NOT NULL AND POSITION('\' IN sf.FilePath) > 0
        //     THEN substr(sf.FilePath, length(sf.FilePath) - POSITION('\' IN  REVERSE(sf.FilePath)) + 2 , length(sf.FilePath))
        //     ELSE sf.FilePath
        //     END
        SQLFragment filePathColSql = new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".FilePath");
        SQLFragment filePathLengthSql = new SQLFragment(dialect.getVarcharLengthFunction()).append("(").append(filePathColSql).append(")");
        return new SQLFragment("CASE")
                .append(" WHEN ").append(filePathColSql).append(" IS NOT NULL AND ").append(dialect.getStringIndexOfFunction(new SQLFragment("'\\'"), filePathColSql)).append(" > 0")
                .append(" THEN ").append(dialect.getSubstringFunction(
                        filePathColSql,
                        new SQLFragment(filePathLengthSql).append(" - ")
                                .append(dialect.getStringIndexOfFunction(new SQLFragment("'\\'"), new SQLFragment(" REVERSE(").append(filePathColSql)).append(") + 2 ")),
                        filePathLengthSql))
                .append(" ELSE ").append(filePathColSql).append(" END");
    }

    private static class DownloadLinkColumn extends DataColumn
    {
        private final FieldKey _containerFieldKey = FieldKey.fromParts("ReplicateId", "RunId", "Container");

        public DownloadLinkColumn(ColumnInfo col)
        {
            super(col);
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            Long sampleFileId = ctx.get(this.getColumnInfo().getFieldKey(), Long.class);
            Container container = ctx.get(_containerFieldKey, Container.class);

            if (sampleFileId != null && container != null)
            {
                SampleFile sampleFile = ReplicateManager.getSampleFile(sampleFileId);

                if(sampleFile != null)
                {
                    MsDataSourceUtil.RawDataInfo downloadInfo = MsDataSourceUtil.getInstance().getDownloadInfo(sampleFile, container);
                    if(downloadInfo != null)
                    {
                        Long dataSize = downloadInfo.getSize();
                        String size = dataSize != null ? FileUtils.byteCountToDisplaySize(dataSize) : "";
                        ExpData expData = downloadInfo.getExpData();
                        String url = expData.getWebDavURL(ExpData.PathType.full);
                        if(!downloadInfo.isFile())
                        {
                            int idx = url.lastIndexOf('/');
                            url = idx != -1 ? url.substring(0, idx) : url;
                            url = url + "?method=zip&depth=-1&file=" + expData.getName() + "&zipName=" + expData.getName();
                        }

                        out.write(PageFlowUtil.iconLink("fa fa-download", null).href(url).toString());
                        out.write("&nbsp;");
                        out.write(PageFlowUtil.filter(size));
                        return;
                    }
                }
            }
            out.write("<em>Not available</em>");
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(_containerFieldKey);
        }

        @Override
        public boolean isFilterable()
        {
            return false;
        }

        @Override
        public boolean isSortable()
        {
            return false;
        }
    }
}
