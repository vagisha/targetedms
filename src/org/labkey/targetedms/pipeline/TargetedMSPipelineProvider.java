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

package org.labkey.targetedms.pipeline;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSPipelineProvider extends PipelineProvider
{
    static String name = "Targeted MS";
    private static String ACTION_LABEL = "Import Skyline Results";

    public TargetedMSPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = getActionId();
        addAction(actionId, TargetedMSController.SkylineDocUploadAction.class, ACTION_LABEL,
                directory, directory.listFiles(new UploadFileFilter()), true, false, includeAll);
    }

    public static class UploadFileFilter implements DirectoryStream.Filter<Path>
    {
        @Override
        public boolean accept(Path file)
        {
            String ext =  FileUtil.getExtension(FileUtil.getFileName(file));
            if (ext == null)
            {
                return false;
            }
            ext = ext.toLowerCase();
            return ext.equals("sky") ||
                   ext.equals("zip");
        }
    }

    @Override
    public boolean supportsCloud()
    {
        return true;
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        return Collections.singletonList(new PipelineActionConfig(getActionId(), PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
    }
    private String getActionId()
    {
        return createActionId(TargetedMSController.SkylineDocUploadAction.class, ACTION_LABEL);
    }
}