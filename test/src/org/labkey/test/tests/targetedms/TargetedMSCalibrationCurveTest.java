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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.TabLoader;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.CalibrationCurveWebpart;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    {setIsBootstrapWhitelisted(true);}

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
        runScenario("MergedDocuments");
    }

    @Test
    public void testCalibrationScenario() throws Exception
    {
        runScenario("CalibrationTest");
        testCalibrationCurvePrecursorsByReplicate();
    }

    @Test
    public void testP180Scenario() throws Exception
    {
        runScenario("p180test_calibration_DukeApril2016");
    }

    private void testCalibrationCurvePrecursorsByReplicate() throws IOException, CommandException
    {

        String folderHome = getProjectName() + "/CalibrationTest";
        goToProjectHome(folderHome);
        goToSchemaBrowser();
        DataRegionTable dataRegionTable = viewQueryData("targetedms", "CalibrationCurvePrecursorsByReplicate");

        List<String> expectedColumnNames = new ArrayList<>();
        expectedColumnNames.add("Sequence");
        expectedColumnNames.add("charge");
        expectedColumnNames.add("bestRtMean");
        expectedColumnNames.add("lightTotalAreaMean");
        expectedColumnNames.add("heavyTotalAreaMean");
        expectedColumnNames.add("ratioMean");
        expectedColumnNames.add("analyteConcentrationMean");
        expectedColumnNames.add("calculatedConcentrationMean");
        expectedColumnNames.add("replicateId");

        assertEquals("Wrong column names ",expectedColumnNames, dataRegionTable.getColumnNames());

        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("VIFDANAPVAVR");
        expectedValues.add("2+");
        expectedValues.add("0.21276666224002838");
        expectedValues.add("2288.001708984375");
        expectedValues.add("102404.5703125");
        expectedValues.add("0.02234276942908764");
        expectedValues.add("0.05");
        expectedValues.add("0.0869748587539913");
        expectedValues.add("Cal 2_0_5 ng_mL_VIFonly");

        assertEquals("Wrong column values ",expectedValues, dataRegionTable.getRowDataAsText(4));

        //TODO check that values have changed to mean of two replicates
        //Will require getting replicate id for the sample file represented by the row being validated.
        //This sample data has a one to one sample file to replicate relationship.
        //To test the actual mean values another sample file will need to have the same replicate id
        //Once this portion of the test is complete it would be best to set the sample file's replicate id back to
        //its original value.
        //This will also require addressing the user permission issue in setSampleFileReplicate()
//        setSampleFileReplicate(targetSampleFileId,replicateIdFromBaseSampleFile);

        goToSchemaBrowser();
        selectQuery("targetedms", "CalibrationCurveMoleculePrecursorsByReplicate");
        waitAndClickAndWait(Locator.linkWithText("view data"));
        dataRegionTable = new DataRegionTable("query",this);

        expectedColumnNames = new ArrayList<>();
        expectedColumnNames.add("Sequence");
        expectedColumnNames.add("charge");
        expectedColumnNames.add("bestRtMean");
        expectedColumnNames.add("lightTotalAreaMean");
        expectedColumnNames.add("heavyTotalAreaMean");
        expectedColumnNames.add("ratioMean");
        expectedColumnNames.add("analyteConcentrationMean");
        expectedColumnNames.add("calculatedConcentrationMean");
        expectedColumnNames.add("replicateId");

        assertEquals("Wrong column names ",expectedColumnNames, dataRegionTable.getColumnNames());

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

    private void runScenario(String scenario) throws Exception
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
            double expectedRSquared = -1;
            for (Map<String, Object> expectedRow : expected)
            {
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
                    double actualRSquared = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "RSquared"));
                    expectedRSquared = Double.parseDouble(expectedRow.get("RSquared").toString());
                    assertEquals(expectedRSquared, actualRSquared, 1E-4);
                    rowWithData = rowIndex;
                }
            }

            log("Verify calibration curve");
            assertTrue("No data found for calibration curves", rowWithData >= 0);
            clickAndWait(calibrationCurvesTable.detailsLink(rowWithData));
            CalibrationCurveWebpart calibrationCurveWebpart = new CalibrationCurveWebpart(getDriver());
            List<String> actualLegendText = calibrationCurveWebpart.getSvgLegendItems();
            DecimalFormat df = new DecimalFormat("#.#####");
            List<String> expectedLegendText = new ArrayList<>(Arrays.asList(
                    "Standard",
                    "QC",
                    "Unknown",
                    "Calibration Curve",
                    "Slope: " + df.format(expectedSlope),
                    "Intercept: " + df.format(expectedIntercept),
                    "rSquared: " + df.format(expectedRSquared)));
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
                clickAndWait(calibrationCurvesTable.detailsLink(rowWithoutData));

                calibrationCurveWebpart = new CalibrationCurveWebpart(getDriver());
                assertEquals("Calibration curve with no data shouldn't have any points", 0, calibrationCurveWebpart.getSvgPoints().size());
                actualLegendText = calibrationCurveWebpart.getSvgLegendItems();
                expectedLegendText = Arrays.asList(
                        "Standard",
                        "QC",
                        "Unknown",
                        "Calibration Curve",
                        "Slope: 0",
                        "Intercept: 0",
                        "rSquared: 0");
                assertEquals("Wrong legend text", expectedLegendText, actualLegendText);
            }
        }
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
