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
package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.nio.file.Path;

/**
 * User: jeckels
 * Date: Apr 13, 2012
 */
public class SkylineBinaryDataHandler extends AbstractExperimentDataHandler
{
    public static final String EXTENSION = "skyd";

    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        TargetedMSRun run = TargetedMSManager.getRunBySkydDataId(data.getRowId());
        if (run != null)
        {
            TargetedMSManager.deleteRun(container, user, run);
        }
        data.delete(user);
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        // Update the file path in the new ExpData
        Path sourceFile = newData.getFilePath();
        if(sourceFile != null)
        {
            String sourceFileName = FileUtil.getFileName(sourceFile); // Name of the skyd file
            String sourceFileDir = FileUtil.getFileName(sourceFile.getParent().getFileName()); // Name of the exploded folder where the skyd file lives
            PipeRoot targetRoot = PipelineService.get().findPipelineRoot(targetContainer);
            if(targetRoot == null)
            {
                throw new ExperimentException("Could not find pipeline root for target container " + targetContainer.getPath());
            }
            Path destFile = targetRoot.getRootNioPath().resolve(sourceFileDir).resolve(sourceFileName);
            newData.setDataFileURI(destFile.toUri());
        }
        // Update the container on ExpData
        newData.setContainer(targetContainer);
        newData.save(user);

        TargetedMSRun run = TargetedMSManager.getRunByLsid(oldRunLSID, container);
        if(run == null)
        {
            // Query the run by the new LSID in case the run has already been updated in TargetedMSDataHandler.runMoved
            run = TargetedMSManager.getRunByLsid(newRunLSID, targetContainer);
            if(run == null)
            {
                throw new ExperimentException("Run with old LSID " + oldRunLSID + " was not found in the source container " + container.getPath()
                        + ". And run with new LSID " + newRunLSID + " was not found in the target container " + targetContainer.getPath());
            }
        }

        // Update the run
        run.setSkydDataId(newData.getRowId());
        TargetedMSManager.updateRun(run, user);

        ExpData oldData = ExperimentService.get().getExpData(oldDataRowID);
        if(oldData != null)
        {
            // Delete the old entry in exp.data -- it is no longer linked to the run.
            oldData.delete(user);
        }
    }

    @Override
    public Handler.Priority getPriority(ExpData data)
    {
        String url = data.getDataFileUrl();
        if (url == null)
            return null;
        String ext = FileUtil.getExtension(url);
        if (ext == null)
            return null;
        ext = ext.toLowerCase();
        // we handle only *.skyd files
        return EXTENSION.equals(ext) ? Handler.Priority.HIGH : null;
    }
}
