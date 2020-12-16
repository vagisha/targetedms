package org.labkey.targetedms.datasource;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.UnexpectedException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

abstract class MsDataDirSource extends MsDataSource
{
    MsDataDirSource(String instrumentVendor, String extension)
    {
        super(instrumentVendor, Collections.singletonList(extension));
    }

    // Condition for validating directory-based raw data
    abstract boolean isExpectedDirContent(Path p);

    // Filter for validating a directory source by finding rows for expected directory contents in the exp.data table.
    // Used only if the source directory was uploaded without auto-zipping (e.g. when the directory was uploaded to
    // a network drive mapped to a LabKey folder)
    abstract SimpleFilter.FilterClause getDirContentsFilterClause();

    @Override
    public boolean isFileSource()
    {
        return false;
    }

    @Override
    public boolean isValidPath(@NotNull Path path)
    {
        if(Files.exists(path))
        {
            if(isZip(FileUtil.getFileName(path)))
            {
                return !Files.isDirectory(path) && isValidZip(path);
            }
            else
            {
                return Files.isDirectory(path) && hasExpectedDirContents(path);
            }
        }
        return false;
    }

    private boolean hasExpectedDirContents(@NotNull Path path)
    {
        try
        {
            return Files.list(path).anyMatch(this::isExpectedDirContent);
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e,"Error validating directory source for " + name() + ". Path: " + path);
        }
    }

    private boolean isValidZip(@NotNull Path path)
    {
        try
        {
            for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                    .getRootDirectories())
            {
                boolean dataInRoot = hasExpectedDirContents(root);
                if (dataInRoot)
                {
                    return true;
                }
                else
                {
                    String subdirName = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                    // Look for match in the subdirectory. The zip may look like this (Waters example):
                    // datasouce.raw.zip
                    // -- datasource.raw
                    //    -- _FUNC001.DAT
                    Path subDir = root.resolve(subdirName);
                    if(Files.exists(subDir) && hasExpectedDirContents(subDir))
                    {
                        return true;
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e, "Error validating zip source for " + name() + ". Path: " + path);
        }
        return false;
    }

    @Override
    boolean isValidData(@NotNull ExpData expData, ExperimentService expSvc)
    {
        String fileName = expData.getName();
        if(isZip(fileName))
        {
            return MsDataFileSource.isNotDirInExpData(expData, expSvc);
        }
        else
        {
            // This is a directory source. Check for rows in exp.data for the expected directory contents.
            TableInfo expDataTInfo = expSvc.getTinfoData();
            return new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"), getExpDataFilter(expData, getDirContentsFilterClause()), null).exists();
        }
    }

    private SimpleFilter getExpDataFilter(ExpData expData, SimpleFilter.FilterClause filterClause)
    {
        SimpleFilter filter = MsDataSource.getExpDataFilter(expData.getContainer(), expData.getDataFileUrl());
        filter.addClause(filterClause);
        return filter;
    }
}
