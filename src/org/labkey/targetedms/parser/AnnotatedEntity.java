package org.labkey.targetedms.parser;

import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public abstract class AnnotatedEntity<AnnotationType extends AbstractAnnotation> extends SkylineEntity
{
    private List<AnnotationType> _annotations = Collections.emptyList();

    public List<AnnotationType> getAnnotations()
    {
        return _annotations;
    }

    public void setAnnotations(List<AnnotationType> annotations)
    {
        _annotations = Collections.unmodifiableList(annotations);
    }
}
