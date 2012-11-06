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
package org.labkey.targetedms.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 10/23/12
 * Time: 9:56 PM
 */
public class DataSettings
{
    public enum AnnotationType { text, number, true_false, value_list }

    public enum AnnotationTarget {
        protein,
        peptide,
        precursor,
        transition,
        replicate,
        precursor_result,
        transition_result
    }

    private Map<String, AnnotationDefinition> _annotationDefinitions = new HashMap<String, AnnotationDefinition>();
    private Map<AnnotationTarget, List<AnnotationDefinition>> _targetAnnotationsMap =
                        new HashMap<AnnotationTarget, List<AnnotationDefinition>>();

    public void addAnnotations(String name, String targetsString, String type)
    {
        String[] targetsArr = targetsString.replaceAll("\\s", "").split(",");
        if(targetsArr.length == 0)
        {
            throw new IllegalStateException("No targets found for annotation "+name);
        }
        List<AnnotationTarget> targets = new ArrayList<AnnotationTarget>(targetsArr.length);
        for(String targetStr: targetsArr)
        {
            targets.add(AnnotationTarget.valueOf(targetStr));
        }

        AnnotationDefinition annot = new AnnotationDefinition(
                                        name,
                                        targets,
                                        AnnotationType.valueOf(type));
        _annotationDefinitions.put(name, annot);

        for(AnnotationTarget target: annot.getTargets())
        {
            List<AnnotationDefinition> targetAnnotations = _targetAnnotationsMap.get(target);
            if(targetAnnotations == null)
            {
                targetAnnotations = new ArrayList<AnnotationDefinition>();
                _targetAnnotationsMap.put(target, targetAnnotations);
            }
            targetAnnotations.add(annot);
        }
    }

    public boolean isBooleanAnnotation(String name) {

        AnnotationDefinition annot = _annotationDefinitions.get(name);
        return annot != null && annot.getType() == AnnotationType.true_false;
    }

    public <AnnotationTargetType extends AbstractAnnotation> List<String> getMissingBooleanAnnotations(List<AnnotationTargetType> annotations, AnnotationTarget target)
    {
        Set<String> annotNames = new HashSet<String>();
        for(AnnotationTargetType annot: annotations)
        {
            annotNames.add(annot.getName());
        }

        List<AnnotationDefinition> annotDefs = _targetAnnotationsMap.get(target);
        if(annotDefs == null)
            return Collections.emptyList();

        List<String> missingAnnotations = new ArrayList<String>();
        for(AnnotationDefinition def: annotDefs)
        {
            if(def.getType() == AnnotationType.true_false && !annotNames.contains(def.getName()))
            {
                missingAnnotations.add(def.getName());
            }
        }
        return missingAnnotations;
    }

    private class AnnotationDefinition
    {
        private String _name;
        private AnnotationType _type;
        private List<AnnotationTarget> _targetList;

        AnnotationDefinition(String name, List<AnnotationTarget> targets, AnnotationType type)
        {
            _name = name;
            _type = type;
            _targetList = targets;
        }

        public String getName()
        {
            return _name;
        }

        public AnnotationType getType()
        {
            return _type;
        }

        public List<AnnotationTarget> getTargets()
        {
            return _targetList;
        }
    }
}
