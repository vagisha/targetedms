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
package org.labkey.targetedms.search;

import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;

/**
 * User: cnathe
 * Date: 3/22/13
 */
public class ModificationSearchBean
{
    private final TargetedMSController.ModificationSearchForm _form;

    public ModificationSearchBean(TargetedMSController.ModificationSearchForm form)
    {
        _form = form;
    }

    public TargetedMSController.ModificationSearchForm getForm()
    {
        return _form;
    }
}
