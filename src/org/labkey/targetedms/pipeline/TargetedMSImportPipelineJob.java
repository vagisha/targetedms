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

package org.labkey.targetedms.pipeline;

import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.SkylineDocImporter;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;

import java.sql.SQLException;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSImportPipelineJob extends PipelineJob
{
    private final ExpData _expData;
    private SkylineDocImporter.RunInfo _runInfo;
    private final boolean _representative;

    public TargetedMSImportPipelineJob(ViewBackgroundInfo info, ExpData expData, SkylineDocImporter.RunInfo runInfo, PipeRoot root, boolean representative) throws SQLException
    {
        super(TargetedMSPipelineProvider.name, info, root);
        _expData = expData;
        _runInfo = runInfo;
        _representative = representative;

        String basename = FileUtil.getBaseName(_expData.getFile(), 1);
        setLogFile(FT_LOG.newFile(_expData.getFile().getParentFile(), basename));
    }

    public ActionURL getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return "Skyline document import";
    }

    public void run()
    {
        if (!setStatus("LOADING"))
        {
            return;
        }

        boolean completeStatus = false;
        try
        {
            XarContext context = new XarContext(getDescription(), getContainer(), getUser());
            SkylineDocImporter importer = new SkylineDocImporter(getUser(), getContainer(), context.getJobDescription(), _expData, getLogger(), context, _representative);
            TargetedMSRun run = importer.importRun(_runInfo);

            TargetedMSManager.ensureWrapped(run, getUser());
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception e)
        {
            getLogger().error("Skyline document import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }
    }
}
