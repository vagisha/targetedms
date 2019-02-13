package org.labkey.targetedms.outliers;

import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.security.User;
import org.labkey.targetedms.model.LJOutlier;
import org.labkey.targetedms.model.QCMetricConfiguration;

import java.util.List;
import java.util.Set;

public class LevyJenningsOutliers extends Outliers
{
    private static LevyJenningsOutliers _instance = new LevyJenningsOutliers();

    private LevyJenningsOutliers()
    {
        // prevent external construction with a private default constructor
    }

    public static LevyJenningsOutliers get()
    {
        return _instance;
    }

    public List<LJOutlier> getLJOutliers(List<QCMetricConfiguration> configurations, Container container, User user)
    {
        Set<String> columnNames = Set.of("guideSetId","metricId","metricName","metricLabel","sampleFile","acquiredTime","ignoreInQC","nonConformers","totalCount");
        return executeQuery(container, user, queryContainerSampleFileStats(configurations), columnNames, new Sort("-acquiredTime,metricLabel")).getArrayList(LJOutlier.class);
    }

    public String queryContainerSampleFileStats(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        String sep = "";

        for(QCMetricConfiguration qcMetricConfiguration :configurations)
        {
            int id = qcMetricConfiguration.getId();
            String name = qcMetricConfiguration.getName();
            String label = qcMetricConfiguration.getSeries1Label();
            String schema = qcMetricConfiguration.getSeries1SchemaName();
            String query = qcMetricConfiguration.getSeries1QueryName();

            sqlBuilder.append(sep).append("(").append(getLatestSampleFileStatSql(id, name, label, schema, query)).append(")");
            sep = "\nUNION\n";

            if(qcMetricConfiguration.getSeries2SchemaName() != null && qcMetricConfiguration.getSeries2QueryName() != null) {
                label = qcMetricConfiguration.getSeries2Label();
                schema = qcMetricConfiguration.getSeries2SchemaName();
                query = qcMetricConfiguration.getSeries2QueryName();

                sqlBuilder.append(sep).append("(").append(getLatestSampleFileStatSql(id, name, label, schema, query)).append(")");
                sep = "\nUNION\n";
            }
        }

        return sqlBuilder.toString();
    }


    private String includeAllValuesSql(String schema, String table, String series, String exclusion)
    {
        return "\nUNION"
                + "\nSELECT NULL AS GuideSetId, MIN(SampleFileId.AcquiredTime) AS TrainingStart, MAX(SampleFileId.AcquiredTime) AS TrainingEnd,"
                + "\nNULL AS ReferenceEnd, SeriesLabel, \'" + series + "\' AS SeriesType, COUNT(SampleFileId) AS NumRecords, AVG(MetricValue) AS Mean, STDDEV(MetricValue) AS StandardDev"
                + "\nFROM " + schema + "." + table
                + exclusion
                + "\nGROUP BY SeriesLabel";
    }

    private String getMetricGuideSetSql(int id, String schema1Name, String query1Name)
    {
        //boolean includeSeries2 = !schema2Name.isEmpty() && !query2Name.isEmpty();
        String selectCols = "SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue";
        String series1SQL = "SELECT \'series1\' AS SeriesType, " + selectCols + " FROM "+ schema1Name + "." + query1Name;
        //String series2SQL = !includeSeries2 ? "" : " UNION SELECT \'series2\' AS SeriesType, " + selectCols + " FROM "+ schema2Name + "." + query2Name;
        String exclusionWhereSQL = this.getExclusionWhereSql(id);

        //        sqlBuilder.append(!includeAllValues ? "": (this.includeAllValuesSql(schema1Name, query1Name, "series1", exclusionWhereSQL) +
//                (includeSeries2 ? (includeAllValuesSql(schema2Name, query2Name, "series2", exclusionWhereSQL)) : "")));

        return "SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.SeriesType, " +
                "\nCOUNT(p.SampleFileId) AS NumRecords, " +
                "\nAVG(p.MetricValue) AS Mean, " +
                "\nSTDDEV(p.MetricValue) AS StandardDev " +
                "\nFROM guideset gs" +
                "\nLEFT JOIN (" + series1SQL + /*series2SQL +*/ exclusionWhereSQL + ") as p" + /*series2SQL +*/
                "\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd" +
                "\nGROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.SeriesType";
    }

    /**
     *
     * @param id - metricId
     * @param name - metricName
     * @param label - metricLabel
     * @param schema - schemaName
     * @param query - queryName
     * @return UNION SQL query for the relevant metrics to get the summary info for the last N sample files
     *
     */
    private String getLatestSampleFileStatSql(int id, String name, String label, String schema, String query)
    {
        return "SELECT stats.GuideSetId,"
                + "\n'" + id + "' AS MetricId,"
                + "\n'" + name + "' AS MetricName,"
                + "\n'" + label + "' AS MetricLabel,"
                + "\nX.SampleFile,"
                + "\nX.AcquiredTime,"
                + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
                + "\nSUM(CASE WHEN exclusion.ReplicateId IS NULL AND (X.MetricValue > (stats.Mean + (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))"
                + "\n   OR X.MetricValue < (stats.Mean - (3 * (CASE WHEN stats.StandardDev IS NULL THEN 0 ELSE stats.StandardDev END)))) THEN 1 ELSE 0 END) AS NonConformers,"
                + "\nCOUNT(*) AS TotalCount"
                + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
                + "\n      FROM " + schema + "." + query
                + "\n      WHERE SampleFileId.Id IN (SELECT Id FROM SampleFile WHERE AcquiredTime IS NOT NULL ORDER BY AcquiredTime DESC LIMIT 3)"
                + "\n) X"
                + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + id + ") exclusion"
                + "\nON X.ReplicateId = exclusion.ReplicateId"
                + "\nLEFT JOIN (" + getMetricGuideSetSql(id, schema, query) + ") stats"
                + "\nON X.SeriesLabel = stats.SeriesLabel"
                + "\nAND ((X.AcquiredTime >= stats.TrainingStart AND X.AcquiredTime < stats.ReferenceEnd)"
                + "\n   OR (X.AcquiredTime >= stats.TrainingStart AND stats.ReferenceEnd IS NULL))"
                + "\nGROUP BY stats.GuideSetId, X.SampleFile, X.AcquiredTime, exclusion.ReplicateId";
        //+ "\nORDER BY X.AcquiredTime DESC";
    }
}