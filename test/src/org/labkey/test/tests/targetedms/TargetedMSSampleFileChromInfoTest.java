/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSSampleFileChromInfoTest extends TargetedMSTest
{
    @BeforeClass
    public static void setupProject()
    {
        TargetedMSTest init = (TargetedMSTest) getCurrentTest();
        init.setupFolder(FolderType.Experiment);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMSSampleFileChromInfo";
    }

    /**
     * Tests uploading a document that includes pressure traces as sample file-scoped chromatograms, and verifies
     * they show up as expected.
     * Data was taken from the public CPTAC Study9S_Site95 data at https://panoramaweb.org/project/Panorama%20Public/2018/Study_9S/begin.view?
     */
    @Test
    public void testUpload() throws Exception
    {
        goToProjectHome();
        importData("SampleFileChromInfo.sky.zip");
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText("SampleFileChromInfo.sky.zip"));
        clickAndWait(Locator.linkContainingText("2 replicates"));

        DataRegionTable table = new DataRegionTable("Replicate", getDriver());
        Assert.assertEquals("Wrong number of rows", 2, table.getDataRowCount());
        assertTextPresent("6ProtMix_QC_01", "6ProtMix_QC_02");

        clickAndWait(table.detailsLink(0));
        assertTextPresent("U02630409", // Serial number
                "Site95_STUDY9S_PHASEI_6ProtMix_QC_01", // Sample File Name
                "C:\\Users\\Josh\\Downloads\\95 files\\Site95_031109_Study9S_Cond.wiff|Site95_STUDY9S_PHASEI_6ProtMix_QC_01|0", // File path
                "ColumnOven_FC_BridgeFlow (channel 5)", // Pressure trace plots
                "ColumnPressure (channel 4)",
                "LoadingPump_Pressure (channel 2)",
                "MicroPump_MasterPressure (channel 1)");



        Connection cn = createDefaultConnection(false);
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "samplefilechrominfo");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("SampleFileId", "StartTime", "EndTime", "TextId", "NumPoints", "UncompressedSize", "ChromatogramFormat", "ChromatogramOffset", "ChromatogramLength", "SampleFileId/ReplicateId"));

        Assert.assertEquals("Wrong number of SampleFileChromInfo rows", 8, cmd.execute(cn, "/" + getProjectName()).getRowset().getSize());

        cmd.setFilters(Arrays.asList(new Filter("StartTime", 0.0), new Filter("EndTime", 79.966)));
        Assert.assertEquals("Wrong number of time-filtered SampleFileChromInfo rows", 8, cmd.execute(cn, "/" + getProjectName()).getRowset().getSize());

        cmd.setFilters(Arrays.asList(new Filter("TextId", "ColumnOven_FC_BridgeFlow (channel 5)")));
        Assert.assertEquals("Wrong number of TextId-filtered SampleFileChromInfo rows", 2, cmd.execute(cn, "/" + getProjectName()).getRowset().getSize());

        cmd.setFilters(Arrays.asList(new Filter("SampleFileId/SampleName", "Site95_STUDY9S_PHASEI_6ProtMix_QC_01")));
        Assert.assertEquals("Wrong number of SampleName-filtered SampleFileChromInfo rows", 4, cmd.execute(cn, "/" + getProjectName()).getRowset().getSize());
    }
}
