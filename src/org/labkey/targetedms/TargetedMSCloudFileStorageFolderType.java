/*
 * Copyright (c) 2017 LabKey Corporation
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
