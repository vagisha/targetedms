/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.calculations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.AbstractAnnotation;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.SkylineEntity;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by nicksh on 4/13/2016.
 */
public class ReplicateDataSet
{
    Map<Integer, Replicate> _replicates = new HashMap<>();
    Map<Integer, Integer> _fileIdToReplicateId = new HashMap<>();
    Map<Integer, Map<String, ReplicateAnnotation>> _replicateAnnotations;
    Map<Integer, PeptideSettings.IsotopeLabel> _isotopeLabels;

    public ReplicateDataSet(TargetedMSRun run)
    {
        _replicates = ReplicateManager.getReplicatesForRun(run.getRunId()).stream()
                .collect(Collectors.toMap(Replicate::getId, Function.identity()));
        _fileIdToReplicateId = ReplicateManager.getSampleFilesForRun(run.getRunId()).stream()
                .collect(Collectors.toMap(SampleFile::getId, SampleFile::getReplicateId));
        _replicateAnnotations = ReplicateManager.getReplicateAnnotationsForRun(run.getRunId()).stream()
                .collect(Collectors.groupingBy(ReplicateAnnotation::getReplicateId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toAnnotationMap(entry.getValue())));
        _isotopeLabels = IsotopeLabelManager.getIsotopeLabels(run.getRunId()).stream()
                .collect(Collectors.toMap(SkylineEntity::getId, Function.identity()));
    }

    public Collection<Replicate> listReplicates()
    {
        return _replicates.values();
    }

    @Nullable
    public Replicate getReplicateFromSampleFileId(int sampleFileId)
    {
        Integer replicateId = _fileIdToReplicateId.get(sampleFileId);
        if (replicateId == null)
        {
            return null;
        }
        return _replicates.get(replicateId);
    }

    @NotNull
    public String getIsotopeLabelName(int isotopeLabelId)
    {
        PeptideSettings.IsotopeLabel isotopeLabel = _isotopeLabels.get(isotopeLabelId);
        if (isotopeLabel == null)
        {
            return "light";
        }
        return isotopeLabel.getName();
    }

    @NotNull
    public Map<String, ReplicateAnnotation> getReplicateAnnotations(Replicate replicate)
    {
        Map<String, ReplicateAnnotation> annotations = _replicateAnnotations.get(replicate.getId());
        if (annotations != null)
        {
            return annotations;
        }
        return Collections.emptyMap();
    }

    @Nullable
    public String getReplicateAnnotationValue(Replicate replicate, String annotationName)
    {
        ReplicateAnnotation annotation = getReplicateAnnotations(replicate).get(annotationName);
        if (annotation == null)
        {
            return null;
        }
        return annotation.getValue();
    }

    private static <TAnnotation extends AbstractAnnotation> Map<String, TAnnotation> toAnnotationMap(
            Collection<TAnnotation> annotations)
    {
        return annotations.stream().collect(Collectors.toMap(AbstractAnnotation::getName, Function.identity()));
    }

    public Set<String> listAnnotationValues(String annotationName)
    {
        Set<String> result = new HashSet<>();
        for (Replicate replicate : listReplicates())
        {
            result.add(getReplicateAnnotationValue(replicate, annotationName));
        }
        return result;
    }

    public Collection<PeptideSettings.IsotopeLabel> listIsotopeLabels()
    {
        return _isotopeLabels.values();
    }
}
