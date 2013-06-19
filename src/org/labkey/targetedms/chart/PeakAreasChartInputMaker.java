/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.ReplicateManager;

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
 * Date: 10/11/12
 * Time: 2:34 PM
 */
public class PeakAreasChartInputMaker
{
    static enum ChartType {PEPTIDE_COMPARISON, REPLICATE_COMPARISON}

    private ChartType _chartType;
    private int _runId;
    private List<PrecursorChromInfoPlus> _pciPlusList;  // precursor = modified sequence + charge + isotope label
    private String _groupByAnnotationName;
    private boolean _cvValues = false;

    public void setChartType(ChartType chartType, int runId)
    {
        _chartType = chartType;
        _runId = runId;
    }

    public void setPrecursorChromInfoList(List<PrecursorChromInfoPlus> pciPlusList)
    {
        _pciPlusList = pciPlusList;
    }

    public void setGroupByAnnotationName(String groupByAnnotationName)
    {
        if(!"None".equalsIgnoreCase(groupByAnnotationName))
            _groupByAnnotationName = groupByAnnotationName;
    }

    public void setCvValues(boolean cvValues)
    {
        _cvValues = cvValues;
    }

    public PeakAreaDataset make()
    {
        // If we are grouping by an annotation, create a map of sample fileID and annotation value
        Map<Integer, String> sampleFileAnnotMap = getSampleAnnotationMap();

        if(_chartType == ChartType.PEPTIDE_COMPARISON)
        {
            Map<String, List<Integer>> peptideChargeMap = getPeptideChargeMap(_pciPlusList);

            Map<PeptideCategory, List<PrecursorChromInfoPlus>> datasetMap = new HashMap<>();

            for(PrecursorChromInfoPlus pciPlus: _pciPlusList)
            {
                PeptideCategory categoryLabel = getPeptideCategoryLabel(pciPlus, sampleFileAnnotMap, peptideChargeMap);
                if(!StringUtils.isBlank(_groupByAnnotationName) && !categoryLabel.hasAnnotationValue())
                    continue;

                List<PrecursorChromInfoPlus> categoryPciList = datasetMap.get(categoryLabel);
                if(categoryPciList == null)
                {
                    categoryPciList = new ArrayList<>();
                    datasetMap.put(categoryLabel, categoryPciList);
                }
                categoryPciList.add(pciPlus);
            }

            PeakAreaDataset dataset = new PeakAreaDataset();
            dataset.setSortByPeakAreas(StringUtils.isBlank(_groupByAnnotationName));

            for (PeptideCategory category: datasetMap.keySet())
            {
                PeakAreaCategoryDataset categoryDataset = new PeakAreaCategoryDataset(category.getLabel());
                categoryDataset.setData(datasetMap.get(category), _cvValues, _chartType);
                dataset.addCategory(categoryDataset);
            }
            return dataset;
        }
        else
        {
            Map<Integer, String> sampleFileReplicateMap = getSampleFileReplicateMap();

            Map<String, List<PrecursorChromInfoPlus>> datasetMap = new HashMap<>();

            for(PrecursorChromInfoPlus pciPlus: _pciPlusList)
            {
                String categoryLabel = StringUtils.isBlank(_groupByAnnotationName) ?
                                       sampleFileReplicateMap.get(pciPlus.getSampleFileId()) :
                                       sampleFileAnnotMap.get(pciPlus.getSampleFileId());
                if(categoryLabel == null)
                    continue;


                List<PrecursorChromInfoPlus> categoryPciList = datasetMap.get(categoryLabel);
                if(categoryPciList == null)
                {
                    categoryPciList = new ArrayList<>();
                    datasetMap.put(categoryLabel, categoryPciList);
                }
                categoryPciList.add(pciPlus);
            }

            PeakAreaDataset dataset = new PeakAreaDataset();

            for (String categoryLabel: datasetMap.keySet())
            {
                PeakAreaCategoryDataset categoryDataset = new PeakAreaCategoryDataset(categoryLabel);
                categoryDataset.setData(datasetMap.get(categoryLabel), _cvValues, _chartType);
                dataset.addCategory(categoryDataset);
            }
            return dataset;
        }
    }

    private Map<Integer, String> getSampleFileReplicateMap()
    {
        Map<Integer, String> sampleFileReplicateMap = new HashMap<>();
        List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(_runId);
        List<Replicate> replicates = ReplicateManager.getReplicatesForRun(_runId);
        Map<Integer, String> replicateNameMap = new HashMap<>();
        for(Replicate replicate: replicates)
        {
            replicateNameMap.put(replicate.getId(), replicate.getName());
        }
        for(SampleFile sFile: sampleFiles)
        {
            sampleFileReplicateMap.put(sFile.getId(), replicateNameMap.get(sFile.getReplicateId()));
        }
        return sampleFileReplicateMap;
    }

    private Map<String, List<Integer>> getPeptideChargeMap(List<PrecursorChromInfoPlus> pciPlusList)
    {
        Map<String, List<Integer>> pepChargeMap = new HashMap<>();

        for(PrecursorChromInfoPlus pciPlus: pciPlusList)
        {
            List<Integer> pepChargeStates = pepChargeMap.get(pciPlus.getModifiedSequence());
            if(pepChargeStates == null)
            {
                pepChargeStates = new ArrayList<>();
                pepChargeMap.put(pciPlus.getModifiedSequence(), pepChargeStates);
            }
            pepChargeStates.add(pciPlus.getCharge());
        }

        return pepChargeMap;
    }

    private Map<Integer, String> getSampleAnnotationMap()
    {
        Map<Integer, String> sampleFileAnnotMap = new HashMap<>();
        if(_groupByAnnotationName != null)
        {
            List<ReplicateAnnotation> replicateAnnotationList = ReplicateManager.getReplicateAnnotationsForRun(_runId);
            Map<Integer, String> replicateAnnotationMap = new HashMap<>();
            for(ReplicateAnnotation annot: replicateAnnotationList)
            {
                if(!annot.getName().equals(_groupByAnnotationName))
                    continue;
                replicateAnnotationMap.put(annot.getReplicateId(), annot.getValue());
            }
            List<SampleFile> sampleFileList = ReplicateManager.getSampleFilesForRun(_runId);
            for(SampleFile file: sampleFileList)
            {
                String annotation = replicateAnnotationMap.get(file.getReplicateId());
                if(annotation != null)
                {
                    sampleFileAnnotMap.put(file.getId(), annotation);
                }
            }
        }
        return sampleFileAnnotMap;
    }

    private PeptideCategory getPeptideCategoryLabel(PrecursorChromInfoPlus pciPlus,
                                                    Map<Integer, String> sampleFileAnnotMap,
                                                    Map<String, List<Integer>> peptideChargeMap)
    {
        int charge = pciPlus.getCharge();
        if(peptideChargeMap != null && peptideChargeMap.get(pciPlus.getModifiedSequence()).size() == 1)
            charge = 0;
        return new PeptideCategory(pciPlus.getModifiedSequence(),
                                   charge,
                                   pciPlus.getIsotopeLabel(),
                                   sampleFileAnnotMap.get(pciPlus.getSampleFileId()));
    }

    static class PeptideCategory
    {
        private String _modifiedSequence;
        private int _charge;
        private String _isotopeLabel;
        private String _annotationValue;

        public PeptideCategory(String modifiedSequence, int charge, String isotopeLabel, String annotValue)
        {
            _modifiedSequence = modifiedSequence;
            _charge = charge;
            _isotopeLabel = isotopeLabel;
            _annotationValue = annotValue;
        }

        public String getLabel()
        {
            StringBuilder label = new StringBuilder();

            if(_annotationValue != null && !StringUtils.isBlank(_annotationValue))
            {
                label.append(_annotationValue).append(",  ");
            }
            label.append(_modifiedSequence);
            if(_charge > 0)
            {
                label.append(LabelFactory.getChargeLabel(_charge));
            }
            return label.toString();
        }

        public boolean hasAnnotationValue()
        {
            return !StringUtils.isBlank(_annotationValue);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PeptideCategory that = (PeptideCategory) o;

            if (_charge != that._charge) return false;
            if (_annotationValue != null ? !_annotationValue.equals(that._annotationValue) : that._annotationValue != null)
                return false;
            if (_isotopeLabel != null ? !_isotopeLabel.equals(that._isotopeLabel) : that._isotopeLabel != null)
                return false;
            if (!_modifiedSequence.equals(that._modifiedSequence)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _modifiedSequence.hashCode();
            result = 31 * result + _charge;
            result = 31 * result + (_isotopeLabel != null ? _isotopeLabel.hashCode() : 0);
            result = 31 * result + (_annotationValue != null ? _annotationValue.hashCode() : 0);
            return result;
        }
    }

    public static class PeakAreaDataset
    {
        private Map<String, PeakAreaCategoryDataset> _categoryDatasetMap;
        private List<String> _sortedCategoryLabels;
        private List<SeriesLabel> _sortedSeriesLabels;
        private boolean _sortByPeakAreas = false;
        private boolean _numericSort = true;
        private double _maxPeakArea = 0;

        public void setSortByPeakAreas(boolean sortByPeakAreas)
        {
            _sortByPeakAreas = sortByPeakAreas;
        }

        public void addCategory(PeakAreaCategoryDataset categoryDataset)
        {
            if(_categoryDatasetMap == null)
            {
                _categoryDatasetMap = new HashMap<>();
            }
            _categoryDatasetMap.put(categoryDataset.getCategoryLabel(), categoryDataset);
            _maxPeakArea = Math.max(_maxPeakArea, categoryDataset.getSeriesMaxValue());
        }

        public PeakAreaCategoryDataset getCategoryDataset(String categoryLabel)
        {
            return _categoryDatasetMap.get(categoryLabel);
        }

        public List<String> getSortedCategoryLabels()
        {
            if(_sortedCategoryLabels != null)
                return _sortedCategoryLabels;

           if(!_sortByPeakAreas)
            {
                for(PeakAreaCategoryDataset categoryDataset: _categoryDatasetMap.values())
                {
                    if(!categoryDataset.getCategoryLabel().matches("-?\\d+(\\.\\d+)?"))  //match a number with optional '-' and decimal.)
                    {
                        _numericSort = false;
                        break;
                    }
                }
            }
            List<PeakAreaCategoryDataset> categoryDatasets = new ArrayList<>(_categoryDatasetMap.values());
            Collections.sort(categoryDatasets, new Comparator<PeakAreaCategoryDataset>()
            {
                @Override
                public int compare(PeakAreaCategoryDataset o1, PeakAreaCategoryDataset o2)
                {
                    if(_sortByPeakAreas)
                        return Double.valueOf(o2.getSeriesMaxValue()).compareTo(o1.getSeriesMaxValue());
                    else
                    {
                        if(_numericSort)
                            return Float.valueOf(o1.getCategoryLabel()).compareTo(Float.valueOf(o2.getCategoryLabel()));
                        else
                            return o1.getCategoryLabel().compareTo(o2.getCategoryLabel());
                    }
                }
            });

            _sortedCategoryLabels = new ArrayList<>();
            for(PeakAreaCategoryDataset dataset: categoryDatasets)
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
            for(PeakAreaCategoryDataset dataset: _categoryDatasetMap.values())
            {
                seriesLabels.addAll(dataset.getSeriesLabels());
            }
            _sortedSeriesLabels = new ArrayList<>(seriesLabels);
            Collections.sort(_sortedSeriesLabels);

            return _sortedSeriesLabels;
        }

        public boolean isStatistical()
        {
            for(PeakAreaCategoryDataset categoryDataset: _categoryDatasetMap.values())
            {
                if(categoryDataset.isStatistical())
                    return true;
            }
            return false;
        }

        public double getMaxPeakArea()
        {
            return _maxPeakArea;
        }

        public int getMaxSeriesCount()
        {
            int maxCount = 0;
            for(PeakAreaCategoryDataset dataset: _categoryDatasetMap.values())
            {
                maxCount = Math.max(maxCount, dataset.getSeriesLabels().size());
            }
            return maxCount;
        }
    }

    public static class PeakAreaCategoryDataset
    {
        private String _categoryLabel;  // goes on the X-axis
        private Map<SeriesLabel, PeakAreaSeriesDataset> _seriesDatasetsMap;
        private double _maxPeakArea;

        public PeakAreaCategoryDataset(String label)
        {
            _categoryLabel = label;
        }

        public String getCategoryLabel()
        {
            return _categoryLabel;
        }

        public void setData(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues, ChartType chartType)
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
                PeakAreaSeriesDataset seriesDataset = new PeakAreaSeriesDataset(seriesLabel);
                seriesDataset.setData(seriesDataMap.get(seriesLabel), cvValues);
                _seriesDatasetsMap.put(seriesLabel, seriesDataset);
                _maxPeakArea = Math.max(_maxPeakArea, seriesDataset.getValue());
            }
        }
        public PeakAreaSeriesDataset getSeriesDataset(SeriesLabel seriesLabel)
        {
            return _seriesDatasetsMap.get(seriesLabel);
        }

        public double getSeriesMaxValue()
        {
//            double maxVal = 0;
//            for(PeakAreaSeriesDataset dataset: _seriesDatasetsMap.values())
//            {
//                maxVal = Math.max(maxVal, dataset.getValue());
//            }
//            return maxVal;
            return _maxPeakArea;
        }

        public List<SeriesLabel> getSeriesLabels()
        {
            return new ArrayList<>(_seriesDatasetsMap.keySet());
        }

        public boolean isStatistical()
        {
            for(PeakAreaSeriesDataset seriesDataset: _seriesDatasetsMap.values())
            {
                if(seriesDataset.isStatistical())
                    return true;
            }
            return false;
        }
    }

    static class SeriesLabel implements Comparable<SeriesLabel>
    {
        private int _charge;
        private String _isotopeLabel = "";

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

            return true;
        }

        @Override
        public int hashCode()
        {
            int result;
            result = _charge;
            result = 31 * result + (_isotopeLabel != null ? _isotopeLabel.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(SeriesLabel o)
        {
            int cmp = Integer.valueOf(this.getCharge()).compareTo(o.getCharge());
            if(cmp != 0)
            {
                return cmp;
            }
            else
            {
                if(this.getIsotopeLabel() == null && o.getIsotopeLabel() == null)
                    return 0;
                if(this.getIsotopeLabel() == null)
                    return 1;
                if(o.getIsotopeLabel() == null)
                    return -1;
                return this.getIsotopeLabel().compareTo(o.getIsotopeLabel());
            }
        }
    }

    public static class PeakAreaSeriesDataset
    {
        private SeriesLabel _seriesLabel;    // goes in the legend
        private double _value;
        private double _sdev;
        private boolean _isStatistical;

        public PeakAreaSeriesDataset(SeriesLabel label)
        {
            _seriesLabel = label;
        }

        public SeriesLabel getSeriesLabel()
        {
            return _seriesLabel;
        }

        public void setData(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues)
        {
            //double denominator = 1000000.0;
            if(pciPlusList.size() == 1)
            {
                Double peakArea = pciPlusList.get(0).getTotalArea();
                if(peakArea != null && !cvValues)
                    _value = peakArea;
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

                _value = stats.getMean();
                _sdev = stats.getStandardDeviation();
                if(cvValues)
                {
                    _value = (_sdev * 100.0) / _value;
                    _sdev = 0;
                }
                else
                {
                    _isStatistical = true;
                }
            }
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
}
