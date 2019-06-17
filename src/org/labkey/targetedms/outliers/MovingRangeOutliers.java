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

import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.model.GuideSetAvgMR;
import org.labkey.targetedms.model.RawGuideSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MovingRangeOutliers
{
    public Map<GuideSetAvgMR, List<Map<String, Double>>> getGuideSetAvgMRs(List<RawGuideSet> rawGuideSet)
    {
        Map<GuideSetAvgMR, Map<String, List<Double>>> guideSetDataMap = new LinkedHashMap<>();
        Map<GuideSetAvgMR, List<Map<String, Double>>> movingRangeMap = new LinkedHashMap<>();

        for (RawGuideSet row : rawGuideSet)
        {
            GuideSetAvgMR guideSetAvgMR = new GuideSetAvgMR();
            guideSetAvgMR.setGuideSetid(row.getGuideSetId());
            guideSetAvgMR.setSeriesLabel(row.getSeriesLabel());
            guideSetAvgMR.setSeriesType(row.getSeriesType());
            guideSetAvgMR.setSeries("Series");
            String metricValues = "MetricValues";

            if (guideSetDataMap.get(guideSetAvgMR) == null)
            {
                Map<String, List<Double>> serTypeMap = new LinkedHashMap<>();
                guideSetDataMap.put(guideSetAvgMR, serTypeMap);
            }

            if (guideSetDataMap.get(guideSetAvgMR).get(metricValues) == null)
            {
                List<Double> metValues = new ArrayList<>();
                if (row.getMetricValue() != null)
                {
                    rawGuideSet.forEach(gs -> {
                        String label = gs.getSeriesLabel();
                        Integer guideSetId = gs.getGuideSetId();
                        if (guideSetAvgMR.getSeriesLabel().equals(label) && guideSetAvgMR.getGuideSetid() == guideSetId)
                        {
                            if (gs.getMetricValue() != null)
                                metValues.add(gs.getMetricValue());
                        }
                    });
                }
                guideSetDataMap.get(guideSetAvgMR).put(metricValues, metValues);
            }
        }

        guideSetDataMap.forEach((guideSetAvgMR, seriesTypeVal) -> {
            List<Map<String, Double>> typeNameList = new ArrayList<>();
            List<Double> metricVals = seriesTypeVal.get("MetricValues");

            if (metricVals == null || metricVals.size() == 0)
                return;

            Double[] mVals = metricVals.toArray(new Double[0]);
            Double[] metricMovingRanges = Stats.getMovingRanges(mVals, false, null);

            Map<String, Double> avgMRMap = new LinkedHashMap<>();
            Map<String, Double> stddevMRMap = new LinkedHashMap<>();

            avgMRMap.put("avgMR", Stats.getMean(metricMovingRanges));
            stddevMRMap.put("stddevMR", Stats.getStdDev(metricMovingRanges));
            typeNameList.add(avgMRMap);
            typeNameList.add(stddevMRMap);

            movingRangeMap.put(guideSetAvgMR, typeNameList);

        });

        return movingRangeMap;
    }
}
