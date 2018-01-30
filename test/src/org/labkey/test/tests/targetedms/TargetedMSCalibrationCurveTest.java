/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.TabLoader;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.CalibrationCurveWebpart;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests uploading Skyline documents that contain calibration curve settings. Makes sure that the calculated results
 * match the values that are in the CSV files in /SampleData/TargetedMS/Quantification/CalibrationScenariosTest.
 * Those data were generated from the Skyline unit test "CalibrationScenariosTest".
 */
@Category({DailyB.class, MS2.class})
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
    public void testMergeDocumentsScenario() throws Exception
    {
        runScenario("MergedDocuments", "none");
        testCalibrationCurveMoleculePrecursorsByReplicate();
    }

    @Test
    public void testCalibrationScenario() throws Exception
    {
        runScenario("CalibrationTest", "none");
        testCalibrationCurvePrecursorsByReplicate();
    }

    @Test
    public void testP180Scenario() throws Exception
    {
        runScenario("p180test_calibration_DukeApril2016", "1/x");
    }

    @Test
    public void  testCalibrationPK() throws Exception
    {
        String subFolderName = "MergedDocuments.sky";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + subFolderName + ".zip");
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(subFolderName + ".zip"));
        clickAndWait(Locator.linkContainingText("calibration curve"));
        clickAndWait(Locator.linkWithText("PK"));
        Locator.tagContainingText("td","7").waitForElement(getDriver(),1000);

        log("Verifying the Time content");
        String expectedValueForTime = "0 0.3 1 1.2 1.5 3 4 5 7";
        String dataOfTimeCol = columnDataAsString(Locator.tagWithId("table","pk-table-input").findElement(getDriver()),1);
        assertEquals("Missing value in the Time column",expectedValueForTime,dataOfTimeCol);

        log("Verifying the Concentration content");
        String expectedValueForConc ="2.169 2.715 3.266 0.801 0.380 19.225 46.520 0.575";
        String dataOfConcentration = columnDataAsString(Locator.tagWithId("table","pk-table-input").findElement(getDriver()),4);
        assertEquals("Missing value in the Conc column",expectedValueForConc,dataOfConcentration);

        log("Verifying the Statistic - Name content");
        String expectedValueForStatsName ="Dose IV CO k': %AUC Extrap: MRT (0-inf): MRT (0-t): CL (0-inf): CL (0-t): Vdss (0-inf): Vdss (0-t): T1/2: Effective T1/2:";
        String dataOfStatsName = columnDataAsString(Locator.tagWithId("table","pk-table-stats").findElement(getDriver()),1);
        assertEquals("Missing value in the Statistic - name column",expectedValueForStatsName,dataOfStatsName);

        log("Verifying the Statistic - Value content");
        String expectedValueForStatsValue ="1 0.678 1.317 0.112 0.835 0.828 0.000 0.000 0.000 0.000 0.526 0.579";
        String dataOfStatsValue = columnDataAsString(Locator.tagWithId("table","pk-table-stats").findElement(getDriver()),2);
        assertEquals("Missing value in the Statistic - value column",expectedValueForStatsValue,dataOfStatsValue);

        log("Checking the check box for Terminal and CO");
        checkCheckbox(Locator.xpath("//*[@id=\"pk-table-input\"]/tbody/tr[4]/td[2]/input"));
        checkCheckbox(Locator.xpath("//*[@id=\"pk-table-input\"]/tbody/tr[6]/td[3]/input"));
        String expectedValuesAfterAddTerminal = "1 0.635 0.093 8.312 5.660 4.560 0.000 0.000 0.000 0.000 7.474 3.923";
        String dataofStatsValue2 = columnDataAsString(Locator.tagWithId("table","pk-table-stats").findElement(getDriver()),2);
        assertEquals("Missing value in the Statistic - value column after adding additional Terminal",expectedValuesAfterAddTerminal,dataofStatsValue2);

    }

    private String columnDataAsString (WebElement table,int col)
    {
        String retVal="";

        int size = table.findElements(By.tagName("tr")).size();
        for (int i=1;i < size ; i++)
            retVal += Locator.xpath("//tbody/tr[" + i + "]/td[" + col + "]").findElement(table).getText() + " ";

        return retVal.trim();
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

    private void verifyFomTable(String tableId, List<String> groups, List<String> concentrations, List<String> biases)
    {
        List<String> distinctGroups = new ArrayList<>();
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

        int row = 0, grpIndex, maxRow = 0;
        String grp = "";
        SummaryStatistics stats = new SummaryStatistics();
        List<Double> means = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        List<Double> stddevs = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        List<Double> cvs = new ArrayList<>(Collections.nCopies(distinctGroups.size(), 0.0));
        for (int rawIndex = 0; rawIndex < concentrations.size(); rawIndex++)
        {
            if ( grp.equals(groups.get(rawIndex)) )
            {
                row++;
            }
            else {
                stats = new SummaryStatistics();
                row = 0;
            }

            if (maxRow < row)
                maxRow = row;

            grp = groups.get(rawIndex);
            grpIndex = distinctGroups.indexOf(grp);

            stats.addValue(Double.parseDouble(concentrations.get(rawIndex)));

            means.set(grpIndex, Math.round(stats.getMean() * 100)/100.00);
            stddevs.set(grpIndex, Math.round(stats.getStandardDeviation() * 100)/100.00);
            cvs.set(grpIndex, Math.round(((100 * stats.getStandardDeviation())/stats.getMean()) * 100)/100.00);


            assertEquals(concentrations.get(rawIndex), getFomTableBodyValue(tableId, row, (distinctGroups.indexOf(grp) * 2) + 1));
            assertEquals(biases.get(rawIndex), getFomTableBodyValue(tableId, row, (distinctGroups.indexOf(grp) * 2) + 2));
        }

        boolean singleValue;
        for (int index = 0; index < means.size(); index++)
        {
            singleValue = Integer.parseInt(getFomTableBodyValue(tableId, maxRow + 2, index * 2 + 1)) == 1;
            assertTrue(Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 3, index * 2 + 1)) == means.get(index));
            if (singleValue)
            {
                assertTrue(getFomTableBodyValue(tableId, maxRow + 4, index * 2 + 1).equals("NA"));
                assertTrue(getFomTableBodyValue(tableId, maxRow + 5, index * 2 + 1).equals("NA"));
            }
            else
            {
                assertTrue(Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 4, index * 2 + 1)) == stddevs.get(index));
                assertTrue(Math.abs(Double.parseDouble(getFomTableBodyValue(tableId, maxRow + 5, index * 2 + 1)) - cvs.get(index)) < .02);
            }
        }
    }



    private void testFiguresOfMerit(String scenario) throws Exception
    {
        goToProjectHome();
        clickFolder(scenario);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
        clickAndWait(Locator.linkContainingText("calibration curve"));
        boolean peptide = countText("Peptide Calibration Curves") > 0;

        DataRegionTable calibrationCurvesTable = new DataRegionTable((peptide?"calibration_curves":"calibration_curves_sm_mol"), this);
        String molName = calibrationCurvesTable.getDataAsText(0, "GeneralMoleculeId");


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

        dataRegionTable.setFilter("SampleType", "Equals", "qc");

        List<String> qcGroups = dataRegionTable.getColumnDataAsText("AnalyteConcentration");
        List<String> qcConcentrations = dataRegionTable.getColumnDataAsText("ReplicateConcentration");
        List<String> qcBiases = dataRegionTable.getColumnDataAsText("Bias");

        goToProjectHome();
        clickFolder(scenario);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
        clickAndWait(Locator.linkContainingText("calibration curve"));

        clickAndWait(Locator.linkContainingText("Fom"));

        waitForText("LoadingDone");
        sleep(3000);

        verifyFomTable("fom-table-standard", stdGroups, stdConcentrations, stdBiases);
        verifyFomTable("fom-table-qc", qcGroups, qcConcentrations, qcBiases);

        File file = getFomExport();
        assertTrue("Wrong file type for export pdf [" + file.getName() + "]", file.getName().endsWith(".xlsx"));
        assertTrue("Empty pdf downloaded [" + file.getName() + "]", file.length() > 0);
    }

    private void runScenario(String scenario, String expectedWeighting) throws Exception
    {
        setupSubfolder(getProjectName(), scenario, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + scenario + ".sky.zip");
        List<Map<String, Object>> allCalibrationCurves = readScenarioCsv(scenario, "CalibrationCurves");

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
            clickAndWait(Locator.linkContainingText("calibration curve"));
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

        testFiguresOfMerit(scenario);
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

    private List<Map<String, Object>> readScenarioCsv(String scenarioName, String reportName) throws Exception
    {
        File file = TestFileUtils.getSampleData("TargetedMS/" + SAMPLEDATA_FOLDER + scenarioName + "_" + reportName + ".csv");
        try (TabLoader tabLoader = new TabLoader(file, true))
        {
            tabLoader.parseAsCSV();
            tabLoader.setInferTypes(false);
            return tabLoader.load();
        }
    }
}
