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
package org.labkey.targetedms.outliers;

import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Set;


public class Outliers
{

    /**
     * LabKey Sql executor
     * @param container: container
     * @param user: user
     * @param sql: labkey sql to execute
     * @param columnNames: set of column names
     * @param sort: Sort object
     * @return retuns map collection
     */
    public static TableSelector executeQuery(Container container, User user, String sql, Set<String> columnNames, Sort sort)
    {
        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        assert query != null;
        return QueryService.get().selector(query, sql, columnNames, null, sort);
    }

    protected static String getExclusionWhereSql(int metricId)
    {
        return " WHERE SampleFileId.ReplicateId NOT IN "
                + "(SELECT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricId + ")";
    }
}
