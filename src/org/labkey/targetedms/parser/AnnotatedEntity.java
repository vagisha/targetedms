/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
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

    /** Utility for small molecules to separate adducts from the rest of the formula */
    @Nullable
    protected String extractAdduct(String ionFormula)
    {
        if (StringUtils.stripToNull(ionFormula) == null)
        {
            return null;
        }
        if (!ionFormula.contains("[") || !ionFormula.endsWith("]"))
        {
            // Skyline shouldn't be writing out formulas without adducts but a to-be-fixed bug in the compact,
            // BASE64-encoded format means that some documents
            return null;
        }
        // Pull out the text between the brackets at the end
        String adduct = ionFormula.substring(ionFormula.lastIndexOf("[") + 1);
        return adduct.substring(0, adduct.length() - 1);
    }

    /** Utilities for small molecules to drop the adducts from the rest of the formula */
    @Nullable
    protected String stripAdduct(String ionFormula)
    {
        if (StringUtils.stripToNull(ionFormula) == null)
        {
            return null;
        }
        if (!ionFormula.contains("[") || !ionFormula.endsWith("]"))
        {
            // Skyline shouldn't be writing out formulas without adducts but a to-be-fixed bug in the compact,
            // BASE64-encoded format means that some documents
            return ionFormula;
        }
        // Pull out the text before the last bracket
        return ionFormula.substring(0, ionFormula.lastIndexOf("["));
    }
}
