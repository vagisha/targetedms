/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * User: gktaylor
 * Date: 5/13/13
 * Time: 10:49 AM
 */
public class AnnotationUIDisplayColumn extends DataColumn
{

    private final FieldKey _noteFieldKey;
    private final FieldKey _annotationFieldKey;

    public AnnotationUIDisplayColumn(ColumnInfo colInfo)
    {
        super(colInfo);
        FieldKey parentFK = colInfo.getFieldKey().getParent();

        _noteFieldKey = new FieldKey(parentFK, "Note");
        _annotationFieldKey = new FieldKey(parentFK, "Annotations");

    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_noteFieldKey);
        keys.add(_annotationFieldKey);
    }

    @Override
    public Object getDisplayValue(RenderContext ctx)
    {
        return getValue(ctx);
    }

    @Override @NotNull
    public String getFormattedValue(RenderContext ctx)
    {
        String result = h(getValue(ctx));
        result = result.replaceAll("\\n", "<br />");
        return result;
    }

    @Override
    public Object getValue(RenderContext ctx)
    {
        String note = (String)ctx.get(_noteFieldKey);
        String annotation = (String)ctx.get(_annotationFieldKey);

        StringBuilder response = new StringBuilder();

        if (note != null && annotation != null)
            response.append("Note: ").append(note).append("\n");
        else if (note != null)
            response.append(note);
        if (annotation != null)
            response.append(annotation);

        return response.toString();
    }

    @Override
    public boolean isSortable()
    {
        return false;
    }

    @Override
    public boolean isFilterable()
    {
        return false;
    }
}


