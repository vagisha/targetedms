/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    public static final String EXT_BLIB = "blib";
    public static final String EXT_SKY_LOG = "skyl";

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

    @Nullable
    public static Path getSkylineFile(@Nullable String runLSID, @NotNull Container container)
    {
        if (runLSID == null)
        {
            return null;
        }

        TargetedMSRun run = TargetedMSManager.getRunByLsid(runLSID, container);
        if (run == null)
        {
            LOG.warn("Run with experimentRunLSID " + runLSID + " does not exist in container " + container.getPath());
            return null;
        }

        ExpData data = ExperimentService.get().getExpData(run.getDataId());
        if(data != null)
        {
            Path skyDocfile = data.getFilePath();
            if (null != skyDocfile && Files.exists(skyDocfile))
            {
                return skyDocfile;
            }
            else
            {
                LOG.warn("Skyline file does not exist: " + (skyDocfile != null ? FileUtil.getFileName(skyDocfile) : null) + ", referenced from " + container.getPath());
                return null;
            }
        }
        LOG.warn("No input data found for targetedms run " + run.getId() + " in container " + container.getPath());
        return null;
    }
}
