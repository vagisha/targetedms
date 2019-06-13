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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.GuideSet;

import java.sql.SQLException;

/**
 * Created by cnathe on 4/9/2015.
 */
public class GuideSetTable extends FilteredTable<TargetedMSSchema>
{
    public GuideSetTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableInfoGuideSet(), schema);

        wrapAllColumns(true);
        getMutableColumn("Container").setFk(new ContainerForeignKey(schema));

        // add expr column to calculate the reference end date for a guide set, can be null if it is the last guide set
        ExprColumn referenceEndCol = new ExprColumn(this, FieldKey.fromParts("ReferenceEnd"), getReferenceEndSql(), JdbcType.TIMESTAMP);
        referenceEndCol.setDescription("The end date and time for runs that reference this guide set. A null value in "
                + "this field indicates that the guide is open-ended and still in use.");
        referenceEndCol.setFormat("yyyy-MM-dd HH:mm");
        addColumn(referenceEndCol);

        setImportURL(LINK_DISABLER);
    }

    private SQLFragment getReferenceEndSql()
    {
        SQLFragment sql = new SQLFragment("(SELECT MIN(gs2.TrainingStart) FROM ");
        sql.append(TargetedMSManager.getTableInfoGuideSet(), "gs2");
        sql.append(" WHERE gs2.TrainingStart > ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".TrainingEnd AND gs2.Container = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".Container)");
        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new GuideSetTableUpdateService(this);
    }

    public static class GuideSetTableUpdateService extends RowIdQueryUpdateService<GuideSet>
    {
        public GuideSetTableUpdateService(GuideSetTable guideSetTable)
        {
            super(guideSetTable);
        }

        @Override
        protected GuideSet createNewBean()
        {
            return new GuideSet();
        }

        @Override
        public GuideSet get(User user, Container container, int key)
        {
            return new TableSelector(TargetedMSManager.getTableInfoGuideSet()).getObject(key, GuideSet.class);
        }

        @Override
        protected GuideSet insert(User user, Container container, GuideSet bean) throws ValidationException
        {
            bean.beforeInsert(user, container.getId());
            validateGuideSetDates(bean, container);
            return Table.insert(user, TargetedMSManager.getTableInfoGuideSet(), bean);
        }

        @Override
        protected GuideSet update(User user, Container container, GuideSet bean, Integer oldKey) throws ValidationException
        {
            if (oldKey == null)
            {
                throw new ValidationException("RowId is required for updates");
            }
            bean.beforeUpdate(user);
            validateGuideSetDates(bean, container);
            return Table.update(user, TargetedMSManager.getTableInfoGuideSet(), bean, oldKey);
        }

        @Override
        public void delete(User user, Container container, int key)
        {
            Table.delete(TargetedMSManager.getTableInfoGuideSet(), key);
        }

        private void validateGuideSetDates(GuideSet bean, Container container) throws ValidationException
        {
            // check that the training start date is before training end date
            if (bean.getTrainingStart().after(bean.getTrainingEnd()))
            {
                throw new ValidationException("The training start date/time must be before the training end date/time.");
            }

            // check to make sure this training date range doesn't overlap any other guide set training date ranges (overlap if StartA < EndB and EndA > StartB)
            SqlSelector selector = new SqlSelector(TargetedMSManager.getSchema(), getOverlappingTrainingDateRangeSql(bean, container));
            if (selector.getRowCount() > 0)
            {
                throw new ValidationException("The training date range overlaps with an existing guide set's training date range.");
            }
        }

        private SQLFragment getOverlappingTrainingDateRangeSql(GuideSet bean, Container container)
        {
            SQLFragment sql = new SQLFragment("SELECT RowId FROM ");
            sql.append(TargetedMSManager.getTableInfoGuideSet(), "gs");
            sql.append(" WHERE ? <= gs.TrainingEnd AND ? >= gs.TrainingStart AND Container = ? ");
            sql.add(bean.getTrainingStart());
            sql.add(bean.getTrainingEnd());
            sql.add(container.getId());
            if (bean.getRowId() != 0)
            {
                sql.append(" AND gs.RowId != ?");
                sql.add(bean.getRowId());
            }
            return sql;
        }
    }
}
