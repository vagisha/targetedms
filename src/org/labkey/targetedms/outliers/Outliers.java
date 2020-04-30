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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.model.LJOutlier;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.QCMetricConfiguration;

import java.util.List;
import java.util.Set;


public class Outliers
{

    public static List<LJOutlier> getLJOutliers(List<QCMetricConfiguration> configurations, Container container, User user, @Nullable Integer sampleLimit)
    {
        Set<String> columnNames = Set.of("guideSetId","metricId","metricName","metricLabel","sampleFile","acquiredTime","ignoreInQC","leveyJennings","totalCount");
        return executeQuery(container, user, queryContainerSampleFileStats(configurations, sampleLimit), columnNames, new Sort("-acquiredTime,metricLabel")).getArrayList(LJOutlier.class);
    }

    public static String queryContainerSampleFileStats(List<QCMetricConfiguration> configurations, @Nullable Integer sampleLimit)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        String sep = "";

        for(QCMetricConfiguration qcMetricConfiguration :configurations)
        {
            int id = qcMetricConfiguration.getId();
            String schema = qcMetricConfiguration.getSeries1SchemaName();
            String query = qcMetricConfiguration.getSeries1QueryName();

            sqlBuilder.append(sep).append("(").append(getLatestSampleFileStatSql(id,"Series1Label", schema, query, sampleLimit)).append(")");
            sep = "\nUNION\n";

            if(qcMetricConfiguration.getSeries2SchemaName() != null && qcMetricConfiguration.getSeries2QueryName() != null) {
                schema = qcMetricConfiguration.getSeries2SchemaName();
                query = qcMetricConfiguration.getSeries2QueryName();

                sqlBuilder.append(sep).append("(").append(getLatestSampleFileStatSql(id,"Series2Label", schema, query, sampleLimit)).append(")");
                sep = "\nUNION\n";
            }
        }

        return sqlBuilder.toString();
    }

    private static String getMetricGuideSetSql(int id, String schemaName, String queryName)
    {
        String series1SQL = "SELECT SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue FROM " + schemaName + "." + queryName;
        String exclusionWhereSQL = getExclusionWhereSql(id);

        return "SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, " +
                "\nCOUNT(p.SampleFileId) AS NumRecords, " +
                "\nAVG(p.MetricValue) AS Mean, " +
                "\nSTDDEV(p.MetricValue) AS StandardDev " +
                "\nFROM GuideSetForOutliers gs" +
                "\nLEFT JOIN (" + series1SQL + exclusionWhereSQL + ") as p" +
                "\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd" +
                "\nGROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel";
    }

    /**
     * @return UNION SQL query for the relevant metrics to get the summary info for the last N sample files
     */
    private static String getLatestSampleFileStatSql(int metricId, String metricLabel, String schema, String query, @Nullable Integer sampleLimit)
    {
        return "SELECT MAX(stats.GuideSetId) AS GuideSetId,"
                + "\n'" + metricId + "' AS MetricId,"
                + "\n" + "(SELECT name FROM qcmetricconfiguration where id = " + metricId + ")" + " AS MetricName,"
                + "\n" + "(SELECT " + metricLabel + " FROM qcmetricconfiguration where id = " + metricId + ")" + " AS MetricLabel,"
                + "\nX.SampleFile,"
                + "\nX.AcquiredTime,"
                + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
                + "\nSUM(CASE WHEN exclusion.ReplicateId IS NULL AND (X.MetricValue > (stats.Mean + (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))"
                + "\n   OR X.MetricValue < (stats.Mean - (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))) THEN 1 ELSE 0 END) AS LeveyJennings,"
                + "\nCOUNT(*) AS TotalCount"
                + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
                + "\n      FROM " + schema + "." + query
                + (sampleLimit == null ? "" : "\n      WHERE SampleFileId.Id IN (SELECT Id FROM SampleFile WHERE AcquiredTime IS NOT NULL ORDER BY AcquiredTime DESC LIMIT " + sampleLimit + ")")
                + "\n) X"
                + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricId + ") exclusion"
                + "\nON X.ReplicateId = exclusion.ReplicateId"
                + "\nLEFT JOIN (" + getMetricGuideSetSql(metricId, schema, query) + ") stats"
                + "\nON X.SeriesLabel = stats.SeriesLabel"
                + "\nAND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)"
                + "\n   OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))"
                + "\nGROUP BY X.SampleFile, X.AcquiredTime, exclusion.ReplicateId";
    }

    public static TableSelector executeQuery(Container container, User user, String labkeySQL, Set<String> columnNames, Sort sort)
    {
        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        assert query != null;
        return QueryService.get().selector(query, labkeySQL, columnNames, null, sort);
    }

    protected static String getExclusionWhereSql(int metricId)
    {
        return " WHERE SampleFileId.ReplicateId NOT IN "
                + "(SELECT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricId + ")";
    }
}
