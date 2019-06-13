/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.targetedms.TargetedMSModule;

public class ProteinSearchWebPart extends JspView<ProteinService.ProteinSearchForm>
{
    public ProteinSearchWebPart(ProteinService.ProteinSearchForm form)
    {
        super("/org/labkey/targetedms/view/proteinSearch.jsp", form);
        setTitle("Protein Search");
    }

    public static class ProteinSearchFormViewProvider implements ProteinService.FormViewProvider<ProteinService.ProteinSearchForm>
    {
        @Override
        public WebPartView createView(ViewContext viewContext, ProteinService.ProteinSearchForm form)
        {
            if (viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
            {
                return new ProteinSearchWebPart(form); // enable only if targetedms module is active. 
            }

            return null;
        }
    }
}
