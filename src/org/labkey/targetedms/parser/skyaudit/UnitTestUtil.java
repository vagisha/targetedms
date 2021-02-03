/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Filter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.SkylineFileUtils;
import org.labkey.targetedms.TargetedMSManager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UnitTestUtil
{
    private static String[] _resourcePath = {"server", "customModules", "targetedms", "resources"};

    public static File getSampleDataFile(String pFileName) throws IOException
    {
        String relativePath = "TargetedMS/" + pFileName;
        File sampleDataFile = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule("targetedms"), relativePath);
        if (sampleDataFile == null)
            throw new IOException("Sampledata not found: " + relativePath);
        return sampleDataFile;
    }

    public static File getResourcesFile(String pFileName) throws UnsupportedEncodingException
    {
        return getTestFile(pFileName, UnitTestUtil._resourcePath);
    }

    private static File getTestFile(String pFileName, String[] pPathList){

        //we get location of the current class file, trace it up tp the LabKey folder (which is
        //not good for production code but for testing util it should be fine) and append the
        //known path and file name to the Labkey root.
        URL r = UnitTestUtil.class.getResource("/");
        String decodedUrl = "";
        decodedUrl = URLDecoder.decode(r.getFile(), StandardCharsets.UTF_8);
        if(decodedUrl.startsWith("/"))
            decodedUrl = decodedUrl.replaceFirst("/", "");

        //java.nio.file.Path currentDir = new File(System.getProperty("user.dir")).toPath();
        java.nio.file.Path currentDir = new File(decodedUrl).toPath();
        while(!currentDir.getFileName().toString().toLowerCase().equals("build"))
            currentDir = currentDir.getParent();
        currentDir = currentDir.getParent();
        String[] pathComponents = new String[] {currentDir.toString(), String.join(File.separator, pPathList), pFileName};
        File result = new File(String.join(File.separator, pathComponents));
        if(result.exists() && result.isFile())
            return result;
        else
            return null;
    }

    //delete testing records from the database;
    public static void cleanupDatabase(GUID pDocumentGUID){

        TableInfo entryTbl = TargetedMSManager.getTableInfoSkylineAuditLogEntry();
        Filter entryFilter = new SimpleFilter(FieldKey.fromParts("documentGuid"), pDocumentGUID.toString());
        //retrieving entry record Ids based on the testing document GUID
        SQLFragment query = Table.getSelectSQL(entryTbl, entryTbl.getPkColumns(), entryFilter, null);
        BaseSelector.ResultSetHandler<List<Integer>> resultSetHandler = (rs, conn) -> {
            List<Integer> idsList = new ArrayList<>(10);
            while(rs.next()){
                idsList.add(rs.getInt(1));
            }
            return  idsList;
        };
        List<Integer> recordIds = new SqlExecutor(entryTbl.getSchema()).executeWithResults(query, resultSetHandler);

        //deleting entries messages
        SimpleFilter messageDeleteFilter = new SimpleFilter();
        messageDeleteFilter.addInClause(FieldKey.fromParts("entryId"), recordIds);

        Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogMessage(), messageDeleteFilter);

        //deleting the entries
        Table.delete(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), entryFilter);

        SimpleFilter runDeleteFilter = new SimpleFilter();
        TableInfo runsTable = TargetedMSManager.getTableInfoRuns();
        runDeleteFilter.addCondition(runsTable.getColumn("documentGUID"), pDocumentGUID, CompareType.EQUAL);
        Table.delete(runsTable, runDeleteFilter);
    }

    public static File extractLogFromZip(File pZip, Logger pLogger) throws IOException
    {
        File workingDir = new File(System.getProperty(SkylineAuditLogParser.TestCase.SYS_PROPERTY_CWD) + "/temp");
        if(!workingDir.exists())
            workingDir.mkdir();
        if (!workingDir.exists())
            throw new FileExistsException("Cannot get a working dir for testing: " + workingDir.getPath());
        File zipDir = new File(workingDir, SkylineFileUtils.getBaseName(pZip.getName()));

        if (zipDir.exists())
        {
            FileUtils.deleteDirectory(zipDir);
        }

        List<File> files = ZipUtil.unzipToDirectory(pZip, zipDir, pLogger);
        for(File file : files){
            String ext = FileUtil.getExtension(file.getName());
            if(SkylineAuditLogParser.TestCase.SKYLINE_LOG_EXTENSION.equals(ext))
                return file;
        }
        return null;
    }
}
