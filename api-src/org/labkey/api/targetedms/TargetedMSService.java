/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
package org.labkey.api.targetedms;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.services.ServiceRegistry;

import java.util.List;

/**
 * User: vsharma
 * Date: 8/26/2015
 * Time: 11:34 AM
 */
public interface TargetedMSService
{
    static TargetedMSService get()
    {
        return ServiceRegistry.get().getService(TargetedMSService.class);
    }

    static void setInstance(TargetedMSService impl)
    {
        ServiceRegistry.get().registerService(TargetedMSService.class, impl);
    }

    ITargetedMSRun getRun(int runId, Container container);
    ITargetedMSRun getRunByFileName(String fileName, Container container);
    List<ITargetedMSRun> getRuns(Container container);
    List<? extends SkylineAnnotation> getReplicateAnnotations(Container container);
}
