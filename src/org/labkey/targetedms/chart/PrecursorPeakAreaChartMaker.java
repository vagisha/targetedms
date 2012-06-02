/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 5/8/12
 * Time: 3:50 PM
 */
public class PrecursorPeakAreaChartMaker
{
    private PrecursorPeakAreaChartMaker() {}

    public static JFreeChart make(PeptideGroup peptideGroup)
    {
        List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(peptideGroup.getRunId());

        Map<Integer, PrecursorChromInfoPlus> precursorChromInfoMap = new HashMap<Integer, PrecursorChromInfoPlus>();
        for(SampleFile file: sampleFiles)
        {
            List<PrecursorChromInfoPlus> samplePrecChromInfos = PrecursorManager.getPrecursorChromInfosForPeptideGroup(
                                                                                peptideGroup.getId(),
                                                                                file.getId());
            for(PrecursorChromInfoPlus spci: samplePrecChromInfos)
            {
                PrecursorChromInfoPlus pci = precursorChromInfoMap.get(spci.getPrecursorId());
                if(pci == null)
                {
                    precursorChromInfoMap.put(spci.getPrecursorId(), spci);
                }
                else {
                    if(spci.getTotalArea() > pci.getTotalArea())
                    {
                        precursorChromInfoMap.put(spci.getPrecursorId(), spci);
                    }
                }
            }
        }
        return make(peptideGroup, new ArrayList<PrecursorChromInfoPlus>(precursorChromInfoMap.values()));
    }

    public static JFreeChart make(PeptideGroup peptideGroup, SampleFile file)
    {
        // Get all the precursor areas for this peptide group
        List<PrecursorChromInfoPlus> precursorChromInfos = PrecursorManager.getPrecursorChromInfosForPeptideGroup(
                                                                                    peptideGroup.getId(), file.getId());
        return make(peptideGroup, precursorChromInfos);
    }

    public static JFreeChart make(PeptideGroup peptideGroup, List<PrecursorChromInfoPlus> precursorChromInfos)
    {
        // Get all the isotope labels for this run
        List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(peptideGroup.getRunId());
        Collections.sort(labels, new Comparator<PeptideSettings.IsotopeLabel>()
        {
            @Override
            public int compare(PeptideSettings.IsotopeLabel one, PeptideSettings.IsotopeLabel two)
            {
                return Integer.valueOf(one.getId()).compareTo(two.getId());
            }
        });

        Map<String, Color> labelColors = new HashMap<String, Color>();
        int lightLabelId = labels.get(0).getId();
        for(PeptideSettings.IsotopeLabel label: labels)
        {
            labelColors.put(label.getName(), ChartColors.getIsotopeColor(label.getId() - lightLabelId));
        }

        // Sort by peak area
        Collections.sort(precursorChromInfos, new Comparator<PrecursorChromInfoPlus>()
        {
            @Override
            public int compare(PrecursorChromInfoPlus one, PrecursorChromInfoPlus two)
            {
                return two.getTotalArea().compareTo(one.getTotalArea());
            }
        });


        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for(PrecursorChromInfoPlus chromInfo: precursorChromInfos)
        {
            String categoryLabel = chromInfo.getSequence().substring(0, 3);
            if(!chromInfo.getLabel().equalsIgnoreCase("light"))
            {
                categoryLabel = "("+chromInfo.getLabel()+")"+categoryLabel;
            }
            dataset.addValue(chromInfo.getTotalArea() / 1000000, "", categoryLabel);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                            peptideGroup.getLabel(),
                            "Peptide",
                            "Peak Area (10^6)",
                            dataset,
                            PlotOrientation.VERTICAL,
                            true,   // include legend
                            false,  // tooltips
                            false   // URLs
        );

        chart.getPlot().setBackgroundPaint(Color.WHITE);
        CustomRenderer renderer = new CustomRenderer(precursorChromInfos, labelColors);
        chart.getCategoryPlot().setRenderer(renderer);

        // If we have multiple isotope labels create a custom legend
        if(labels.size() == 1)
        {
            chart.removeLegend();
        }
        else
        {
            LegendItemCollection legendItems = new LegendItemCollection();

            for(PeptideSettings.IsotopeLabel label: labels)
            {
                Color color = ChartColors.getIsotopeColor(label.getId() - lightLabelId);
                LegendItem legendItem = new LegendItem(label.getName(), "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, color);
                legendItems.add(legendItem);
            }
            chart.getCategoryPlot().setFixedLegendItems(legendItems);
        }

        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI * 0.5)
        );

        return chart;
    }

    private static class CustomRenderer extends BarRenderer
    {
        private List<PrecursorChromInfoPlus> _precursorChromInfoPlusList;
        private Map<String, Color> _labelColors;

        public CustomRenderer(final List<PrecursorChromInfoPlus> precursorChromInfoPlusList,
                              final Map<String, Color> labelColors)
        {
            _precursorChromInfoPlusList = precursorChromInfoPlusList;
            _labelColors = labelColors;
            setDefaultShadowsVisible(false);
            setDrawBarOutline(false);
            setShadowVisible(false);
            setBarPainter(new StandardBarPainter());
        }

        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_precursorChromInfoPlusList.get(column).getLabel());
        }
    }

//    public static JFreeChart makeStatisticalBarChart(PeptideGroup peptideGroup)
//    {
//        List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(peptideGroup.getRunId());
//
//        // key in the map is precursorId
//        Map<Integer, List<PrecursorChromInfoPlus>> precursorChromInfoMap = new HashMap<Integer, List<PrecursorChromInfoPlus>>();
//
//        for(SampleFile file: sampleFiles)
//        {
//            List<PrecursorChromInfoPlus> samplePrecChromInfos = PrecursorManager.getPrecursorChromInfosForPeptideGroup(
//                                                                                peptideGroup.getId(),
//                                                                                file.getId());
//            for(PrecursorChromInfoPlus spci: samplePrecChromInfos)
//            {
//                List<PrecursorChromInfoPlus> pciList = precursorChromInfoMap.get(spci.getPrecursorId());
//                if(pciList == null)
//                {
//                    pciList = new ArrayList<PrecursorChromInfoPlus>();
//                    precursorChromInfoMap.put(spci.getPrecursorId(), pciList);
//                }
//                pciList.add(spci);
//            }
//        }
//
//        // If we have multiple precursor chrom infos for any of the precursors, generate Statistical bar plot
//        // otherwise generate simple bar plot
//        boolean createStatPlot = false;
//        for(Integer precursorId: precursorChromInfoMap.keySet())
//        {
//            if(precursorChromInfoMap.get(precursorId).size() > 1)
//            {
//                createStatPlot = true;
//                break;
//            }
//        }
//
//        if(createStatPlot)
//        {
//
//        }
//        else {
//
//        }
//        // TODO
//        return null;
//    }
}
