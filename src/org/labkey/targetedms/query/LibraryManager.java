/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.SkylineFileUtils;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.PeptideSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * User: vsharma
 * Date: 5/6/12
 * Time: 10:23 PM
 */
public class LibraryManager
{
    private LibraryManager() {}

    public static List<PeptideSettings.SpectrumLibrary> getLibraries(long runId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), runId);

        return new TableSelector(TargetedMSManager.getTableInfoSpectrumLibrary(),
                               filter,
                               null).getArrayList(PeptideSettings.SpectrumLibrary.class);
    }

    public static @Nullable Path getLibraryFilePath(long runId, @NotNull ISpectrumLibrary library)
    {
        return getLibraryPath(getSkylineFilesDir(runId), library);
    }

    public static LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> getLibraryFilePaths(long runId)
    {
        List<PeptideSettings.SpectrumLibrary> libraries = LibraryManager.getLibraries(runId);
        return getLibraryFilePaths(runId, libraries);
    }

    private static LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> getLibraryFilePaths(long runId, List<PeptideSettings.SpectrumLibrary> libraries)
    {
        if(libraries.size() == 0)
            return new LinkedHashMap<>(Collections.emptyMap());

        Path skyFilesDir = getSkylineFilesDir(runId);

        LinkedHashMap<PeptideSettings.SpectrumLibrary, Path> libraryPathsMap = new LinkedHashMap<>();
        for(PeptideSettings.SpectrumLibrary library: libraries)
        {
            Path libPath = getLibraryPath(skyFilesDir, library);
            if (libPath != null)
            {
                libraryPathsMap.put(library, libPath);
            }
        }
        return libraryPathsMap;
    }

    @NotNull
    private static Path getSkylineFilesDir(long runId)
    {
        TargetedMSRun run = TargetedMSManager.getRun(runId);
        if(run == null)
        {
            throw new NotFoundException("No run found for Id "+ runId);
        }
        if(run.getDataId() == null)
        {
            throw new NotFoundException("No dataId found for run Id "+ runId);
        }

        ExpData expData = ExperimentService.get().getExpData(run.getDataId());
        if(expData == null)
        {
            throw new IllegalStateException("Experiment data not found for runId "+ runId +" and dataId "+run.getDataId());
        }

        Path file = expData.getFilePath();
        if(null == file)
            throw new IllegalStateException("ExpData file not found.");

        Path skyFilesDir = file.getParent().resolve(SkylineFileUtils.getBaseName(FileUtil.getFileName(file)));
        return skyFilesDir;
    }

    private static Path getLibraryPath(Path skyFilesDir, ISpectrumLibrary library)
    {
        String libFileName = library.getFileNameHint();
        if(libFileName == null)
        {
            // This could be a "document" library. Skyline xml does not include a "file_name_hint"
            // attribute for these libraries.  They have the same name as the Skyline document.
            // We are now adding a FileNameHint for "document" libraries during import.
            // But we need to be able to display library spectra for Skyline documents imported before the fix.
            // If the FileNameHint is null we assume that this is a document library.
            // Documents libraries have the same name as the .sky file, with a .blib extension.
            libFileName = getDocumentLibFileName(skyFilesDir);
        }
        return libFileName != null ? skyFilesDir.resolve(libFileName) : null;
    }

    private static String getDocumentLibFileName(Path skyFilesDir)
    {
        // Get the name of the .sky file in the directory
        List<String> skyFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.list(skyFilesDir))
        {
            paths.forEach(path -> {
                String filename = FileUtil.getFileName(path).toLowerCase();
                if (filename.endsWith(SkylineFileUtils.EXT_SKY_W_DOT))
                    skyFiles.add(filename);
            });
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if(!skyFiles.isEmpty())
        {
            // Documents libraries have the same name as the .sky file, with a .blib extension.
            return skyFiles.get(0).substring(0, skyFiles.get(0).lastIndexOf('.')) + SkylineFileUtils.EXT_BLIB_W_DOT;
        }

        return null;
    }

}
