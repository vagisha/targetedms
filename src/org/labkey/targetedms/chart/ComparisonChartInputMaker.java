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
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.ReplicateManager;

import java.util.ArrayList;
import java.util.Collections;
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
public class ComparisonChartInputMaker
{
    private final ComparisonDataset.ChartType _chartType;
    private final int _runId;
    private List<PrecursorChromInfoPlus> _pciPlusList;  // precursor = modified sequence + charge + isotope label
    private String _groupByAnnotationName;
    private String _filterByAnnotationValue;
    private boolean _cvValues = false;
    private boolean _logValues = false;

    public ComparisonChartInputMaker(int runId, List<PrecursorChromInfoPlus> pciPlusList, ComparisonDataset.ChartType chartType)
    {
        _runId = runId;
        _chartType = chartType;
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

    public ComparisonDataset make(ComparisonDataset.SeriesItemMaker seriesItemMaker)
    {

        if (_filterByAnnotationValue != null)
        {
            _pciPlusList = filterInputList();
        }

        // If we are grouping by an annotation, create a map of sample fileID and annotation value
        Map<Integer, String> sampleFileAnnotMap = getSampleAnnotationMap();

        if (_chartType == ComparisonDataset.ChartType.PEPTIDE_COMPARISON)
        {
            Map<ComparisonCategory.PeptideCategory, List<PrecursorChromInfoPlus>> datasetMap = new HashMap<>();

            for (PrecursorChromInfoPlus pciPlus : _pciPlusList)
            {
                ComparisonCategory.PeptideCategory categoryLabel = getPeptideCategoryLabel(pciPlus, sampleFileAnnotMap);
                if (!StringUtils.isBlank(_groupByAnnotationName) && !categoryLabel.hasAnnotationValue())
                    continue;

                List<PrecursorChromInfoPlus> categoryPciList = datasetMap.get(categoryLabel);
                if (categoryPciList == null)
                {
                    categoryPciList = new ArrayList<>();
                    datasetMap.put(categoryLabel, categoryPciList);
                }
                categoryPciList.add(pciPlus);
            }

            ComparisonCategory.PeptideCategory.trimPeptideCategoryLabels(datasetMap.keySet());

            ComparisonDataset dataset = new ComparisonDataset(_runId, seriesItemMaker, _logValues);
            dataset.setSetSortByValues(StringUtils.isBlank(_groupByAnnotationName));

            for (ComparisonCategory.PeptideCategory category : datasetMap.keySet())
            {
                ComparisonDataset.ComparisonCategoryItem categoryDataset = new ComparisonDataset.ComparisonCategoryItem(category);
                categoryDataset.setData(seriesItemMaker, datasetMap.get(category), _cvValues, _chartType);
                dataset.addCategory(categoryDataset);
            }
            return dataset;
        }
        else
        {
            Map<Integer, String> sampleFileReplicateMap = getSampleFileReplicateMap();

            Map<String, List<PrecursorChromInfoPlus>> datasetMap = new HashMap<>();

            for (PrecursorChromInfoPlus pciPlus : _pciPlusList)
            {
                String categoryLabel = StringUtils.isBlank(_groupByAnnotationName) ?
                        sampleFileReplicateMap.get(pciPlus.getSampleFileId()) :
                        sampleFileAnnotMap.get(pciPlus.getSampleFileId());
                if (categoryLabel == null)
                    continue;


                List<PrecursorChromInfoPlus> categoryPciList = datasetMap.get(categoryLabel);
                if (categoryPciList == null)
                {
                    categoryPciList = new ArrayList<>();
                    datasetMap.put(categoryLabel, categoryPciList);
                }
                categoryPciList.add(pciPlus);
            }

            ComparisonDataset dataset = new ComparisonDataset(_runId, seriesItemMaker, _logValues);

            for (String categoryLabel : datasetMap.keySet())
            {
                ComparisonDataset.ComparisonCategoryItem categoryDataset = new ComparisonDataset.ComparisonCategoryItem(new ComparisonCategory.ReplicateCategory(categoryLabel));
                categoryDataset.setData(seriesItemMaker, datasetMap.get(categoryLabel), _cvValues, _chartType);
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

    private ComparisonCategory.PeptideCategory getPeptideCategoryLabel(PrecursorChromInfoPlus pciPlus,
                                                    Map<Integer, String> sampleFileAnnotMap)
    {
        return new ComparisonCategory.PeptideCategory(pciPlus.getPeptideModifiedSequence(),
                                   pciPlus.getCharge(),
                                   pciPlus.getIsotopeLabel(),
                                   sampleFileAnnotMap.get(pciPlus.getSampleFileId()));
    }
}
