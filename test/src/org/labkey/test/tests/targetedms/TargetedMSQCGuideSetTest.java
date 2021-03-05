/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.targetedms;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.Rowset;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.GuideSetStats;
import org.labkey.test.components.targetedms.GuideSetWebPart;
import org.labkey.test.components.targetedms.ParetoPlotsWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.pages.targetedms.ParetoPlotPage;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 25)
public class TargetedMSQCGuideSetTest extends TargetedMSTest
{
    private static final String[] PRECURSORS = {
            "ATEEQLK",
            "FFVAPFPEVFGK",
            "GASIVEDK",
            "LVNELTEFAK",
            "VLDALDSIK",
            "VLVLDTDYK",
            "VYVEELKPTPEGDLEILLQK"};

    private static final GuideSet gs1 = new GuideSet("2013/08/01", "2013/08/01 00:00:01", "first guide set, entirely before initial data with no data points in range");
    private static final GuideSet gs2 = new GuideSet("2013/08/02", "2013/08/11", "second guide set, starts before initial data start date with only one data point in range");
    private static final GuideSet gs3 = new GuideSet("2013/08/14 22:48:37", "2013/08/16 20:26:28", "third guide set, ten data points in range", 10);
    private static final GuideSet gs4 = new GuideSet("2013/08/21 07:56:12", "2013/08/21 13:15:01", "fourth guide set, four data points in range", 4);
    private static final GuideSet gs5 = new GuideSet("2013/08/27 03:00", "2013/08/31 00:00", "fifth guide set, extends beyond last initial data point with two data points in range");

    private static final GuideSet gsSmallMolecule = new GuideSet("2014/07/15 12:40", "2014/07/15 13:40", "Guide set for small molecules");


    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCGuideSetTest init = (TargetedMSQCGuideSetTest)getCurrentTest();

        init.setupFolder(FolderType.QC);
        init.importData(SProCoP_FILE);

        init.createGuideSet(gs1);
        init.createGuideSet(gs2);
        init.createGuideSet(gs3);
        init.createGuideSet(gs4);
        init.createGuideSet(gs5);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // Use the API-based approach for deletion so that we don't trigger AJAX requests navigating to the delete page
        // that may run in the background and cause SQL Server deadlock exceptions
        new APIContainerHelper(this).deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void runTestsInOrder() throws IOException, CommandException
    {
        testGuideSetStats();
        testGuideSetCreateValidation();
        testGuideSetPlotDisplay();
        testParetoPlot();
        testEmptyParetoPlot();
        testSmallMoleculePareto();
        testReplicateAnnotations();
    }

    private void createQueries()
    {
        List<String> metricNames = Arrays.asList("retentionTime", "peakArea", "fwhm", "fwb", "lhRatio", "transitionPrecursorRatio", "massAccuracy", "transitionArea", "precursorArea");
        for (String metricName : metricNames)
        {
            String sql = "SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, \n" +
                    "COALESCE(pci.PrecursorId.PeptideId.Sequence, COALESCE(pci.PrecursorId.ModifiedSequence,\n" +
                    "((CASE WHEN pci.MoleculePrecursorId.CustomIonName IS NULL THEN '' ELSE (pci.MoleculePrecursorId.CustomIonName || ', ') END)\n" +
                    "   || (CASE WHEN pci.MoleculePrecursorId.IonFormula IS NULL THEN '' ELSE (pci.MoleculePrecursorId.IonFormula || ', ') END)\n" +
                    "   || ('[' || CAST (ROUND(pci.MoleculePrecursorId.massMonoisotopic, 4) AS VARCHAR) || '/') \n" +
                    "   || CAST (ROUND(pci.MoleculePrecursorId.massAverage, 4) AS VARCHAR) || '] '))) \n" +
                    "|| (CASE WHEN COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) > 0 THEN ' +' ELSE ' ' END) \n" +
                    "|| CAST(COALESCE(pci.PrecursorId.Charge, pci.MoleculePrecursorId.Charge) AS VARCHAR) || ' ' \n" +
                    "|| CAST (ROUND(COALESCE (pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz), 4) AS VARCHAR)" +
                    " AS SeriesLabel, \n" +
                    "COUNT(MetricValue) AS NumRecords, AVG(MetricValue) AS Mean, STDDEV(MetricValue) AS StandardDev\n" +
                    "FROM guideset gs\n" +
                    "LEFT JOIN QCMetric_" + metricName + " as p\n" +
                    "  ON p.SampleFileId.AcquiredTime >= gs.TrainingStart AND p.SampleFileId.AcquiredTime <= gs.TrainingEnd\n" +
                    "LEFT JOIN PrecursorChromInfo pci ON p.precursorchrominfoid = pci.Id\n" +
                    "GROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, \n"+
                    " pci.PrecursorId.PeptideId.Sequence, pci.PrecursorId.Charge, \n" +
                    "pci.PrecursorId.ModifiedSequence, pci.MoleculePrecursorId.CustomIonName, \n" +
                    "pci.MoleculePrecursorId.IonFormula, pci.MoleculePrecursorId.massMonoisotopic, pci.MoleculePrecursorId.massAverage, \n" +
                    "pci.MoleculePrecursorId.Charge, pci.PrecursorId.Mz, pci.MoleculePrecursorId.Mz";
            createQuery(getCurrentContainerPath(), "GuideSetStats_" + metricName, "targetedms", sql, null, false);
        }
    }

    public void testGuideSetStats() throws IOException, CommandException
    {
        // verify guide set mean/std dev/num records from SQL queries
        preTest();

        createQueries();

        verifyGuideSet1Stats(gs1);
        verifyGuideSet2Stats(gs2);
        verifyGuideSet3Stats(gs3);
        verifyGuideSet4Stats(gs4);
        verifyGuideSet5Stats(gs5);
    }

    public void testGuideSetCreateValidation()
    {
        preTest();
        String overlapErrorMsg = "The training date range overlaps with an existing guide set's training date range.";

        // test validation error message from Guide Set Insert New page
        goToProjectHome();
        createGuideSet(new GuideSet("2013/08/10 00:00:01", "2013/08/10 00:00:00", null), "The training start date/time must be before the training end date/time.");
        createGuideSet(new GuideSet("2013/08/01", "2013/08/12", null), overlapErrorMsg);
        createGuideSet(new GuideSet("2013/08/01", "2013/08/03", null), overlapErrorMsg);
        createGuideSet(new GuideSet("2013/08/10", "2013/08/12", null), overlapErrorMsg);

        // test validation error message from QC plot guide set creation mode
        goToProjectHome();
        createGuideSet(new GuideSet("", "2013/08/11 18:34:14", null, 2), overlapErrorMsg);
        createGuideSet(new GuideSet("2013/08/21 01:12:00", "2013/08/21 07:56:12", null, 5), overlapErrorMsg);
        createGuideSet(new GuideSet("", "", null, 47), overlapErrorMsg);
    }

    public void testGuideSetPlotDisplay()
    {
        preTest();
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.resetInitialQCPlotFields();
        qcPlotsWebPart.setShowReferenceGuideSet(false);

        // 4 of the 5 guide sets are visible in plot region based on the initial data
        List<Pair<String, Integer>> shapeCounts = new ArrayList<>();
        shapeCounts.add(Pair.of(SvgShapes.CIRCLE.getPathPrefix(), 4));
        shapeCounts.add(Pair.of(SvgShapes.TRIANGLE.getPathPrefix(), 23));
        shapeCounts.add(Pair.of(SvgShapes.SQUARE.getPathPrefix(), 18));
        shapeCounts.add(Pair.of(SvgShapes.DIAMOND.getPathPrefix(), 2));
        verifyGuideSetRelatedElementsForPlots(qcPlotsWebPart, 4, shapeCounts, 47);

        // check box for group x-axis values by date and verify
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        verifyGuideSetRelatedElementsForPlots(qcPlotsWebPart, 4, shapeCounts, 20);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        assertEquals("Unexpected number of training range rects visible", 4, qcPlotsWebPart.getGuideSetTrainingRectCount());
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, null);
        qcPlotsWebPart.setGroupXAxisValuesByDate(false);

        // filter plot by start/end date to check reference points without training points in view
        qcPlotsWebPart.filterQCPlots("2013-08-19", "2013-08-19", PRECURSORS.length);
        shapeCounts = new ArrayList<>();
        shapeCounts.add(Pair.of(SvgShapes.CIRCLE.getPathPrefix(), 2));
        shapeCounts.add(Pair.of(SvgShapes.TRIANGLE.getPathPrefix(), 0));
        verifyGuideSetRelatedElementsForPlots(qcPlotsWebPart, 0, shapeCounts, 2);
    }

    public void testParetoPlot()
    {
        preTest();
        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab

        waitForElement(Locator.css("svg"));

        ParetoPlotPage paretoPage = new ParetoPlotPage(getDriver());
        ParetoPlotsWebPart paretoPlotsWebPart = paretoPage.getParetoPlotsWebPart();

        assertEquals("Wrong number of Pareto plots", 20, paretoPlotsWebPart.getNumOfParetoPlots());
        verifyDownloadableParetoPlots(paretoPlotsWebPart.getNumOfParetoPlots());

        ParetoPlotsWebPart.ParetoPlotType plotType = ParetoPlotsWebPart.ParetoPlotType.LeveyJennings;
        int guideSetId = 4;
        log("Verifying Pareto Plots for " + plotType.getLabel());
        assertEquals("Wrong number of non-conformers for PA", 69, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 0));
        assertEquals("Wrong number of non-conformers for P Area", 64, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 1));
        assertEquals("Wrong number of non-conformers for T Area", 60, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 2));
        assertEquals("Wrong number of non-conformers for MA", 57, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 3));
        assertEquals("Wrong number of non-conformers for T/P Ratio", 29, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 4));
        assertEquals("Wrong number of non-conformers for RT", 16, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 5));
        assertEquals("Wrong number of non-conformers for FWHM", 13, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 6));
        assertEquals("Wrong number of non-conformers for FWB", 7, paretoPlotsWebPart.getPlotBarHeight(guideSetId, 7));
        verifyTicksOnPlots(paretoPlotsWebPart, guideSetId, plotType);
        verifyNavigationToPanoramaDashboard(guideSetId, 0, QCPlotsWebPart.MetricType.PEAK, true);

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab
        waitForElement(Locator.css("svg"));
        plotType = ParetoPlotsWebPart.ParetoPlotType.MovingRange;
        log("Verifying non-conformers for " + plotType.getLabel());
        assertEquals("Wrong number of non-conformers for PA", 37, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 0));
        assertEquals("Wrong number of non-conformers for T Area", 34, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 1));
        assertEquals("Wrong number of non-conformers for FWB", 30, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 2));
        assertEquals("Wrong number of non-conformers for P Area", 30, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 3));
        assertEquals("Wrong number of non-conformers for MA", 21, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 4));
        assertEquals("Wrong number of non-conformers for RT", 17, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 5));
        assertEquals("Wrong number of non-conformers for FWHM", 12, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 6));
        assertEquals("Wrong number of non-conformers for T/P Ratio", 4, paretoPlotsWebPart.getPlotBarHeight(guideSetId, plotType, 7));
        verifyTicksOnPlots(paretoPlotsWebPart, guideSetId, plotType);
        verifyNavigationToPanoramaDashboard(guideSetId, QCPlotsWebPart.QCPlotType.MovingRange, 0, QCPlotsWebPart.MetricType.PEAK, true);

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab
        waitForElement(Locator.css("svg"));
        plotType = ParetoPlotsWebPart.ParetoPlotType.CUSUMv;
        log("Verifying non-conformers for " + plotType.getLabel());
        assertEquals("Wrong number of non-conformers for FWHM", "CUSUM-: 2 CUSUM+: 0 Total: 2", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 0));
        assertEquals("Wrong number of non-conformers for T Area", "CUSUM-: 1 CUSUM+: 0 Total: 1", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 1));
        verifyTicksOnPlots(paretoPlotsWebPart, guideSetId, plotType);
        verifyNavigationToPanoramaDashboard(guideSetId, QCPlotsWebPart.QCPlotType.CUSUMv, 0, QCPlotsWebPart.MetricType.FWHM, true);

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab
        waitForElement(Locator.css("svg"));
        plotType = ParetoPlotsWebPart.ParetoPlotType.CUSUMm;
        guideSetId = 3;
        log("Verifying non-conformers for " + plotType.getLabel());
        assertEquals("Wrong number of non-conformers for PA", "CUSUM-: 3 CUSUM+: 4 Total: 7", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 0));
        assertEquals("Wrong number of non-conformers for P Area", "CUSUM-: 2 CUSUM+: 4 Total: 6", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 1));
        assertEquals("Wrong number of non-conformers for MA", "CUSUM-: 0 CUSUM+: 5 Total: 5", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 2));
        assertEquals("Wrong number of non-conformers for T Area", "CUSUM-: 4 CUSUM+: 0 Total: 4", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 3));
        assertEquals("Wrong number of non-conformers for T/P Ratio", "CUSUM-: 3 CUSUM+: 1 Total: 4", paretoPlotsWebPart.getPlotBarTooltip(guideSetId, plotType, 4));
        verifyTicksOnPlots(paretoPlotsWebPart, guideSetId, plotType);
        verifyNavigationToPanoramaDashboard(guideSetId, QCPlotsWebPart.QCPlotType.CUSUMm, 0, QCPlotsWebPart.MetricType.PEAK, true);

    }

    public void testEmptyParetoPlot()
    {
        preTest();
        setupSubfolder(getProjectName(), "Empty Pareto Plot Test", FolderType.QC); //create a Panorama folder of type QC

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab

        ParetoPlotPage paretoPage = new ParetoPlotPage(getDriver());
        ParetoPlotsWebPart paretoPlotsWebPart = paretoPage.getParetoPlotsWebPart();

        paretoPlotsWebPart.verifyEmpty();
   }

    public void testSmallMoleculePareto() throws IOException, CommandException
    {
        preTest();
        String subFolderName = "Small Molecule Pareto Plot Test";
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC); //create a Panorama folder of type QC

        importData(SKY_FILE_SMALLMOL_PEP);
        createGuideSet(gsSmallMolecule, null, subFolderName);

        createQueries();

        verifyGuideSetSmallMoleculeStats(gsSmallMolecule);

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab
        ParetoPlotPage paretoPage = new ParetoPlotPage(getDriver());
        ParetoPlotsWebPart paretoPlotsWebPart = paretoPage.getParetoPlotsWebPart();

        verifyTicksOnPlots(paretoPlotsWebPart, 1);

        clickExportPDFIcon("chart-render-div", 0);
        clickExportPNGIcon("chart-render-div", 0);
        verifyNavigationToPanoramaDashboard(2, 0, QCPlotsWebPart.MetricType.FWHM, false);
    }

    public void testReplicateAnnotations() throws IOException, CommandException
    {
        preTest();
        String folderName = "Annotations";
        setupSubfolder(getProjectName(), folderName, FolderType.QC);
        importData(SProCoP_FILE_ANNOTATED);
        goToSchemaBrowser();
        selectQuery("targetedms", "Replicate");
        //confirm table columns are present and of correct type representing replicate annotations:
        GetQueryDetailsCommand queryDetailsCommand = new GetQueryDetailsCommand("targetedms", "Replicate");
        GetQueryDetailsResponse queryDetailsResponse = queryDetailsCommand.execute(createDefaultConnection(),getProjectName() + "/" + folderName);
        List<GetQueryDetailsResponse.Column> columns = queryDetailsResponse.getColumns();

        int groupingIndex = 11;
        assertEquals("","Grouping", columns.get(groupingIndex).getName());
        assertEquals("","Grouping", columns.get(groupingIndex).getCaption());
        assertEquals("", "Text (String)", columns.get(groupingIndex).getType());

        int ignoreIndex = 12;
        assertEquals("","ignore_in_QC", columns.get(ignoreIndex).getName());
        assertEquals("","ignore_in_QC", columns.get(ignoreIndex).getCaption());
        assertEquals("", "True/False (Boolean)", columns.get(ignoreIndex).getType());

        int timeIndex = 13;
        assertEquals("","Time", columns.get(timeIndex).getName());
        assertEquals("","Time", columns.get(timeIndex).getCaption());
        assertEquals("", "Number (Double)", columns.get(timeIndex).getType());

        //confirm data in grid view
        waitAndClickAndWait(Locator.linkWithText("view data"));
        DataRegionTable table = new DataRegionTable("query", this);
        List<String> strings = table.getRowDataAsText(0);
        List<String> expected = new ArrayList<>();
        expected.add("SProCoPTutorial_withAnnotations.zip");
        expected.add("Q_Exactive_08_09_2013_JGB_02");
        expected.add(" ");
        expected.add(" ");
        expected.add(" ");
        expected.add(" ");
        expected.add(" ");
        expected.add(" ");
        expected.add(" ");
        expected.add("Grouping: Group A\nignore_in_QC: true\nTime: 5");
        expected.add("Group A");
        expected.add("true");
        expected.add("5.0");
        assertEquals("Wrong data in first row", expected, strings);
    }

    private void verifyGuideSetRelatedElementsForPlots(QCPlotsWebPart qcPlotsWebPart, int visibleTrainingRanges, List<Pair<String, Integer>> shapeCounts, int axisTickCount)
    {
        for (Pair<String, Integer> shapeCount : shapeCounts)
        {
            String pathPrefix = shapeCount.getLeft();
            int count = qcPlotsWebPart.getPointElements("d", pathPrefix, true).size();
            assertEquals("Unexpected guide set shape count for " + pathPrefix, shapeCount.getRight() * PRECURSORS.length, count);
        }

        assertEquals("Unexpected number of training range rects visible", visibleTrainingRanges * PRECURSORS.length, qcPlotsWebPart.getGuideSetTrainingRectCount());
        assertEquals("Unexpected number of error bar elements", axisTickCount * PRECURSORS.length * 4, qcPlotsWebPart.getGuideSetErrorBarPathCount("error-bar-vert"));
    }

    private void validateGuideSetStats(GuideSet gs) throws IOException, CommandException
    {
        for (GuideSetStats stats : gs.getStats())
        {

            SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "GuideSetStats_" + stats.getMetricName());
            cmd.setRequiredVersion(9.1);
            cmd.setColumns(Arrays.asList("GuideSetId", "TrainingStart", "TrainingEnd", "ReferenceEnd", "SeriesLabel", "NumRecords", "Mean", "StandardDev"));
            cmd.addFilter("GuideSetId", gs.getRowId(), Filter.Operator.EQUAL);

            // Filter with a Contains to catch the "+2" or similar suffix
            if (stats.getPrecursor() != null)
                cmd.addFilter("SeriesLabel", stats.getPrecursor() + " ", Filter.Operator.CONTAINS);
            else
                cmd.addFilter("SeriesLabel", null, Filter.Operator.ISBLANK);

            SelectRowsResponse response = cmd.execute(createDefaultConnection(), getCurrentContainerPath());

            Rowset rowset = response.getRowset();
            assertEquals("Unexpected number of filtered rows", 1, rowset.getSize());
            Row row = rowset.iterator().next();
            assertEquals("Unexpected guide set stats record count", stats.getNumRecords(), ((Number)row.getValue("NumRecords")).intValue());

            if (stats.getMean() != null)
            {
                double delta = Math.abs(stats.getMean() * 0.001);
                double actual = ((Number) row.getValue("Mean")).doubleValue();
                assertEquals("Unexpected guide set stats mean for " + stats.getMetricName(), stats.getMean(), actual, delta);
            }

            if (stats.getStdDev() != null)
            {
                double delta = Math.abs(stats.getStdDev() * 0.001);
                double actual = ((Number)row.getValue("StandardDev")).doubleValue();
                assertEquals("Unexpected guide set stats std dev for " + stats.getMetricName(), stats.getStdDev(), actual, delta);
            }
        }
    }

    private void verifyGuideSet1Stats(GuideSet gs) throws IOException, CommandException
    {
        gs.addStats(new GuideSetStats("retentionTime", 0));
        gs.addStats(new GuideSetStats("peakArea", 0));
        gs.addStats(new GuideSetStats("fwhm", 0));
        gs.addStats(new GuideSetStats("fwb", 0));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 0));
        gs.addStats(new GuideSetStats("massAccuracy", 0));
        gs.addStats(new GuideSetStats("transitionArea", 0));
        gs.addStats(new GuideSetStats("precursorArea", 0));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet2Stats(GuideSet gs) throws IOException, CommandException
    {
        gs.addStats(new GuideSetStats("retentionTime", 1, PRECURSORS[0], 14.8795, null));
        gs.addStats(new GuideSetStats("peakArea", 1, PRECURSORS[0], 1.1613580288E10, null));
        gs.addStats(new GuideSetStats("fwhm", 1, PRECURSORS[0], 0.0962294191122055, null));
        gs.addStats(new GuideSetStats("fwb", 1, PRECURSORS[0], 0.29160022735595703, null));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 1, PRECURSORS[0], 0.06410326063632965, null));
        gs.addStats(new GuideSetStats("massAccuracy", 1, PRECURSORS[0], -0.0025051420088857412, null));
        gs.addStats(new GuideSetStats("transitionArea", 1, PRECURSORS[0], 6.99620416E8, null));
        gs.addStats(new GuideSetStats("precursorArea", 1, PRECURSORS[0], 1.0913959936E10, null));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet3Stats(GuideSet gs) throws IOException, CommandException
    {
        gs.addStats(new GuideSetStats("retentionTime", 10, PRECURSORS[1], 32.1514, 0.0265));
        gs.addStats(new GuideSetStats("peakArea", 10, PRECURSORS[1], 293_073_490_739.2000, 64_545_315_906.7533));
        gs.addStats(new GuideSetStats("fwhm", 10, PRECURSORS[1], 0.1096, 0.014915757937698814));
        gs.addStats(new GuideSetStats("fwb", 10, PRECURSORS[1], 0.32562103271484377, 0.02468766649130722));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 10, PRECURSORS[1], 0.16636697351932525, 0.024998646348985));
        gs.addStats(new GuideSetStats("massAccuracy", 10, PRECURSORS[1], -0.14503030776977538, 0.5113428116648383));
        gs.addStats(new GuideSetStats("transitionArea", 10, PRECURSORS[1], 4.086185472E10, 6.243547152656243E9));
        gs.addStats(new GuideSetStats("precursorArea", 10, PRECURSORS[1], 2.52211666944E11, 5.881135711787484E10));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet4Stats(GuideSet gs) throws IOException, CommandException
    {
        gs.addStats(new GuideSetStats("retentionTime", 4, PRECURSORS[2], 14.031, 0.24425653767257782));
        gs.addStats(new GuideSetStats("peakArea", 4, PRECURSORS[2], 1.1564451072E10, 1.5713155146840603E9));
        gs.addStats(new GuideSetStats("fwhm", 4, PRECURSORS[2], 0.08786291070282459, 0.006));
        gs.addStats(new GuideSetStats("fwb", 4, PRECURSORS[2], 0.2592000961303711, 0.013227298183650286));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 4, PRECURSORS[2], 0.0, 0.0));
        gs.addStats(new GuideSetStats("massAccuracy", 4, PRECURSORS[2], 1.7878320217132568, 0.09473514310269647));
        gs.addStats(new GuideSetStats("transitionArea", 4, PRECURSORS[2], 0.0, 0.0));
        gs.addStats(new GuideSetStats("precursorArea", 4, PRECURSORS[2], 1.1564451584E10, 1.5713148731374376E9));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet5Stats(GuideSet gs) throws IOException, CommandException
    {
        gs.addStats(new GuideSetStats("retentionTime", 2, PRECURSORS[3], 24.5812, 0.01144101490937325));
        gs.addStats(new GuideSetStats("peakArea", 2, PRECURSORS[3], 56_306_905_088.0000, 1_534_794_886.5359));
        gs.addStats(new GuideSetStats("fwhm", 2, PRECURSORS[3], 0.072, 0.008541938666195341));
        gs.addStats(new GuideSetStats("fwb", 2, PRECURSORS[3], 0.21870040893554688, 0.011455850600049085));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 2, PRECURSORS[3], 0.06426714546978474, 0.02016935064728605));
        gs.addStats(new GuideSetStats("massAccuracy", 2, PRECURSORS[3], 1.6756309866905212, 0.23667992679147354));
        gs.addStats(new GuideSetStats("transitionArea", 2, PRECURSORS[3], 3.376995072E9, 9.104153900486555E8));
        gs.addStats(new GuideSetStats("precursorArea", 2, PRECURSORS[3], 5.2929908736E10, 2.4452091904685783E9));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSetSmallMoleculeStats(GuideSet gs) throws IOException, CommandException
    {
        String precursor = "C16,";

        gs.addStats(new GuideSetStats("retentionTime", 2, precursor, 0.7729333639144897, 9.424035327035906E-5));
        gs.addStats(new GuideSetStats("peakArea", 2, precursor, 2.4647614E7, 5061170.5265));
        gs.addStats(new GuideSetStats("fwhm", 2, precursor, 0.023859419859945774, 0.0010710133238455678));
        gs.addStats(new GuideSetStats("fwb", 2, precursor, 0.11544176936149597, 0.012810408164340708));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 0, precursor, null, null));
        gs.addStats(new GuideSetStats("transitionArea", 2, precursor, 2.4647614E7, 5061170.5265));
        gs.addStats(new GuideSetStats("precursorArea", 2, precursor, 0.0, 0.0));

        validateGuideSetStats(gs);
    }

    private void createGuideSet(GuideSet guideSet)
    {
        createGuideSet(guideSet, null);
    }

    private void createGuideSet(GuideSet guideSet, String expectErrorMsg)
    {
        createGuideSet(guideSet, expectErrorMsg, null);
    }

    private void createGuideSet(GuideSet guideSet, String expectErrorMsg, String subfolder)
    {
        if (guideSet.getBrushSelectedPoints() != null)
        {
            // create the guide set from the QC plot brush selection
            if (null != getUrlParam("pageId"))
                clickTab("Panorama Dashboard");

            PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
            qcDashboard.getQcPlotsWebPart().createGuideSet(guideSet, expectErrorMsg);
            qcDashboard.getQcPlotsWebPart().waitForPlots();
        }
        else
        {
            createGuideSetFromTable(guideSet);
        }

        if (expectErrorMsg == null)
            addRowIdForCreatedGuideSet(guideSet, subfolder);
    }

    private void addRowIdForCreatedGuideSet(GuideSet guideSet, String subfolder)
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        String projectName = subfolder == null ? getProjectName() : getProjectName() + "/" + subfolder;

        GuideSetWebPart guideSetWebPart = new GuideSetWebPart(this, projectName);
        guideSet.setRowId(guideSetWebPart.getRowId(guideSet));
    }

    private void verifyTicksOnPlots(ParetoPlotsWebPart paretoPlotsWebPart, int guideSetNum)
    {
        verifyTicksOnPlots(paretoPlotsWebPart, guideSetNum, ParetoPlotsWebPart.ParetoPlotType.LeveyJennings);
    }

    private void verifyTicksOnPlots(ParetoPlotsWebPart paretoPlotsWebPart, int guideSetNum, ParetoPlotsWebPart.ParetoPlotType plotType)
    {
        paretoPlotsWebPart.waitForTickLoad(guideSetNum, plotType);

        List<String> ticks = paretoPlotsWebPart.getTicks(guideSetNum, plotType);

        for(String metricType : ticks)
            assertTrue("Metric Type tick '" + metricType + "' is not valid", paretoPlotsWebPart.isMetricTypeTickValid(metricType));
    }

    private void verifyDownloadableParetoPlots(int expectedPlotCount)
    {
        //Check for clickable pdf and png for Pareto Plot
        assertEquals("Unexpected number of plot export PDF icons", expectedPlotCount, getExportPDFIconCount("chart-render-div"));
        clickExportPDFIcon("chart-render-div", expectedPlotCount - 1);
        assertEquals("Unexpected number of plot export PNG icons", expectedPlotCount, getExportPNGIconCount("chart-render-div"));
        clickExportPNGIcon("chart-render-div", expectedPlotCount - 1);
    }

    private void verifyNavigationToPanoramaDashboard(int guideSetNum, int barPlotNum, QCPlotsWebPart.MetricType metricType, Boolean checkEndDate)
    {
        verifyNavigationToPanoramaDashboard(guideSetNum, QCPlotsWebPart.QCPlotType.LeveyJennings, barPlotNum, metricType, checkEndDate);
    }

    private void verifyNavigationToPanoramaDashboard(int guideSetNum, QCPlotsWebPart.QCPlotType plotType, int barPlotNum, QCPlotsWebPart.MetricType metricType, Boolean checkEndDate)
    {
        //click on 1st bar
        clickAndWait(Locator.css("#paretoPlot-GuideSet-" + guideSetNum + plotType.getIdSuffix() + "-" + barPlotNum + " > a:nth-child(1) > rect"));

        //check navigation to 'Panorama Dashboard' tab
        assertEquals("Panorama Dashboard", getText(Locator.tagWithClass("ul", "lk-nav-tabs").append(Locator.tagWithClass("li", "active"))));

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        //test for correct metric type
        assertEquals(metricType, qcPlotsWebPart.getCurrentMetricType());

        //test for correct plot type
        qcPlotsWebPart.isPlotTypeSelected(plotType);

        //compare url Start Date with input form Start Date
        assertEquals("startDate in the URL does not equal 'Start Date' on the page", parseUrlDate(getUrlParam("startDate", true)), parseFormDate(qcPlotsWebPart.getCurrentStartDate()));

        //compare url End Date with input form End Date
        if(checkEndDate)
            assertEquals("endDate in the URL does not equal 'End Date' on the page", parseUrlDate(getUrlParam("endDate", true)), parseFormDate(qcPlotsWebPart.getCurrentEndDate()));
    }

    private Date parseUrlDate(String urlDate)
    {
        SimpleDateFormat urlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            return urlDateFormat.parse(urlDate);
        }
        catch (ParseException fail)
        {
            throw new RuntimeException(fail);
        }
    }

    private Date parseFormDate(String formDate)
    {
        SimpleDateFormat formDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            return formDateFormat.parse(formDate);
        }
        catch (ParseException fail)
        {
            throw new RuntimeException(fail);
        }
    }
}
