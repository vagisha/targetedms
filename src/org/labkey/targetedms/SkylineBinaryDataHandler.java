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
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.parser.SkylineBinaryParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * User: jeckels
 * Date: Apr 13, 2012
 */
public class SkylineBinaryDataHandler extends AbstractExperimentDataHandler
{
    private static final String EXTENSION = ".skyd";

    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        SkylineBinaryParser parser = new SkylineBinaryParser(dataFile, log);
        try
        {
            parser.parse();
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId(), container);
        if (run != null)
        {
            TargetedMSManager.markDeleted(Arrays.asList(run.getRunId()), container, user);
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
        // we handle only *.skys files
        return EXTENSION.equals(ext) ? Handler.Priority.HIGH : null;
    }
}
