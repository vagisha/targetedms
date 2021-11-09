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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.MS2;
import org.labkey.test.pages.targetedms.PKReportPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests uploading Skyline documents that contain calibration curve settings. Makes sure that the calculated results
 * match the values that are in the CSV files in /SampleData/TargetedMS/Quantification/CalibrationScenariosTest.
 * Those data were generated from the Skyline unit test "CalibrationScenariosTest".
 */
@Category({Daily.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 25)
public class TargetedMSCalibrationCurveTest extends AbstractQuantificationTest
{
    @Test
    public void testMergedDocumentsScenario() throws Exception
    {
        FiguresOfMerit fom = new FiguresOfMerit("VIFDANAPVAVR");
        fom.setLoq("1.0");
        fom.setUloq("10.0");
        fom.setBiasLimit("20.0%");
        fom.setCvLimit("20.0%");
        fom.setLod("0.11");
        fom.setCalc("Blank plus 2 * SD");

        runScenario("MergedDocuments", "none", fom);
        testCalibrationCurveMoleculePrecursorsByReplicate();
    }



    @Test
    public void testCalibrationScenario() throws Exception
    {
        FiguresOfMerit fom = new FiguresOfMerit("VIFDANAPVAVR");
        fom.setLoq("0.05");
        fom.setUloq("10.0");
        fom.setBiasLimit("30.0%");
        fom.setCvLimit("N/A");
        fom.setLod("0.10");
        fom.setCalc("Blank plus 3 * SD");

        runScenario("CalibrationTest", "none", fom);
        testCalibrationCurvePrecursorsByReplicate();
    }

    @Test
    public void testCalibrationExcludeScenario() throws Exception
    {
        runScenario("CalibrationExcludedTest", "none", null);
    }

    @Test
    public void testP180Scenario() throws Exception
    {
        FiguresOfMerit fom = new FiguresOfMerit("Gly");
        fom.setLoq("500.0");
        fom.setUloq("500.0");
        fom.setBiasLimit("30.0%");
        fom.setCvLimit("1.0%");
        fom.setLod("-5.84");
        fom.setCalc("Blank plus 2 * SD");

        runScenario("p180test_calibration_DukeApril2016", "1/x", fom);
    }

    @Test
    public void testDilutionFactorScenario() throws Exception
    {
        runScenario("DilutionFactorTest", "1/(x*x)", null);
    }

    @Test
    public void  testCalibrationPK()
    {
        String subFolderName = "MergedDocuments.sky";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + subFolderName + ".zip");
        clickAndWait(Locator.folderTab("Panorama Dashboard"));
        clickAndWait(Locator.linkWithText(subFolderName + ".zip"));
        clickAndWait(Locator.linkWithText("27 calibration curves"));
        clickAndWait(Locator.linkWithText("PK")); // first one in the Peptide Calibration Curves grid
        PKReportPage pkReportPage = new PKReportPage(getDriver(), 10);

        log("Verifying the Time content");
        pkReportPage.verifyTableColumnValues("input", "SB1", 1, "0 0.3 1 1.2 1.5");
        pkReportPage.verifyTableColumnValues("input", "SB2", 1, "0 3 4 5 7");

        log("Verifying the Concentration content");
        pkReportPage.verifyTableColumnValues("input", "SB1", 4, "2.169 2.715 3.266 0.801");
        pkReportPage.verifyTableColumnValues("input", "SB2", 4, "0.380 19.225 46.520 0.575");

        log("Verifying the Count content");
        pkReportPage.verifyTableColumnValues("input", "SB1", 5, "1 1 1 1");
        pkReportPage.verifyTableColumnValues("input", "SB2", 5, "1 2 1 1");

        log("Verifying the StdDev content");
        pkReportPage.verifyTableColumnValues("input", "SB1", 6, "");
        pkReportPage.verifyTableColumnValues("input", "SB2", 6, "24.781");

        log("Verifying the Statistic - Name content");
        String expectedValueForStatsName ="Route Dose IV CO k': %AUC Extrap: AUC (0-inf): AUC (0-t): MRT (0-inf): MRT (0-t): CL (0-inf): CL (0-t): Vdss (0-inf): Vdss (0-t): T1/2: Effective T1/2:";
        pkReportPage.verifyTableColumnValues("stats", "SB1", 1, expectedValueForStatsName);
        pkReportPage.verifyTableColumnValues("stats", "SB2", 1, expectedValueForStatsName);

        log("Verifying the Statistic - Value content");
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 2.618 4.021 7.614 7.308 0.457 0.397 2.189 2.281 60.012 54.357 0.265 0.317");
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1 -12.734 1.317 0.662 65.950 65.513 4.748 4.728 0.253 0.254 72.001 72.174 0.526 3.291");

        log("SB1: Checking input for C0 @ 1.2 and unchecking Terminal @ 4");
        pkReportPage.setSubgroupTimeCheckbox("SB1", true, 4, true);
        pkReportPage.setSubgroupTimeCheckbox("SB1", false, 5, false);
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 2.453 3.321 0.925 0.792 6.794 5.019 377.118 238.650 -0.750 0.641");

        log("SB2: Set non-IV C0 input and recalculate");
        pkReportPage.setNonIVC0("SB2", "2");
        pkReportPage.verifyTableColumnValues("standard", "SB2", 4, "2.000 -0.966 2.956 3.840 -0.554");

        log("SB2: Unchecking input for C0 @ 4 and checking Terminal @ 3");
        pkReportPage.setSubgroupTimeCheckbox("SB2", true, 3, false);
        pkReportPage.setSubgroupTimeCheckbox("SB2", false, 2, true);
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 76.877 70.678 5.510 4.434 0.217 0.236 71.674 62.730 7.474 3.819");

        // all of the form input changes from above should have been persisted for this container/molecule/subgroup
        // so verify those by reloading the page and checking again for calculated values
        refresh();
        pkReportPage = new PKReportPage(getDriver(), 10);
        // this verifies that the SB1 C0 and Terminal checkboxes are set according to last selected options
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 2.453 3.321 0.925 0.792 6.794 5.019 377.118 238.650 -0.750 0.641");
        // this verifies that the SB2 C0 and Terminal checkboxes are set according to last selected options
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 76.877 70.678 5.510 4.434 0.217 0.236 71.674 62.730 7.474 3.819");
        // this verifies that the SB2 non-IV C0 value is set according to last entered value
        pkReportPage.verifyTableColumnValues("standard", "SB2", 4, "2.000 -0.966 2.956 3.840 -0.554");

        // impersonate a reader, who should be able to change the settings/inputs but those don't get persisted
        pushLocation();
        impersonateRole("Reader");
        popLocation();
        pkReportPage = new PKReportPage(getDriver(), 10);
        // uncheck all the SB1 and SB2 time inputs
        pkReportPage.setAllSubgroupTimeCheckboxes("SB1", 5, false);
        pkReportPage.setAllSubgroupTimeCheckboxes("SB2", 5, false);
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1");
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1     70.678  4.434  0.236  62.730");
        // now go back to the admin user, verify the Reader changes weren't persisted
        pushLocation();
        stopImpersonating();
        popLocation();
        pkReportPage = new PKReportPage(getDriver(), 10);
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 2.453 3.321 0.925 0.792 6.794 5.019 377.118 238.650 -0.750 0.641");
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 76.877 70.678 5.510 4.434 0.217 0.236 71.674 62.730 7.474 3.819");
        pkReportPage.verifyTableColumnValues("standard", "SB2", 4, "2.000 -0.966 2.956 3.840 -0.554");
    }

    private void testCalibrationCurvePrecursorsByReplicate()
    {
        String folderHome = getProjectName() + "/CalibrationTest";
        goToProjectHome(folderHome);
        goToSchemaBrowser();
        DataRegionTable dataRegionTable = viewQueryData("targetedms", "CalibrationCurvePrecursorsByReplicate");

        List<String> expectedColumnNames = new ArrayList<>();
        expectedColumnNames.add("Sequence");
        expectedColumnNames.add("charge");
        expectedColumnNames.add("replicateId");
        expectedColumnNames.add("bestRtMean");
        expectedColumnNames.add("lightTotalAreaMean");
        expectedColumnNames.add("heavyTotalAreaMean");
        expectedColumnNames.add("ratioMean");
        expectedColumnNames.add("analyteConcentrationMean");
        expectedColumnNames.add("calculatedConcentrationMean");
        assertEquals("Wrong column names ",expectedColumnNames, dataRegionTable.getColumnNames());

        String replicate = "Cal 2_0_5 ng_mL_VIFonly";
        dataRegionTable.setFilter("replicateId", "Equals", replicate);
        assertEquals("Unexpected number of filtered rows", 1, dataRegionTable.getDataRowCount());
        assertEquals("Unexpected value for " + replicate, "VIFDANAPVAVR", dataRegionTable.getDataAsText(0, "Sequence"));
        assertEquals("Unexpected value for " + replicate, "2+", dataRegionTable.getDataAsText(0, "charge"));
        assertEquals("Unexpected value for " + replicate, "0.2128", dataRegionTable.getDataAsText(0, "bestRtMean"));
        assertEquals("Unexpected value for " + replicate, "2,288.0017", dataRegionTable.getDataAsText(0, "lightTotalAreaMean"));
        assertEquals("Unexpected value for " + replicate, "102,404.5703", dataRegionTable.getDataAsText(0, "heavyTotalAreaMean"));
        assertEquals("Unexpected value for " + replicate, "0.02", dataRegionTable.getDataAsText(0, "ratioMean"));
        assertEquals("Unexpected value for " + replicate, "0.05", dataRegionTable.getDataAsText(0, "analyteConcentrationMean"));
        assertEquals("Unexpected value for " + replicate, "0.064", dataRegionTable.getDataAsText(0, "calculatedConcentrationMean"));

        //TODO check that values have changed to mean of two replicates
        //Will require getting replicate id for the sample file represented by the row being validated.
        //This sample data has a one to one sample file to replicate relationship.
        //To test the actual mean values another sample file will need to have the same replicate id
        //Once this portion of the test is complete it would be best to set the sample file's replicate id back to
        //its original value.
        //This will also require addressing the user permission issue in setSampleFileReplicate()
        //setSampleFileReplicate(targetSampleFileId,replicateIdFromBaseSampleFile);
    }

    private void testCalibrationCurveMoleculePrecursorsByReplicate()
    {
        String folderHome = getProjectName() + "/MergedDocuments";
        goToProjectHome(folderHome);
        goToSchemaBrowser();
        DataRegionTable dataRegionTable = viewQueryData("targetedms", "CalibrationCurveMoleculePrecursorsByReplicate");

        List<String> expectedColumnNames = new ArrayList<>();
        expectedColumnNames.add("CustomIonName");
        expectedColumnNames.add("charge");
        expectedColumnNames.add("replicateId");
        expectedColumnNames.add("bestRtMean");
        expectedColumnNames.add("lightTotalAreaMean");
        expectedColumnNames.add("heavyTotalAreaMean");
        expectedColumnNames.add("ratioMean");
        expectedColumnNames.add("analyteConcentrationMean");
        expectedColumnNames.add("calculatedConcentrationMean");
        assertEquals("Wrong column names ",expectedColumnNames, dataRegionTable.getColumnNames());

        String replicate = "49_0_1_1_02_437301";
        String customIonName = "Gly";
        dataRegionTable.setFilter("replicateId", "Equals", replicate);
        dataRegionTable.setFilter("CustomIonName", "Equals", customIonName);
        assertEquals("Unexpected number of filtered rows", 1, dataRegionTable.getDataRowCount());
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "1+", dataRegionTable.getDataAsText(0, "charge"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "2.2351", dataRegionTable.getDataAsText(0, "bestRtMean"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "653,201.5625", dataRegionTable.getDataAsText(0, "lightTotalAreaMean"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "8,643,496.0000", dataRegionTable.getDataAsText(0, "heavyTotalAreaMean"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "0.08", dataRegionTable.getDataAsText(0, "ratioMean"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "25.0", dataRegionTable.getDataAsText(0, "analyteConcentrationMean"));
        assertEquals("Unexpected value for " + replicate + " - " + customIonName, "-13.90", dataRegionTable.getDataAsText(0, "calculatedConcentrationMean"));

        //TODO check that values have changed to mean of two replicates
        //Will require getting replicate id for the sample file represented by the row being validated.
        //This sample data has a one to one sample file to replicate relationship.
        //To test the actual mean values another sample file will need to have the same replicate id
        //Once this portion of the test is complete it would be best to set the sample file's replicate id back to
        //its original value.
        //This will also require addressing the user permission issue in setSampleFileReplicate()
        //setSampleFileReplicate(targetSampleFileId,replicateIdFromBaseSampleFile);
    }

    private void setSampleFileReplicate(long sampleFileId, long replicateId) throws IOException, CommandException
    {
        Connection connection = createDefaultConnection(true);
        List<Map<String, Object>> sampleFileRows = Arrays.asList(
                Maps.of("id", sampleFileId,
                        "replicateId", replicateId
                )
        );

        UpdateRowsCommand command = new UpdateRowsCommand("targetedms", "samplefile");
        command.setRows(sampleFileRows);
        command.execute(connection, getProjectName() + "/CalibrationTest");
    }
}
