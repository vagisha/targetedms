/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.PeptideSettings;
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
    private String _filterByAnnotationValue;
    private boolean _cvValues = false;
    private boolean _logValues = false;

    public void setChartType(ChartType chartType, int runId)
    {
        _chartType = chartType;
        _runId = runId;
    }

    public void setPrecursorChromInfoList(List<PrecursorChromInfoPlus> pciPlusList)
    {
        if(pciPlusList != null)
        {
            _pciPlusList = pciPlusList;
            Collections.sort(_pciPlusList, new PrecursorChromInfoPlus.PrecursorChromInfoComparator());
        }
        else
        {
            _pciPlusList = Collections.emptyList();
        }
    }

    public void setGroupByAnnotationName(String groupByAnnotationName)
    {
        if(!"None".equalsIgnoreCase(groupByAnnotationName))
            _groupByAnnotationName = groupByAnnotationName;
    }

    public void setFilterByAnnotationValue(String filterByAnnotationValue)
    {
        if(!"None".equalsIgnoreCase(filterByAnnotationValue))
            _filterByAnnotationValue = filterByAnnotationValue;
    }

    public void setCvValues(boolean cvValues)
    {
        _cvValues = cvValues;
    }
    public void setLogValues(boolean logValues)
    {
        _logValues = logValues;
    }

    public PeakAreaDataset make()
    {

        if(_filterByAnnotationValue != null)
        {
            _pciPlusList = filterInputList();
        }

        // If we are grouping by an annotation, create a map of sample fileID and annotation value
        Map<Integer, String> sampleFileAnnotMap = getSampleAnnotationMap();

        if(_chartType == ChartType.PEPTIDE_COMPARISON)
        {
            Map<PeptideCategory, List<PrecursorChromInfoPlus>> datasetMap = new HashMap<>();

            for(PrecursorChromInfoPlus pciPlus: _pciPlusList)
            {
                PeptideCategory categoryLabel = getPeptideCategoryLabel(pciPlus, sampleFileAnnotMap);
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

            trimPeptideCategoryLabels(datasetMap.keySet());

            PeakAreaDataset dataset = new PeakAreaDataset();
            dataset.setSortByPeakAreas(StringUtils.isBlank(_groupByAnnotationName));

            for (PeptideCategory category: datasetMap.keySet())
            {
                PeakAreaCategoryDataset categoryDataset = new PeakAreaCategoryDataset(category);
                categoryDataset.setData(datasetMap.get(category), _cvValues, _logValues, _chartType);
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
                PeakAreaCategoryDataset categoryDataset = new PeakAreaCategoryDataset(new ReplicateComparisonCategory(categoryLabel));
                categoryDataset.setData(datasetMap.get(categoryLabel), _cvValues, _logValues, _chartType);
                dataset.addCategory(categoryDataset);
            }
            return dataset;
        }
    }

    private List<PrecursorChromInfoPlus> filterInputList()
    {
        List<SampleFile> sampleFileList = ReplicateManager.getSampleFilesForRun(_runId);
        Map<Integer, Integer> sampleFileReplicateMap = new HashMap<>();
        for(SampleFile file: sampleFileList)
        {
            sampleFileReplicateMap.put(file.getId(), file.getReplicateId());
        }

        List<ReplicateAnnotation> annotationList = ReplicateManager.getReplicateAnnotationsForRun(_runId);
        Set<Integer> replicateIdsToKeep = new HashSet<>();
        for(ReplicateAnnotation annotation: annotationList)
        {
            if(_filterByAnnotationValue.equalsIgnoreCase(annotation.getDisplayName()))
            {
                replicateIdsToKeep.add(annotation.getReplicateId());
            }
        }

        List<PrecursorChromInfoPlus> listToKeep =  new ArrayList<>();
        for(PrecursorChromInfoPlus pci: _pciPlusList)
        {
            if(replicateIdsToKeep.contains(sampleFileReplicateMap.get(pci.getSampleFileId())))
            {
                listToKeep.add(pci);
            }
        }
        return listToKeep;
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

    private static Map<String, Set<Integer>> getPeptideChargeMap(Set<PeptideCategory> peptideCategories)
    {
        Map<String, Set<Integer>> pepChargeMap = new HashMap<>();

        for(PeptideCategory pepCategory: peptideCategories)
        {
            String peptideChargeMapKey = getPeptideChargeMapKey(pepCategory);
            if(peptideChargeMapKey == null)
                continue;
            Set<Integer> pepChargeStates = pepChargeMap.get(peptideChargeMapKey);
            if(pepChargeStates == null)
            {
                pepChargeStates = new HashSet<>();
                pepChargeMap.put(peptideChargeMapKey, pepChargeStates);
            }
            pepChargeStates.add(pepCategory.getCharge());
        }

        return pepChargeMap;
    }

    private static String getPeptideChargeMapKey(PeptideCategory pepCategory)
    {
        if(pepCategory != null)
        {
            return pepCategory.getModifiedSequence() + "_" + pepCategory.getIsotopeLabel();
        }
        return null;
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
                                                    Map<Integer, String> sampleFileAnnotMap)
    {
        return new PeptideCategory(pciPlus.getPeptideModifiedSequence(),
                                   pciPlus.getCharge(),
                                   pciPlus.getIsotopeLabel(),
                                   sampleFileAnnotMap.get(pciPlus.getSampleFileId()));
    }

    private static void trimPeptideCategoryLabels(Set<PeptideCategory> peptideCategories)
    {
        Map<String, Set<Integer>> peptideChargeMap = getPeptideChargeMap(peptideCategories);
        for(PeptideCategory pepCategory: peptideCategories)
        {
            if(peptideChargeMap != null && peptideChargeMap.get(getPeptideChargeMapKey(pepCategory)).size() == 1)
                pepCategory.setUseChargeInDisplayLabel(false);
        }

        makeUniquePrefixes(new ArrayList<>(peptideCategories), 3);
    }

    private static void makeUniquePrefixes(List<PeptideCategory> peptideCategories, int prefixLen)
    {
        if(peptideCategories == null || peptideCategories.size() == 0)
            return;

        if(peptideCategories.size() == 1)
        {
            PeptideCategory category = peptideCategories.get(0);
            String sequence = category.getSequence();
            prefixLen = Math.max(3, prefixLen - 1);
            category.setSeqPrefix(sequence.substring(0, Math.min(prefixLen, sequence.length())));
            return;
        }

        Set<String> uniqSequences = new HashSet<>();
        for(PeptideCategory category: peptideCategories)
        {
            uniqSequences.add(category.getSequence());
        }
        if(uniqSequences.size() == 1)
        {
            prefixLen = Math.max(3, prefixLen - 1);
            prefixLen = Math.min(prefixLen, peptideCategories.get(0).getSequence().length());

            // If all the given categories have the same sequence, set the
            for(PeptideCategory category: peptideCategories)
            {
                String sequence = category.getSequence();
                category.setSeqPrefix(sequence.substring(0, prefixLen));
            }
            return;
        }

        Map<String, List<PeptideCategory>> prefixCategoryMap = new HashMap<>();
        for(PeptideCategory category: peptideCategories)
        {
            String sequence = category.getSequence();
            String prefix = category.getSequence().substring(0, Math.min(sequence.length(), prefixLen));
            List<PeptideCategory> categoriesForPrefix = prefixCategoryMap.get(prefix);
            if(categoriesForPrefix == null)
            {
                categoriesForPrefix = new ArrayList<>();
                prefixCategoryMap.put(prefix, categoriesForPrefix);

            }
            categoriesForPrefix.add(category);
        }

        for(String prefix: prefixCategoryMap.keySet())
        {
            List<PeptideCategory> categoriesForPrefix = prefixCategoryMap.get(prefix);
            makeUniquePrefixes(categoriesForPrefix, prefixLen + 1);
        }
    }

    static interface PeakAreaCategory
    {
        public String getCategoryLabel();
        public String getDisplayLabel();
    }

    static class ReplicateComparisonCategory implements PeakAreaCategory
    {
        private final String _label;

        public ReplicateComparisonCategory(String label)
        {
            _label = label;
        }

        @Override
        public String getCategoryLabel()
        {
            return _label;
        }

        @Override
        public String getDisplayLabel()
        {
            return _label;
        }
    }

    static class PeptideCategory implements PeakAreaCategory
    {
        private String _modifiedSequence;
        private int _charge;
        private String _isotopeLabel;
        private String _annotationValue;
        private String _sequence;
        private String _seqPrefix;
        private boolean _useChargeInDisplayLabel = true;

        public PeptideCategory(String modifiedSequence, int charge, String isotopeLabel, String annotValue)
        {
            _modifiedSequence = modifiedSequence;
            _sequence = makeSequenceWithLowerCaseMods(modifiedSequence);
            _seqPrefix = _sequence;
            _charge = charge;
            _isotopeLabel = isotopeLabel;
            _annotationValue = annotValue;
        }

        private String makeSequenceWithLowerCaseMods(String modifiedSequence)
        {
            StringBuilder sb = new StringBuilder(modifiedSequence.length());
            int index = 0;
            while (true)
            {
                int modificationIndex = modifiedSequence.indexOf('[', index);
                if (modificationIndex < 0)
                {
                    sb.append(modifiedSequence.substring(index));
                    return sb.toString();
                }
                sb.append(modifiedSequence.substring(index, modificationIndex - 1));
                sb.append(Character.toLowerCase(modifiedSequence.charAt(modificationIndex - 1)));
                index = modifiedSequence.indexOf(']', modificationIndex + 1) + 1;
                if (index == 0)
                    return sb.toString();
            }
        }

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public String getSequence()
        {
            return _sequence;
        }

        public int getCharge()
        {
            return _charge;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public String getCategoryLabel()
        {
            StringBuilder label = new StringBuilder();

            if(hasAnnotationValue())
            {
                label.append(_annotationValue).append(", ");
            }
            label.append(_modifiedSequence);
            if(_charge > 0)
            {
                label.append(LabelFactory.getChargeLabel(_charge));
            }
            if(_isotopeLabel != null && !_isotopeLabel.equalsIgnoreCase(PeptideSettings.IsotopeLabel.LIGHT))
            {
                label.append(" (").append(_isotopeLabel).append(")");
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

        public void setUseChargeInDisplayLabel(boolean useChargeInDisplayLabel)
        {
            _useChargeInDisplayLabel = useChargeInDisplayLabel;
        }

        public void setSeqPrefix(String seqPrefix)
        {
            _seqPrefix = seqPrefix;
        }

        @Override
        public String getDisplayLabel()
        {
            StringBuilder label = new StringBuilder();

            if(hasAnnotationValue())
            {
                label.append(_annotationValue).append(", ");
            }
            label.append(_seqPrefix
            );
            if(_useChargeInDisplayLabel)
            {
                label.append(LabelFactory.getChargeLabel(_charge));
            }
            return label.toString();
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
        private PeakAreaCategory _category;
        private Map<SeriesLabel, PeakAreaSeriesDataset> _seriesDatasetsMap;
        private double _maxPeakArea;

        public PeakAreaCategoryDataset(PeakAreaCategory category)
        {
            _category = category;
        }

        public String getCategoryLabel()
        {
            return _category.getCategoryLabel();
        }

        public PeakAreaCategory getCategory()
        {
            return _category;
        }

        public void setData(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues, boolean logValues, ChartType chartType)
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
                seriesDataset.setData(seriesDataMap.get(seriesLabel), cvValues, logValues);
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

        public void setData(List<PrecursorChromInfoPlus> pciPlusList, boolean cvValues, boolean logValues)
        {
            //double denominator = 1000000.0;
            if(pciPlusList.size() == 1)
            {
                Double peakArea = pciPlusList.get(0).getTotalArea();
                if(peakArea != null && !cvValues)
                {
                    _value = peakArea;
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

     public static class TestCase extends Assert
    {
        @Test
        public void testTrimPeptideCategoryLabels() throws Exception
        {
            PeptideCategory category1 = new PeptideCategory("A", 2, "light", null);
            PeptideCategory category2 = new PeptideCategory("AB", 2, "light", null);
            PeptideCategory category3 = new PeptideCategory("ABCXYZ", 2, "light", null);
            PeptideCategory category4 = new PeptideCategory("ABCXYZ", 3, "light", null);
            PeptideCategory category5 = new PeptideCategory("ABCXYZ", 2, "heavy", null);
            PeptideCategory category6 = new PeptideCategory("ABCXYZ", 3, "heavy", null);
            PeptideCategory category7 = new PeptideCategory("ABDAAA", 2, "light", null);
            PeptideCategory category8 = new PeptideCategory("ABDEEEE", 2, "light", null);
            PeptideCategory category9 = new PeptideCategory("ABDFAAA", 2, "light", null);
            PeptideCategory category10 = new PeptideCategory("UVWXYZ", 2, "light", null);
            PeptideCategory category11 = new PeptideCategory("S[+122.0]DKPDM[+16.0]AEIEKFDK", 2, "light", null);
            PeptideCategory category12 = new PeptideCategory("S[+122.0]DKPDMAEIEKFDK", 2, "light", null);


            Set<PeptideCategory> peptideCategoryList = new HashSet<PeptideCategory>(1);
            peptideCategoryList.add(category1);
            peptideCategoryList.add(category2);
            peptideCategoryList.add(category3);
            peptideCategoryList.add(category4);
            peptideCategoryList.add(category5);
            peptideCategoryList.add(category6);
            peptideCategoryList.add(category7);
            peptideCategoryList.add(category8);
            peptideCategoryList.add(category9);
            peptideCategoryList.add(category10);
            peptideCategoryList.add(category11);
            peptideCategoryList.add(category12);

            PeakAreasChartInputMaker.trimPeptideCategoryLabels(peptideCategoryList);
            assertTrue(category1.getCategoryLabel().equals("A++"));
            assertTrue(category1.getDisplayLabel().equals("A"));
            assertTrue(category2.getCategoryLabel().equals("AB++"));
            assertTrue(category2.getDisplayLabel().equals("AB"));
            assertTrue(category3.getCategoryLabel().equals("ABCXYZ++"));
            assertTrue(category3.getDisplayLabel().equals("ABC++"));
            assertTrue(category4.getCategoryLabel().equals("ABCXYZ+++"));
            assertTrue(category4.getDisplayLabel().equals("ABC+++"));
            assertTrue(category5.getCategoryLabel().equals("ABCXYZ++ (heavy)"));
            assertTrue(category5.getDisplayLabel().equals("ABC++"));
            assertTrue(category6.getCategoryLabel().equals("ABCXYZ+++ (heavy)"));
            assertTrue(category6.getDisplayLabel().equals("ABC+++"));
            assertTrue(category7.getCategoryLabel().equals("ABDAAA++"));
            assertTrue(category7.getDisplayLabel().equals("ABDA"));
            assertTrue(category8.getCategoryLabel().equals("ABDEEEE++"));
            assertTrue(category8.getDisplayLabel().equals("ABDE"));
            assertTrue(category9.getCategoryLabel().equals("ABDFAAA++"));
            assertTrue(category9.getDisplayLabel().equals("ABDF"));
            assertTrue(category10.getCategoryLabel().equals("UVWXYZ++"));
            assertTrue(category10.getDisplayLabel().equals("UVW"));
            assertTrue(category11.getCategoryLabel().equals("S[+122.0]DKPDM[+16.0]AEIEKFDK++"));
            assertTrue(category11.getDisplayLabel().equals("sDKPDm"));
            assertTrue(category12.getCategoryLabel().equals("S[+122.0]DKPDMAEIEKFDK++"));
            assertTrue(category12.getDisplayLabel().equals("sDKPDM"));
        }
    }
}
