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
