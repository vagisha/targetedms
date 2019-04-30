/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.parser.skyaudit.AuditLogException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSDataHandler extends AbstractExperimentDataHandler
{
    private static final Logger _log = Logger.getLogger(TargetedMSDataHandler.class);

    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        importFile(data, dataFile != null ? dataFile.toPath() : null, info, log, context);
    }
    public void importFile(ExpData data, Path dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        String description = data.getFile().getName();
        SkylineDocImporter importer = new SkylineDocImporter(info.getUser(), context.getContainer(), description,
                                                             data, log, context, TargetedMSRun.RepresentativeDataState.NotRepresentative, null,
                                                             PipelineService.get().findPipelineRoot(context.getContainer()));
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

            // We are importing runs from a xar archive.  The runs may be associated with a user-entered description.
            // Copy the run description to the newly imported run.
            String runDesc = expRun.getName();
            if(!StringUtils.isBlank(runDesc) && !runDesc.equals(run.getDescription()))
            {
                run.setDescription(runDesc);
            }

            TargetedMSManager.updateRun(run, info.getUser());
        }
        catch (IOException | XMLStreamException | PipelineJobException | AuditLogException e)
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
        TargetedMSRun run = TargetedMSManager.getRunByDataId(data.getRowId());
        if (run != null)
        {
            TargetedMSManager.deleteRun(container, user, run);
        }
        data.delete(user);
    }

    @Override
    public void beforeDeleteData(List<ExpData> data)
    {
        for (ExpData expData : data)
        {
            Container container = expData.getContainer();
            TargetedMSRun run = TargetedMSManager.getRunByDataId(expData.getRowId(), container);
            // r26691 (jeckels@labkey.com  5/30/2013 1:28:42 PM) -- Fix container delete when a TargetedMS run failed to import correctly.
            //        -- FK violation happens if a container with failed targeted ms run imports is deleted.
            //
            // Issue #21935 Add support to move targeted MS runs between folders
            // beforeDeleteData() is called prior to actual deletion of data. It is also called when moving a run
            // to another container: ExperimentServiceImpl.deleteExperimentRunForMove() -> deleteRun() -> deleteProtocolApplications()
            // Do not delete the run if it was successfully imported.  If the run was successfully imported
            // it will be deleted via the deleteData() method, in the case of actual run deletion.
            if (run != null && run.getStatusId() != SkylineDocImporter.STATUS_SUCCESS)
            {
                TargetedMSManager.deleteRun(container, null, run);
            }
        }
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        TargetedMSModule.FolderType sourceFolderType = TargetedMSManager.getFolderType(container);
        TargetedMSModule.FolderType targetFolderType = TargetedMSManager.getFolderType(targetContainer);

        if(sourceFolderType != TargetedMSModule.FolderType.Experiment || targetFolderType != TargetedMSModule.FolderType.Experiment)
        {
            StringBuilder error = new StringBuilder();
            if(sourceFolderType != TargetedMSModule.FolderType.Experiment)
            {
                error.append("Source folder \"").append(container.getPath()).append("\" is")
                .append((sourceFolderType == null) ? " not a Panorama type folder. " : " a \"" + sourceFolderType.name() + "\" folder. ");
            }
            if(targetFolderType != TargetedMSModule.FolderType.Experiment)
            {
                error.append("Target folder \"").append(targetContainer.getPath()).append("\" is")
                .append((targetFolderType == null) ? " not a Panorama type folder. " : " a \"" + targetFolderType.name() + "\" folder. ");
            }
            error.append("Runs can only be moved between Panorama \"Experimental Data\" folders. For other folder types please delete the run in the source folder and import it in the target folder.");
            throw new ExperimentException(error.toString());
        }

        Path sourceFile = newData.getFilePath();
        if(null == sourceFile)
            throw new ExperimentException("Source file is null.");

        // Fail if a Skyline document with the same source file name exists in the new location.
        String sourceFileName = FileUtil.getFileName(sourceFile);
        if(skylineDocExistsInTarget(sourceFileName, targetContainer))
        {
            throw new ExperimentException("A run with filename " + sourceFileName + " already exists in the target container " + targetContainer.getPath());
        }

        TargetedMSRun run = TargetedMSManager.getRunByLsid(oldRunLSID, container);
        if(run == null)
        {
            throw new ExperimentException("Run with lsid " + oldRunLSID + " was not found in container " + container.getPath());
        }

        if(!FileUtil.hasCloudScheme(sourceFile))                              // TODO: do we need to createDirectories for cloud? Probably not
            NetworkDrive.ensureDrive(sourceFile.toFile().getPath());

        PipeRoot targetRoot = PipelineService.get().findPipelineRoot(targetContainer);
        if(targetRoot == null)
        {
            throw new ExperimentException("Could not find pipeline root for target container " + targetContainer.getPath());
        }

        String srcFileExt = FileUtil.getExtension(sourceFileName);

        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(srcFileExt))
        {
            // Copy the source Skyline zip file to the new location.
            Path destFile = targetRoot.getRootNioPath().resolve(sourceFileName);
            // It is only meaningful to copy the file it is a shared zip file that may contain spectrum and/or irt libraries.
            // When rendering the MS/MS spectrum we read scan peaks directly from the .blib (spectrum library) files.
            // The contents of these files are not stored in the database.
            // TODO: Should we only allow import of shared zip files?  If a user uploads and imports a .sky file they have to upload
            //       the .skyd file and any .blib files as well to the same folder. This use case must be quite rare.

            // When using a pipeline root, it's possible that both the source and destination containers are pointing
            // at the same file system
            if (!sourceFile.equals(destFile))
            {
                try
                {
                    Files.copy(sourceFile, destFile);
                }
                catch (IOException e)
                {
                    throw new ExperimentException("Could not copy " + sourceFile + " to destination directory " + targetRoot.getRootNioPath(), e);
                }
            }

            if (!FileUtil.hasCloudScheme(destFile))
            {
                // Expand the zip file in the new location
                File zipDir = new File(destFile.toFile().getParent(), SkylineFileUtils.getBaseName(FileUtil.getFileName(destFile)));
                try
                {
                    ZipUtil.unzipToDirectory(destFile.toFile(), zipDir, null);
                }
                catch (IOException e)
                {
                    throw new ExperimentException("Unable to unzip file " + FileUtil.getFileName(destFile), e);
                }
            }
            else
            {
                // Copy any blibs from source
                try
                {
                    Path sourceDir = sourceFile.getParent().resolve(SkylineFileUtils.getBaseName(sourceFileName));
                    Path destDir = destFile.getParent().resolve(SkylineFileUtils.getBaseName(sourceFileName));
                    if (Files.exists(sourceDir) && Files.isDirectory(sourceDir))
                    {
                        if (!Files.exists(destDir))
                            Files.createDirectory(destDir);
                        if (Files.isDirectory(destDir))
                        {
                            try (Stream<Path> paths = Files.list(sourceDir))
                            {
                                for (Path path : paths.collect(Collectors.toSet()))
                                {
                                    String filename = FileUtil.getFileName(path);
                                    if (SkylineFileUtils.EXT_BLIB.equalsIgnoreCase(FileUtil.getExtension(filename)))
                                        Files.copy(path, destDir.resolve(filename));
                                }
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new ExperimentException("Unable copy blibs to cloud " + FileUtil.getFileName(destFile), e);
                }
            }
            // Update the file path in ExpData
            newData.setDataFileURI(destFile.toUri());
            newData.setContainer(targetContainer);
            newData.save(user);
        }

        // Update the run
        TargetedMSManager.moveRun(run, targetContainer, newRunLSID, newData.getRowId(), user);

        // Delete the old entry in exp.data -- it is no longer linked to the run.
        ExpData oldData = ExperimentService.get().getExpData(oldDataRowID);
        if(oldData != null)
        {
            oldData.delete(user);
        }

        if(SkylineFileUtils.EXT_ZIP.equalsIgnoreCase(srcFileExt))
        {
            try
            {
                // Delete the Skyline file in the old location
                Files.delete(sourceFile);

                // Delete the unzipped directory in the old location, if it exists
                Path oldZipDir = sourceFile.getParent().resolve(SkylineFileUtils.getBaseName(sourceFileName));
                FileUtil.deleteDir(oldZipDir);
            }
            catch (IOException e)
            {
                throw new ExperimentException(e);
            }
        }

    }

    private boolean skylineDocExistsInTarget(String fileName, Container targetContainer)
    {
        return TargetedMSManager.getRunByFileName(fileName, targetContainer) != null;
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
        if ("sky".equals(ext))
            return Priority.HIGH;

        if ("zip".equals(ext))
        {
            String firstExt = FileUtil.getExtension(FileUtil.getBaseName(url));     // See if it's sky.zip
            if (null != firstExt && "sky".equalsIgnoreCase(firstExt))
                return Priority.HIGH;

            if (TargetedMSManager.getRunByDataId(data.getRowId(), data.getContainer()) != null)
                return Priority.HIGH;

            if (zipContainsSkyFile(data.getFilePath()))
                return Priority.HIGH;
        }

        return null;
    }

    /** @return true if the zip file contains a .sky file, which means that we can import it (after extracting) */
    private boolean zipContainsSkyFile(Path f)
    {
        if (f == null || !Files.exists(f))
            return false;

        if (FileUtil.hasCloudScheme(f))
            return true;        // If it's in cloud, don't bother to crack open; say it's ours

        try (ZipFile zipFile = new ZipFile(f.toFile()))
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry zEntry = entries.nextElement();
                if ("sky".equalsIgnoreCase(FileUtil.getExtension(zEntry.getName())))
                {
                    return true;
                }
            }
        }
        catch (IOException | IllegalArgumentException e)
        {
            //ignore - see issue 29485 for IllegalArgumentException case
            _log.warn("Failed to open zip file " + f + " to check if it contains .sky files" + e.getMessage());
        }

        return false;
    }
}
