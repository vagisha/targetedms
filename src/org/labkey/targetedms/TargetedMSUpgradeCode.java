/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.ModuleContext;

/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    // called at 13.13->13.14
    @SuppressWarnings({"UnusedDeclaration"})
    public void setContainersToExperimentType(final ModuleContext moduleContext)
    {
        setContainersToExperimentType(ContainerManager.getRoot());
    }

    private void setContainersToExperimentType(Container container)
    {
        // Look for all TargetedMS containers. Need to check the folder type name, since the folder type itself hasn't been registered yet
        if (TargetedMSFolderType.NAME.equals(ContainerManager.getFolderTypeName(container)))
        {
            // If we don't already have a subtype specified, assume it's an Experiment folder
            String currentValue = TargetedMSModule.FOLDER_TYPE_PROPERTY.getValueContainerSpecific(container);
            if (currentValue == null || TargetedMSModule.FolderType.Undefined.toString().equalsIgnoreCase(currentValue))
            {
                TargetedMSModule.FOLDER_TYPE_PROPERTY.saveValue(null, container, TargetedMSModule.FolderType.Experiment.toString());
            }
        }

        // Recurse into the child containers
        for (Container child : container.getChildren())
        {
            setContainersToExperimentType(child);
        }
    }
}
