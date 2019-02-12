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
     * @return retuns map collection
     */
    public TableSelector executeQuery(Container container, User user, String sql)
    {
        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        assert query != null;
        return QueryService.get().selector(query, sql);
    }

    public TableSelector executeQuery(Container container, User user, String sql, Set<String> columnNames, Sort sort)
    {
        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        assert query != null;
        return QueryService.get().selector(query, sql, columnNames, null, sort);
    }

    protected String getExclusionWhereSql(int metricId)
    {
        return " WHERE SampleFileId.ReplicateId NOT IN "
                + "(SELECT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricId + ")";
    }
}
