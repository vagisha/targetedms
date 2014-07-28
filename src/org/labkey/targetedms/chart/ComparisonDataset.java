package org.labkey.targetedms.chart;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;

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
* Date: 7/23/2014
* Time: 3:40 PM
*/
public class ComparisonDataset
{
    public static enum ChartType {PEPTIDE_COMPARISON, REPLICATE_COMPARISON}

    private Map<String, ComparisonCategoryItem> _categoryItemMap;
    private List<String> _sortedCategoryLabels;
    private List<SeriesLabel> _sortedSeriesLabels;
    private boolean _setSortByValues = false;
    private boolean _numericSort = true;
    private double _maxValue = 0;
    private final SeriesItemMaker _seriesItemMaker;
    private final boolean _logScale;
    private final int _runId;
    private PrecursorColorIndexer _colorIndexer;

    public ComparisonDataset(int runId, SeriesItemMaker seriesItemMaker, boolean logScale)
    {
        _seriesItemMaker = seriesItemMaker;
        _logScale = logScale;
        _runId = runId;
    }

    public void setSetSortByValues(boolean setSortByValues)
    {
        _setSortByValues = setSortByValues;
    }

    public void addCategory(ComparisonCategoryItem categoryDataset)
    {
        if(_categoryItemMap == null)
        {
            _categoryItemMap = new HashMap<>();
        }
        _categoryItemMap.put(categoryDataset.getCategoryLabel(), categoryDataset);
        _maxValue = Math.max(_maxValue, categoryDataset.getSeriesMaxValue());
    }

    public ComparisonCategoryItem getCategoryItem(String categoryLabel)
    {
        return _categoryItemMap.get(categoryLabel);
    }

    public List<String> getSortedCategoryLabels()
    {
        if(_sortedCategoryLabels != null)
            return _sortedCategoryLabels;

       if(!_setSortByValues)
        {
            for(ComparisonCategoryItem categoryDataset: _categoryItemMap.values())
            {
                if(!categoryDataset.getCategoryLabel().matches("-?\\d+(\\.\\d+)?"))  //match a number with optional '-' and decimal.)
                {
                    _numericSort = false;
                    break;
                }
            }
        }
        List<ComparisonCategoryItem> categoryDatasets = new ArrayList<>(_categoryItemMap.values());
        Collections.sort(categoryDatasets, new Comparator<ComparisonCategoryItem>()
        {
            @Override
            public int compare(ComparisonCategoryItem o1, ComparisonCategoryItem o2)
            {
                if (_setSortByValues)
                    return Double.valueOf(o2.getSeriesMaxValue()).compareTo(o1.getSeriesMaxValue());
                else
                {
                    if (_numericSort)
                        return Float.valueOf(o1.getCategoryLabel()).compareTo(Float.valueOf(o2.getCategoryLabel()));
                    else
                        return o1.getCategoryLabel().compareTo(o2.getCategoryLabel());
                }
            }
        });

        _sortedCategoryLabels = new ArrayList<>();
        for(ComparisonCategoryItem dataset: categoryDatasets)
        {
            _sortedCategoryLabels.add(dataset.getCategoryLabel());
        }
        return _sortedCategoryLabels;
    }

    public List<SeriesLabel> getSortedSeriesLabels()
    {
        if(_sortedSeriesLabels != null)
            return _sortedSeriesLabels;

        Set<SeriesLabel> seriesLabels = new HashSet<>();
        for(ComparisonCategoryItem dataset: _categoryItemMap.values())
        {
            seriesLabels.addAll(dataset.getSeriesLabels());
        }
        _sortedSeriesLabels = new ArrayList<>(seriesLabels);
        Collections.sort(_sortedSeriesLabels);

        return _sortedSeriesLabels;
    }

    public boolean isStatistical()
    {
        for(ComparisonCategoryItem categoryDataset: _categoryItemMap.values())
        {
            if(categoryDataset.isStatistical())
                return true;
        }
        return false;
    }

    private double getMaxValue()
    {
        return _maxValue;
    }

    public Map<String, ComparisonCategory> getCategoryMap()
    {
        Map<String, ComparisonCategory> categoryMap = new HashMap<>();
        List<String> categoryLabels = getSortedCategoryLabels();
        for(String label: categoryLabels)
        {
            ComparisonDataset.ComparisonCategoryItem cd = getCategoryItem(label);
            if(cd != null)
            {
                categoryMap.put(label, cd.getCategory());
            }
        }
        return categoryMap;
    }

    public Color getSeriesColor(SeriesLabel seriesLabel)
    {
        if(getSortedSeriesLabels().size() == 1)
        {
            return ChartColors.getPrecursorColor(0);
        }

        if(_colorIndexer == null)
        {
            _colorIndexer = new PrecursorColorIndexer(_runId);
            int minCharge = Integer.MAX_VALUE;
            for(ComparisonDataset.SeriesLabel sl: getSortedSeriesLabels())
            {
                minCharge = Math.min(minCharge, sl.getCharge());
            }
            _colorIndexer.setMinCharge(minCharge);
        }

        int colorIndex = _colorIndexer.getColorIndex(seriesLabel.getIsotopeLabelId(), seriesLabel.getCharge());
        return ChartColors.getIsotopeColor(colorIndex);
    }

    public static class ComparisonCategoryItem
    {
        private ComparisonCategory _category;
        private Map<SeriesLabel, ComparisonSeriesItem> _seriesDatasetsMap;
        private double _maxCategoryValue;

        public ComparisonCategoryItem(ComparisonCategory category)
        {
            _category = category;
        }

        public String getCategoryLabel()
        {
            return _category.getCategoryLabel();
        }

        public ComparisonCategory getCategory()
        {
            return _category;
        }

        public void setData(SeriesItemMaker seriesItemMaker, List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues, ChartType chartType)
        {
            Map<SeriesLabel, List<PrecursorChromInfoPlus>> seriesDataMap = new HashMap<>();
            for(PrecursorChromInfoPlus pciPlus: pciPlusList)
            {
                SeriesLabel seriesLabel = new SeriesLabel();
                if(chartType == ChartType.REPLICATE_COMPARISON)
                {
                    // For PEPTIDE_COMPARISON charts each precursor is treated as a separate category.
                    // So, the precursor charge should not be part of the series label.
                    // For REPLICATE_COMPARISON charts each precursor of a peptide is displayed as a series so we
                    // need both the charge and isotope label to uniquely distinguish a series.
                    seriesLabel.setCharge(pciPlus.getCharge());
                }
                seriesLabel.setIsotopeLabel(pciPlus.getIsotopeLabel());
                seriesLabel.setIsotopeLabelId(pciPlus.getIsotopeLabelId());

                List<PrecursorChromInfoPlus> seriesData = seriesDataMap.get(seriesLabel);
                if(seriesData == null)
                {
                    seriesData = new ArrayList<>();
                    seriesDataMap.put(seriesLabel, seriesData);
                }
                seriesData.add(pciPlus);
            }

            _seriesDatasetsMap = new HashMap<>();
            for(SeriesLabel seriesLabel: seriesDataMap.keySet())
            {
                ComparisonSeriesItem seriesDataset = new ComparisonSeriesItem();
                seriesDataset.setData(seriesItemMaker, seriesDataMap.get(seriesLabel), cvValues);
                _seriesDatasetsMap.put(seriesLabel, seriesDataset);
                if(seriesDataset.getSeriesItemData() != null)
                {
                    _maxCategoryValue = Math.max(_maxCategoryValue, seriesDataset.getSeriesItemData().getValue());
                }
            }
        }
        public ComparisonSeriesItem getSeriesDataset(SeriesLabel seriesLabel)
        {
            return _seriesDatasetsMap.get(seriesLabel);
        }

        public double getSeriesMaxValue()
        {
            return _maxCategoryValue;
        }

        public List<SeriesLabel> getSeriesLabels()
        {
            return new ArrayList<>(_seriesDatasetsMap.keySet());
        }

        public boolean isStatistical()
        {
            for(ComparisonSeriesItem seriesDataset: _seriesDatasetsMap.values())
            {
                SeriesItemData seriesItemData = seriesDataset.getSeriesItemData();
                if(seriesItemData.isStatistical())
                    return true;
            }
            return false;
        }
    }

    public static class ComparisonSeriesItem
    {
        // private SeriesLabel _seriesLabel;    // Goes in the legend.
        private SeriesItemData _seriesItemData;

        public ComparisonSeriesItem(/*SeriesLabel label*/)
        {
            // _seriesLabel = label;
        }

//        public SeriesLabel getSeriesLabel()
//        {
//            return _seriesLabel;
//        }

        public void setData(SeriesItemMaker seriesItemMaker, List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues)
        {
            _seriesItemData = seriesItemMaker.make(pciPlusList, cvValues);
        }

        public SeriesItemData getSeriesItemData()
        {
            return _seriesItemData;
        }
    }

    public static class SeriesLabel implements Comparable<SeriesLabel>
    {
        private int _charge;
        private String _isotopeLabel = "";
        private int _isotopeLabelId;

        public int getCharge()
        {
            return _charge;
        }

        public void setCharge(int charge)
        {
            _charge = charge;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(String isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public void setIsotopeLabelId(int isotopeLabelId)
        {
            _isotopeLabelId = isotopeLabelId;
        }

        public int getIsotopeLabelId()
        {
            return _isotopeLabelId;
        }

        @Override
        public String toString()
        {
            StringBuilder label = new StringBuilder();
            label.append(LabelFactory.getChargeLabel(getCharge()));
            if(getIsotopeLabel() != null)
            {
                label.append(" ").append(getIsotopeLabel());
            }
            return label.toString();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SeriesLabel that = (SeriesLabel) o;

            if (_charge != that._charge) return false;
            if (_isotopeLabel != null ? !_isotopeLabel.equals(that._isotopeLabel) : that._isotopeLabel != null)
                return false;
            if(_isotopeLabelId != that._isotopeLabelId) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result;
            result = _charge;
            result = 31 * result + (_isotopeLabel != null ? _isotopeLabel.hashCode() : 0);
            result = 31 * result + _isotopeLabelId;
            return result;
        }

        @Override
        public int compareTo(@NotNull SeriesLabel o)
        {
            int cmp = Integer.valueOf(this.getCharge()).compareTo(o.getCharge());
            if (cmp != 0)
            {
                return cmp;
            }
            else
            {
                return Integer.valueOf(this.getIsotopeLabelId()).compareTo(o.getIsotopeLabelId());
            }
        }
    }

    public CategoryDataset createJfreeDataset()
    {
        return createJfreeDataset(getYaxisScale());
    }

    private CategoryDataset createJfreeDataset(int peakAreaAxisMagnitude)
    {
        if(this.isStatistical())
        {
            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for(String categoryLabel: this.getSortedCategoryLabels())
            {
                ComparisonDataset.ComparisonCategoryItem categoryDataset = this.getCategoryItem(categoryLabel);

                for(ComparisonDataset.SeriesLabel seriesLabel: this.getSortedSeriesLabels())
                {
                    ComparisonDataset.ComparisonSeriesItem seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                    if(seriesDataset == null)
                    {
                        // Add an empty series otherwise color order of series can be incorrect.
                        dataset.add(0, 0, seriesLabel.toString(), categoryLabel);
                    }
                    else
                    {
                        // NOTE: This will always be a BarChartSeriesItemData
                        BarChartSeriesItemData seriesItem = (BarChartSeriesItemData) seriesDataset.getSeriesItemData();
                        dataset.add(getDataItemValue(seriesItem.getValue(), peakAreaAxisMagnitude, _logScale),
                                getDataItemValue(seriesItem.getSdev(), peakAreaAxisMagnitude, _logScale),
                                seriesLabel.toString(),
                                categoryLabel);
                    }
                }
            }
            return dataset;
        }
        else
        {
            if(_seriesItemMaker instanceof PeakAreasSeriesItemMaker)
            {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (String categoryLabel : this.getSortedCategoryLabels())
                {
                    ComparisonDataset.ComparisonCategoryItem categoryDataset = this.getCategoryItem(categoryLabel);

                    for (ComparisonDataset.SeriesLabel seriesLabel : this.getSortedSeriesLabels())
                    {
                        ComparisonDataset.ComparisonSeriesItem seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                        if (seriesDataset == null)
                        {
                            // Add an empty series otherwise color order of series can be incorrect.
                            dataset.addValue(0, seriesLabel.toString(), categoryLabel);
                        }
                        else
                        {
                            dataset.addValue(getDataItemValue(seriesDataset.getSeriesItemData().getValue(), peakAreaAxisMagnitude, _logScale), seriesLabel.toString(), categoryLabel);
                        }
                    }
                }
                return dataset;
            }
            else if(_seriesItemMaker instanceof RetentionTimesAllValuesSeriesItemMaker)
            {
                DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
                for (String categoryLabel : this.getSortedCategoryLabels())
                {
                    ComparisonDataset.ComparisonCategoryItem categoryDataset = this.getCategoryItem(categoryLabel);

                    for (ComparisonDataset.SeriesLabel seriesLabel : this.getSortedSeriesLabels())
                    {
                        ComparisonDataset.ComparisonSeriesItem seriesDataset = categoryDataset.getSeriesDataset(seriesLabel);
                        if(seriesDataset != null)
                        {
                            BoxAndWhisterSeriesItemData seriesItem = (BoxAndWhisterSeriesItemData) seriesDataset.getSeriesItemData();
                            if(seriesItem == null)
                            {
                                continue;
                            }
                            RetentionTimeDatasetItem item = new RetentionTimeDatasetItem(seriesItem.getRtAtPeak(),
                                                                                       seriesItem.getMinRt(),
                                                                                       seriesItem.getMaxRt(),
                                                                                       seriesItem.getFwhmStart(),
                                                                                       seriesItem.getFwhmEnd());
                            dataset.add(item, seriesLabel.toString(), categoryLabel);
                        }
                    }
                }
                return dataset;
            }
        }
        return null;
    }

    private double getDataItemValue(double value, int peakAreaAxisMagnitude, boolean logScale)
    {
        return logScale ? value : value / peakAreaAxisMagnitude;
    }

    public int getYaxisScale()
    {
        double quotient =  getMaxValue() / 1000;
        return quotient < 1 ? 1 : (quotient > 1000 ? 1000000 : 1000);
    }

    public String getYaxisScaleString()
    {
        int scale = getYaxisScale();
        return scale == 1 ? "" : (scale == 1000 ? "10^3" : "10^6");
    }

    private static interface SeriesItemData
    {
        public double getValue();

        public boolean isStatistical();
    }

    private static class BarChartSeriesItemData implements SeriesItemData
    {
        private final double _value;
        private final double _sdev;
        private final boolean _isStatistical;

        public BarChartSeriesItemData(double value, double sdev, boolean isStatistical)
        {
            _value = value;
            _sdev = sdev;
            _isStatistical = isStatistical;
        }

        public double getValue()
        {
            return _value;
        }

        public double getSdev()
        {
            return _sdev;
        }

        public boolean isStatistical()
        {
            return _isStatistical;
        }
    }

    private static class BoxAndWhisterSeriesItemData implements SeriesItemData
    {
        private final Double _rtAtPeak;
        private final Double _minRt;
        private final Double _maxRt;
        private final double _fwhmStart;
        private final double _fwhmEnd;

        public BoxAndWhisterSeriesItemData(Double rtAtPeak, Double minRt, Double maxRt, Double fwhm)
        {
            _minRt = minRt;
            _maxRt = maxRt;
            if (rtAtPeak == null || fwhm == null)
            {
                _rtAtPeak = minRt;
                _fwhmStart = minRt;
                _fwhmEnd = maxRt;
            }
            else
            {
                _rtAtPeak = rtAtPeak;
                _fwhmStart = Math.max(_minRt, rtAtPeak - (fwhm / 2.0));
                _fwhmEnd = Math.min(_maxRt, rtAtPeak + (fwhm / 2.0));
            }
        }
        @Override
        public double getValue()
        {
            return getRtAtPeak();
        }

        @Override
        public boolean isStatistical()
        {
            return false;
        }

        public double getRtAtPeak()
        {
            return _rtAtPeak;
        }

        public double getMinRt()
        {
            return _minRt;
        }

        public double getMaxRt()
        {
            return _maxRt;
        }

        public double getFwhmStart()
        {
            return _fwhmStart;
        }

        public double getFwhmEnd()
        {
            return _fwhmEnd;
        }
    }

    static interface SeriesItemMaker
    {
        public SeriesItemData make(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues);
    }

    public static class PeakAreasSeriesItemMaker implements SeriesItemMaker
    {
        public BarChartSeriesItemData make(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues)
        {
            double value = 0.0;
            double sdev = 0;
            boolean isStatistical = false;

            if(pciPlusList.size() == 1)
            {
                Double peakArea = pciPlusList.get(0).getTotalArea();
                if(peakArea != null && !cvValues)
                {
                    value = peakArea;
                }
            }
            else
            {
                SummaryStatistics stats = new SummaryStatistics();
                for(PrecursorChromInfoPlus chromInfo: pciPlusList)
                {
                    if(chromInfo.getTotalArea() == null)
                        continue;
                    stats.addValue(chromInfo.getTotalArea());
                }

                value = stats.getMean();
                sdev = stats.getStandardDeviation();
                if(cvValues)
                {
                    value = (sdev * 100.0) / value;
                    sdev = 0;
                }
                else
                {
                    isStatistical = true;
                }
            }
            return new BarChartSeriesItemData(value, sdev, isStatistical);
        }
    }

    // Makes a series item with the min, max and peak apex retention times as well as fwhm.
    public static class RetentionTimesAllValuesSeriesItemMaker implements SeriesItemMaker
    {

        @Override
        public BoxAndWhisterSeriesItemData make(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues)
        {
            if(pciPlusList.size() == 1)
            {
                PrecursorChromInfoPlus pciPlus = pciPlusList.get(0);
                if(pciPlus.getBestRetentionTime() == null)
                {
                    return null;
                }
                return new BoxAndWhisterSeriesItemData(pciPlus.getBestRetentionTime(),
                        pciPlus.getMinStartTime(),
                        pciPlus.getMaxEndTime(),
                        pciPlus.getMaxFwhm());
            }


            double rtAtPeakApex = 0;
            double minStartTime = 0;
            double maxEndTime = 0;
            double fwhm = 0;
            int count = 0;
            for(PrecursorChromInfoPlus pciPlus: pciPlusList)
            {
                if(pciPlus.getBestRetentionTime() == null)
                    continue;
                rtAtPeakApex += pciPlus.getBestRetentionTime();
                minStartTime += pciPlus.getMinStartTime();
                maxEndTime += pciPlus.getMaxEndTime();
                fwhm += pciPlus.getMaxFwhm();
                count++;
            }

            if(count == 0)
                return null;

            return new BoxAndWhisterSeriesItemData(rtAtPeakApex / count,
                    minStartTime / count,
                    maxEndTime / count,
                    fwhm / count);
        }
    }

    public static class RetentionTimeDatasetItem extends BoxAndWhiskerItem
    {
        private Number _fwhmStart;
        private Number _fwhmEnd;

        public RetentionTimeDatasetItem(Number rtAtPeak, Number minRt, Number maxRt, Number fwhmStart, Number fwhmEnd)
        {
            super(null,
                    rtAtPeak, // Median
                    minRt,  // Q1
                    maxRt,  // Q3
                    minRt,  // Use the same value as the Q1 value, otherwise the plot gets cut off.
                    maxRt,  // Use the same value as the Q3 value, otherwise the plot gets cut off.
                    null, null, null);

            _fwhmStart = fwhmStart;
            _fwhmEnd = fwhmEnd;
        }

        public Number getFwhmStart()
        {
            return _fwhmStart;
        }

        public Number getFwhmEnd()
        {
            return _fwhmEnd;
        }
    }
}
