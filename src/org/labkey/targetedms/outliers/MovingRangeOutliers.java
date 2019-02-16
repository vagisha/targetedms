package org.labkey.targetedms.outliers;

import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.RawGuideSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MovingRangeOutliers
{
    public Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>> getGuideSetAvgMRs(List<RawGuideSet> rawGuideSet)
    {
        Map<Integer, Map< String, Map<String, Map<String, Map<String, List<Double>>>>>> guideSetDataMap = new LinkedHashMap<>();
        Map<Integer, Map<String, Map<String, Map<String, List<Map<String, Double>>>>>> movingRangeMap = new LinkedHashMap<>();

        for(RawGuideSet row: rawGuideSet)
        {
            int guideSetId = row.getGuideSetId();
            String seriesLabel = row.getSeriesLabel();
            String seriesType = row.getSeriesType();
            String series = "Series";
            String metricValues = "MetricValues";

            if(guideSetDataMap.get(guideSetId) == null)
            {
                Map<String, Map<String, Map<String, Map<String, List<Double>>>>> guideMap = new LinkedHashMap<>();
                guideSetDataMap.put(guideSetId, guideMap);
            }

            if(guideSetDataMap.get(guideSetId).get(series) == null)
            {
                Map<String, Map<String, Map<String, List<Double>>>> seriesMap = new LinkedHashMap<>();
                guideSetDataMap.get(guideSetId).put(series, seriesMap);
            }

            if(guideSetDataMap.get(guideSetId).get(series).get(seriesLabel) == null)
            {
                Map<String, Map<String, List<Double>>> serLabelMap = new LinkedHashMap<>();
                guideSetDataMap.get(guideSetId).get(series).put(seriesLabel, serLabelMap);
            }

            if(guideSetDataMap.get(guideSetId).get(series).get(seriesLabel).get(seriesType) == null)
            {
                Map<String, List<Double>> serTypeMap = new LinkedHashMap<>();
                guideSetDataMap.get(guideSetId).get(series).get(seriesLabel).put(seriesType, serTypeMap);
            }

            if(guideSetDataMap.get(guideSetId).get(series).get(seriesLabel).get(seriesType).get(metricValues) == null)
            {
                List<Double> metValues = new ArrayList<>();
                if(row.getMetricValue() != null)
                {
                    rawGuideSet.forEach(gs -> {
                        String label = gs.getSeriesLabel();
                        if (seriesLabel.equals(label))
                        {
                            if(gs.getMetricValue()!=null)
                                metValues.add(gs.getMetricValue());
                        }
                    });
                }
                guideSetDataMap.get(guideSetId).get(series).get(seriesLabel).get(seriesType).put(metricValues, metValues);
            }
        }

        guideSetDataMap.forEach((guideSetId, series)-> {
            Map<String, Map<String, Map<String, List<Map<String, Double>>>>> seriesMap = new LinkedHashMap<>();
            movingRangeMap.put(guideSetId,seriesMap);
            series.get("Series").forEach((seriesLabel, seriesType) -> seriesType.forEach((typeName, seriesTypeVal) -> {
                List<Double> metricVals = seriesTypeVal.get("MetricValues");

                if(metricVals == null || metricVals.size() == 0)
                    return;

                Double[] mVals = metricVals.toArray(new Double[0]);
                //Double[] mVals = metricVals.stream().mapToDouble(i->i).toArray();

                Double[] metricMovingRanges = Stats.get().getMovingRanges(mVals, false, null);
                if(movingRangeMap.get(guideSetId).get("Series") == null)
                {
                    Map<String, Map<String, List<Map<String, Double>>>> serLabelMap = new LinkedHashMap<>();
                    movingRangeMap.get(guideSetId).put("Series", serLabelMap);
                }

                if(movingRangeMap.get(guideSetId).get("Series").get(seriesLabel) == null)
                {
                    Map<String, List<Map<String, Double>>> serTypeMap = new LinkedHashMap<>();
                    movingRangeMap.get(guideSetId).get("Series").put(seriesLabel, serTypeMap);
                }

                Map<String, Double> avgMRMap = new LinkedHashMap<>();
                Map<String, Double> stddevMRMap = new LinkedHashMap<>();
                List<Map<String, Double>> typeNameList = new ArrayList<>();

                avgMRMap.put("avgMR", Stats.get().getMean(metricMovingRanges));
                stddevMRMap.put("stddevMR", Stats.get().getStdDev(metricMovingRanges));
                typeNameList.add(avgMRMap);
                typeNameList.add(stddevMRMap);

                movingRangeMap.get(guideSetId).get("Series").get(seriesLabel).put(typeName, typeNameList);

            }));

        });

        return movingRangeMap;
    }
}
