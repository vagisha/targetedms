/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

package org.labkey.targetedms;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;

import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSFolderType extends MultiPortalFolderType
{
    public static final String NAME = "Targeted MS";

    public TargetedMSFolderType(TargetedMSModule module)
    {
        super(NAME,
                "Manage targeted MS assays generated in Skyline.",
            Collections.<Portal.WebPart>emptyList(),
            // Issue 17966: Add the setup webpart by default. It will be removed once the user selects a folder type.
            Collections.<Portal.WebPart>singletonList(Portal.getPortalPart(TargetedMSModule.TARGETED_MS_SETUP).createWebPart()),
            getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment")),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return new HelpTopic("panorama");
    }

    @NotNull
    @Override
    public List<NavTree> getExtraSetupSteps(Container c)
    {
        // If the TargetedMS folder type has already been set (e.g. if this folder was created from a template folder),
        // we don't need any extra steps.
        for(Module module: c.getActiveModules())
        {
            if (module instanceof TargetedMSModule)
            {
                ModuleProperty moduleProperty = module.getModuleProperties().get(TargetedMSModule.TARGETED_MS_FOLDER_TYPE);
                if(TargetedMSModule.FolderType.valueOf(moduleProperty.getValueContainerSpecific(c)) == TargetedMSModule.FolderType.Undefined)
                {
                    return Collections.singletonList(new NavTree(TargetedMSController.CONFIGURE_TARGETED_MS_FOLDER, new ActionURL(TargetedMSController.SetupAction.class, c)));
                }
                break;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getLabel()
    {
        return "Panorama";
    }

     @Override
    public String getStartPageLabel(ViewContext ctx)
    {
        return "Panorama Dashboard";
    }
}
