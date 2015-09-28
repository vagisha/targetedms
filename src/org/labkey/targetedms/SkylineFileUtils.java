/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;

import java.io.File;
import java.io.IOException;

/**
 * User: vsharma
 * Date: 11/26/13
 * Time: 4:36 PM
 */
public class SkylineFileUtils
{
    public static final String EXT_ZIP = "zip";
    private static final String EXT_SKY_ZIP_W_DOT = ".sky.zip";
    public static final String EXT_SKY = "sky";
    public static final String EXT_SKY_W_DOT = ".sky";
    public static final String EXT_BLIB_W_DOT = ".blib";

    private static final Logger LOG = Logger.getLogger(SkylineFileUtils.class);

    public static String getBaseName(String fileName)
    {
        if(fileName == null)
            return "";
        if(fileName.toLowerCase().endsWith(EXT_SKY_ZIP_W_DOT))
            return FileUtil.getBaseName(fileName, 2);
        else
            return FileUtil.getBaseName(fileName, 1);
    }

    public static File getSkylineFile(int runId)
    {
        TargetedMSRun run = TargetedMSManager.getRun(runId);
        return getSkylineFile(run);
    }

    public static File getSkylineFile(TargetedMSRun run)
    {
        if(run == null)
        {
            return null;
        }
        PipeRoot root = PipelineService.get().getPipelineRootSetting(run.getContainer());
        if(root != null)
        {
            File skyDocfile = new File(root.getRootPath(), run.getFileName());
            if(NetworkDrive.exists(skyDocfile))
            {
                return skyDocfile;
            }
            else
            {
                LOG.warn("File does not exist: " + skyDocfile.getPath());
                return null;
            }
        }
        return null;
    }
}
