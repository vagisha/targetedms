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
import org.labkey.remoteapi.query.GetQueryDetailsCommand;
import org.labkey.remoteapi.query.GetQueryDetailsResponse;
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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.RelativeUrl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    private static GuideSet gs1 = new GuideSet("2013/08/01", "2013/08/01 00:00:01", "first guide set, entirely before initial data with no data points in range");
    private static GuideSet gs2 = new GuideSet("2013/08/02", "2013/08/11", "second guide set, starts before initial data start date with only one data point in range");
    private static GuideSet gs3 = new GuideSet("2013/08/14 22:48:37", "2013/08/16 20:26:28", "third guide set, ten data points in range", 10);
    private static GuideSet gs4 = new GuideSet("2013/08/21 07:56:12", "2013/08/21 13:15:01", "fourth guide set, four data points in range", 4);
    private static GuideSet gs5 = new GuideSet("2013/08/27 03:00", "2013/08/31 00:00", "fifth guide set, extends beyond last initial data point with two data points in range");

    private static GuideSet gsSmallMolecule = new GuideSet("2014/07/15 12:40", "2014/07/15 13:40", "Guide set for small molecules");


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
        _containerHelper.deleteProject(getProjectName(), afterTest);
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

    public void testGuideSetStats()
    {
        // verify guide set mean/std dev/num records from SQL queries
        preTest();
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

        assertEquals("Wrong number of Pareto plots", 16, paretoPlotsWebPart.getNumOfParetoPlots());
        verifyDownloadableParetoPlots(paretoPlotsWebPart.getNumOfParetoPlots());

        ParetoPlotsWebPart.ParetoPlotType plotType = ParetoPlotsWebPart.ParetoPlotType.LeveyJennings;
        int guideSetId = 3;
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
        guideSetId = 2;
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

        paretoPlotsWebPart.clickQCPlotsLink(this);

        assertElementPresent(Locator.tagWithClass("span", "labkey-wp-title-text").withText(QCPlotsWebPart.DEFAULT_TITLE));
    }

    public void testSmallMoleculePareto()
    {
        preTest();
        String subFolderName = "Small Molecule Pareto Plot Test";
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC); //create a Panorama folder of type QC

        importData(SMALL_MOLECULE);
        createGuideSet(gsSmallMolecule, null, subFolderName);
        verifyGuideSetSmallMoleculeStats(gsSmallMolecule);

        clickAndWait(Locator.linkWithText("Pareto Plot")); //go to Pareto Plot tab
        ParetoPlotPage paretoPage = new ParetoPlotPage(getDriver());
        ParetoPlotsWebPart paretoPlotsWebPart = paretoPage.getParetoPlotsWebPart();

        verifyTicksOnPlots(paretoPlotsWebPart, 1);

        clickExportPDFIcon("chart-render-div", 0);
        clickExportPNGIcon("chart-render-div", 0);
        verifyNavigationToPanoramaDashboard(1, 0, QCPlotsWebPart.MetricType.FWHM, false);
    }

    public void testReplicateAnnotations() throws IOException, CommandException
    {
        preTest();
        String folderName = "Annotations";
        setupSubfolder(getProjectName(), folderName, FolderType.QC);
        importData(SProCoP_FILE_ANNOTATED);
        goToSchemaBrowser();
        selectQuery("targetedms", "replicate");
        //confirm table columns are present and of correct type representing replicate annotations:
        GetQueryDetailsCommand queryDetailsCommand = new GetQueryDetailsCommand("targetedms", "replicate");
        GetQueryDetailsResponse queryDetailsResponse = queryDetailsCommand.execute(createDefaultConnection(true),getProjectName() + "/" + folderName);
        List<GetQueryDetailsResponse.Column> columns = queryDetailsResponse.getColumns();

        int groupingIndex = 9;
        assertEquals("","Grouping", columns.get(groupingIndex).getName());
        assertEquals("","Grouping", columns.get(groupingIndex).getCaption());
        assertEquals("", "Text (String)", columns.get(groupingIndex).getType());

        int ignoreIndex = 10;
        assertEquals("","ignore_in_QC", columns.get(ignoreIndex).getName());
        assertEquals("","ignore_in_QC", columns.get(ignoreIndex).getCaption());
        assertEquals("", "True/False (Boolean)", columns.get(ignoreIndex).getType());

        int timeIndex = 11;
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

    private void validateGuideSetStats(GuideSet gs)
    {
        for (GuideSetStats stats : gs.getStats())
        {
            navigateToGuideSetStatsQuery(stats.getMetricName());

            DataRegionTable table = new DataRegionTable("query", this);
            table.setFilter("GuideSetId", "Equals", String.valueOf(gs.getRowId()));
            if (stats.getPrecursor() != null)
                table.setFilter("SeriesLabel", "Equals", stats.getPrecursor());
            else
                table.setFilter("SeriesLabel", "Is Blank", null);

            assertEquals("Unexpected number of filtered rows", 1, table.getDataRowCount());
            assertEquals("Unexpected guide set stats record count", stats.getNumRecords(), Integer.parseInt(table.getDataAsText(0, "NumRecords")));

            if (stats.getMean() != null)
            {
                Double delta = stats.getMean() > 1E8 ? 5000.0 : 0.0005;
                assertEquals("Unexpected guide set stats mean", stats.getMean(), Double.parseDouble(table.getDataAsText(0, "Mean")), delta);
            }
            else
                assertNull("Unexpected guide set stats mean", stats.getMean());

            if (stats.getStdDev() != null)
            {
                Double delta = stats.getMean() > 1E8 ? 5000.0 : 0.0005;
                assertEquals("Unexpected guide set stats std dev", stats.getStdDev(), Double.parseDouble(table.getDataAsText(0, "StandardDev")), delta);
            }
            else
                assertNull("Unexpected guide set stats std dev", stats.getStdDev());
        }
    }

    private void navigateToGuideSetStatsQuery(String metricName)
    {
        RelativeUrl queryURL = new RelativeUrl("query", "executequery");
        queryURL.setContainerPath(getCurrentContainerPath());
        queryURL.addParameter("schemaName", "targetedms");
        queryURL.addParameter("query.queryName", "GuideSetStats_" + metricName);
        queryURL.navigate(this);

        // if the query does not exist, create it
        if (isElementPresent(Locator.tagContainingText("h3", "doesn't exist")))
        {
            String sql = "SELECT gs.RowId AS GuideSetId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, SeriesLabel,\n" +
                    "COUNT(MetricValue) AS NumRecords, AVG(MetricValue) AS Mean, STDDEV(MetricValue) AS StandardDev\n" +
                    "FROM guideset gs\n" +
                    "LEFT JOIN QCMetric_" + metricName + " as p\n" +
                    "  ON p.SampleFileId.AcquiredTime >= gs.TrainingStart AND p.SampleFileId.AcquiredTime <= gs.TrainingEnd\n" +
                    "GROUP BY gs.RowId, gs.TrainingStart, gs.TrainingEnd, gs.ReferenceEnd, p.SeriesLabel";
            createQuery(getCurrentContainerPath(), "GuideSetStats_" + metricName, "targetedms", sql, null, false);
            queryURL.navigate(this);
        }
    }

    private void verifyGuideSet1Stats(GuideSet gs)
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

    private void verifyGuideSet2Stats(GuideSet gs)
    {
        gs.addStats(new GuideSetStats("retentionTime", 1, PRECURSORS[0], 14.880, null));
        gs.addStats(new GuideSetStats("peakArea", 1, PRECURSORS[0], 1.1613580288E10, null));
        gs.addStats(new GuideSetStats("fwhm", 1, PRECURSORS[0], 0.096, null));
        gs.addStats(new GuideSetStats("fwb", 1, PRECURSORS[0], 0.292, null));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 1, PRECURSORS[0], 0.06410326063632965, null));
        gs.addStats(new GuideSetStats("massAccuracy", 1, PRECURSORS[0], -0.0025051420088857412, null));
        gs.addStats(new GuideSetStats("transitionArea", 1, PRECURSORS[0], 6.99620390375E8, null));
        gs.addStats(new GuideSetStats("precursorArea", 1, PRECURSORS[0], 1.0913960576E10, null));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet3Stats(GuideSet gs)
    {
        gs.addStats(new GuideSetStats("retentionTime", 10, PRECURSORS[1], 32.151, 0.026));
        gs.addStats(new GuideSetStats("peakArea", 10, PRECURSORS[1], 2.930734907392E11, 6.454531590675328E10));
        gs.addStats(new GuideSetStats("fwhm", 10, PRECURSORS[1], 0.11, 0.015));
        gs.addStats(new GuideSetStats("fwb", 10, PRECURSORS[1], 0.326, 0.025));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 10, PRECURSORS[1], 0.16636697351932525, 0.024998646348985));
        gs.addStats(new GuideSetStats("massAccuracy", 10, PRECURSORS[1], -0.14503030776977538, 0.5113428116648383));
        gs.addStats(new GuideSetStats("transitionArea", 10, PRECURSORS[1], 4.0861855873442184E10, 6.243547152656243E9));
        gs.addStats(new GuideSetStats("precursorArea", 10, PRECURSORS[1], 2.522116655104E11, 5.881135711787484E10));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet4Stats(GuideSet gs)
    {
        gs.addStats(new GuideSetStats("retentionTime", 4, PRECURSORS[2], 14.031, 0.244));
        gs.addStats(new GuideSetStats("peakArea", 4, PRECURSORS[2], 1.1564451072E10, 1.5713155146840603E9));
        gs.addStats(new GuideSetStats("fwhm", 4, PRECURSORS[2], 0.088, 0.006));
        gs.addStats(new GuideSetStats("fwb", 4, PRECURSORS[2], 0.259, 0.013));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 4, PRECURSORS[2], 0.0, 0.0));
        gs.addStats(new GuideSetStats("massAccuracy", 4, PRECURSORS[2], 1.7878320217132568, 0.09473514310269647));
        gs.addStats(new GuideSetStats("transitionArea", 4, PRECURSORS[2], 0.0, 0.0));
        gs.addStats(new GuideSetStats("precursorArea", 4, PRECURSORS[2], 1.15644516E10, 1.57131477994273E9));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSet5Stats(GuideSet gs)
    {
        gs.addStats(new GuideSetStats("retentionTime", 2, PRECURSORS[3], 24.581, 0.011));
        gs.addStats(new GuideSetStats("peakArea", 2, PRECURSORS[3], 5.6306905088E10, 1.5347948865359387E9));
        gs.addStats(new GuideSetStats("fwhm", 2, PRECURSORS[3], 0.072, 0.009));
        gs.addStats(new GuideSetStats("fwb", 2, PRECURSORS[3], 0.219, 0.011));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 2, PRECURSORS[3], 0.06426714546978474, 0.02016935064728605));
        gs.addStats(new GuideSetStats("massAccuracy", 2, PRECURSORS[3], 1.6756309866905212, 0.23667992679147354));
        gs.addStats(new GuideSetStats("transitionArea", 2, PRECURSORS[3], 3.376995236234375E9, 9.104157411050748E8));
        gs.addStats(new GuideSetStats("precursorArea", 2, PRECURSORS[3], 5.2929907456E10, 2.4452102765845675E9));

        validateGuideSetStats(gs);
    }

    private void verifyGuideSetSmallMoleculeStats(GuideSet gs)
    {
        String precursor = "C16";

        gs.addStats(new GuideSetStats("retentionTime", 2, precursor, 0.7729333639144897, 9.424035327035906E-5));
        gs.addStats(new GuideSetStats("peakArea", 2, precursor, 2.4647615E7, 5061166.2838173965));
        gs.addStats(new GuideSetStats("fwhm", 2, precursor, 0.023859419859945774, 0.0010710133238455678));
        gs.addStats(new GuideSetStats("fwb", 2, precursor, 0.11544176936149597, 0.012810408164340708));
        gs.addStats(new GuideSetStats("lhRatio", 0));
        gs.addStats(new GuideSetStats("transitionPrecursorRatio", 0, precursor, null, null));
        gs.addStats(new GuideSetStats("transitionArea", 2, precursor, 2.4647615E7, 5061166.2838173965));
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
