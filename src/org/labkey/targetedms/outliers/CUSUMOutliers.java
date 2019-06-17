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

import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.security.User;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.GuideSetAvgMR;
import org.labkey.targetedms.model.LJOutlier;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.RawGuideSet;
import org.labkey.targetedms.model.RawMetricDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CUSUMOutliers extends  Outliers
{

    private String metricGuideSetRawSql(int id, String schema1Name, String query1Name, String schema2Name, String query2Name, boolean includeAllValues, String series)
    {
        boolean includeSeries2 = schema2Name != null && query2Name != null;
        String selectCols = "SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue";
        String series1SQL = "SELECT \'" + (series != null ? series : "series1") + "\' AS SeriesType, " + selectCols + " FROM "+ schema1Name + "." + query1Name;
        String series2SQL = !includeSeries2 ? "" : " UNION SELECT \'series2\' AS SeriesType, " + selectCols + " FROM "+ schema2Name + "." + query2Name;
        String exclusionWhereSQL = getExclusionWhereSql(id);

        return "SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel, p.AcquiredTime, p.MetricValue, p.SeriesType, " +
                "\nFROM guideset gs" +
                (includeAllValues ? "\nFULL" : "\nLEFT") + " JOIN (" + series1SQL + series2SQL + exclusionWhereSQL + ") as p"+
                "\n  ON p.AcquiredTime >= gs.TrainingStart AND p.AcquiredTime <= gs.TrainingEnd" +
                "\n ORDER BY GuideSetId, p.SeriesLabel, p.AcquiredTime";
    }

    private String getSingleMetricGuideSetRawSql(int metricId, String metricType, String schemaName, String queryName, String series)
    {
        return  "SELECT s.*, g.Comment, '" +  metricType + "' AS MetricType FROM (" +
                metricGuideSetRawSql(metricId, schemaName, queryName, null, null, false, series) +
                ") s" +
                " LEFT JOIN GuideSet g ON g.RowId = s.GuideSetId";
    }

    private String queryContainerSampleFileRawGuideSetStats(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        String sep = "";

        for(QCMetricConfiguration configuration: configurations)
        {
            int id = configuration.getId();
            String label = configuration.getSeries1Label();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sqlBuilder.append(sep).append("(").append(getSingleMetricGuideSetRawSql(id, label, schema1Name, query1Name, "series1")).append(")");
            sep = "\nUNION\n";

            if(configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null) {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                label = configuration.getSeries2Label();
                sqlBuilder.append(sep).append("(").append(getSingleMetricGuideSetRawSql(id, label, schema2Name, query2Name, "series2")).append(")");
            }
        }
        return "SELECT * FROM (" + sqlBuilder.toString() + ") a"; //wrap unioned results in sql to support sorting
    }

    private String getEachSeriesTypePlotDataSql(String type, int id, String schemaName, String queryName, String whereClause, String metricType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT '").append(type).append("' AS SeriesType, X.SampleFile, ");
        if(metricType != null)
        {
            sb.append("'").append(metricType).append("'").append(" AS MetricType, ");
        }
        sb.append("\nX.PrecursorId, X.PrecursorChromInfoId, X.SeriesLabel, X.DataType, X.mz, X.AcquiredTime,"
                + "\nX.FilePath, X.MetricValue, x.ReplicateId, gs.RowId AS GuideSetId,"
                + "\nCASE WHEN (exclusion.ReplicateId IS NOT NULL) THEN TRUE ELSE FALSE END AS IgnoreInQC,"
                + "\nCASE WHEN (X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime <= gs.TrainingEnd) THEN TRUE ELSE FALSE END AS InGuideSetTrainingRange"
                + "\nFROM (SELECT *, SampleFileId.AcquiredTime AS AcquiredTime, SampleFileId.FilePath AS FilePath,"
                + "\n      SampleFileId.SampleName AS SampleFile, SampleFileId.ReplicateId AS ReplicateId"
                + "\n      FROM " + schemaName + '.' + queryName + whereClause + ") X "
                + "\nLEFT JOIN (SELECT DISTINCT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + id + ") exclusion"
                + "\nON X.ReplicateId = exclusion.ReplicateId"
                + "\nLEFT JOIN guideset gs"
                + "\nON ((X.AcquiredTime >= gs.TrainingStart AND X.AcquiredTime < gs.ReferenceEnd) OR (X.AcquiredTime >= gs.TrainingStart AND gs.ReferenceEnd IS NULL))"
                + "\nORDER BY X.SeriesLabel, SeriesType, X.AcquiredTime");

        return "(" + sb.toString() + ")";
    }

    private String queryContainerSampleFileRawData(List<QCMetricConfiguration> configurations)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        String sep = "";
        String where ="";

        for(QCMetricConfiguration configuration: configurations)
        {
            int id = configuration.getId();
            String label = configuration.getSeries1Label();
            String schema1Name = configuration.getSeries1SchemaName();
            String query1Name = configuration.getSeries1QueryName();
            sqlBuilder.append(sep).append("(").append(getEachSeriesTypePlotDataSql("series1", id, schema1Name, query1Name, where, label)).append(")");
            sep = "\nUNION\n";

            if(configuration.getSeries2SchemaName() != null && configuration.getSeries2QueryName() != null) {
                String schema2Name = configuration.getSeries2SchemaName();
                String query2Name = configuration.getSeries2QueryName();
                label = configuration.getSeries2Label();
                sqlBuilder.append(sep).append("(").append(getEachSeriesTypePlotDataSql("series2", id, schema2Name, query2Name, where, label)).append(")");
            }
        }
        return "SELECT * FROM (" + sqlBuilder.toString() + ") a"; //wrap unioned results in sql to support sorting
    }

    public List<RawGuideSet> getRawGuideSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        Set<String> columnNames = Set.of("trainingStart","trainingEnd","seriesLabel","metricValue","metricType","acquiredTime","seriesType","comment","referenceEnd","guideSetId");

        return executeQuery(container, user, queryContainerSampleFileRawGuideSetStats(configurations), columnNames, new Sort("guideSetId,seriesLabel,acquiredTime")).getArrayList(RawGuideSet.class);
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        Set<String> columnNames = Set.of("seriesType","sampleFile","metricType","precursorId","precursorChromInfoId","seriesLabel",
                "dataType","mz","acquiredTime","filePath","metricValue","replicateId","guideSetId","ignoreInQC","inGuideSetTrainingRange");

        return executeQuery(container, user, queryContainerSampleFileRawData(configurations), columnNames, new Sort("seriesType,seriesLabel,acquiredTime")).getArrayList(RawMetricDataSet.class);
    }

    private Map<String, Map<GuideSetAvgMR, List<Map<String, Double>>>> getAllProcessedMetricGuideSets(List<RawGuideSet> rawGuideSets)
    {
        Map<String, List<RawGuideSet>> metricGuideSet = new LinkedHashMap<>();
        Map<String, Map<GuideSetAvgMR, List<Map<String, Double>>>> processedMetricGuides = new LinkedHashMap<>();
        MovingRangeOutliers mr = new MovingRangeOutliers();

        for (RawGuideSet row : rawGuideSets)
        {
            String metricType = row.getMetricType();
            if(metricGuideSet.get(metricType) == null) {
                List<RawGuideSet> sets = new ArrayList<>();
                rawGuideSets.forEach(gs->{
                    if(gs.getMetricType() != null && gs.getMetricType().equals(metricType))
                        sets.add(gs);
                });
                metricGuideSet.put(row.getMetricType(), sets);
            }
        }

        metricGuideSet.forEach((key, val) -> processedMetricGuides.put(key, mr.getGuideSetAvgMRs(val)));
        return processedMetricGuides;
    }

    private class PlotData
    {
        String seriesLabel;
        String series;
        String seriesType;

        public String getSeriesLabel()
        {
            return seriesLabel;
        }

        public void setSeriesLabel(String seriesLabel)
        {
            this.seriesLabel = seriesLabel;
        }

        public String getSeries()
        {
            return series;
        }

        public void setSeries(String series)
        {
            this.series = series;
        }

        public String getSeriesType()
        {
            return seriesType;
        }

        public void setSeriesType(String seriesType)
        {
            this.seriesType = seriesType;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlotData plotData = (PlotData) o;
            return seriesLabel.equals(plotData.seriesLabel) &&
                    series.equals(plotData.series) &&
                    seriesType.equals(plotData.seriesType);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(seriesLabel, series, seriesType);
        }
    }

    private Map<PlotData, List<Map<String, List<?>>>> preprocessPlotData(List<RawMetricDataSet> plotDataRows, boolean hasMR, boolean hasCUSUMm, boolean hasCUSUMv, boolean isLogScale)
    {
        Map<PlotData, List<Map<String, List<?>>>> plotDataMap = new LinkedHashMap<>();

        if(plotDataRows.size() > 0)
        {
            plotDataRows.forEach(row-> {
                PlotData plotData = new PlotData();
                plotData.setSeriesLabel(row.getSeriesLabel());
                plotData.setSeries("Series");
                plotData.setSeriesType(row.getSeriesType());

                if(plotDataMap.get(plotData) == null)
                {
                    List<Map<String, List<?>>> serTypeList = new ArrayList<>();
                    Map<String, List<?>> rowsMap = new LinkedHashMap<>();
                    Map<String, List<?>> metricValuesMap = new LinkedHashMap<>();
                    List<RawMetricDataSet> rowsList = new ArrayList<>();
                    List<Double> metricValuesList = new ArrayList<>();

                    plotDataRows.forEach(pR ->{
                        if(pR.getSeriesLabel().equalsIgnoreCase(plotData.getSeriesLabel()) &&
                            pR.getSeriesType().equalsIgnoreCase(plotData.getSeriesType()))
                        {
                            rowsList.add(pR);
                            if(pR.getMetricValue() == null)
                                metricValuesList.add(0.0d);
                            else
                               metricValuesList.add((double) Math.round(pR.getMetricValue() * 10000) / 10000.0);

                        }
                    });

                    rowsMap.put("Rows", rowsList);
                    metricValuesMap.put("MetricValues", metricValuesList);

                    serTypeList.add(rowsMap);
                    serTypeList.add(metricValuesMap);

                    plotDataMap.put(plotData, serTypeList);
                }
            });

            if(hasMR || hasCUSUMm || hasCUSUMv)
            {
                plotDataMap.forEach((plotData, seriesList) ->  {
                    List<?> metricValsList = seriesList.get(1).get("MetricValues");
                    Double[] metricVals = metricValsList.toArray(new Double[0]);
                    Double[] mRs = new Double[0];
                    double[] positiveCUSUMm = new double[0];
                    double[] negativeCUSUMm = new double[0];
                    double[] positiveCUSUMv = new double[0];
                    double[] negativeCUSUMv = new double[0];

                    if(hasMR)
                    {
                        mRs = Stats.getMovingRanges(metricVals, isLogScale, null);
                    }

                    if(hasCUSUMm)
                    {
                        positiveCUSUMm = Stats.getCUSUMS(metricVals, false, false, isLogScale, null);
                        negativeCUSUMm = Stats.getCUSUMS(metricVals, true, false, isLogScale, null);
                    }

                    if(hasCUSUMv)
                    {
                        positiveCUSUMv = Stats.getCUSUMS(metricVals, false, true, isLogScale, null);
                        negativeCUSUMv = Stats.getCUSUMS(metricVals, true, true, isLogScale, null);
                    }

                    List<?> serTypeObjList =  seriesList.get(0).get("Rows");
                    if(serTypeObjList.size() == positiveCUSUMm.length)
                    {
                        for (int i = 0; i < serTypeObjList.size(); i++)
                        {
                            RawMetricDataSet row = (RawMetricDataSet) serTypeObjList.get(i);
                            if (hasMR)
                            {
                                row.setmR(mRs[i]);
                            }
                            if (hasCUSUMm)
                            {
                                row.setcUSUMmP(positiveCUSUMm[i]);
                                row.setcUSUMmN(negativeCUSUMm[i]);
                            }
                            if (hasCUSUMv)
                            {
                                row.setCUSUMvP(positiveCUSUMv[i]);
                                row.setCUSUMvN(negativeCUSUMv[i]);
                            }
                        }
                    }

                });
            }
        }

        return plotDataMap;
    }

    private Map<String, Map<Integer, Map<PlotData, List<Map<String, List<?>>>>>> getAllProcessedMetricDataSets(List<RawMetricDataSet> rawMetricDataSets)
    {
        Map<String, Map<Integer, List<RawMetricDataSet>>> metricDataSet = new LinkedHashMap<>();
        rawMetricDataSets.forEach(row-> {
            if(metricDataSet.get(row.getMetricType()) == null)
            {
                Map<Integer, List<RawMetricDataSet>> metTypeMap = new LinkedHashMap<>();
                metricDataSet.put(row.getMetricType(), metTypeMap);
            }

            if(metricDataSet.get(row.getMetricType()).get(row.getGuideSetId()) == null)
            {
                List<RawMetricDataSet> setList = new ArrayList<>();

                String metricType = row.getMetricType();
                Integer guideId = row.getGuideSetId();
                rawMetricDataSets.forEach(ds-> {
                    if(ds.getMetricType().equalsIgnoreCase(metricType) && ds.getGuideSetId().equals(guideId))
                        setList.add(ds);
                });
                metricDataSet.get(row.getMetricType()).put(row.getGuideSetId(), setList);
            }
        });

        Map<String, Map<Integer, Map<PlotData, List<Map<String, List<?>>>>>> processedMetricDataSet = new LinkedHashMap<>();
        metricDataSet.forEach((metric, guides) -> {
            if(processedMetricDataSet.get(metric) == null)
            {
                Map<Integer, Map<PlotData, List<Map<String, List<?>>>>> metricMap = new LinkedHashMap<>();
                processedMetricDataSet.put(metric, metricMap);
            }
            guides.forEach((guideId, guideset) -> {
                processedMetricDataSet.get(metric).put(guideId, preprocessPlotData(guideset, true, true, true, false));
            });
        });

        return processedMetricDataSet;
    }

    private class PlotOutlier
    {
        int totalCount;
        List<Map<String, Map<String, Integer>>> outliers;

        public int getTotalCount()
        {
            return totalCount;
        }

        public void setTotalCount(int totalCount)
        {
            this.totalCount = totalCount;
        }

        public List<Map<String, Map<String, Integer>>> getOutliers()
        {
            return outliers;
        }

        public void setOutliers(List<Map<String, Map<String, Integer>>> outliers)
        {
            this.outliers = outliers;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlotOutlier that = (PlotOutlier) o;
            return totalCount == that.totalCount &&
                    outliers.equals(that.outliers);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(totalCount, outliers);
        }
    }

    private Map<String, PlotOutlier> getQCPlotMetricOutliers(Map<String, Map<GuideSetAvgMR, List<Map<String, Double>>>> processedMetricGuides, Map<String, Map<Integer, Map<PlotData, List<Map<String, List<?>>>>>> processedMetricDataSet, boolean CUSUMm, boolean CUSUMv, boolean mR, boolean groupByGuideSet, Set<String> sampleFiles)
    {
        Map<String, PlotOutlier> plotOutliers = new LinkedHashMap<>();
        processedMetricDataSet.forEach((metric, metricVal) -> {
            Map<String, Integer> countCUSUMmP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMmN = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvN = new LinkedHashMap<>();
            Map<String, Integer> countMR = new LinkedHashMap<>();


            Map<String, Map<String, Integer>> CUSUMmNmap = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> CUSUMmPmap = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> CUSUMvNmap = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> CUSUMvPmap = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> mRmap = new LinkedHashMap<>();


            CUSUMmNmap.put("CUSUMmN", new HashMap<>());
            CUSUMmPmap.put("CUSUMmP", new HashMap<>());
            CUSUMvPmap.put("CUSUMvP", new HashMap<>());
            CUSUMvNmap.put("CUSUMvN", new HashMap<>());
            mRmap.put("mR", new HashMap<>());
            PlotOutlier plotOutlier = new PlotOutlier();

            metricVal.forEach((guideSetId, peptides) -> {

                int totalCount = peptides.keySet().size();

                plotOutlier.setTotalCount(totalCount);

                peptides.forEach((plotData, plotDataList) -> {
                    if(plotDataList == null)
                        return;
                    plotDataList.forEach((series) -> {
                        if(series == null)
                            return;

                        List<RawMetricDataSet> rows = (List<RawMetricDataSet>) series.get("Rows");

                        if(rows != null)
                        {
                            if (CUSUMm)
                            {
                                rows.forEach(data -> {
                                    String sampleFile = data.getSampleFile();
                                    if (data.getcUSUMmN() != null && data.getcUSUMmN() > Stats.CUSUM_CONTROL_LIMIT)
                                    {
                                        CUSUMmNmap.put("CUSUMmN", processEachOutlier(groupByGuideSet, countCUSUMmN, guideSetId, sampleFiles, sampleFile));
                                    }
                                    if (data.getcUSUMmP() != null && data.getcUSUMmP() > Stats.CUSUM_CONTROL_LIMIT)
                                    {
                                        CUSUMmPmap.put("CUSUMmP", processEachOutlier(groupByGuideSet, countCUSUMmP, guideSetId, sampleFiles, sampleFile));
                                    }
                                });

                            }
                            if (CUSUMv)
                            {
                                rows.forEach(data -> {
                                    String sampleFile = data.getSampleFile();
                                    if (data.getCUSUMvN() != null && data.getCUSUMvN() > Stats.CUSUM_CONTROL_LIMIT)
                                    {
                                        CUSUMvNmap.put("CUSUMvN", processEachOutlier(groupByGuideSet, countCUSUMvN, guideSetId, sampleFiles, sampleFile));
                                    }
                                    if (data.getCUSUMvP() != null && data.getCUSUMvP() > Stats.CUSUM_CONTROL_LIMIT)
                                    {
                                        CUSUMvPmap.put("CUSUMvP", processEachOutlier(groupByGuideSet, countCUSUMvP, guideSetId, sampleFiles, sampleFile));
                                    }
                                });

                            }

                            if (mR)
                            {
                                rows.forEach(row -> {
                                    if (processedMetricGuides.get(metric) != null)
                                    {
                                        GuideSetAvgMR guideSetAvgMR = new GuideSetAvgMR();
                                        guideSetAvgMR.setGuideSetid(guideSetId);
                                        guideSetAvgMR.setSeriesLabel(plotData.getSeriesLabel());
                                        guideSetAvgMR.setSeriesType(plotData.getSeriesType());
                                        guideSetAvgMR.setSeries("Series");
                                        if (processedMetricGuides.get(metric).get(guideSetAvgMR) != null)
                                        {
                                            double controlRange = processedMetricGuides.get(metric).get(guideSetAvgMR).get(0).get("avgMR");
                                            if (row.getmR() != null && row.getmR() > Stats.MOVING_RANGE_UPPER_LIMIT_WEIGHT * controlRange)
                                            {
                                                String sampleFile = row.getSampleFile();
                                                mRmap.put("mR", processEachOutlier(groupByGuideSet, countMR, guideSetId, sampleFiles, sampleFile));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });

                });
            });
            List<Map<String, Map<String, Integer>>> outliersList = new ArrayList<>();
            outliersList.add(CUSUMmNmap);
            outliersList.add(CUSUMmPmap);
            outliersList.add(CUSUMvNmap);
            outliersList.add(CUSUMvPmap);
            outliersList.add(mRmap);
            plotOutlier.setOutliers(outliersList);

            plotOutliers.put(metric, plotOutlier);

        });

        return plotOutliers;
    }


    private Map<String, Integer> processEachOutlier(boolean groupByGuideSet, Map<String, Integer> countObj, int guideSetId, Set<String> sampleFiles, String sampleFile)
    {
        if(groupByGuideSet)
        {
            int count = countObj.get(String.valueOf(guideSetId)) != null ? countObj.get(String.valueOf(guideSetId)) : 0;
            countObj.put(String.valueOf(guideSetId), ++count);
        }else if(sampleFiles.contains(sampleFile))
        {
            int count = countObj.get(sampleFile) != null ? countObj.get(sampleFile) : 0;
            countObj.put(sampleFile, ++count);
        }
        return countObj;
    }

    private Map<String, Map<String, Map<String, Integer>>> getMetricOutliersByFileOrGuideSetGroup(Map<String, PlotOutlier> metricOutlier)
    {
        Map<String, Map<String, Map<String, Integer>>> transformedOutliers = new LinkedHashMap<>();
        metricOutlier.forEach((metric, vals) -> {
            int totalCount = vals.getTotalCount();
            List<Map<String, Map<String, Integer>>> outliersList = vals.getOutliers();

            outliersList.forEach(outlier -> {
                outlier.forEach((type, groups) -> {

                    if(groups.size() > 0) {
                        groups.forEach((group, count) -> {
                            if(transformedOutliers.get(group) == null)
                            {
                                Map<String, Map<String, Integer>> groupMap = new LinkedHashMap<>();
                                transformedOutliers.put(group, groupMap);
                            }
                            Map<String, Integer> metricMap = new LinkedHashMap<>();

                            outliersList.forEach( o ->{
                                o.forEach((t, g) -> {
                                    g.forEach((gp, ct) -> {

                                        if(group.equalsIgnoreCase(gp))
                                        {
                                            metricMap.put("TotalCount", totalCount);
                                            metricMap.put(t, ct);
                                        }
                                    });
                                });
                            });

                            transformedOutliers.get(group).put(metric, metricMap);
                        });
                    }
                });
            });
        });
        return transformedOutliers;
    }

    public JSONObject getOtherQCSampleFileStats(List<LJOutlier> ljOutliers, List<RawGuideSet> rawGuideSets, List<RawMetricDataSet> rawMetricDataSets, String containerPath)
    {
        Map<String, Info> sampleFiles = setSampleFiles(ljOutliers, containerPath);
        List<Integer> validGuideSetIds = new ArrayList<>();
        sampleFiles.forEach((key,val) -> validGuideSetIds.add(val.getGuideSetId()));
        Map<String, Map<GuideSetAvgMR, List<Map<String, Double>>>> processedMetricGuides = getAllProcessedMetricGuideSets(rawGuideSets);

        List<RawMetricDataSet> filteredRawMetricDataSets = new ArrayList<>();

        rawMetricDataSets.forEach(row -> {
            if(validGuideSetIds.contains(row.getGuideSetId()) && !row.isIgnoreInQC())
                filteredRawMetricDataSets.add(row);
        });

        Map<String, Map<Integer, Map<PlotData, List<Map<String, List<?>>>>>> processedMetricDataSet = getAllProcessedMetricDataSets(filteredRawMetricDataSets);
        Map<String, PlotOutlier> metricOutlier = getQCPlotMetricOutliers(processedMetricGuides, processedMetricDataSet, true, true, true, false, sampleFiles.keySet());
        Map<String, Map<String, Map<String, Integer>>> transformedOutliers = getMetricOutliersByFileOrGuideSetGroup(metricOutlier);

        transformedOutliers.forEach((fileName, metrics) -> {
            Info info = sampleFiles.get(fileName);
            metrics.forEach((metric, outliers) -> {
                LJOutlier matchedItem = null;
                for(LJOutlier item : info.items)
                {
                    if(item.getMetricLabel() != null && item.getMetricLabel().equalsIgnoreCase(metric))
                    {
                        matchedItem = item;
                    }
                }

                if(matchedItem != null)
                {
                   for(Map.Entry<String, Integer> outlier : outliers.entrySet())
                    {
                        if (outlier.getKey().equalsIgnoreCase("mr"))
                            matchedItem.setmR(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMmP"))
                            matchedItem.setCUSUMmP(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMmN"))
                            matchedItem.setCUSUMmN(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMvP"))
                            matchedItem.setCUSUMvP(outlier.getValue());
                        if(outlier.getKey().equalsIgnoreCase("CUSUMvN"))
                            matchedItem.setCUSUMvN(outlier.getValue());
                    }
                    matchedItem.setCUSUMm(matchedItem.getCUSUMmP()+matchedItem.getCUSUMmN());
                    matchedItem.setCUSUMv(matchedItem.getCUSUMvP()+matchedItem.getCUSUMvN());
                }
            });

        });

        sampleFiles.forEach((name, sample) -> {
            int CUSUMmP = 0, CUSUMmN = 0, CUSUMvP = 0, CUSUMvN = 0, mR = 0;
            for (LJOutlier item : sample.getItems())
            {
                CUSUMmN += item.getCUSUMmN();
                CUSUMmP += item.getCUSUMmP();
                CUSUMvP += item.getCUSUMvP();
                CUSUMvN += item.getCUSUMvN();
                mR += item.getmR();
            }
            sample.setCUSUMm(CUSUMmN + CUSUMmP);
            sample.setCUSUMv(CUSUMvN + CUSUMvP);
            sample.setCUSUMmP(CUSUMmP);
            sample.setCUSUMvP(CUSUMvP);
            sample.setCUSUMmN(CUSUMmN);
            sample.setCUSUMvN(CUSUMvN);
            sample.setmR(mR);
            sample.setHasOutliers(!(sample.getNonConformers() == 0 && sample.getCUSUMm() == 0 && sample.getCUSUMv() == 0 && sample.getmR() == 0));
        });

        return getSampleFilesJSON(sampleFiles);
    }

    public JSONObject getSampleFilesJSON(Map<String, Info> sampleFiles)
    {
        JSONObject sampleFilesJSON = new JSONObject();
        sampleFiles.forEach((name, sample) -> {
            sampleFilesJSON.put(name, sample.toJSON());
        });

        return sampleFilesJSON;
    }

    private Map<String, Info> setSampleFiles(List<LJOutlier> ljOutliers, String containerPath)
    {
        int index = 1;
        Info info = null;
        Map<String, Info> sampleFiles = new HashMap<>();

        for (LJOutlier ljOutlier : ljOutliers)
        {
            if(info == null || (!(ljOutlier.getSampleFile() != null && ljOutlier.getSampleFile().equals(info.getSampleFile()))))
            {
                if(info != null)
                    sampleFiles.put(info.getSampleFile(), info);
                info = new Info();
                info.setIndex(index++);
                info.setSampleFile(ljOutlier.getSampleFile());
                info.setAcquiredTime(ljOutlier.getAcquiredTime());
                info.setMetrics(0);
                info.setNonConformers(0);
                info.setTotalCount(0);
                if(ljOutlier.getGuideSetId() != null)
                    info.setGuideSetId(ljOutlier.getGuideSetId());
                info.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC());
            }

            info.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC() && info.isIgnoreForAllMetric());
            if(!ljOutlier.isIgnoreInQC()) {
                info.metrics++;
                info.nonConformers += ljOutlier.getNonConformers();
                info.totalCount += ljOutlier.getTotalCount();
            }
            ljOutlier.setContainerPath(containerPath);
            info.items.add(ljOutlier);
        }
        assert info != null;
        sampleFiles.put(info.getSampleFile(), info);

        return sampleFiles;
    }


    private class Info
    {
        int index;
        String sampleFile;
        Date acquiredTime;
        int metrics;
        int nonConformers;
        int totalCount;
        List<LJOutlier> items;
        int guideSetId;
        boolean ignoreForAllMetric;
        int CUSUMm;
        int CUSUMv;
        int CUSUMmP;
        int CUSUMvP;
        int CUSUMmN;
        int CUSUMvN;
        int mR;
        boolean hasOutliers;

        public Info()
        {
            items = new ArrayList<>();
        }

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index = index;
        }

        public String getSampleFile()
        {
            return sampleFile;
        }

        public void setSampleFile(String sampleFile)
        {
            this.sampleFile = sampleFile;
        }

        public Date getAcquiredTime()
        {
            return acquiredTime;
        }

        public void setAcquiredTime(Date acquiredTime)
        {
            this.acquiredTime = acquiredTime;
        }

        public int getMetrics()
        {
            return metrics;
        }

        public void setMetrics(int metrics)
        {
            this.metrics = metrics;
        }

        public int getNonConformers()
        {
            return nonConformers;
        }

        public void setNonConformers(int nonConformers)
        {
            this.nonConformers = nonConformers;
        }

        public int getTotalCount()
        {
            return totalCount;
        }

        public void setTotalCount(int totalCount)
        {
            this.totalCount = totalCount;
        }

        public int getGuideSetId()
        {
            return guideSetId;
        }

        public void setGuideSetId(int guideSetId)
        {
            this.guideSetId = guideSetId;
        }

        public boolean isIgnoreForAllMetric()
        {
            return ignoreForAllMetric;
        }

        public void setIgnoreForAllMetric(boolean ignoreForAllMetric)
        {
            this.ignoreForAllMetric = ignoreForAllMetric;
        }

        public List<LJOutlier> getItems()
        {
            return items;
        }

        public void setItems(List<LJOutlier> items)
        {
            this.items = items;
        }

        public int getCUSUMm()
        {
            return CUSUMm;
        }

        public void setCUSUMm(int CUSUMm)
        {
            this.CUSUMm = CUSUMm;
        }

        public int getCUSUMv()
        {
            return CUSUMv;
        }

        public void setCUSUMv(int CUSUMv)
        {
            this.CUSUMv = CUSUMv;
        }

        public int getCUSUMmP()
        {
            return CUSUMmP;
        }

        public void setCUSUMmP(int CUSUMmP)
        {
            this.CUSUMmP = CUSUMmP;
        }

        public int getCUSUMvP()
        {
            return CUSUMvP;
        }

        public void setCUSUMvP(int CUSUMvP)
        {
            this.CUSUMvP = CUSUMvP;
        }

        public int getCUSUMmN()
        {
            return CUSUMmN;
        }

        public void setCUSUMmN(int CUSUMmN)
        {
            this.CUSUMmN = CUSUMmN;
        }

        public int getCUSUMvN()
        {
            return CUSUMvN;
        }

        public void setCUSUMvN(int CUSUMvN)
        {
            this.CUSUMvN = CUSUMvN;
        }

        public int getmR()
        {
            return mR;
        }

        public void setmR(int mR)
        {
            this.mR = mR;
        }

        public boolean isHasOutliers()
        {
            return hasOutliers;
        }

        public void setHasOutliers(boolean hasOutliers)
        {
            this.hasOutliers = hasOutliers;
        }

        public List<JSONObject> getItemsJSON(List<LJOutlier> ljOutliers)
        {
            List<JSONObject> jsonLJOutliers = new ArrayList<>();
            for (LJOutlier ljOutlier : ljOutliers)
            {
                jsonLJOutliers.add(ljOutlier.toJSON());
            }
            return jsonLJOutliers;
        }

        public JSONObject toJSON(){
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("Index", index);
            jsonObject.put("SampleFile", sampleFile);
            jsonObject.put("AcquiredTime", acquiredTime);
            jsonObject.put("Metrics", metrics);
            jsonObject.put("NonConformers", nonConformers);
            jsonObject.put("TotalCount",  totalCount);
            jsonObject.put("GuideSetId",  guideSetId);
            jsonObject.put("IgnoreForAllMetric",  ignoreForAllMetric);
            jsonObject.put("Items", getItemsJSON(items));
            jsonObject.put("CUSUMm", CUSUMm);
            jsonObject.put("CUSUMv", CUSUMv);
            jsonObject.put("CUSUMmN", CUSUMmN);
            jsonObject.put("CUSUMmP", CUSUMmP);
            jsonObject.put("CUSUMvN", CUSUMvN);
            jsonObject.put("CUSUMvP", CUSUMvP);
            jsonObject.put("mR", mR);
            jsonObject.put("hasOutliers", hasOutliers);

            return jsonObject;
        }
    }
}
