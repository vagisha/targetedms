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

package org.labkey.targetedms;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.labkey.targetedms.TargetedMSController.FolderSetupAction.RAW_FILES_TAB;

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
        this(module,
             NAME,
             "Manage targeted MS assays generated in Skyline.",
             getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment")));
    }

    public TargetedMSFolderType(TargetedMSModule module, String name, String description, Set<Module> activeModules)
    {
        super(name,
              description,
              Collections.emptyList(),
              // Issue 17966: Add the setup webpart by default. It will be removed once the user selects a folder type.
              Collections.singletonList(Portal.getPortalPart(TargetedMSModule.TARGETED_MS_SETUP).createWebPart()),
              activeModules,
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
                    List<NavTree> extraSteps = new ArrayList<>();
                    ActionURL setupUrl = new ActionURL(TargetedMSController.SetupAction.class, c);

                    if (c.isProject())
                    {
                        ActionURL fileRootsUrl = new ActionURL("admin", "fileRootsStandAlone", c)
                                .addParameter("folderSetup", true)
                                .addReturnURL(setupUrl);
                        extraSteps.add(new NavTree("Change File Root", fileRootsUrl));
                    }
                    extraSteps.add(new NavTree(TargetedMSController.CONFIGURE_TARGETED_MS_FOLDER, setupUrl));
                    return extraSteps;
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

    @Override
    public void addManageLinks(NavTree adminNavTree, Container container, User user)
    {
        super.addManageLinks(adminNavTree, container, user);

        if (container.hasPermission(user, AdminPermission.class))
        {
            if (Portal.getParts(container, RAW_FILES_TAB).size() == 0)
            {
                ActionURL url = new ActionURL(TargetedMSController.AddRawDataTabAction.class, container);
                NavTree addRawData = new NavTree("Add Raw Data Tab", "javascript:{}");
                addRawData.setScript(PageFlowUtil.postOnClickJavaScript(url));
                adminNavTree.addChild(addRawData);
            }
        }
    }
}
