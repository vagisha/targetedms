/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;

import java.util.Arrays;
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
            null, // do not add any WebParts to the page before configuration is complete
            getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment"), getModule("MS2"), getModule("MS1")),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        // TODO:
        return new HelpTopic("targetedms");
    }

    @NotNull
    @Override
    public List<NavTree> getExtraSetupSteps(Container c)
    {
        return Collections.singletonList(new NavTree(TargetedMSController.CONFIGURE_TARGETED_MS_FOLDER, new ActionURL(TargetedMSController.SetupAction.class, c)));
    }
}
