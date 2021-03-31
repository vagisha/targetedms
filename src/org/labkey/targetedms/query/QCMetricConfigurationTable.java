/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.QCMetricConfiguration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QCMetricConfigurationTable extends FilteredTable<TargetedMSSchema>
{
    public QCMetricConfigurationTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableInfoQCMetricConfiguration(), schema, cf);
        wrapAllColumns(true);
        TargetedMSTable.fixupLookups(this);
        setInsertURL(LINK_DISABLER);
        setDeleteURL(LINK_DISABLER);
        setUpdateURL(LINK_DISABLER);
        setImportURL(LINK_DISABLER);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        if (filter.getType() == ContainerFilter.Type.Current)
            filter = getDefaultMetricContainerFilter(getContainer());

        super.applyContainerFilter(filter);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        Class<? extends Permission> permissionToCheck = perm == ReadPermission.class ? ReadPermission.class : AdminPermission.class;
        return getContainer().hasPermission(user, permissionToCheck);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new QCMetricConfigurationTableUpdateService(this, getRealTable());
    }

    public static ContainerFilter getDefaultMetricContainerFilter(Container currentContainer)
    {
        // the base set of configuration live at the root container
        List<Container> containers = new ArrayList<>();
        containers.add(ContainerManager.getRoot());
        containers.add(currentContainer);
        return new ContainerFilter.SimpleContainerFilter(containers);
    }

    public static class QCMetricConfigurationTableUpdateService extends DefaultQueryUpdateService
    {
        protected QCMetricConfigurationTableUpdateService(TableInfo queryTable, TableInfo realTable)
        {
            super(queryTable, realTable);
        }

        @Override
        protected Map<String, Object> insertRow(User user, Container container, Map<String, Object> row) throws DuplicateKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            var insertedRow = super.insertRow(user, container, row);
            calculateAndInsertTraceValuesForMetric((int) insertedRow.get("Id"), container, user);
            return insertedRow;
        }

        @Override
        protected Map<String, Object> updateRow(User user, Container container, Map<String, Object> row, @NotNull Map<String, Object> oldRow) throws InvalidKeyException, ValidationException, QueryUpdateServiceException, SQLException
        {
            var updatedRow = super.updateRow(user, container, row, oldRow);
            var metricId = (int) updatedRow.get("Id");
            deleteTraceValueForMetric(metricId, container);
            calculateAndInsertTraceValuesForMetric(metricId, container, user);
            return updatedRow;
        }

        @Override
        protected Map<String, Object> deleteRow(User user, Container container, Map<String, Object> oldRow) throws InvalidKeyException, QueryUpdateServiceException, SQLException
        {
            deleteTraceValueForMetric((Integer) oldRow.get("id"), container);
            return super.deleteRow(user, container, oldRow);
        }

        private void deleteTraceValueForMetric(int metricId, Container container)
        {
            var sql = new SQLFragment(" DELETE FROM " + TargetedMSManager.getTableQCTraceMetricValues() + " WHERE metric IN ("+
                    " SELECT qc.Id FROM " + TargetedMSManager.getTableInfoQCMetricConfiguration() + " qc" +
                    " INNER JOIN " + TargetedMSManager.getTableInfoSampleFileChromInfo() + " sfi ON sfi.TextId = qc.TraceName" +
                    " INNER JOIN " + TargetedMSManager.getTableInfoSampleFile() + " sf ON sf.id = sfi.SampleFileId" +
                    " INNER JOIN " + TargetedMSManager.getTableInfoReplicate() + " rep ON rep.Id = sf.ReplicateId" +
                    " INNER JOIN " + TargetedMSManager.getTableInfoRuns() + " r ON rep.RunId = r.Id" +
                    " WHERE qc.Id = ? AND r.container = ? ) ").add(metricId).add(container.getId());
            new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
        }

        private void calculateAndInsertTraceValuesForMetric(int metricId, Container container, User user)
        {
            var qcMetricConfigurations = TargetedMSManager
                    .getEnabledQCMetricConfigurations(container, user)
                    .stream()
                    .filter(qcMetricConfiguration -> qcMetricConfiguration.getId() == metricId)
                    .collect(Collectors.toList());
            var runsInContainer = TargetedMSManager.getRunsInContainer(container);

            for (TargetedMSRun run : runsInContainer)
            {
                var qcTraceMetricValues = TargetedMSManager.calculateTraceMetricValues(qcMetricConfigurations, run);
                qcTraceMetricValues.forEach(qcTraceMetricValue -> Table.insert(user, TargetedMSManager.getTableQCTraceMetricValues(), qcTraceMetricValue));
            }
        }

    }
}
