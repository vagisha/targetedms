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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.QCPlotFragment;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.parser.SampleFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutlierGenerator
{
    private static final OutlierGenerator INSTANCE = new OutlierGenerator();

    private OutlierGenerator() {}

    public static OutlierGenerator get()
    {
        return INSTANCE;
    }

    private String getEachSeriesTypePlotDataSql(int seriesIndex, QCMetricConfiguration configuration, List<AnnotationGroup> annotationGroups)
    {
        String schemaName;
        String queryName;
        if (seriesIndex == 1)
        {
            schemaName = configuration.getSeries1SchemaName();
            queryName = configuration.getSeries1QueryName();
        }
        else
        {
            schemaName = configuration.getSeries2SchemaName();
            queryName = configuration.getSeries2QueryName();
        }
        StringBuilder sql = new StringBuilder();

        // handle trace metrics
        if (configuration.getTraceName() != null)
        {
            sql.append("(SELECT 0 AS PrecursorChromInfoId, SampleFileId, SampleFileId.FilePath, SampleFileId.ReplicateId.Id AS ReplicateId ,");
            sql.append(" metric.Name AS SeriesLabel, ");
            sql.append("\nvalue as MetricValue, metric, ").append(seriesIndex).append(" AS MetricSeriesIndex, ").append(configuration.getId()).append(" AS MetricId");
            sql.append("\n FROM ").append(schemaName).append('.').append(TargetedMSManager.getTableQCTraceMetricValues().getName());
            sql.append(" WHERE metric = ").append(configuration.getId());
            sql.append(")");
        }
        else
        {
            sql.append("(SELECT PrecursorChromInfoId, SampleFileId, SampleFileId.FilePath, SampleFileId.ReplicateId.Id AS ReplicateId ,");
            sql.append(" CAST(IFDEFINED(SeriesLabel) AS VARCHAR) AS SeriesLabel, ");
            sql.append("\nMetricValue, 0 as metric, ").append(seriesIndex).append(" AS MetricSeriesIndex, ").append(configuration.getId()).append(" AS MetricId");
            sql.append("\n FROM ").append(schemaName).append('.').append(queryName);
            if (!annotationGroups.isEmpty())
            {
                sql.append(" WHERE ");
                StringBuilder filterClause = new StringBuilder("SampleFileId.ReplicateId IN (");
                var intersect = "";
                var selectSql = "(SELECT ReplicateId FROM targetedms.ReplicateAnnotation WHERE ";
                for (AnnotationGroup annotation : annotationGroups)
                {
                    filterClause.append(intersect)
                            .append(selectSql)
                            .append(" Name='")
                            .append(annotation.getName().replace("'", "''"))
                            .append("'");


                    var annotationValues = annotation.getValues();
                    if (!annotationValues.isEmpty())
                    {
                        var quoteEscapedVals = annotationValues.stream().map(s -> s.replace("'", "''")).collect(Collectors.toList());
                        var vals = "'" + StringUtils.join(quoteEscapedVals, "','") + "'";
                        filterClause.append(" AND  Value IN (").append(vals).append(" )");
                    }
                    filterClause.append(" ) ");
                    intersect = " INTERSECT ";
                }
                filterClause.append(") ");
                sql.append(filterClause.toString());
            }
            if (configuration.getTraceName() != null)
            {
                sql.append(" WHERE metric = ").append(configuration.getId());
            }
            sql.append(")");
        }
        return sql.toString();
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations, Date startDate, Date endDate, List<AnnotationGroup> annotationGroups, boolean showExcluded)
    {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT X.MetricSeriesIndex, X.MetricId, X.SampleFileId, ");

        sql.append(" X.FilePath, X.ReplicateId, ");

        sql.append("\nCOALESCE(pci.PrecursorId.Id, pci.MoleculePrecursorId.Id) AS PrecursorId,");

        sql.append("\nX.SeriesLabel,");

        sql.append("\npci.PrecursorId.ModifiedSequence,");
        sql.append("\npci.MoleculePrecursorId.CustomIonName,");
        sql.append("\npci.MoleculePrecursorId.IonFormula,");

        sql.append("\npci.MoleculePrecursorId.massMonoisotopic,");
        sql.append("\npci.MoleculePrecursorId.massAverage,");
        sql.append("\n(CASE WHEN COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) > 0 THEN ' +' ELSE ' ' END)");
        sql.append("\n    || CAST(COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) AS VARCHAR) AS PrecursorCharge,");

        sql.append("\nCASE WHEN pci.PrecursorId.Id IS NOT NULL THEN 'Peptide' WHEN pci.MoleculePrecursorId.Id IS NOT NULL THEN 'Fragment' ELSE 'Other' END AS DataType,");
        sql.append("\nCOALESCE(pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz) AS MZ,");

        sql.append("\nX.PrecursorChromInfoId, sf.AcquiredTime, X.MetricValue, COALESCE(gs.RowId, 0) AS GuideSetId,");
        sql.append("\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,");
        sql.append("\nCASE WHEN (sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange");
        sql.append("\nFROM (");

        String sep = "";
        for (QCMetricConfiguration configuration : configurations)
        {
            sql.append(sep).append(getEachSeriesTypePlotDataSql(1, configuration, annotationGroups));
            sep = "\nUNION\n";
            if (configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null)
            {
                sql.append(sep).append(getEachSeriesTypePlotDataSql(2, configuration, annotationGroups));
            }
        }

        sql.append(") X");
        sql.append("\nINNER JOIN SampleFile sf ON X.SampleFileId = sf.Id");
        sql.append("\nLEFT JOIN PrecursorChromInfo pci ON pci.Id = X.PrecursorChromInfoId");
        sql.append("\nLEFT JOIN QCMetricExclusion exclusion");
        sql.append("\nON sf.ReplicateId = exclusion.ReplicateId AND (exclusion.MetricId IS NULL OR exclusion.MetricId = x.MetricId)");
        sql.append("\nLEFT JOIN GuideSetForOutliers gs");
        sql.append("\nON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))");
        if (null != startDate || null != endDate)
        {
            var sqlSeparator = "WHERE";

            if (null != startDate)
            {
                sql.append("\n").append(sqlSeparator);
                sql.append(" sf.AcquiredTime >= '");
                sql.append(startDate);
                sql.append("' ");
                sqlSeparator = "AND";
            }

            if (null != endDate)
            {
                sql.append("\n").append(sqlSeparator);
                sql.append("\n sf.AcquiredTime < TIMESTAMPADD('SQL_TSI_DAY', 1, CAST('");
                sql.append(endDate);
                sql.append("' AS TIMESTAMP))");
            }
        }
        else
        {
            sql.append("\nWHERE sf.AcquiredTime IS NOT NULL");
        }
        if (!showExcluded)
        {
            sql.append(" AND sf.Excluded = false");
        }

        return sql.toString();
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations, Date startDate, Date endDate, List<AnnotationGroup> annotationGroups, boolean showExcluded)
    {
        String labkeySQL = queryContainerSampleFileRawData(configurations, startDate, endDate, annotationGroups, showExcluded);

        return QueryService.get().selector(
                new TargetedMSSchema(user, container),
                labkeySQL,
                TableSelector.ALL_COLUMNS,
                null,
                new Sort("MetricSeriesIndex,seriesLabel,acquiredTime")).getArrayList(RawMetricDataSet.class);
    }

    /**
     * Calculate guide set stats for Levey-Jennings and moving range comparisons.
     * @param guideSets id to GuideSet
     */
    public Map<GuideSetKey, GuideSetStats> getAllProcessedMetricGuideSets(List<RawMetricDataSet> rawMetricData, Map<Integer, GuideSet> guideSets)
    {
        Map<GuideSetKey, GuideSetStats> result = new HashMap<>();

        for (RawMetricDataSet row : rawMetricData)
        {
            GuideSetKey key = row.getGuideSetKey();
            GuideSetStats stats = result.computeIfAbsent(row.getGuideSetKey(), x -> new GuideSetStats(key, guideSets.get(key.getGuideSetId())));
            stats.addRow(row);
        }

        result.values().forEach(GuideSetStats::calculateStats);
        return result;
    }

    /**
     * @param metrics id to QC metric  */
    public List<SampleFileInfo> getSampleFiles(List<RawMetricDataSet> dataRows, Map<GuideSetKey, GuideSetStats> allStats, Map<Integer, QCMetricConfiguration> metrics, Container container, Integer limit)
    {
        List<SampleFileInfo> result = TargetedMSManager.getSampleFiles(container, new SQLFragment("sf.AcquiredTime IS NOT NULL")).stream().map(SampleFile::toSampleFileInfo).collect(Collectors.toList());
        Map<Long, SampleFileInfo> sampleFiles = result.stream().collect(Collectors.toMap(SampleFileInfo::getSampleId, Function.identity()));

        for (RawMetricDataSet dataRow : dataRows)
        {
            SampleFileInfo sampleFile = sampleFiles.get(dataRow.getSampleFileId());
            GuideSetStats stats = allStats.get(dataRow.getGuideSetKey());

            // If data was deleted after the full metric data was queried, but before we got here, the sample file
            // might not be present anymore. Not a real-world scenario, but turns up when TeamCity is deleting
            // the container at the end of the test run immediately after the crawler has fired a bunch of requests
            if (sampleFile != null)
            {
                dataRow.increment(sampleFile, stats);

                String metricLabel = getMetricLabel(metrics, dataRow);
                dataRow.increment(sampleFile.getMetricCounts(metricLabel), stats);
            }
        }

        // Order so most recent are at the top, and limit if requested
        result.sort(Comparator.comparing(SampleFileInfo::getAcquiredTime).reversed());
        if (limit != null && result.size() > limit.intValue())
        {
            result = result.subList(0, limit.intValue());
        }

        return result;
    }

    /** @param metrics id to QC metric */
    public String getMetricLabel(Map<Integer, QCMetricConfiguration> metrics, RawMetricDataSet dataRow)
    {
        QCMetricConfiguration metric = metrics.get(dataRow.getMetricId());
        return switch (dataRow.getMetricSeriesIndex())
                {
                    case 1 -> metric.getSeries1Label();
                    case 2 -> metric.getSeries2Label();
                    default -> throw new IllegalArgumentException("Unexpected metric series index: " + dataRow.getMetricSeriesIndex());
                };
    }
    /**
     * returns the separated plots data per peptide
     * */
    public List<QCPlotFragment> getQCPlotFragment(List<RawMetricDataSet> rawMetricData, Map<GuideSetKey, GuideSetStats> stats)
    {
        List<QCPlotFragment> qcPlotFragments = new ArrayList<>();
        Map<String, List<RawMetricDataSet>> rawMetricDataSetMapByLabel = new HashMap<>();
        for (RawMetricDataSet rawMetricDataSet : rawMetricData)
        {
            rawMetricDataSetMapByLabel.computeIfAbsent(rawMetricDataSet.getSeriesLabel(),label -> new ArrayList<>());
            rawMetricDataSetMapByLabel.get(rawMetricDataSet.getSeriesLabel()).add(rawMetricDataSet);

        }

        for (Map.Entry<String, List<RawMetricDataSet>> entry : rawMetricDataSetMapByLabel.entrySet())
        {
            QCPlotFragment qcPlotFragment = new QCPlotFragment();
            qcPlotFragment.setSeriesLabel(entry.getKey());

            /* Common values for the whole peptide */
            qcPlotFragment.setDataType(entry.getValue().get(0).getDataType());
            qcPlotFragment.setmZ(entry.getValue().get(0).getMz());

            qcPlotFragment.setQcPlotData(entry.getValue());

            qcPlotFragments.add(qcPlotFragment);

            List<GuideSetStats> guideSetStatsList = new ArrayList<>();
            stats.forEach(((guideSetKey, guideSetStats) -> {
                if (guideSetKey.getSeriesLabel().equalsIgnoreCase(qcPlotFragment.getSeriesLabel()))
                {
                    guideSetStatsList.add(guideSetStats);
                }
            }));
            qcPlotFragment.setGuideSetStats(guideSetStatsList);
        }

        qcPlotFragments.sort(Comparator.comparing(QCPlotFragment::getSeriesLabel));
        return qcPlotFragments;
    }

    public static class AnnotationGroup
    {
        private String name;
        private List<String> values;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List<String> getValues()
        {
            return values;
        }

        public void setValues(List<String> values)
        {
            this.values = values;
        }
    }
}
