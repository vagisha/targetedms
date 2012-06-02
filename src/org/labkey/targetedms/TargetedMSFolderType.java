/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.Portal;

import java.util.Arrays;
import java.util.Collections;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSFolderType extends DefaultFolderType
{
    public TargetedMSFolderType(TargetedMSModule module)
    {
        super("Targeted MS",
                "Manage targeted MS assays generated in Skyline.",
            Arrays.asList(
                    Portal.getPortalPart("Data Pipeline").createWebPart(),
                    Portal.getPortalPart(TargetedMSModule.TARGETED_MS_PROTEIN_SEARCH).createWebPart(),
                    Portal.getPortalPart(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME).createWebPart()
            ),
            Collections.<Portal.WebPart>emptyList(),
            getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment")),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        // TODO:
        return new HelpTopic("targetedms");
    }
}
