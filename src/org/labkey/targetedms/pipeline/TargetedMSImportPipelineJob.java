/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.SkylineDocImporter;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

import java.sql.SQLException;

/**
 * Simple wrapper job around a {@link TargetedMSImportTask}.
 * User: vsharma
 * Date: 4/1/12
 */
public class TargetedMSImportPipelineJob extends PipelineJob
{
    private final ExpData _expData;
    private SkylineDocImporter.RunInfo _runInfo;
    private final TargetedMSRun.RepresentativeDataState _representative;

    public TargetedMSImportPipelineJob(ViewBackgroundInfo info, ExpData expData, SkylineDocImporter.RunInfo runInfo, PipeRoot root, TargetedMSRun.RepresentativeDataState representative) throws SQLException
    {
        super(TargetedMSPipelineProvider.name, info, root);
        _expData = expData;
        _runInfo = runInfo;
        _representative = representative;

        String baseLogFileName = FileUtil.makeFileNameWithTimestamp(
                FileUtil.getBaseName(_expData.getName(), 1).replace(" ", "_"));     // No space in temp name because Files.copy(from, toS3) throws exception; issue in S3Path.toUri()

        if ((_expData.hasFileScheme() && root.isCloudRoot()) || (!_expData.hasFileScheme() && !root.isCloudRoot()))
            throw new RuntimeException("Cannot process ExpData when its schema does not match root schema.");

        LocalDirectory localDirectory = LocalDirectory.create(root, TargetedMSModule.NAME, baseLogFileName,
                null != _expData.getFile() ? _expData.getFile().getParentFile().getPath() : FileUtil.getTempDirectory().getPath());
        setLocalDirectory(localDirectory);
        setLogFile(localDirectory.determineLogFile());
    }

    @Override
    public TaskPipeline getTaskPipeline()
    {
        return PipelineJobService.get().getTaskPipeline(new TaskId(TargetedMSImportPipelineJob.class));
    }

    public ActionURL getStatusHref()
    {
        if (_runInfo.getRunId() > 0)
        {
            return TargetedMSController.getShowRunURL(getContainer(), _runInfo.getRunId());
        }
        return null;
    }

    public String getDescription()
    {
        return "Skyline document import - " + _expData.getName();
    }

    public SkylineDocImporter.RunInfo getRunInfo()
    {
        return _runInfo;
    }

    public ExpData getExpData()
    {
        return _expData;
    }

    public TargetedMSRun.RepresentativeDataState getRepresentative()
    {
        return _representative;
    }
}
