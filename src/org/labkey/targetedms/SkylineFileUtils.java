/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
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
    public static Path getSkylineFile(@Nullable String runLSID)
    {
        if (runLSID == null)
        {
            return null;
        }

        ExpRun expRun = ExperimentService.get().getExpRun(runLSID);
        if (expRun == null)
        {
            LOG.warn("Run " + runLSID + " does not exist.");
            return null;
        }

        List<? extends ExpData> inputDatas = expRun.getAllDataUsedByRun();
        if (inputDatas != null && !inputDatas.isEmpty())
        {
            // The first file will be the .zip file since we only use one file as input data.
            Path skyDocfile = inputDatas.get(0).getFilePath();
            if (Files.exists(skyDocfile))
            {
                return skyDocfile;
            }
            else
            {
                LOG.warn("Skyline file does not exist: " + (skyDocfile != null ? FileUtil.getFileName(skyDocfile) : null) + ", referenced from " + expRun.getContainer().getPath());
                return null;
            }
        }
        LOG.warn("No input data found for run " + expRun.getRowId());
        return null;
    }
}
