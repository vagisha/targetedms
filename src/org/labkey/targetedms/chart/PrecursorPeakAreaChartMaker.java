/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.ui.RectangleEdge;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 5/8/12
 * Time: 3:50 PM
 */
public class PrecursorPeakAreaChartMaker
{
    public JFreeChart make(PeptideGroup peptideGroup, int replicateId,
                                  Peptide peptide, String groupByAnnotation,
                                  boolean cvValues, boolean logValues)
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

        if(pciPlusList == null || pciPlusList.size() == 0)
        {
            return null;
        }

        inputMaker.setGroupByAnnotationName(groupByAnnotation);
        inputMaker.setPrecursorChromInfoList(pciPlusList);
        inputMaker.setCvValues(cvValues);
        inputMaker.setLogValues(logValues);
        final PeakAreasChartInputMaker.PeakAreaDataset peakAreaDataset = inputMaker.make();

        double maxCategoryValue = peakAreaDataset.getMaxPeakArea();
        int peakAreaAxisMagnitude = getMagnitude(maxCategoryValue);
        CategoryDataset dataset = createDataset(peakAreaDataset, peakAreaAxisMagnitude, logValues);

        String yLabel = cvValues ? "Peak Area CV(%)" : "Peak Area "+getMagnitudeString(peakAreaAxisMagnitude);
        if(cvValues && logValues){
           yLabel = "Log Peak Area CV(%)";
        }
        if(!cvValues && logValues){
            yLabel =   "Log Peak Area";
        }


        String xLabel;
        if(peptide == null)
        {
            xLabel = noAnnotation(groupByAnnotation) ? "Peptide" : "Annotation, Peptide";
        }
        else
        {
            xLabel = noAnnotation(groupByAnnotation) ? "Replicate" : "Annotation";
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

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);

        // Get a map of colors for each series in the chart
        Map<String, Color> labelColors = getLabelColors(peptideGroup.getRunId(), peakAreaDataset);

        // Create a custom legend only if we have more than 1 isotope labels
        if(peakAreaDataset.getSortedSeriesLabels().size() == 1)
        {
            chart.removeLegend();
        }
        else
        {
            LegendItemCollection legendItems = new LegendItemCollection();

            for(PeakAreasChartInputMaker.SeriesLabel label: peakAreaDataset.getSortedSeriesLabels())
            {
                Color color = labelColors.get(label.toString());
                LegendItem legendItem = new LegendItem(label.toString(), "-", null, null, Plot.DEFAULT_LEGEND_ITEM_BOX, color);
                legendItems.add(legendItem);
            }
            plot.setFixedLegendItems(legendItems);
        }

        CategoryAxis xAxis;
        if(peptide == null)
        {
            Map<String, PeakAreasChartInputMaker.PeakAreaCategory> categoryPeptideMap = new HashMap<>();
            List<String> categoryLabels = peakAreaDataset.getSortedCategoryLabels();
            for(String label: categoryLabels)
            {
                PeakAreasChartInputMaker.PeakAreaCategoryDataset cd = peakAreaDataset.getCategoryDataset(label);
                if(cd != null)
                {
                    categoryPeptideMap.put(label, cd.getCategory());
                }
            }
            xAxis = new PeptideComparisonCategoryAxis(chart.getCategoryPlot().getDomainAxis().getLabel(), categoryPeptideMap);
        }
        else
        {
            xAxis = new ReplicateComparisonCategoryAxis(chart.getCategoryPlot().getDomainAxis().getLabel());
        }
        xAxis.setMaximumCategoryLabelWidthRatio(0.3f);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        ValueAxis yAxis = plot.getRangeAxis();
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

        setRenderer(chart, peakAreaDataset, labelColors);


        // For statistical bar plots we may get standard deviation bars that extend
        // below 0.  We want to cut off at 0.
        plot.getRangeAxis().setLowerBound(0.0);

        return chart;
    }

    private static boolean noAnnotation(String groupByAnnotation)
    {
        return "None".equalsIgnoreCase(groupByAnnotation) || StringUtils.isBlank(groupByAnnotation);
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

    private List<PrecursorChromInfoPlus> getPrecursorChromInfo(Peptide peptide)
    {
        return PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId());
    }

    private Map<String, Color> getLabelColors(int runId, PeakAreasChartInputMaker.PeakAreaDataset peakAreaDataset)
    {
        // If this dataset has only one series for each category use a single color
        if(peakAreaDataset.getSortedSeriesLabels().size() == 1)
        {
            Map<String, Color> labelColors = new HashMap<>();
            labelColors.put(peakAreaDataset.getSortedSeriesLabels().get(0).toString(), ChartColors.getPrecursorColor(0));
            return labelColors;
        }

        List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(runId);
        Map<String, PeptideSettings.IsotopeLabel> labelMap = new HashMap<>();
        int lightLabelId = Integer.MAX_VALUE;
        for(PeptideSettings.IsotopeLabel label: labels)
        {
            labelMap.put(label.getName(), label);
            lightLabelId = Math.min(lightLabelId, label.getId());
        }

        List<PeakAreasChartInputMaker.SeriesLabel> sortedSeriesLabels = peakAreaDataset.getSortedSeriesLabels();
        int minCharge = Integer.MAX_VALUE;
        for(PeakAreasChartInputMaker.SeriesLabel seriesLabel: sortedSeriesLabels)
        {
            minCharge = Math.min(minCharge, seriesLabel.getCharge());
        }

        Map<String, Color> labelColors = new HashMap<>();
        for(PeakAreasChartInputMaker.SeriesLabel seriesLabel: sortedSeriesLabels)
        {
            int colorIndex = (seriesLabel.getCharge() - minCharge) * labels.size() + (labelMap.get(seriesLabel.getIsotopeLabel()).getId() - lightLabelId);
            labelColors.put(seriesLabel.toString(), ChartColors.getIsotopeColor(colorIndex));
        }
        return labelColors;
    }

    private void setRenderer(JFreeChart chart, PeakAreasChartInputMaker.PeakAreaDataset dataset, Map<String, Color> labelColors)
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
        private List<PeakAreasChartInputMaker.SeriesLabel> _sortedSeriesLabels;
        private Map<String, Color> _labelColors;

        public CustomBarRenderer(final List<PeakAreasChartInputMaker.SeriesLabel> sortedSeriesLabels,
                                 final Map<String, Color> labelColors)
        {
            _sortedSeriesLabels = sortedSeriesLabels;
            _labelColors = labelColors;
            setDefaultShadowsVisible(false);
            setDrawBarOutline(false);
            setShadowVisible(false);
            setBarPainter(new StandardBarPainter());
            setMaximumBarWidth(0.35);
            setItemMargin(0.1);
        }

        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_sortedSeriesLabels.get(row).toString()); // row = series index
        }
    }

    private static class CustomStatisticalBarRenderer extends StatisticalBarRenderer
    {
        private List<PeakAreasChartInputMaker.SeriesLabel> _sortedSeriesLabels;
        private Map<String, Color> _labelColors;

        private CustomStatisticalBarRenderer(List<PeakAreasChartInputMaker.SeriesLabel> sortedSeriesLabels, Map<String, Color> labelColors)
        {
             _sortedSeriesLabels = sortedSeriesLabels;
            _labelColors = labelColors;
            setItemMargin(0.1);
        }
        public Paint getItemPaint(final int row, final int column) {
            return _labelColors.get(_sortedSeriesLabels.get(row).toString()); // row = series index
        }
    }
    private CategoryDataset createDataset(PeakAreasChartInputMaker.PeakAreaDataset peakAreaDataset, int peakAreaAxisMagnitude, boolean logScale)
    {

        if(peakAreaDataset.isStatistical())
        {
            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for(String categoryLabel: peakAreaDataset.getSortedCategoryLabels())
            {
                PeakAreasChartInputMaker.PeakAreaCategoryDataset categoryDataset = peakAreaDataset.getCategoryDataset(categoryLabel);

                for(PeakAreasChartInputMaker.SeriesLabel seriesLabel: peakAreaDataset.getSortedSeriesLabels())
                {
                    PeakAreasChartInputMaker.PeakAreaSeriesDataset seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                    if(seriesDataset == null)
                    {
                        // Add an empty series otherwise color order of series can be incorrect.
                        dataset.add(0, 0, seriesLabel.toString(), categoryLabel);
                    }
                    else
                    {
                        dataset.add(getDatasetValue(seriesDataset.getValue(), peakAreaAxisMagnitude, logScale),
                                    getDatasetValue(seriesDataset.getSdev(), peakAreaAxisMagnitude, logScale),
                                    seriesLabel.toString(),
                                    categoryLabel);
                    }
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

                for(PeakAreasChartInputMaker.SeriesLabel seriesLabel: peakAreaDataset.getSortedSeriesLabels())
                {
                    PeakAreasChartInputMaker.PeakAreaSeriesDataset seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                    if(seriesDataset == null)
                    {
                        // Add an empty series otherwise color order of series can be incorrect.
                        dataset.addValue(0, seriesLabel.toString(), categoryLabel);
                    }
                    else
                    {
                        dataset.addValue(getDatasetValue(seriesDataset.getValue(), peakAreaAxisMagnitude, logScale), seriesLabel.toString(), categoryLabel);
                    }
                }
            }
            return dataset;
        }
    }

    private double getDatasetValue(double value, int peakAreaAxisMagnitude, boolean logScale)
    {
        return logScale ? value : value / peakAreaAxisMagnitude;
    }
    private static class LabelMinimizer
    {
        private static final char LABEL_SEP_CHAR = '_';
        private static final String ELIPSIS = "...";
        private static final String ELIPSIS_PATTERN = "\\.\\.\\.";
        private static final char[] SPACE_CHARS = new char[] { '_', '-', ' ', '.', ',' };
        private static final String SPACE_CHAR_PATTERN = "[_\\- .,]";
        private enum ReplaceLocation {start, end, middle}

        public static Map<String, String> minimizeReplicateNames(List<String> labels)
        {
            if(labels == null || labels.size() == 0)
                return Collections.emptyMap();

            List<String> normalizedLabels = normalizeLabels(labels);

            String[] labelParts = normalizedLabels.get(0).split(String.valueOf(LABEL_SEP_CHAR));
            if(labelParts.length == 1)
            {
                return originalLabels(labels);
            }

            // If all labels start with the first part
            String replaceString = labelParts[0];
            String partFirst = replaceString + LABEL_SEP_CHAR;
            boolean allStartWith = labelsContain(normalizedLabels, partFirst, ReplaceLocation.start);
            if(allStartWith)
            {
                return updateLabels(labels, replaceString, ReplaceLocation.start);
            }

            // If all labels end with the last part
            replaceString = labelParts[labelParts.length - 1];
            String partLast = LABEL_SEP_CHAR + replaceString;
            boolean allEndWith = labelsContain(normalizedLabels, partLast, ReplaceLocation.end);
            if(allEndWith)
            {
                return updateLabels(labels, replaceString, ReplaceLocation.end);
            }

            for (int i = 1 ; i < labelParts.length - 1; i++)
            {
                replaceString = labelParts[i];
                if (StringUtils.isBlank(replaceString))
                    continue;
                String partMiddle = LABEL_SEP_CHAR + replaceString + LABEL_SEP_CHAR;
                // If all labels contain the middle part
                boolean allContain = labelsContain(normalizedLabels, partMiddle, ReplaceLocation.middle);
                if (allContain)
                {
                    return updateLabels(labels, replaceString, ReplaceLocation.middle);
                }
            }
            return originalLabels(labels);
        }

        private static Map<String, String> originalLabels(List<String> labels)
        {
            Map<String, String> displayLabels = new HashMap<>();
            for(String label: labels)
            {
                displayLabels.put(label, label);
            }
            return displayLabels;
        }

        private static List<String> normalizeLabels(List<String> labels)
        {
            List<String> normalized = new ArrayList<>(labels.size());
            for(Object label: labels)
            {
                normalized.add(normalizeLabel((String) label));
            }
            return normalized;
        }

        private static String normalizeLabel(String label)
        {
            String normalized = label.replaceAll(ELIPSIS_PATTERN, String.valueOf(LABEL_SEP_CHAR));
            normalized = normalized.replaceAll(SPACE_CHAR_PATTERN, String.valueOf(LABEL_SEP_CHAR));
            return normalized;
        }

        private static boolean labelsContain(List<String> normalizedLabels, String substring, ReplaceLocation location)
        {
            for(String label: normalizedLabels)
            {
                switch (location)
                {
                    case start:
                        if (!label.startsWith(substring))
                        {
                            return false;
                        }
                        break;
                    case end:
                        if (!label.endsWith(substring))
                        {
                            return false;
                        }
                        break;
                    case middle:
                        if (!label.contains(substring))
                        {
                            return false;
                        }
                        break;
                }
            }
            return true;
        }

        private static Map<String, String> updateLabels(List<String> labels, String replaceString, ReplaceLocation location)
        {
            Map<String, String> displayLabels = new HashMap<>();
            for (String label: labels)
            {
                String newLabel = removeString(label, replaceString, location);
                displayLabels.put(label, newLabel);
            }
            return displayLabels;
        }

        private static String removeString(String label, String replaceString, ReplaceLocation location)
        {
            int startIndex = -1;
            while ((startIndex = label.indexOf(replaceString, startIndex + 1)) != -1)
            {
                int endIndex = startIndex + replaceString.length();
                // Not start string and does not end with space
                if ((startIndex != 0 && !isSpaceChar(label.charAt(startIndex - 1))) ||
                    (startIndex == 0 && location != ReplaceLocation.start))
                    continue;

                // Not end string and does not start with space
                if ((endIndex != label.length() && !isSpaceChar(label.charAt(endIndex))) ||
                    (endIndex == label.length() && location != ReplaceLocation.end))
                    continue;

                boolean elipsisSeen = false;
                boolean middle = true;
                // Check left of the string for the start of the label or a space char
                if (startIndex == 0)
                    middle = false;
                else if (startIndex >= ELIPSIS.length() && label.lastIndexOf(ELIPSIS, startIndex) == startIndex - ELIPSIS.length())
                    elipsisSeen = true;
                else
                    startIndex--;

                // Check right of the string for the end of the label or a space char
                if (endIndex == label.length())
                    middle = false;
                else if (label.indexOf(ELIPSIS, endIndex) == endIndex)
                    elipsisSeen = true;
                else
                    endIndex++;
                StringBuilder newLabel = new StringBuilder(label.substring(0, startIndex));
                // Insert an elipsis, if this is in the middle and no elipsis has been seen
                if (middle && !elipsisSeen && location == ReplaceLocation.middle)
                    newLabel.append(ELIPSIS);
                newLabel.append(label.substring(endIndex));
                return newLabel.toString();
            }
            return label;
        }

        private static boolean isSpaceChar(char c)
        {
            for(char sc: SPACE_CHARS)
            {
                if (sc == c)
                    return true;
            }
            return false;
        }
    }

    private static class PeptideComparisonCategoryAxis extends PeakAreaCategoryAxis
    {
        private Map<String, PeakAreasChartInputMaker.PeakAreaCategory> _categoryMap;
        public PeptideComparisonCategoryAxis(String label, Map<String, PeakAreasChartInputMaker.PeakAreaCategory> categoryMap)
        {
            super(label);
            _categoryMap = categoryMap;
        }

        protected Map<String, String> getFullLengthLabels(Collection<String> categories)
        {
            Iterator iterator = categories.iterator();
            Map<String, String> originalLabels = new HashMap<>();
            while (iterator.hasNext()) {
                String category = iterator.next().toString();
                PeakAreasChartInputMaker.PeakAreaCategory pepCategory = _categoryMap.get(category);
                if(pepCategory != null)
                {
                    originalLabels.put(category, pepCategory.getDisplayLabel());
                }
                else
                {
                    originalLabels.put(category, category);
                }
            }
            return originalLabels;
        }

        @Override
        protected boolean hasTrimmedLabels()
        {
            return false;
        }

        @Override
        protected Map<String, String> getTrimmedLabels(Collection<String> originalLabels)
        {
            return getFullLengthLabels(originalLabels);
        }
    }

    private static class ReplicateComparisonCategoryAxis extends PeakAreaCategoryAxis
    {
        public ReplicateComparisonCategoryAxis(String label)
        {
            super(label);
        }

        protected Map<String, String> getFullLengthLabels(Collection<String> categories)
        {
            Iterator iterator = categories.iterator();
            Map<String, String> originalLabels = new HashMap<>();
            while (iterator.hasNext()) {
                String category = iterator.next().toString();
                originalLabels.put(category, category);
            }
            return originalLabels;
        }

        @Override
        protected boolean hasTrimmedLabels()
        {
            return true;
        }

        @Override
        protected Map<String, String> getTrimmedLabels(Collection<String> originalLabels)
        {
            List<String> labels = new ArrayList<>(originalLabels);
            return LabelMinimizer.minimizeReplicateNames(labels);
        }
    }

    private static abstract class PeakAreaCategoryAxis extends CategoryAxis
    {
        private Map<String, String> _displayLabels;

        public PeakAreaCategoryAxis(String label)
        {
            super(label);
        }

        @Override
        protected TextBlock createLabel(Comparable category, float width, RectangleEdge edge, Graphics2D g2)
        {
            String label = getDisplayLabel(category.toString(), g2, width);
            return super.createLabel(label, width, edge, g2);    //To change body of overridden methods use File | Settings | File Templates.
        }

        private String getDisplayLabel(String category, Graphics2D g2, float width)
        {
            if(_displayLabels == null)
            {
                calculateDisplayLabels(width, g2);
            }
            String displayLabel = _displayLabels.get(category);
            return displayLabel == null ? category : displayLabel;
        }

        protected abstract Map<String, String> getFullLengthLabels(Collection<String> categories);
        protected abstract boolean hasTrimmedLabels();
        protected abstract Map<String, String> getTrimmedLabels(Collection<String> originalLabels);

        private void calculateDisplayLabels(float availableWidth, Graphics2D g2)
        {
            CategoryPlot plot = (CategoryPlot) getPlot();
            List categories = plot.getCategoriesForAxis(this);

            Map<String, String> originalLabels = getFullLengthLabels(categories);

            FontMetrics fm = g2.getFontMetrics(getTickLabelFont());
            final Font font = fm.getFont();
            final int originalSize = font.getSize();
            final int smallestSize = originalSize; // 8; // Changing the font makes the legend overlap category labels.
            for(int i = originalSize; i >= smallestSize; i--)
            {
                Font newFont = font.deriveFont((float)i);
                if(tryFont(availableWidth, originalLabels.values(), newFont, g2))
                {
                    _displayLabels = originalLabels;
                    return;
                }
            }

            Map<String, String> trimmedLabels = getFullLengthLabels(categories);
            if(hasTrimmedLabels())
            {
                // Full size labels did not fit. Try minimized labels
                while(minimizeLabels(trimmedLabels))
                {
                    for(int i = originalSize; i >= smallestSize; i--)
                    {
                        Font newFont = font.deriveFont((float)i);
                        if(tryFont(availableWidth, trimmedLabels.values(), newFont, g2))
                        {
                            _displayLabels = trimmedLabels;
                            return;
                        }
                    }
                }
            }

            // If we are here we did not find a combination of labels and font size that fits.
            // Select the smallest font-size and trimmed labels
            setTickLabelFont(font.deriveFont((float)smallestSize));
            _displayLabels = trimmedLabels;
        }

        boolean minimizeLabels(Map<String, String> trimmedLabels)
        {
            Map<String, String> newTrimmedLabels = getTrimmedLabels(trimmedLabels.values());
            // check if the labels have changed from what was given
            for(String key: newTrimmedLabels.keySet())
            {
                if(key.equals(newTrimmedLabels.get(key)))
                {
                    return false;
                }
                break; // enough to check only one
            }

            for(String category: trimmedLabels.keySet())
            {
                String oldTrimmed = trimmedLabels.get(category);
                String newTrimmed = newTrimmedLabels.get(oldTrimmed);
                if(newTrimmed != null)
                {
                    trimmedLabels.put(category, newTrimmed);
                }
            }
            return true;
        }

        private float getMaxRequiredWidth(FontMetrics fm, Collection<String> labels)
        {
            float maxWidth = Float.MIN_VALUE;
            for (String label: labels) {
                int labelWidth = fm.stringWidth(label);
                maxWidth = Math.max(maxWidth, labelWidth);
            }
            return maxWidth;
        }

        private boolean tryFont(float availableWidth, Collection<String> labels, Font font, Graphics2D g2)
        {
            FontMetrics newFm = g2.getFontMetrics(font);
            float maxWidth = getMaxRequiredWidth(newFm, labels);
            if(maxWidth <= availableWidth)
            {
                setTickLabelFont(font);
                return true;
            }
            return false;
        }
    }

     public static class TestCase extends Assert
    {
        @Test
        public void testReplicateNameMinimization() throws Exception
        {
            ReplicateComparisonCategoryAxis axis = new ReplicateComparisonCategoryAxis("Replicates");

            Map<String, String> labels = new HashMap<String, String>();
            labels.put("Prefix_name_1_Suffix", "Prefix_name_1_Suffix");
            labels.put("Prefix_name_2_Suffix", "Prefix_name_2_Suffix");

            int iteration = 0;
            while(axis.minimizeLabels(labels))
            {
                iteration++;
                if(iteration == 1)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "name_1_Suffix");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "name_2_Suffix");
                }
                if(iteration == 2)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "1_Suffix");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "2_Suffix");
                }
                if(iteration == 3)
                {
                    assertEquals(labels.get("Prefix_name_1_Suffix"), "1");
                    assertEquals(labels.get("Prefix_name_2_Suffix"), "2");
                }
            }
            assertEquals(3, iteration);

            labels.clear();
            labels.put("1,ABC-XYZ_File1", "1,ABC-XYZ_File1");
            labels.put("2_ABC-XYZ_File2", "2_ABC-XYZ_File2");
            iteration = 0;
            while(axis.minimizeLabels(labels))
            {
                iteration++;
                if(iteration == 1)
                {
                    assertEquals(labels.get("1,ABC-XYZ_File1"), "1...XYZ_File1");
                    assertEquals(labels.get("2_ABC-XYZ_File2"), "2...XYZ_File2");
                }
                if(iteration == 2)
                {
                    assertEquals(labels.get("1,ABC-XYZ_File1"), "1...File1");
                    assertEquals(labels.get("2_ABC-XYZ_File2"), "2...File2");
                }
            }
            assertEquals(2, iteration);
        }
    }
}