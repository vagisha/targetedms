package org.labkey.targetedms.outliers;

import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.LJOutlier;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.RawGuideSet;
import org.labkey.targetedms.model.RawMetricDataSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CUSUMOutliers extends  Outliers
{

    private String metricGuideSetRawSql(int id, String schema1Name, String query1Name, String schema2Name, String query2Name, boolean includeAllValues, String series)
    {
        boolean includeSeries2 = schema2Name != null && query2Name != null;
        String selectCols = "SampleFileId, SampleFileId.AcquiredTime, SeriesLabel, MetricValue";
        String series1SQL = "SELECT \'" + (series != null ? series : "series1") + "\' AS SeriesType, " + selectCols + " FROM "+ schema1Name + "." + query1Name;
        String series2SQL = !includeSeries2 ? "" : " UNION SELECT \'series2\' AS SeriesType, " + selectCols + " FROM "+ schema2Name + "." + query2Name;
        String exclusionWhereSQL = this.getExclusionWhereSql(id);

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
        List<RawGuideSet> rawGuideSets = executeQuery(container, user, queryContainerSampleFileRawGuideSetStats(configurations)).getArrayList(RawGuideSet.class);

        rawGuideSets.sort(Comparator.comparing(RawGuideSet::getGuideSetId));

        return rawGuideSets;
    }

    public List<RawMetricDataSet> getRawMetricDataSets(Container container, User user, List<QCMetricConfiguration> configurations)
    {
        List<RawMetricDataSet> rawMetricDataSets = executeQuery(container, user, queryContainerSampleFileRawData(configurations)).getArrayList(RawMetricDataSet.class);

        rawMetricDataSets.sort(Comparator.comparing(RawMetricDataSet::getSeriesType));
        rawMetricDataSets.sort(Comparator.comparing(RawMetricDataSet::getSeriesLabel));
        rawMetricDataSets.sort(Comparator.comparing(RawMetricDataSet::getAcquiredTime));

        return rawMetricDataSets;
    }

    private Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>>> getAllProcessedMetricGuideSets(List<RawGuideSet> rawGuideSets)
    {
        Map<String, List<RawGuideSet>> metricGuideSet = new LinkedHashMap<>();
        Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>>> processedMetricGuides = new LinkedHashMap<>();
        MovingRangeOutliers mr = new MovingRangeOutliers();

        for (RawGuideSet row : rawGuideSets)
        {
            String metricType = row.getMetricType();
            if(metricGuideSet.get(metricType) == null) {
                List<RawGuideSet> sets = new ArrayList<>();
                rawGuideSets.forEach(gs->{
                    if(gs.getMetricType().equals(metricType))
                        sets.add(gs);
                });
                metricGuideSet.put(row.getMetricType(), sets);
            }
        }

        metricGuideSet.forEach((key, val) -> processedMetricGuides.put(key, mr.getGuideSetAvgMRs(val)));
        return processedMetricGuides;
    }

    private Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>> preprocessPlotData(List<RawMetricDataSet> plotDataRows, boolean hasMR, boolean hasCUSUMm, boolean hasCUSUMv, boolean isLogScale)
    {
        Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>> plotDataMap = new LinkedHashMap<>();

        if(plotDataRows.size() > 0)
        {
            plotDataRows.forEach(row-> {
                String seriesLabel = row.getSeriesLabel();
                if(plotDataMap.get(seriesLabel) == null)
                {
                    Map<String, Map<String, List<Map<String, List<?>>>>> seriesLabelMap = new LinkedHashMap<>();
                    plotDataMap.put(seriesLabel, seriesLabelMap);
                    Map<String, List<Map<String, List<?>>>> seriesMap = new LinkedHashMap<>();
                    plotDataMap.get(seriesLabel).put("Series", seriesMap);
                }
                String seriesType = row.getSeriesType();
                if(plotDataMap.get(seriesLabel).get("Series").get(seriesType) == null)
                {
                    List<Map<String, List<?>>> serTypeList = new ArrayList<>();
                    Map<String, List<?>> rowsMap = new LinkedHashMap<>();
                    Map<String, List<?>> metricValuesMap = new LinkedHashMap<>();
                    List<RawMetricDataSet> rowsList = new ArrayList<>();
                    List<Double> metricValuesList = new ArrayList<>();

                    plotDataRows.forEach(pR ->{
                        if(pR.getSeriesLabel().equalsIgnoreCase(seriesLabel))
                        {
                            rowsList.add(pR);
                            metricValuesList.add((double) Math.round(pR.getMetricValue() * 10000) / 10000.0);

                        }
                    });

                    rowsMap.put("Rows", rowsList);
                    metricValuesMap.put("MetricValues", metricValuesList);

                    serTypeList.add(rowsMap);
                    serTypeList.add(metricValuesMap);

                    plotDataMap.get(seriesLabel).get("Series").put(seriesType, serTypeList);
                }
            });

            if(hasMR || hasCUSUMm || hasCUSUMv)
            {
                plotDataMap.forEach((seriesLabel, seriesVal) -> seriesVal.get("Series").forEach((seriesType, seriesTypeObj) -> {
                    List<?> metricValsList = seriesTypeObj.get(1).get("MetricValues");
                    Double[] metricVals = metricValsList.toArray(new Double[0]);
                    Double[] mRs = new Double[0];
                    double[] positiveCUSUMm = new double[0];
                    double[] negativeCUSUMm = new double[0];
                    double[] positiveCUSUMv = new double[0];
                    double[] negativeCUSUMv = new double[0];

                    if(hasMR)
                    {
                        mRs = Stats.get().getMovingRanges(metricVals, isLogScale, null);
                    }

                    if(hasCUSUMm)
                    {
                        positiveCUSUMm = Stats.get().getCUSUMS(metricVals, false, false, isLogScale, null);
                        negativeCUSUMm = Stats.get().getCUSUMS(metricVals, true, false, isLogScale, null);
                    }

                    if(hasCUSUMv)
                    {
                        positiveCUSUMv = Stats.get().getCUSUMS(metricVals, false, true, isLogScale, null);
                        negativeCUSUMv = Stats.get().getCUSUMS(metricVals, true, true, isLogScale, null);
                    }

                    List<?> serTypeObjList =  seriesTypeObj.get(0).get("Rows");
                    for(int i = 0; i < serTypeObjList.size(); i++)
                    {
                        RawMetricDataSet row = (RawMetricDataSet) serTypeObjList.get(i);
                        if(hasMR)
                        {
                            row.setmR(mRs[i]);
                        }
                        if(hasCUSUMm)
                        {
                            row.setcUSUMmP(positiveCUSUMm[i]);
                            row.setcUSUMmN(negativeCUSUMm[i]);
                        }
                        if(hasCUSUMv)
                        {
                            row.setCUSUMvP(positiveCUSUMv[i]);
                            row.setCUSUMvN(negativeCUSUMv[i]);
                        }
                    }

                }));
            }
        }

        return plotDataMap;
    }

    private Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>>>> getAllProcessedMetricDataSets(List<RawMetricDataSet> rawMetricDataSets)
    {
        //rawMetricDataSets not in order
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
                rawMetricDataSets.forEach(ds-> {
                    if(ds.getMetricType().equalsIgnoreCase(metricType))
                        setList.add(ds);
                });
                metricDataSet.get(row.getMetricType()).put(row.getGuideSetId(), setList);
            }
        });

        Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>>>> processedMetricDataSet = new LinkedHashMap<>();
        metricDataSet.forEach((metric, guides) -> {
            if(processedMetricDataSet.get(metric) == null)
            {
                Map<Integer, Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>>> metricMap = new LinkedHashMap<>();
                processedMetricDataSet.put(metric, metricMap);
            }
            guides.forEach((guideId, guideset) -> {
                processedMetricDataSet.get(metric).put(guideId, preprocessPlotData(guideset, true, true, true, false));
            });
        });

        return processedMetricDataSet;
    }

    private Map<String, List<Map<String, ?>>> getQCPlotMetricOutliers(Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>>> processedMetricGuides, Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>>>> processedMetricDataSet, boolean CUSUMm, boolean CUSUMv, boolean mR, boolean groupByGuideSet, Set<String> sampleFiles)
    {
        Map<String, List<Map<String, ?>>> plotOutliers = new LinkedHashMap<>();
        processedMetricDataSet.forEach((metric, metricVal) -> {
            Map<String, Integer> countCUSUMmP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMmN = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvP = new LinkedHashMap<>();
            Map<String, Integer> countCUSUMvN = new LinkedHashMap<>();
            Map<String, Integer> countMR = new LinkedHashMap<>();

            List<Map<String, ?>> metricMapList = new ArrayList<>();

            Map<String, List<Map<String, Integer>>> outliersMap = new LinkedHashMap<>();
            Map<String, Integer> totalCountMap = new HashMap<>();

            metricVal.forEach((guideSetId, peptides) -> {
                int totalCount = peptides.keySet().size();
                totalCountMap.put("TotalCount", totalCount);
                metricMapList.add(totalCountMap);

                peptides.forEach((peptide, peptideVal) -> {
                    if(peptideVal == null || peptideVal.get("Series") == null)
                        return;
                    peptideVal.get("Series").forEach((series, seriesVal) -> {
                        if(seriesVal == null)
                            return;

                        List<RawMetricDataSet> rows = (List<RawMetricDataSet>) seriesVal.get(0).get("Rows");

                        if (CUSUMm) {
                            rows.forEach(data ->{
                                String sampleFile = data.getSampleFile();
                                if (data.getcUSUMmN() > Stats.CUSUM_CONTROL_LIMIT) {
                                    processEachOutlier(groupByGuideSet, countCUSUMmN, guideSetId, sampleFiles, sampleFile);
                                }
                                else if (data.getcUSUMmP() > Stats.CUSUM_CONTROL_LIMIT) {
                                    processEachOutlier(groupByGuideSet, countCUSUMmP, guideSetId, sampleFiles, sampleFile);
                                }
                            });

                        }
                        if (CUSUMv) {
                            rows.forEach(data -> {
                                String sampleFile = data.getSampleFile();
                                if (data.getCUSUMvN() > Stats.CUSUM_CONTROL_LIMIT) {
                                    processEachOutlier(groupByGuideSet, countCUSUMvN, guideSetId, sampleFiles, sampleFile);
                                }
                                else if (data.getCUSUMvP() > Stats.CUSUM_CONTROL_LIMIT) {
                                    processEachOutlier(groupByGuideSet, countCUSUMvP, guideSetId, sampleFiles, sampleFile);
                                }
                            });

                        }

                        if(mR)
                        {
                            rows.forEach(row -> {
                                double controlRange = processedMetricGuides.get(metric).get(guideSetId).get("Series").get(peptide).get(series).get(0).get("avgMR");
                                if(row.getmR() > Stats.MOVING_RANGE_UPPER_LIMIT_WEIGHT * controlRange)
                                {
                                    String sampleFile = row.getSampleFile();
                                    processEachOutlier(groupByGuideSet, countMR, guideSetId, sampleFiles, sampleFile);
                                }
                            });
                        }
                    });
                });
            });
            List<Map<String, Integer>> outliersList = new ArrayList<>();
            outliersList.add(countCUSUMmP);
            outliersList.add(countCUSUMmN);
            outliersList.add(countCUSUMvP);
            outliersList.add(countCUSUMvN);
            outliersList.add(countMR);

            outliersMap.put("outliers", outliersList);
            metricMapList.add(outliersMap);
            plotOutliers.put(metric, metricMapList);

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

    public Map<String, Info> getOtherQCSampleFileStats(List<LJOutlier> ljOutliers, List<RawGuideSet> rawGuideSets, List<RawMetricDataSet> rawMetricDataSets)
    {
        Map<String, Info> sampleFiles = setSampleFiles(ljOutliers);
        List<Integer> validGuideSetIds = new ArrayList<>();
        sampleFiles.forEach((key,val) -> validGuideSetIds.add(val.getGuideSetId()));
        Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>>> processedMetricGuides = getAllProcessedMetricGuideSets(rawGuideSets);

        List<RawMetricDataSet> filteredRawMetricDataSets = new ArrayList<>();

        rawMetricDataSets.forEach(row -> {
            if(validGuideSetIds.contains(row.getGuideSetId()) && !row.isIgnoreInQC())
                filteredRawMetricDataSets.add(row);
        });

        Map<String, Map<Integer, Map<String, Map<String, Map<String, List<Map<String, List<?>>>>>>>> processedMetricDataSet = getAllProcessedMetricDataSets(filteredRawMetricDataSets);
        getQCPlotMetricOutliers(processedMetricGuides, processedMetricDataSet, true, true, true, false, sampleFiles.keySet());
        //transformedOutliers

        return sampleFiles;
    }

    private Map<String, Info> setSampleFiles(List<LJOutlier> ljOutliers)
    {
        int index = 1;
        Info info = null;
        Map<String, Info> sampleFiles = new HashMap<>();

        for (LJOutlier ljOutlier : ljOutliers)
        {
            if(info == null || (!ljOutlier.getSampleFile().equals(info.getSampleFile())))
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
                info.setGuideSetId(ljOutlier.getGuideSetId());
                info.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC());
            }

            info.setIgnoreForAllMetric(ljOutlier.isIgnoreInQC() && info.isIgnoreForAllMetric());
            if(!ljOutlier.isIgnoreInQC()) {
                info.metrics++;
                info.nonConformers += ljOutlier.getNonConformers();
                info.totalCount += ljOutlier.getTotalCount();
            }
            info.items.add(ljOutlier);
        }
        assert info != null;
        sampleFiles.put(info.getSampleFile(), info);

        return sampleFiles;
    }


    public static class Info
    {
        int index;
        String sampleFile;
        Date acquiredTime;
        int metrics;
        int nonConformers;
        int totalCount;
        ArrayList<LJOutlier> items;
        int guideSetId;
        boolean ignoreForAllMetric;

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
            jsonObject.put("Items", items);

            return jsonObject;
        }
    }
}
