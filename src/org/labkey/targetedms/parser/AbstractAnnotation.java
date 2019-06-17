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

import org.labkey.api.targetedms.SkylineAnnotation;

/**
 * Convenient to be a concrete class so that we can use it to fetch rows from the database for any of the specific
 * annotation tables when all we care about is the name/value/id info.
 *
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class AbstractAnnotation extends SkylineEntity implements SkylineAnnotation
{
    private String _name;
    private String _value;

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getValue()
    {
        return _value;
    }

    public void setValue(String value)
    {
        _value = value;
    }

    public String getDisplayName()
    {
        return (_name != null ? _name : "") + " : " + (_value != null ? _value : "");
    }
}
