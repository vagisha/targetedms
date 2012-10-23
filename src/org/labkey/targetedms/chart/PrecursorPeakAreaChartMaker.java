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

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.awt.*;
import java.text.NumberFormat;
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

    public static JFreeChart make(PeptideGroup peptideGroup, int replicateId,
                                  Peptide peptide, String groupByAnnotation,
                                  boolean cvValues)
    {

        List<PrecursorChromInfoPlus> pciPlusList;

        PeakAreasChartInputMaker inputMaker = new PeakAreasChartInputMaker();
        if(peptide == null)
        {
            pciPlusList = getPrecursorChromInfo(peptideGroup, replicateId);
            inputMaker.setChartType(PeakAreasChartInputMaker.ChartType.PEPTIDE_COMPARISON, peptideGroup.getRunId());
        }
        else
        {
            pciPlusList = getPrecursorChromInfo(peptide);
            inputMaker.setChartType(PeakAreasChartInputMaker.ChartType.REPLICATE_COMPARISON, peptideGroup.getRunId());
        }

        inputMaker.setGroupByAnnotationName(groupByAnnotation);
        inputMaker.setPrecursorChromInfoList(pciPlusList);
        inputMaker.setCvValues(cvValues);
        PeakAreasChartInputMaker.PeakAreaDataset peakAreaDataset = inputMaker.make();

        double maxCategoryValue = peakAreaDataset.getMaxPeakArea();
        int peakAreaAxisMagnitude = getMagnitude(maxCategoryValue);
        CategoryDataset dataset = createDataset(peakAreaDataset, peakAreaAxisMagnitude);

        String yLabel = cvValues ? "Peak Area CV(%)" : "Peak Area "+getMagnitudeString(peakAreaAxisMagnitude);
        String xLabel;
        if(peptide == null)
        {
            xLabel = StringUtils.isBlank(groupByAnnotation) ? "Peptide" : "Annotation, Peptide";
        }
        else
        {
            xLabel = StringUtils.isBlank(groupByAnnotation) ? "Replicate" : "Annotation";
        }

        JFreeChart chart = ChartFactory.createBarChart(
                peptide == null ? peptideGroup.getLabel() : peptide.getSequence(),
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,   // include legend
                false,  // tooltips
                false   // URLs
        );

        chart.getPlot().setBackgroundPaint(Color.WHITE);

        // Get all the isotope labels for this run
        Map<String, Color> labelColors = getIsotopeLabelColors(peptideGroup.getRunId());

        // Create a custom legend only if we have more than 1 isotope labels
        if(peakAreaDataset.getSortedSeriesLabels().size() == 1)
        {
            chart.removeLegend();
        }
        else
        {
            LegendItemCollection legendItems = new LegendItemCollection();

            for(String label: peakAreaDataset.getSortedSeriesLabels())
            {
                Color color = labelColors.get(label);
                LegendItem legendItem = new LegendItem(label, "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, color);
                legendItems.add(legendItem);
            }
            chart.getCategoryPlot().setFixedLegendItems(legendItems);
        }

        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI * 0.5)
        );
        chart.getCategoryPlot().getDomainAxis().setMaximumCategoryLabelWidthRatio(0.3f);

        setRenderer(chart,peakAreaDataset, labelColors);

        // For statistical bar plots we may get standard deviation bars that extend
        // below 0.  We want to cut off at 0.
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0.0);

        return chart;
    }

    private static int getMagnitude(double maxCategoryValue)
    {
        double quotient = maxCategoryValue / 1000;
        return quotient < 1 ? 1 : (quotient > 1000 ? 1000000 : 1000);
    }

    private static String getMagnitudeString(int magnitude)
    {
        return magnitude == 1 ? "" : (magnitude == 1000 ? "10^3" : "10^6");
    }

    private static List<PrecursorChromInfoPlus> getPrecursorChromInfo(PeptideGroup peptideGroup, int replicateId)
    {
        if(replicateId == 0)
        {
            return PrecursorManager.getPrecursorChromInfosForPeptideGroup(peptideGroup.getId());
        }
        else
        {
            List<PrecursorChromInfoPlus> pciPlusList = new ArrayList<PrecursorChromInfoPlus>();

            // Returns the chrom infos only for the sample files with the given replicate ID.
            List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(peptideGroup.getRunId());
            for(SampleFile file: sampleFiles)
            {
                if(file.getReplicateId() != replicateId)
                    continue;

                // chromatograms for this precursor from a single sample file.
                List<PrecursorChromInfoPlus> samplePrecChromInfos = PrecursorManager.getPrecursorChromInfosForPeptideGroup(
                                                                                peptideGroup.getId(),
                                                                                file.getId());
                pciPlusList.addAll(samplePrecChromInfos);
            }
            return pciPlusList;
        }
    }

    private static List<PrecursorChromInfoPlus> getPrecursorChromInfo(Peptide peptide)
    {
        return PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId());
    }

    private static Map<String, Color> getIsotopeLabelColors(int runId)
    {
        List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(runId);
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
        return labelColors;
    }

    private static void setRenderer(JFreeChart chart, PeakAreasChartInputMaker.PeakAreaDataset dataset, Map<String, Color> labelColors)
    {
        if(!dataset.isStatistical())
        {
            chart.getCategoryPlot().setRenderer(new CustomBarRenderer(dataset.getSortedSeriesLabels(), labelColors));
        }
        else
        {
            chart.getCategoryPlot().setRenderer(new CustomStatisticalBarRenderer(dataset.getSortedSeriesLabels(), labelColors));
        }
    }

    private static class CustomBarRenderer extends BarRenderer
    {
        private List<String> _sortedSeriesLabels;
        private Map<String, Color> _labelColors;

        public CustomBarRenderer(final List<String> sortedSeriesLabels,
                                 final Map<String, Color> labelColors)
        {
            _sortedSeriesLabels = sortedSeriesLabels;
            _labelColors = labelColors;
            setDefaultShadowsVisible(false);
            setDrawBarOutline(false);
            setShadowVisible(false);
            setBarPainter(new StandardBarPainter());
        }

        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_sortedSeriesLabels.get(row)); // row = series index
        }
    }

    private static class CustomStatisticalBarRenderer extends StatisticalBarRenderer
    {
        private List<String> _sortedSeriesLabels;
        private Map<String, Color> _labelColors;

        private CustomStatisticalBarRenderer(List<String> sortedSeriesLabels, Map<String, Color> labelColors)
        {
             _sortedSeriesLabels = sortedSeriesLabels;
            _labelColors = labelColors;
        }
        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_sortedSeriesLabels.get(row)); // row = series index
        }
    }
    private static CategoryDataset createDataset(PeakAreasChartInputMaker.PeakAreaDataset peakAreaDataset, int peakAreaAxisMagnitude)
    {

        if(peakAreaDataset.isStatistical())
        {
            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for(String categoryLabel: peakAreaDataset.getSortedCategoryLabels())
            {
                PeakAreasChartInputMaker.PeakAreaCategoryDataset categoryDataset = peakAreaDataset.getCategoryDataset(categoryLabel);

                for(String seriesLabel: peakAreaDataset.getSortedSeriesLabels())
                {
                    PeakAreasChartInputMaker.PeakAreaSeriesDataset seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                    dataset.add(seriesDataset.getValue() / peakAreaAxisMagnitude,
                                seriesDataset.getSdev() / peakAreaAxisMagnitude,
                                seriesLabel,
                                categoryLabel);
                }
            }
            return dataset;
        }
        else
        {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(String categoryLabel: peakAreaDataset.getSortedCategoryLabels())
            {
                PeakAreasChartInputMaker.PeakAreaCategoryDataset categoryDataset = peakAreaDataset.getCategoryDataset(categoryLabel);

                for(String seriesLabel: peakAreaDataset.getSortedSeriesLabels())
                {
                    PeakAreasChartInputMaker.PeakAreaSeriesDataset seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                    dataset.addValue(seriesDataset.getValue() / peakAreaAxisMagnitude, seriesLabel, categoryLabel);
                }
            }
            return dataset;
        }
    }
}