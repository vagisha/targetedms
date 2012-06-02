/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.SampleFile;

import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:54 PM
 */
public class ReplicateManager
{
    private ReplicateManager() {}

    public static SampleFile getSampleFile(int sampleFileId)
    {
        return Table.selectObject(TargetedMSManager.getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE),
                sampleFileId, SampleFile.class);
    }

    public static Replicate getReplicate(int replicateId)
    {
        return Table.selectObject(TargetedMSManager.getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE),
                replicateId, Replicate.class);
    }

    public static SampleFile getSampleFileForPrecursorChromInfo(int precursorChromInfoId)
    {
        String sql = "SELECT sf.* FROM "+
                     TargetedMSManager.getTableInfoSampleFile()+" AS sf, "+
                     TargetedMSManager.getTableInfoPrecursorChromInfo()+" AS pci "+
                     "WHERE sf.Id=pci.SampleFileId "+
                     "AND pci.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(precursorChromInfoId);

        return new SqlSelector(TargetedMSManager.getSchema(), sf).getObject(SampleFile.class);
    }

    public static SampleFile getSampleFileForPeptideChromInfo(int peptideChromInfoId)
    {
        String sql = "SELECT sf.* FROM "+
                     TargetedMSManager.getTableInfoSampleFile()+" AS sf, "+
                     TargetedMSManager.getTableInfoPeptideChromInfo()+" AS pci "+
                     "WHERE sf.Id=pci.SampleFileId "+
                     "AND pci.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(peptideChromInfoId);

        return new SqlSelector(TargetedMSManager.getSchema(), sf).getObject(SampleFile.class);
    }

    public static List<SampleFile> getSampleFilesForRun(int runId)
    {
        String sql = "SELECT sf.* FROM "+
                     TargetedMSManager.getTableInfoSampleFile()+" AS sf, "+
                     TargetedMSManager.getTableInfoReplicate()+" AS rep "+
                     "WHERE rep.Id=sf.ReplicateId "+
                     "AND rep.RunId=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(runId);

        return new ArrayList<SampleFile>(new SqlSelector(TargetedMSManager.getSchema(), sf).getCollection(SampleFile.class));
    }
}
