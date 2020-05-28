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
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.parser.SampleFile;

import java.util.Comparator;
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

    private String getEachSeriesTypePlotDataSql(int seriesIndex, int id, String schemaName, String queryName)
    {
        return "(SELECT PrecursorChromInfoId, SampleFileId, CAST(IFDEFINED(SeriesLabel) AS VARCHAR) AS SeriesLabel, "
                + "\nMetricValue, " + seriesIndex + " AS MetricSeriesIndex, " + id + " AS MetricId"
                + "\n FROM " + schemaName + '.' + queryName + ")";
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT X.MetricSeriesIndex, X.MetricId, X.SampleFileId, ");

        sql.append("\nCOALESCE(pci.PrecursorId.Id, pci.MoleculePrecursorId.Id) AS PrecursorId,");
        sql.append("\nCOALESCE(X.SeriesLabel, COALESCE(pci.PrecursorId.ModifiedSequence, pci.MoleculePrecursorId.CustomIonName, pci.MoleculePrecursorId.IonFormula) || (CASE WHEN COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) > 0 THEN ' +' ELSE ' ' END) || CAST(COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) AS VARCHAR)) AS SeriesLabel,");
        sql.append("\nCASE WHEN pci.PrecursorId.Id IS NOT NULL THEN 'Peptide' WHEN pci.MoleculePrecursorId.Id IS NOT NULL THEN 'Fragment' ELSE 'Other' END AS DataType,");
        sql.append("\nCOALESCE(pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz) AS MZ,");

        sql.append("\nX.PrecursorChromInfoId, sf.AcquiredTime, X.MetricValue, COALESCE(gs.RowId, 0) AS GuideSetId,");
        sql.append("\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,");
        sql.append("\nCASE WHEN (sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange");
        sql.append("\nFROM (");

        String sep = "";
        for (QCMetricConfiguration configuration: configurations)
        {
            int id = configuration.getId();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sql.append(sep).append(getEachSeriesTypePlotDataSql(1, id, schema1Name, query1Name));
            sep = "\nUNION\n";

            if (configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null)
            {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                sql.append(sep).append(getEachSeriesTypePlotDataSql(2, id, schema2Name, query2Name));
            }
        }

        sql.append(") X");
        sql.append("\nINNER JOIN SampleFile sf ON X.SampleFileId = sf.Id");
        sql.append("\nLEFT JOIN PrecursorChromInfo pci ON pci.Id = X.PrecursorChromInfoId");
        sql.append("\nLEFT JOIN QCMetricExclusion exclusion");
        sql.append("\nON sf.ReplicateId = exclusion.ReplicateId AND (exclusion.MetricId IS NULL OR exclusion.MetricId = x.MetricId)");
        sql.append("\nLEFT JOIN GuideSetForOutliers gs");
        sql.append("\nON ((sf.AcquiredTime >= gs.TrainingStart AND sf.AcquiredTime < gs.ReferenceEnd) OR (sf.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))");
        sql.append("\nWHERE sf.AcquiredTime IS NOT NULL");

        return sql.toString();
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        String labkeySQL = queryContainerSampleFileRawData(configurations);

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
    public List<SampleFileInfo> getSampleFiles(List<RawMetricDataSet> dataRows, Map<GuideSetKey, GuideSetStats> stats, Map<Integer, QCMetricConfiguration> metrics, Container container, Integer limit)
    {
        List<SampleFileInfo> result = TargetedMSManager.getSampleFiles(container, new SQLFragment("sf.AcquiredTime IS NOT NULL")).stream().map(SampleFile::toSampleFileInfo).collect(Collectors.toList());
        Map<Integer, SampleFileInfo> sampleFiles = result.stream().collect(Collectors.toMap(SampleFileInfo::getSampleId, Function.identity()));

        for (RawMetricDataSet dataRow : dataRows)
        {
            SampleFileInfo info = sampleFiles.get(dataRow.getSampleFileId());
            dataRow.increment(info, stats.get(dataRow.getGuideSetKey()));

            String metricLabel = getMetricLabel(metrics, dataRow);

            dataRow.increment(info.getMetricCounts(metricLabel), stats.get(dataRow.getGuideSetKey()));
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
        String metricLabel;
        switch (dataRow.getMetricSeriesIndex())
        {
            case 1:
                metricLabel = metric.getSeries1Label();
                break;
            case 2:
                metricLabel = metric.getSeries2Label();
                break;
            default:
                throw new IllegalArgumentException("Unexpected metric series index: " + dataRow.getMetricSeriesIndex());
        }
        return metricLabel;
    }
}
