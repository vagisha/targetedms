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
    public enum AnnotationType { text, true_false, value_list }

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

    public void addAnnotation(String name, String target, String type)
    {
        AnnotationDefinition annot = new AnnotationDefinition(
                                        name,
                                        AnnotationTarget.valueOf(target),
                                        AnnotationType.valueOf(type));
        _annotationDefinitions.put(name, annot);
        List<AnnotationDefinition> targetAnnotations = _targetAnnotationsMap.get(annot.getTarget());
        if(targetAnnotations == null)
        {
            targetAnnotations = new ArrayList<AnnotationDefinition>();
            _targetAnnotationsMap.put(annot.getTarget(), targetAnnotations);
        }
        targetAnnotations.add(annot);
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
        private AnnotationTarget _target;

        AnnotationDefinition(String name, AnnotationTarget target, AnnotationType type)
        {
            _name = name;
            _type = type;
            _target = target;
        }

        public String getName()
        {
            return _name;
        }

        public AnnotationType getType()
        {
            return _type;
        }

        public AnnotationTarget getTarget()
        {
            return _target;
        }
    }
}
