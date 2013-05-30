/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.PeptideSettings;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        ResultSet rs = null;
        Map<String, Integer> result = new HashMap<String, Integer>();
        try {
            rs = Table.select(TargetedMSManager.getTableInfoLibrarySource(), Table.ALL_COLUMNS, null, null);
            while(rs.next())
            {
                result.put(rs.getString("Type"), rs.getInt("Id"));
            }
            return result;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
        }
    }

    public static List<PeptideSettings.SpectrumLibrary> getLibraries(int runId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("RunId", runId);

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
        String path = file.getParent();
        String ext = FileUtil.getExtension(file.getName());
        if("zip".equalsIgnoreCase(ext))
        {
            path = FileUtil.getBaseName(file.getPath());
        }

        Map<PeptideSettings.SpectrumLibrary, String> libraryPathsMap = new HashMap<PeptideSettings.SpectrumLibrary, String>();
        for(PeptideSettings.SpectrumLibrary library: libraries)
        {
            libraryPathsMap.put(library, path + File.separator + library.getFileNameHint());
        }

        return libraryPathsMap;
    }
}
