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

import org.apache.commons.collections15.comparators.ReverseComparator;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // key in the map is the precursorId; value is a list of chrom infos for this precursor in the various replicates.
        Map<Integer, List<PrecursorChromInfoPlus>> precursorChromInfoMap = new HashMap<Integer, List<PrecursorChromInfoPlus>>();
        for(SampleFile file: sampleFiles)
        {
            // chromatograms for this precursor from a single sample file.
            List<PrecursorChromInfoPlus> samplePrecChromInfos = PrecursorManager.getPrecursorChromInfosForPeptideGroup(
                                                                                peptideGroup.getId(),
                                                                                file.getId());
            for(PrecursorChromInfoPlus spci: samplePrecChromInfos)
            {
                List<PrecursorChromInfoPlus> pciList = precursorChromInfoMap.get(spci.getPrecursorId());
                if(pciList == null)
                {
                    pciList = new ArrayList<PrecursorChromInfoPlus>();
                    precursorChromInfoMap.put(spci.getPrecursorId(), pciList);
                }
                pciList.add(spci);
            }
        }

        // If we have more than 1 chrom info for a precursorId we will create a statistical dataset.
        boolean makeStatsDataset = false;
        for(Integer precursorId: precursorChromInfoMap.keySet())
        {
            if(precursorChromInfoMap.get(precursorId).size() > 1)
            {
                makeStatsDataset = true;
                break;
            }
        }

        List<DatasetEntry> datasetEntries = makeDatasetEntries(precursorChromInfoMap, makeStatsDataset);

        // Sort by area
        Collections.sort(datasetEntries, new ReverseComparator<DatasetEntry>(new Comparator<DatasetEntry>()
        {
            @Override
            public int compare(DatasetEntry datasetEntry, DatasetEntry datasetEntry1)
            {
                return Double.valueOf(datasetEntry.getValue()).compareTo(datasetEntry1.getValue());
            }
        }));

        CategoryDataset dataset = createDataset(datasetEntries, makeStatsDataset);

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

        setRenderer(chart, datasetEntries,labelColors,  makeStatsDataset);

        // For statistical bar plots we may get standard deviation bars that extend
        // below 0.  We want to cut off at 0.
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0.0);

        return chart;
    }

    private static void setRenderer(JFreeChart chart, List<DatasetEntry> datasetEntries, Map<String, Color> labelColors,
                                    boolean makeStatsDataset)
    {
        if(!makeStatsDataset)
        {
            chart.getCategoryPlot().setRenderer(new CustomBarRenderer(datasetEntries, labelColors));
        }
        else
        {
            chart.getCategoryPlot().setRenderer(new CustomStatisticalBarRenderer(datasetEntries, labelColors));
        }
    }

    private static CategoryDataset createDataset(List<DatasetEntry> datasetEntryList, boolean makeStatsDataset)
    {
        Set<String> uniqLabels = new HashSet<String>();
        for(DatasetEntry entry: datasetEntryList)
        {
           if(uniqLabels.contains(entry.getPeptide()))
           {
               uniqLabels.remove(entry.getPeptide());
           }
           else
           {
               uniqLabels.add(entry.getPeptide());
           }
        }

        if(makeStatsDataset)
        {
            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for(DatasetEntry entry: datasetEntryList)
            {
                String categoryLabel = getDatasetLabel(entry, uniqLabels);
                dataset.add(entry.getValue(), entry.getSdev(), "", categoryLabel);
            }
            return dataset;
        }
        else
        {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(DatasetEntry entry: datasetEntryList)
            {
                String categoryLabel = getDatasetLabel(entry, uniqLabels);
                dataset.addValue(entry.getValue(), "", categoryLabel);
            }
            return dataset;
        }
    }

    private static String getDatasetLabel(DatasetEntry entry, Set<String> uniqLabels)
    {
        StringBuilder label = new StringBuilder();
        if(!entry.getLabel().equalsIgnoreCase("light"))
        {
            label.append("("+entry.getLabel()+")");
        }
        label.append(entry.getPeptide().substring(0, 3));
        if(!uniqLabels.contains(entry.getPeptide()))
        {
            label.append(LabelFactory.getChargeLabel(entry.getCharge()));
        }
        return label.toString();
    }

    private static List<DatasetEntry> makeDatasetEntries(Map<Integer, List<PrecursorChromInfoPlus>> precursorChromInfoMap,
                                                         boolean makeStatsDataset)
    {
        List<DatasetEntry> datasetEntryList = new ArrayList<DatasetEntry>();

        if(!makeStatsDataset)
        {
            for(List<PrecursorChromInfoPlus> chromInfoList: precursorChromInfoMap.values())
            {
                for(PrecursorChromInfoPlus chromInfo: chromInfoList)
                {
                    if(chromInfo.getTotalArea() == null)
                        continue;
                    DatasetEntry entry = new DatasetEntry();
                    entry.setValue(chromInfo.getTotalArea() / 1000000);
                    entry.setPeptide(chromInfo.getSequence());
                    entry.setCharge(chromInfo.getCharge());
                    entry.setLabel(chromInfo.getLabel());
                    datasetEntryList.add(entry);
                }
            }
            return datasetEntryList;
        }
        else
        {
            for(Integer precursorId: precursorChromInfoMap.keySet())
            {
                List<PrecursorChromInfoPlus> chromInfoList = precursorChromInfoMap.get(precursorId);

                SummaryStatistics stats = new SummaryStatistics();
                for(PrecursorChromInfoPlus chromInfo: chromInfoList)
                {
                    if(chromInfo.getTotalArea() == null)
                        continue;
                    stats.addValue(chromInfo.getTotalArea() / 1000000);
                }
                DatasetEntry entry = new DatasetEntry();
                entry.setValue(stats.getMean());
                entry.setSdev(stats.getStandardDeviation());
                entry.setPeptide(chromInfoList.get(0).getSequence());
                entry.setCharge(chromInfoList.get(0).getCharge());
                entry.setLabel(chromInfoList.get(0).getLabel());
                datasetEntryList.add(entry);
            }
        }

        return datasetEntryList;
    }

    private static class CustomBarRenderer extends BarRenderer
    {
        private List<DatasetEntry> _entryList;
        private Map<String, Color> _labelColors;

        public CustomBarRenderer(final List<DatasetEntry> entryList,
                                 final Map<String, Color> labelColors)
        {
            _entryList = entryList;
            _labelColors = labelColors;
            setDefaultShadowsVisible(false);
            setDrawBarOutline(false);
            setShadowVisible(false);
            setBarPainter(new StandardBarPainter());
        }

        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_entryList.get(column).getLabel());
        }
    }

    private static class CustomStatisticalBarRenderer extends StatisticalBarRenderer
    {
        private List<DatasetEntry> _entryList;
        private Map<String, Color> _labelColors;

        private CustomStatisticalBarRenderer(List<DatasetEntry> entryList, Map<String, Color> labelColors)
        {
             _entryList = entryList;
            _labelColors = labelColors;
        }
        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_entryList.get(column).getLabel());
        }
    }

    private static class DatasetEntry
    {
        private String _peptide;
        private int _charge;
        private String _label;
        private double _value;
        private double _sdev;

        public String getPeptide()
        {
            return _peptide;
        }

        public void setPeptide(String peptide)
        {
            _peptide = peptide;
        }

        public int getCharge()
        {
            return _charge;
        }

        public void setCharge(int charge)
        {
            _charge = charge;
        }

        public String getLabel()
        {
            return _label;
        }

        public void setLabel(String label)
        {
            _label = label;
        }

        public double getValue()
        {
            return _value;
        }

        public void setValue(double value)
        {
            _value = value;
        }

        public double getSdev()
        {
            return _sdev;
        }

        public void setSdev(double sdev)
        {
            _sdev = sdev;
        }
    }
}
