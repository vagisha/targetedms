/*
 * Copyright (c) 2017-2021 LabKey Corporation
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
import org.labkey.remoteapi.query.ExecuteSqlCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.components.targetedms.CalibrationCurveWebpart;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractQuantificationTest extends TargetedMSTest
{
    protected static final String SAMPLEDATA_FOLDER = "Quantification/CalibrationScenariosTest/";

    @BeforeClass
    public static void setupProject()
    {
        AbstractQuantificationTest init = (AbstractQuantificationTest) getCurrentTest();

        init.setupFolder(FolderType.Experiment);
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
        assertElementContains(Locator.tagWithId("td", "lloq-stat"), fom.getLoq());
        assertElementContains(Locator.tagWithId("td", "uloq-stat"), fom.getUloq());
        assertElementContains(Locator.tagWithId("td", "bias-limit"), fom.getBiasLimit());
        assertElementContains(Locator.tagWithId("td", "cv-limit"), fom.getCvLimit());
        assertElementContains(Locator.tagWithId("td", "llod-value"), fom.getLod());
        assertElementContains(Locator.tagWithId("td", "lod-calc"), fom.getCalc());
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

        // Go to the calibration curve detail page
        clickAndWait(Locator.linkContainingText(fom != null ? fom.getName() : molName));
        // Drill into the FOM details page
        clickAndWait(Locator.linkContainingText("Show Details"));

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

    protected void runScenario(String scenario, String expectedWeighting, @Nullable FiguresOfMerit fom) throws Exception
    {
        setupSubfolder(getProjectName(), scenario, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + scenario + ".sky.zip");
        testCalculatedConcentrations(scenario);
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
                        .filter(row -> isValueMissing(row.get("PeptideModifiedSequence")))
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
                        .filter(row -> !isValueMissing(row.get("PeptideModifiedSequence")))
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
            calibrationCurvesTable.clickRowDetails(rowWithData);
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

                assertTextPresent("Unable to calculate curve, since there are no data points available");            }
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

    private void testCalculatedConcentrations(String scenario) throws Exception
    {
        String query = "SELECT COALESCE(generalmoleculechrominfo.PeptideId.Sequence, \n" +
                "generalmoleculechrominfo.MoleculeId.Molecule) AS Peptide, \n" +
                "generalmoleculechrominfo.SampleFileId.ReplicateId.Name AS Replicate, \n" +
                "generalmoleculechrominfo.CalculatedConcentration, \n" +
                "generalmoleculechrominfo.SampleFileId.ReplicateId.RunId.FileName\n" +
                "FROM generalmoleculechrominfo";
        ExecuteSqlCommand sc = new ExecuteSqlCommand("targetedms");
        sc.setSql(query);
        SelectRowsResponse resp = sc.execute(createDefaultConnection(true), getProjectName() + "/" + scenario);
        List<Map<String, Object>> expectedRows = readScenarioCsv(scenario, "PeptideResultQuantification");
        for (Map<String, Object> expectedRow : expectedRows)
        {
            String peptide = (String) expectedRow.get("Peptide");
            String replicate = (String) expectedRow.get("Replicate");
            if (replicate == null)
            {
                continue;
            }
            String message = "Peptide:" + peptide + " Replicate:" + replicate;
            Optional<Map<String, Object>> optional = resp.getRows().stream().filter(
                    row->peptide.equals(row.get("Peptide")) && replicate.equals(row.get("Replicate")))
                    .findFirst();
            assertTrue(message, optional.isPresent());
            Map<String, Object> actualRow = optional.get();
            Double expectedConcentration = parseOptionalDouble((String)expectedRow.get("CalculatedConcentration"));
            Double actualConcentration = toDouble(actualRow.get("CalculatedConcentration"));
            if (expectedConcentration != null)
            {
                if (Double.isInfinite(expectedConcentration) || Double.isNaN(expectedConcentration))
                {
                    assertTrue(actualConcentration == null || Double.isNaN(actualConcentration) || Double.isInfinite(actualConcentration));
                }
                else
                {
                    assertNotNull(message, actualConcentration);
                    assertEquals(message, expectedConcentration, actualConcentration, getDelta(expectedConcentration));
                }
            }
            else
            {
                assertNull(message, actualConcentration);
            }
        }
    }

    private Double parseOptionalDouble(String value)
    {
        if (value == null || "".equals(value) || "#N/A".equals(value))
        {
            return null;
        }
        return Double.parseDouble(value);
    }

    private Double toDouble(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            return parseOptionalDouble((String) value);
        }
        return (Double) value;
    }

    /**
     * Returns true if the value is either null or "#N/A".
     */
    private boolean isValueMissing(Object value)
    {
        return null == value || "#N/A".equals(value);
    }

    protected class FiguresOfMerit
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
}
