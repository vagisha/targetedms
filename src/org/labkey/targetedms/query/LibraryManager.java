/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.api.collections.CsvSet;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.SkylineFileUtils;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.PeptideSettings;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 5/6/12
 * Time: 10:23 PM
 */
public class LibraryManager
{
    private LibraryManager() {}

    public static Map<String, Integer> getLibrarySourceTypes()
    {
        return new TableSelector(TargetedMSManager.getTableInfoLibrarySource(), new CsvSet("Type, Id")).getValueMap();
    }

    public static List<PeptideSettings.SpectrumLibrary> getLibraries(int runId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), runId);

        return new TableSelector(TargetedMSManager.getTableInfoSpectrumLibrary(),
                               filter,
                               null).getArrayList(PeptideSettings.SpectrumLibrary.class);
    }

    public static Map<PeptideSettings.SpectrumLibrary, String> getLibraryFilePaths(int runId, List<PeptideSettings.SpectrumLibrary> libraries)
    {
        if(libraries.size() == 0)
            return Collections.emptyMap();

        TargetedMSRun run = TargetedMSManager.getRun(runId);
        if(run == null)
        {
            throw new NotFoundException("No run found for Id "+runId);
        }
        if(run.getDataId() == null)
        {
            throw new NotFoundException("No dataId found for run Id "+runId);
        }

        ExpData expData = ExperimentService.get().getExpData(run.getDataId());
        if(expData == null)
        {
            throw new IllegalStateException("Experiment data not found for runId "+runId+" and dataId "+run.getDataId());
        }

        File file = expData.getFile();
        File skyFilesDir = new File(file.getParent(), SkylineFileUtils.getBaseName(file.getName()));

        Map<PeptideSettings.SpectrumLibrary, String> libraryPathsMap = new HashMap<>();
        for(PeptideSettings.SpectrumLibrary library: libraries)
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
            if(libFileName != null)
            {
                libraryPathsMap.put(library, skyFilesDir.getAbsolutePath() + File.separator + libFileName);
            }
        }
        return libraryPathsMap;
    }

    private static String getDocumentLibFileName(File skyFilesDir)
    {
        // Get the name of the .sky file in the directory
        String[] skyFile = skyFilesDir.list(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(SkylineFileUtils.EXT_SKY_W_DOT);
            }
        });

        if(skyFile != null && skyFile.length > 0)
        {
            // Documents libraries have the same name as the .sky file, with a .blib extension.
            return skyFile[0].substring(0, skyFile[0].lastIndexOf('.')) + SkylineFileUtils.EXT_BLIB_W_DOT;
        }

        return null;
    }

}
