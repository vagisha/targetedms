/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSDataHandler extends AbstractExperimentDataHandler
{
    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        String description = data.getFile().getName();
        SkylineDocImporter importer = new SkylineDocImporter(info.getUser(), context.getContainer(), description,
                                                             data, log, context, TargetedMSRun.RepresentativeDataState.NotRepresentative);
        try
        {
            SkylineDocImporter.RunInfo runInfo = importer.prepareRun();
            TargetedMSRun run = importer.importRun(runInfo);

            ExpRun expRun = data.getRun();
            if(expRun == null)
            {
                // At this point expRun should not be null
                throw new ExperimentException("ExpRun was null. An entry in the ExperimentRun table should already exist for this data.");
            }
            run.setExperimentRunLSID(expRun.getLSID());

            TargetedMSManager.updateRun(run, info.getUser());
        }
        catch (IOException | SQLException | DataFormatException | XMLStreamException | PipelineJobException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId(), data.getContainer());
        if (run != null)
        {
            return TargetedMSController.getShowRunURL(data.getContainer(), run.getRunId());
        }
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId(), container);
        if (run != null)
        {
            TargetedMSManager.markDeleted(Arrays.asList(run.getRunId()), container, user);
            TargetedMSManager.purgeDeletedRuns();
            TargetedMSManager.deleteiRTscales(container);
        }
    }

    @Override
    public void beforeDeleteData(List<ExpData> data) throws ExperimentException
    {
        for (ExpData expData : data)
        {
            deleteData(expData, expData.getContainer(), null);
        }
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Priority getPriority(ExpData data)
    {
        String url = data.getDataFileUrl();
        if (url == null)
            return null;
        String ext = FileUtil.getExtension(url);
        if (ext == null)
            return null;
        ext = ext.toLowerCase();
        // we handle only *.sky or .zip files
        return "sky".equals(ext) || "zip".equals(ext) ? Priority.HIGH : null;
    }
}
