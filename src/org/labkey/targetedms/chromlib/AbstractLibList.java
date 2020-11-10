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
package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.PeptideGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:22 AM
 */
public abstract class AbstractLibList<ChildType extends AbstractLibMolecule<?>> extends AbstractLibEntity
{
    private String _name;
    private String _description;
    private final List<ChildType> _children = new ArrayList<>();

    public AbstractLibList() {}

    public AbstractLibList(PeptideGroup group)
    {
        setName(group.getLabel());
        _description = group.getDescription();
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public void addChild(ChildType child)
    {
        _children.add(child);
    }

    List<ChildType> getChildren()
    {
        return Collections.unmodifiableList(_children);
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() + getChildren().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();
    }
}
