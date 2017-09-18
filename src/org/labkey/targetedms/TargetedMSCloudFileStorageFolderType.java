package org.labkey.targetedms;

import org.labkey.api.cloud.CloudStoreService;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by davebradlee on 9/8/17.
 */
public class TargetedMSCloudFileStorageFolderType extends TargetedMSFolderType
{
    public static final String NAME = "Cloud File Storage";

    public TargetedMSCloudFileStorageFolderType(TargetedMSModule module)
    {
        super(module, NAME, "Manage targeted MS assays and raw MS files.",
              getDefaultModuleSet(module, getModule("TargetedMS"), getModule("Pipeline"), getModule("Experiment"), getModule("Cloud")));
    }

    @Override
    public String getLabel()
    {
        return "Panorama With Cloud File Storage";
    }

    @Override
    public void configureContainer(Container container, User user)
    {
        CloudStoreService cloudStoreService = CloudStoreService.get();
        Set<String> enabledCloudStores = new HashSet<>();
        cloudStoreService.getCloudStores().forEach(cloudStore -> {
            if (cloudStoreService.isEnabled(cloudStore))
                enabledCloudStores.add(cloudStore);
        });
        if (!enabledCloudStores.isEmpty())
            cloudStoreService.setEnabledCloudStores(container, enabledCloudStores);
    }
}
