/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.targetedms.chart;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 7/24/2014
 * Time: 12:35 PM
 */
public class ComparisonChartMaker
{
    public JFreeChart makePeakAreasChart(int replicateId, PeptideGroup peptideGroup,
                                Peptide peptide, Precursor precursor,
                                String groupByAnnotation, String filterByAnnotation,
                                boolean cvValues, boolean logValues)
    {

        String yLabel = cvValues ? "Peak Area CV(%)" : "Peak Area ";
        if(cvValues && logValues){
            yLabel = "Log Peak Area CV(%)";
        }
        if(!cvValues && logValues){
            yLabel =   "Log Peak Area";
        }

        return makeChart(peptideGroup, replicateId, peptide, precursor,
                                     groupByAnnotation, filterByAnnotation,
                                     cvValues, logValues,
                                     new ComparisonDataset.PeakAreasSeriesItemMaker(),
                                     yLabel, true);
    }

    public JFreeChart makeRetentionTimesChart(int replicateId, PeptideGroup peptideGroup,
                                         Peptide peptide, Precursor precursor,
                                         String groupByAnnotation, String filterByAnnotation)
    {

        String yLabel = "Retention Time";

        return makeChart(peptideGroup, replicateId, peptide, precursor,
                groupByAnnotation, filterByAnnotation,
                false, false,
                new ComparisonDataset.RetentionTimesAllValuesSeriesItemMaker(),
                yLabel, false);
    }

    private JFreeChart makeChart(PeptideGroup peptideGroup, int replicateId,
                                         Peptide peptide, Precursor precursor,
                                         String groupByAnnotation, String filterByAnnotation,
                                         boolean cvValues, boolean logValues,
                                         ComparisonDataset.SeriesItemMaker seriesItemMaker,
                                         String yLabel, boolean barChart)
    {

        ComparisonDataset.ChartType chartType;
        if(peptide == null)
        {
            chartType = ComparisonDataset.ChartType.PEPTIDE_COMPARISON;
        }
        else
        {
            chartType = ComparisonDataset.ChartType.REPLICATE_COMPARISON;
        }

        List<PrecursorChromInfoPlus> pciPlusList = getInputData(peptideGroup, replicateId, peptide, precursor, chartType);
        if (pciPlusList == null || pciPlusList.size() == 0)
        {
            return null;
        }

        ComparisonChartInputMaker inputMaker = new ComparisonChartInputMaker(peptideGroup.getRunId(), pciPlusList, chartType);
        inputMaker.setGroupByAnnotationName(groupByAnnotation);
        inputMaker.setFilterByAnnotationValue(filterByAnnotation);
        inputMaker.setCvValues(cvValues);
        inputMaker.setLogValues(logValues);
        final ComparisonDataset comparisonDataset = inputMaker.make(seriesItemMaker);

        CategoryDataset dataset = comparisonDataset.createJfreeDataset();

        if(!logValues && !cvValues)
        {
            yLabel += " " + comparisonDataset.getYaxisScaleString();
        }

        String xLabel;
        if(chartType == ComparisonDataset.ChartType.PEPTIDE_COMPARISON)
        {
            // X-axis label for the "peptide comparison" graph.
            xLabel = noAnnotation(groupByAnnotation) ? "Peptide" : groupByAnnotation + ", Peptide";
        }
        else
        {
            // X-axis label for the "replicate comparison" graph
            xLabel = noAnnotation(groupByAnnotation) ? "Replicate" : groupByAnnotation;
        }

        JFreeChart chart;
        if(barChart)
        {
            chart = ChartFactory.createBarChart(
                    peptide == null ? peptideGroup.getLabel() : peptide.getSequence(),
                    xLabel,
                    yLabel,
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,   // include legend
                    false,  // tooltips
                    false   // URLs
            );
        }
        else
        {
            chart = ChartFactory.createBoxAndWhiskerChart(
                    peptide == null ? peptideGroup.getLabel() : peptide.getSequence(),
                    xLabel,
                    yLabel,
                    (BoxAndWhiskerCategoryDataset) dataset,
                    true // include legend
            );
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);

        // Create a custom legend only if we have more than 1 isotope labels
        if(comparisonDataset.getSortedSeriesLabels().size() == 1)
        {
            chart.removeLegend();
        }
        else
        {
            LegendItemCollection legendItems = new LegendItemCollection();

            for(ComparisonDataset.SeriesLabel label: comparisonDataset.getSortedSeriesLabels())
            {
                Color color = comparisonDataset.getSeriesColor(label);
                LegendItem legendItem = new LegendItem(label.toString(), "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, color);
                legendItems.add(legendItem);
            }
            plot.setFixedLegendItems(legendItems);
        }

        CategoryAxis xAxis;
        if(peptide == null)
        {
            xAxis = new ComparisonAxis.PeptideAxis(chart.getCategoryPlot().getDomainAxis().getLabel(), comparisonDataset.getCategoryMap());
        }
        else
        {
            xAxis = new ComparisonAxis.ReplicateAxis(chart.getCategoryPlot().getDomainAxis().getLabel());
        }
        xAxis.setMaximumCategoryLabelWidthRatio(0.3f);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        ValueAxis yAxis = plot.getRangeAxis();
        ((NumberAxis)yAxis).setNumberFormatOverride(new DecimalFormat("0.0"));
        xAxis.setLabelFont(yAxis.getLabelFont());
        xAxis.setTickLabelFont(yAxis.getTickLabelFont());
        plot.setDomainAxis(xAxis);
        if(logValues)
        {
            LogarithmicAxis logYAxis = new LogarithmicAxis(yLabel);
            logYAxis.setAllowNegativesFlag(true);
            logYAxis.setLog10TickLabelsFlag(true);
            plot.setRangeAxis(logYAxis);
        }

        setRenderer(chart, comparisonDataset, barChart);

        if(comparisonDataset.isStatistical())
        {
            // For statistical bar plots we may get standard deviation bars that extend
            // below 0.  We want to cut off at 0.
            if(yAxis.getLowerBound() < 0)
            {
                yAxis.setLowerBound(0.0);
            }
        }
        return chart;

    }

    private List<PrecursorChromInfoPlus> getInputData(PeptideGroup peptideGroup, int replicateId, Peptide peptide, Precursor precursor, ComparisonDataset.ChartType chartType)
    {
        List<PrecursorChromInfoPlus> pciPlusList;
        if(chartType == ComparisonDataset.ChartType.PEPTIDE_COMPARISON)
        {
            pciPlusList = getPrecursorChromInfo(peptideGroup, replicateId);
        }
        else
        {
            pciPlusList = getPrecursorChromInfo(peptide, precursor);
        }
        if(pciPlusList == null || pciPlusList.size() == 0)
        {
            return null;
        }
        return pciPlusList;
    }

    private static boolean noAnnotation(String groupByAnnotation)
    {
        return "None".equalsIgnoreCase(groupByAnnotation) || StringUtils.isBlank(groupByAnnotation);
    }

    private List<PrecursorChromInfoPlus> getPrecursorChromInfo(PeptideGroup peptideGroup, int replicateId)
    {
        if(replicateId == 0)
        {
            return PrecursorManager.getPrecursorChromInfosForPeptideGroup(peptideGroup.getId());
        }
        else
        {
            List<PrecursorChromInfoPlus> pciPlusList = new ArrayList<>();

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

    private List<PrecursorChromInfoPlus> getPrecursorChromInfo(Peptide peptide, Precursor precursor)
    {
        return precursor == null ? PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId()) :
                                   PrecursorManager.getPrecursorChromInfosForPrecursor(precursor.getId());
    }

    private void setRenderer(JFreeChart chart, ComparisonDataset dataset, boolean barChart)
    {

        CategoryItemRenderer renderer;
        if(barChart)
        {
            if(!dataset.isStatistical())
            {
                renderer = new BarRenderer();
                BarRenderer.setDefaultShadowsVisible(false);
                ((BarRenderer)renderer).setDrawBarOutline(false);
                ((BarRenderer)renderer).setShadowVisible(false);
                ((BarRenderer)renderer).setBarPainter(new StandardBarPainter());
                ((BarRenderer)renderer).setMaximumBarWidth(0.35);
                ((BarRenderer)renderer).setItemMargin(0.1);
            }
            else
            {
                renderer = new StatisticalBarRenderer();
                ((BarRenderer)renderer).setItemMargin(0.1);
            }
        }
        else
        {
            renderer = new RetentionTimeBoxAndWhiskerRenderer();
        }

        int seriesIndex = 0;
        for(ComparisonDataset.SeriesLabel seriesLabel: dataset.getSortedSeriesLabels())
        {
            Color seriesColor = dataset.getSeriesColor(seriesLabel);
            renderer.setSeriesPaint(seriesIndex, seriesColor);
            renderer.setSeriesOutlinePaint(seriesIndex,seriesColor);
            seriesIndex++;
        }
        chart.getCategoryPlot().setRenderer(renderer);
    }
}
