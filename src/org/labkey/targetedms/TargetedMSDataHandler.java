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

package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSDataHandler extends AbstractExperimentDataHandler
{
    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        // TODO
        ExpRun expRun = data.getRun();
    }

    @Override
    public ActionURL getContentURL(Container container, ExpData data)
    {
        // TODO
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        File file = data.getFile();
        if (file != null)
        {
            TargetedMSRun run = TargetedMSManager.getRunByFileName(file.getParent(), file.getName(), container);
            if (run != null)
            {
                TargetedMSManager.markDeleted(Arrays.asList(run.getRunId()), container);
                TargetedMSManager.purgeDeletedRuns();
            }
        }
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        // TODO
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
