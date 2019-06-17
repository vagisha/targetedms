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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.CalibrationCurveWebpart;
import org.labkey.test.pages.targetedms.PKReportPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests uploading Skyline documents that contain calibration curve settings. Makes sure that the calculated results
 * match the values that are in the CSV files in /SampleData/TargetedMS/Quantification/CalibrationScenariosTest.
 * Those data were generated from the Skyline unit test "CalibrationScenariosTest".
 */
@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 25)
public class TargetedMSCalibrationCurveTest extends TargetedMSTest
{
    private static final String SAMPLEDATA_FOLDER = "Quantification/CalibrationScenariosTest/";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSCalibrationCurveTest init = (TargetedMSCalibrationCurveTest) getCurrentTest();

        init.setupFolder(FolderType.Experiment);
    }

    @Test
    public void testMergeDocumentsScenario()
    {
        FiguresOfMerit fom = new FiguresOfMerit("VIFDANAPVAVR");
        fom.setLoq("1.0");
        fom.setUloq("10.0");
        fom.setBiasLimit("20%");
        fom.setCvLimit("20%");
        fom.setLod("0.11");
        fom.setCalc("Blank plus 2 * SD");

        runScenario("MergedDocuments", "none", fom);
        testCalibrationCurveMoleculePrecursorsByReplicate();
    }

    @Test
    public void testCalibrationScenario()
    {
        FiguresOfMerit fom = new FiguresOfMerit("VIFDANAPVAVR");
        fom.setLoq("0.05");
        fom.setUloq("10.0");
        fom.setBiasLimit("30%");
        fom.setCvLimit("N/A");
        fom.setLod("0.10");
        fom.setCalc("Blank plus 3 * SD");

        runScenario("CalibrationTest", "none", fom);
        testCalibrationCurvePrecursorsByReplicate();
    }

    @Test
    public void testCalibrationExcludeScenario() {
        runScenario("CalibrationExcludedTest", "none", null);
    }

    @Test
    public void testP180Scenario()
    {
        FiguresOfMerit fom = new FiguresOfMerit("Gly");
        fom.setLoq("500.0");
        fom.setUloq("500.0");
        fom.setBiasLimit("30%");
        fom.setCvLimit("1%");
        fom.setLod("-5.84");
        fom.setCalc("Blank plus 2 * SD");

        runScenario("p180test_calibration_DukeApril2016", "1/x", fom);
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
        String expectedValueForStatsName ="Route Dose IV CO k': %AUC Extrap: MRT (0-inf): MRT (0-t): CL (0-inf): CL (0-t): Vdss (0-inf): Vdss (0-t): T1/2: Effective T1/2:";
        pkReportPage.verifyTableColumnValues("stats", "SB1", 1, expectedValueForStatsName);
        pkReportPage.verifyTableColumnValues("stats", "SB2", 1, expectedValueForStatsName);

        log("Verifying the Statistic - Value content");
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 2.618 4.021 0.457 0.397 0.002 0.002 0.000 0.000 0.265 0.317");
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1 -12.734 1.317 0.662 4.748 4.728 0.000 0.000 0.000 0.000 0.526 3.291");

        log("SB1: Checking input for C0 @ 1.2 and unchecking Terminal @ 4");
        pkReportPage.setSubgroupTimeCheckbox("SB1", true, 4, true);
        pkReportPage.setSubgroupTimeCheckbox("SB1", false, 5, false);
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 0.925 0.792 0.007 0.005 0.000 0.000 -0.750 0.641");

        log("SB2: Set non-IV C0 input and recalculate");
        pkReportPage.setNonIVC0("SB2", "2");
        pkReportPage.verifyTableColumnValues("standard", "SB2", 4, "2.000 -0.966 2.956 3.840 -0.554");

        log("SB2: Unchecking input for C0 @ 4 and unchecking Terminal @ 4");
        pkReportPage.setSubgroupTimeCheckbox("SB2", true, 3, false);
        pkReportPage.setSubgroupTimeCheckbox("SB2", false, 2, true);
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 5.510 4.434 0.000 0.000 0.000 0.000 7.474 3.819");

        // all of the form input changes from above should have been persisted for this container/molecule/subgroup
        // so verify those by reloading the page and checking again for calculated values
        refresh();
        pkReportPage = new PKReportPage(getDriver(), 10);
        // this verifies that the SB1 C0 and Terminal checkboxes are set according to last selected options
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 0.925 0.792 0.007 0.005 0.000 0.000 -0.750 0.641");
        // this verifies that the SB2 C0 and Terminal checkboxes are set according to last selected options
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 5.510 4.434 0.000 0.000 0.000 0.000 7.474 3.819");
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
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1     4.434  0.000  0.000");
        // now go back to the admin user, verify the Reader changes weren't persisted
        pushLocation();
        stopImpersonating();
        popLocation();
        pkReportPage = new PKReportPage(getDriver(), 10);
        pkReportPage.verifyTableColumnValues("stats", "SB1", 2, "IV 1 0.635 -0.924 -35.365 0.925 0.792 0.007 0.005 0.000 0.000 -0.750 0.641");
        pkReportPage.verifyTableColumnValues("stats", "SB2", 2, "IM 1  0.093 8.064 5.510 4.434 0.000 0.000 0.000 0.000 7.474 3.819");
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

    private void setSampleFileReplicate(int sampleFileId, int replicateId) throws IOException, CommandException
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

    private File getFomExport()
    {
        mouseOver(Locator.id("fom-table-standard"));
        return doAndWaitForDownload(() -> click(Locator.id("targetedms-fom-export")));
    }

    private String getFomTableHeaderValue(String tableId, int col)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(tableId) + "]/thead/tr/td").findElements(getDriver()).get(col).getText();
    }

    private String getFomTableBodyValue(String tableId, int row, int col)
    {
        return getTableCellText(Locator.id(tableId), row, col);
    }

    private class FiguresOfMerit
    {
        String loq;
        String uloq;
        String biasLimit;
        String cvLimit;
        String lod;
        String calc;
        String name;

        private FiguresOfMerit(){}

        public FiguresOfMerit(@NotNull String molName)
        {
            setName(molName);
        }

        public String getLoq()
        {
            return loq;
        }

        public void setLoq(String loq)
        {
            this.loq = loq;
        }

        public String getUloq()
        {
            return uloq;
        }

        public void setUloq(String uloq)
        {
            this.uloq = uloq;
        }

        public String getBiasLimit()
        {
            return biasLimit;
        }

        public void setBiasLimit(String biasLimit)
        {
            this.biasLimit = biasLimit;
        }

        public String getCvLimit()
        {
            return cvLimit;
        }

        public void setCvLimit(String cvLimit)
        {
            this.cvLimit = cvLimit;
        }

        public String getLod()
        {
            return lod;
        }

        public void setLod(String lod)
        {
            this.lod = lod;
        }

        public String getCalc()
        {
            return calc;
        }

        public void setCalc(String calc)
        {
            this.calc = calc;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    private void verifyFomTable(String tableId, List<String> groups, List<String> concentrations, List<String> biases, List<String> excluded)
    {
        List<String> distinctGroups = new ArrayList<>();
        if (groups != null)
        {
            for (String group : groups)
            {
                if (!group.equals(" ") && !distinctGroups.contains(group))
                    distinctGroups.add(group);
            }

            // No data
            if (distinctGroups.size() < 1)
            {
                assertTextPresent("No data of this type");
                return;
            }

            for (int grpIndex = 0; grpIndex < distinctGroups.size(); grpIndex++)
            {
                assertTrue(getFomTableHeaderValue(tableId, (grpIndex * 2) + 1)
                        .startsWith(distinctGroups.get(grpIndex)));
            }
        }
        else
        {
            distinctGroups.add("");
        }

        int row = (groups == null?-1:0), grpIndex, maxRow = 0;
        String grp = "";
        SummaryStatistics stats = new SummaryStatistics();
        List<Double> means = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        List<Double> stddevs = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        List<Double> cvs = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        for (int rawIndex = 0; rawIndex < concentrations.size(); rawIndex++)
        {
            if ( groups == null || grp.equals(groups.get(rawIndex)) )
            {
                row++;
            }
            else {
                stats = new SummaryStatistics();
                row = 0;
            }

            if (maxRow < row)
                maxRow = row;

            if ( groups != null)
            {
                grp = groups.get(rawIndex);
                grpIndex = distinctGroups.indexOf(grp);
            }
            else
            {
                grpIndex = 0;
            }

            if (excluded != null && excluded.get(rawIndex).equals("false"))
            {
                stats.addValue(Double.parseDouble(concentrations.get(rawIndex)));

                means.set(grpIndex, stats.getMean());
                stddevs.set(grpIndex, stats.getStandardDeviation());
                cvs.set(grpIndex, (100 * stats.getStandardDeviation()) / stats.getMean());

                assertTrue(concentrations.contains(getFomTableBodyValue(tableId, row, (distinctGroups.indexOf(grp) * 2) + 1)));
                if (biases != null)
                {
                    assertTrue(biases.contains(getFomTableBodyValue(tableId, row, (distinctGroups.indexOf(grp) * 2) + 2)));
                }
            }
        }

        boolean singleValue;
        for (int index = 0; index < means.size(); index++)
        {
            singleValue = Integer.parseInt(getFomTableBodyValue(tableId, maxRow + 2, index * 2 + 1)) == 1;
            if (means.get(index) == 0.0)
            {
                assertEquals("NA", getFomTableBodyValue(tableId, maxRow + 3, index * 2 + 1));
            }
            else
            {
                assertEquals(means.get(index), Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 3, index * 2 + 1)), 0.01);
            }

            if (singleValue || (stddevs.get(index) == 0.0 && cvs.get(index) == 0.0))
            {
                assertEquals("NA", getFomTableBodyValue(tableId, maxRow + 4, index * 2 + 1));
                assertEquals("NA", getFomTableBodyValue(tableId, maxRow + 5, index * 2 + 1));
            }
            else
            {
                assertEquals(stddevs.get(index), Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 4, index * 2 + 1)), 0.01);
                assertEquals(cvs.get(index), Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 5, index * 2 + 1)), 6);
            }
        }
    }

    private void verifyFomSummary(FiguresOfMerit fom)
    {
        assertElementContains(Locator.tagWithId("td", "loq-stat"), "Lower: " + fom.getLoq());
        assertElementContains(Locator.tagWithId("td", "uloq-stat"), "Upper: " + fom.getUloq());
        assertElementContains(Locator.tagWithId("td", "bias-limit"), "Bias Limit: " + fom.getBiasLimit());
        assertElementContains(Locator.tagWithId("td", "cv-limit"), "CV Limit: " + fom.getCvLimit());
        assertElementContains(Locator.tagWithId("td", "lod-value"), "Lower: " + fom.getLod());
        assertElementContains(Locator.tagWithId("td", "lod-calc"), "Calculation: " + fom.getCalc());
    }

    private void testFiguresOfMerit(String scenario, @Nullable FiguresOfMerit fom)
    {
        String molName;
        DataRegionTable calibrationCurvesTable;

        goToProjectHome();
        clickFolder(scenario);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
        clickAndWait(Locator.linkContainingText("calibration curve"));
        boolean peptide = countText("Peptide Calibration Curves") > 0;

        if (fom == null)
        {
            calibrationCurvesTable = new DataRegionTable((peptide ? "calibration_curves" : "calibration_curves_sm_mol"), this);
            calibrationCurvesTable.setFilter("ErrorMessage", "Is Blank");
            molName = calibrationCurvesTable.getDataAsText(0, "GeneralMoleculeId");
        }
        else
        {
            molName = fom.getName();
        }

        goToSchemaBrowser();
        DataRegionTable dataRegionTable = viewQueryData("targetedms", "FiguresOfMerit");
        if (peptide)
        {
            dataRegionTable.setFilter("PeptideName", "Equals", molName);
        }
        else
        {
            dataRegionTable.setFilter("MoleculeName", "Equals", molName);
        }


        dataRegionTable.setFilter("SampleType", "Equals", "standard");
        dataRegionTable.setSort("AnalyteConcentration", SortDirection.ASC);

        List<String> stdGroups = dataRegionTable.getColumnDataAsText("AnalyteConcentration");
        List<String> stdConcentrations = dataRegionTable.getColumnDataAsText("ReplicateConcentration");
        List<String> stdBiases = dataRegionTable.getColumnDataAsText("Bias");
        List<String> stdExcluded = dataRegionTable.getColumnDataAsText("ExcludeFromCalibration");

        dataRegionTable.setFilter("SampleType", "Equals", "qc");

        List<String> qcGroups = dataRegionTable.getColumnDataAsText("AnalyteConcentration");
        List<String> qcConcentrations = dataRegionTable.getColumnDataAsText("ReplicateConcentration");
        List<String> qcBiases = dataRegionTable.getColumnDataAsText("Bias");
        List<String> qcExcluded = dataRegionTable.getColumnDataAsText("ExcludeFromCalibration");

        dataRegionTable.setFilter("SampleType", "Equals", "blank");
        List<String> blankConcentrations = dataRegionTable.getColumnDataAsText("ReplicateConcentration");
        List<String> blankExcluded = dataRegionTable.getColumnDataAsText("ExcludeFromCalibration");

        goToProjectHome();
        clickFolder(scenario);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
        clickAndWait(Locator.linkContainingText("calibration curve"));

        calibrationCurvesTable = new DataRegionTable((peptide?"calibration_curves":"calibration_curves_sm_mol"), this);
        calibrationCurvesTable.setFilter("GeneralMoleculeId", "Equals", (fom!=null?fom.getName():molName));

        clickAndWait(Locator.linkContainingText("Fom"));

        waitForElement(Locators.pageSignal("targetedms-fom-loaded"));

        verifyFomTable("fom-table-standard", stdGroups, stdConcentrations, stdBiases, stdExcluded);
        verifyFomTable("fom-table-qc", qcGroups, qcConcentrations, qcBiases, qcExcluded);
        verifyFomTable("fom-table-blank", null, blankConcentrations, null, blankExcluded);

        if (fom != null)
        {
            verifyFomSummary(fom);
        }

        File file = getFomExport();
        assertTrue("Wrong file type for export pdf [" + file.getName() + "]", file.getName().endsWith(".xlsx"));
        assertTrue("Empty pdf downloaded [" + file.getName() + "]", file.length() > 0);
    }

    private void runScenario(String scenario, String expectedWeighting, @Nullable FiguresOfMerit fom)
    {
        setupSubfolder(getProjectName(), scenario, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + scenario + ".sky.zip");
        List<Map<String, Object>> allCalibrationCurves = readScenarioCsv(scenario, "CalibrationCurves");
        assertFalse("No calibration curves found for scenario '" + scenario + "', check sample data.", allCalibrationCurves.isEmpty());

        for (boolean smallMolecule : Arrays.asList(true, false))
        {
            clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
            clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
            List<Map<String, Object>> expected;
            if (smallMolecule)
            {
                expected = allCalibrationCurves.stream()
                        .filter(row -> "#N/A".equals(row.get("PeptideModifiedSequence")))
                        .collect(Collectors.toList());
                if (expected.isEmpty())
                {
                    continue;
                }
                waitAndClick(Locator.linkContainingText("small molecule"));
            }
            else
            {
                expected = allCalibrationCurves.stream()
                        .filter(row -> !"#N/A".equals(row.get("PeptideModifiedSequence")))
                        .collect(Collectors.toList());
                if (expected.isEmpty())
                {
                    continue;
                }
                waitAndClick(Locator.linkContainingText("precursor"));
            }
            waitAndClickAndWait(Locator.linkContainingText("calibration curve"));
            waitForText("Calibration Curves");
            DataRegionTable calibrationCurvesTable = new DataRegionTable("calibration_curves" + (smallMolecule ? "_sm_mol" : ""), this);
            int rowWithData = -1;
            int rowWithoutData = -1;
            double expectedSlope = -1;
            double expectedIntercept = -1;
            double expectedQuadratic = -1;
            double expectedRSquared = -1;
            boolean quadratic = false;
            for (Map<String, Object> expectedRow : expected)
            {
                if (!expectedRow.get("QuadraticCoefficient").toString().equals("#N/A"))
                    quadratic = true;

                String peptide = expectedRow.get("Peptide").toString();
                String msg = scenario + "_" + peptide;
                int rowIndex = calibrationCurvesTable.getRowIndex(smallMolecule ? "Molecule" : "Peptide", peptide);
                assertNotEquals(msg, -1, rowIndex);
                String actualErrorMessage = calibrationCurvesTable.getDataAsText(rowIndex, "Error Message");
                String expectedErrorMessage = (String) expectedRow.get("ErrorMessage");
                if (expectedErrorMessage != null && expectedErrorMessage.length() > 0)
                {
                    assertNotEquals("", actualErrorMessage);
                    rowWithoutData = rowIndex;
                }
                else
                {
                    double actualSlope = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "Slope"));
                    expectedSlope = Double.parseDouble(expectedRow.get("Slope").toString());
                    assertEquals(expectedSlope, actualSlope, getDelta(expectedSlope));
                    double actualIntercept = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "Intercept"));
                    expectedIntercept = Double.parseDouble(expectedRow.get("Intercept").toString());
                    assertEquals(expectedIntercept, actualIntercept, getDelta(expectedIntercept));
                    if (quadratic)
                    {
                        double actualQuadratic = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "QuadraticCoefficient"));
                        expectedQuadratic = Double.parseDouble(expectedRow.get("QuadraticCoefficient").toString());
                        assertEquals(expectedQuadratic, actualQuadratic, getDelta(expectedQuadratic));
                    }
                    double actualRSquared = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "RSquared"));
                    expectedRSquared = Double.parseDouble(expectedRow.get("RSquared").toString());
                    assertEquals(expectedRSquared, actualRSquared, 1E-4);
                    rowWithData = rowIndex;
                }
            }

            List<String> baseLegendText = Arrays.asList(
                "Standard",
                "QC",
                "Unknown",
                "Excluded",
                "Calibration Curve",
                "Regression Fit: " + (quadratic?"quadratic":"linear"),
                "Norm. Method: ratio_to_heavy",
                "Regression Weighting: " + expectedWeighting,
                "MS Level: All"
            );

            log("Verify calibration curve");
            assertTrue("No data found for calibration curves", rowWithData >= 0);
            clickAndWait(calibrationCurvesTable.detailsLink(rowWithData));
            CalibrationCurveWebpart calibrationCurveWebpart = new CalibrationCurveWebpart(getDriver());
            List<String> actualLegendText = calibrationCurveWebpart.getSvgLegendItems();
            DecimalFormat df = new DecimalFormat("#.#####");
            List<String> expectedLegendText = new ArrayList<>(baseLegendText);
            expectedLegendText.addAll(Arrays.asList(
                "Slope: " + df.format(expectedSlope),
                "Intercept: " + df.format(expectedIntercept)
            ));
            if (quadratic)
            {
                expectedLegendText.add("Quadratic Coefficient: " + df.format(expectedQuadratic));
            }
            expectedLegendText.add("rSquared: " + df.format(expectedRSquared));

            assertEquals("Wrong legend text", expectedLegendText, actualLegendText);

            log("Verify calibration curve export");
            File pdf = calibrationCurveWebpart.exportToPdf();
            assertTrue("Wrong file type for export pdf [" + pdf.getName() + "]", pdf.getName().endsWith(".pdf"));
            assertTrue("Empty pdf downloaded [" + pdf.getName() + "]", pdf.length() > 0);
            File png = calibrationCurveWebpart.exportToPng();
            assertTrue("Wrong file type for export png [" + png.getName() + "]", png.getName().endsWith(".png"));
            assertTrue("Empty png downloaded [" + png.getName() + "]", png.length() > 0);

            log("Select a point an re-verify calibration curve");
            calibrationCurveWebpart.selectAnyPoint();
            actualLegendText = calibrationCurveWebpart.getSvgLegendItems();
            assertTrue("Legend didn't update after point selection " + actualLegendText, actualLegendText.contains("Selected Point"));
            File pdfWithSelection = calibrationCurveWebpart.exportToPdf();
            assertTrue("Empty pdf export [" + pdfWithSelection.getName() + "]", pdfWithSelection.length() > 0);
            assertNotEquals("PDF with and without selection shouldn't match", pdf.length(), pdfWithSelection.length());
            File pngWithSelection = calibrationCurveWebpart.exportToPng();
            assertTrue("Empty png export [" + pngWithSelection.getName() + "]", pngWithSelection.length() > 0);
            assertNotEquals("PNG with and without selection shouldn't match", png.length(), pngWithSelection.length());

            if (rowWithoutData >= 0)
            {
                goBack();
                calibrationCurvesTable.clickRowDetails(rowWithoutData);

                calibrationCurveWebpart = new CalibrationCurveWebpart(getDriver());
                assertEquals("Calibration curve with no data shouldn't have any points", 0, calibrationCurveWebpart.getSvgPoints().size());
                actualLegendText = calibrationCurveWebpart.getSvgLegendItems();
                expectedLegendText = new ArrayList<>(baseLegendText);
                expectedLegendText.addAll(Arrays.asList(
                    "Slope: 0",
                    "Intercept: 0",
                    "rSquared: 0"
                ));
                assertEquals("Wrong legend text", expectedLegendText, actualLegendText);
            }
        }

        testFiguresOfMerit(scenario, fom);
    }

    private double getDelta(double expectedValue)
    {
        double delta = 1E-3;
        expectedValue = Math.abs(expectedValue);

        if (expectedValue == 0.0)
            return delta;
        while (expectedValue > 10)
        {
            expectedValue = expectedValue / 10.0;
            delta = delta * 10.0;
        }
        while (expectedValue < 1)
        {
            expectedValue = expectedValue * 10.0;
            delta = delta / 10.0;
        }

        return delta;
    }

    private List<Map<String, Object>> readScenarioCsv(String scenarioName, String reportName)
    {
        File file = TestFileUtils.getSampleData("TargetedMS/" + SAMPLEDATA_FOLDER + scenarioName + "_" + reportName + ".csv");
        try (TabLoader tabLoader = new TabLoader(file, true))
        {
            tabLoader.parseAsCSV();
            return tabLoader.load();
        }
    }
}
