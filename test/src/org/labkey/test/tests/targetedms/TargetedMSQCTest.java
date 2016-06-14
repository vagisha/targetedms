/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCAnnotationTypeWebPart;
import org.labkey.test.components.targetedms.QCAnnotationWebPart;
import org.labkey.test.components.targetedms.QCPlot;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaAnnotations;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.targetedms.QCHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, MS2.class})
public class TargetedMSQCTest extends TargetedMSTest
{
    private static final int INIT_DATA_SAMPLE_FILE_COUNT = 47;
    private static final String[] PRECURSORS = {
            "ATEEQLK",
            "FFVAPFPEVFGK",
            "GASIVEDK",
            "LVNELTEFAK",
            "VLDALDSIK",
            "VLVLDTDYK",
            "VYVEELKPTPEGDLEILLQK"};

    private static QCHelper.Annotation instrumentChange = new QCHelper.Annotation("Instrumentation Change", "We changed it", "2013-08-22 14:43:00");
    private static QCHelper.Annotation reagentChange = new QCHelper.Annotation("Reagent Change", "New reagents", "2013-08-10 15:34:00");
    private static QCHelper.Annotation technicianChange = new QCHelper.Annotation("Technician Change", "New guy on the scene", "2013-08-10 08:43:00");
    private static QCHelper.Annotation candyChange = new QCHelper.Annotation("Candy Change", "New candies!", "2013-08-21 6:57:00");

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCTest init = (TargetedMSQCTest)getCurrentTest();

        init.setupFolder(FolderType.QC);
        init.createUserWithPermissions(USER, init.getProjectName(), "Reader");
        init.importData(SProCoP_FILE);
        init.createAndInsertAnnotations();

        // verify the initial values for the Levey-Jennings plot input form
        init.goToProjectHome();
        PanoramaDashboard qcDashboard = new PanoramaDashboard(init);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        assertEquals(QCPlotsWebPart.Scale.LINEAR, qcPlotsWebPart.getCurrentScale());
        assertEquals(QCPlotsWebPart.ChartType.RETENTION, qcPlotsWebPart.getCurrentChartType());
        assertEquals(QCPlotsWebPart.DateRangeOffset.ALL, qcPlotsWebPart.getCurrentDateRangeOffset());
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testQCDashboard()
    {
        List<String> expectedWebParts = Arrays.asList(QCSummaryWebPart.DEFAULT_TITLE, QCPlotsWebPart.DEFAULT_TITLE);
        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        assertEquals("Wrong precursors", Arrays.asList(PRECURSORS), qcPlotsWebPart.getPlotTitles());
    }

    @Test
    public void testQCAnnotations()
    {
        List<String> expectedWebParts = Arrays.asList(QCAnnotationWebPart.DEFAULT_TITLE, QCAnnotationTypeWebPart.DEFAULT_TITLE);

        clickTab("Annotations");

        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        clickTab("Panorama Dashboard");
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        checkForCorrectAnnotations("Individual Plots", qcPlotsWebPart);
    }

    @Test
    public void testQCPlotInputs()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);

        // test option to "Group X-Axis values by Date"
        String initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        assertFalse(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0")));
        qcPlotsWebPart.setGroupXAxisValuesByDate(false);

        // test that plot0 changes based on scale (log/linear)
        for (QCPlotsWebPart.Scale scale : QCPlotsWebPart.Scale.values())
        {
            if (scale != qcPlotsWebPart.getCurrentScale())
            {
                initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
                qcPlotsWebPart.setScale(scale);
                assertFalse(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0")));
            }
        }
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        // test that plot0 changes based on chart type
        for (QCPlotsWebPart.ChartType type : QCPlotsWebPart.ChartType.values())
        {
            if (type != qcPlotsWebPart.getCurrentChartType())
            {
                initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
                qcPlotsWebPart.setChartType(type, type.hasData());
                if (type.hasData())
                    assertFalse(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0")));

                // back to default chart type for baseline comparison of svg plot change
                qcPlotsWebPart.setChartType(QCPlotsWebPart.ChartType.RETENTION, true, type.hasData());
            }
        }
    }

    @Test
    public void testQCPlotInputsPersistence()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);

        // change all of the plot input fields and filter to a single date
        String testDateStr = "2013-08-20";
        qcPlotsWebPart.setChartType(QCPlotsWebPart.ChartType.PEAK);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LOG);
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        qcPlotsWebPart.filterQCPlots(testDateStr, testDateStr, 1);
        int count = qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
        assertEquals("Unexpected number of points for '" + testDateStr + "'", 21, count);

        // verify that on refresh, the selections are persisted to the inputs
        refresh();
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, true);
        assertEquals("Chart Type not round tripped as expected", QCPlotsWebPart.ChartType.PEAK, qcPlotsWebPart.getCurrentChartType());
        assertEquals("Y-Axis Scale not round tripped as expected", QCPlotsWebPart.Scale.LOG, qcPlotsWebPart.getCurrentScale());
        assertTrue("Group X-Axis not round tripped as expected", qcPlotsWebPart.isGroupXAxisValuesByDateChecked());
        assertTrue("Show All Peptides not round tripped as expected", qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked());
        assertEquals("Date Range Offset not round tripped as expected", QCPlotsWebPart.DateRangeOffset.CUSTOM, qcPlotsWebPart.getCurrentDateRangeOffset());
        assertEquals("Start Date not round tripped as expected", testDateStr, qcPlotsWebPart.getCurrentStartDate());
        assertEquals("End Date not round tripped as expected", testDateStr, qcPlotsWebPart.getCurrentEndDate());
        count = qcPlotsWebPart.getPointElements("d", "M", true).size();
        assertEquals("Unexpected number of points for initial data date range", 21, count);

        // impersonate a different user in this container and verify that initial form fields used
        impersonate(USER);
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, false);
        assertEquals("Chart Type not set to default value", QCPlotsWebPart.ChartType.RETENTION, qcPlotsWebPart.getCurrentChartType());
        assertEquals("Y-Axis Scale not set to default value", QCPlotsWebPart.Scale.LINEAR, qcPlotsWebPart.getCurrentScale());
        assertFalse("Group X-Axis not set to default value", qcPlotsWebPart.isGroupXAxisValuesByDateChecked());
        assertFalse("Show All Peptides not set to default value", qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked());
        assertEquals("Date Range Offset not set to default value", QCPlotsWebPart.DateRangeOffset.ALL, qcPlotsWebPart.getCurrentDateRangeOffset());
    }

    @Test
    public void testQCPlotLogMessages()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);

        // if metric has negative values and we pick log y-axis scale, we should revert to linear scale and show message
        qcPlotsWebPart.setChartType(QCPlotsWebPart.ChartType.MASSACCURACTY);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LOG);
        assertEquals("Unexpected number of plots with invalid log scale.", 3, qcPlotsWebPart.getLogScaleInvalidCount());
        assertEquals("Unexpected number of plots with invalid log scale.", 0, qcPlotsWebPart.getLogScaleWarningCount());

        // if the guide set expected range error bar goes beyond zero, show log plot message about it
        createGuideSetFromTable(new GuideSet("2013-08-09", "2013-08-28", "all initial data points"));
        clickTab("Panorama Dashboard");
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, false);
        assertEquals("Y-axis Scale selection wasn't persisted", QCPlotsWebPart.Scale.LOG, qcPlotsWebPart.getCurrentScale());
        qcPlotsWebPart.setChartType(QCPlotsWebPart.ChartType.PEAK);
        assertEquals("Unexpected number of plots with invalid log scale.", 0, qcPlotsWebPart.getLogScaleInvalidCount());
        assertEquals("Unexpected number of plots with invalid log scale.", 1, qcPlotsWebPart.getLogScaleWarningCount());
        removeAllGuideSets();
    }

    @Test
    public void testMultiSeriesQCPlot()
    {
        String yLeftColor = "#66C2A5";
        String yRightColor = "#FC8D62";
        int pointsPerSeries = 47;

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        qcPlotsWebPart.setChartType(QCPlotsWebPart.ChartType.TPAREAS);

        // check that there are two series per plot by doing a point count by color
        int count = qcPlotsWebPart.getPointElements("fill", yLeftColor, false).size();
        assertEquals("Unexpected number of points for yLeft metric", pointsPerSeries * PRECURSORS.length, count);
        count = qcPlotsWebPart.getPointElements("fill", yRightColor, false).size();
        assertEquals("Unexpected number of points for yRight metric", pointsPerSeries * PRECURSORS.length, count);

        // check a few attributes of the multi-series all peptide plot
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        count = qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
        assertEquals("Unexpected number of points for multi-series all peptide plot", pointsPerSeries * 2 * PRECURSORS.length, count);
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        count = qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
        assertEquals("Unexpected number of points for multi-series all peptide plot", pointsPerSeries * 2 * PRECURSORS.length, count);
        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("Annotations", true));
        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("Change", false), 4);
        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("Transition Area", true));
        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("Precursor Area", true));
        for (String precursor : PRECURSORS)
        {
            assertElementPresent(qcPlotsWebPart.getLegendItemLocator(precursor, true), 2);
        }
    }

    @Test
    public void testBadPlotDateRange()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);

        qcPlotsWebPart.setDateRangeOffset(QCPlotsWebPart.DateRangeOffset.CUSTOM);
        qcPlotsWebPart.setStartDate("2014-08-09");
        qcPlotsWebPart.setEndDate("2014-08-27");
        qcPlotsWebPart.applyRange();
        qcPlotsWebPart.waitForPlots(0, true);
    }

    @Test
    public void testDocsWithOverlappingSampleFiles()
    {
        List<String> precursors = new ArrayList<>(Arrays.asList(PRECURSORS));
        precursors.add("AGGSSEPVTGLADK");
        precursors.add("VEATFGVDESANK");
        Collections.sort(precursors);

        // Upload QC_1.sky.zip
        // File has results from 3 sample files.
        importData(QC_1_FILE, 2);
        goToProjectHome();
        verifyQcSummary(2, INIT_DATA_SAMPLE_FILE_COUNT + 3, precursors.size());

        // Upload QC_2.sky.zip
        // File has results from 3 sample files but two of these are the same as the ones in QC_1.sky.zip.
        // Results from these two sample files will not get imported to the QC folder.
        // This is a test for the fix implemented for issue 22455:
        // https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=22455
        // Importing a file containing two or more sample files that had already been imported from an earlier document
        // in a QC folder was causing an exception in the code that calculates area ratios.
        importData(QC_2_FILE, 3);
        goToProjectHome();
        verifyQcSummary(3, INIT_DATA_SAMPLE_FILE_COUNT + 4, precursors.size());

        // verify if the new start/stop date ranges based on the runs added in this test
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.resetInitialQCPlotFields();
        assertEquals("2013-08-09", qcPlotsWebPart.getCurrentStartDate());
        assertEquals("2015-01-16", qcPlotsWebPart.getCurrentEndDate());

        // Check for the newly added precursors.
        assertEquals("Wrong precursors", precursors, qcPlotsWebPart.getPlotTitles());

        // Filter the grid to a single peptide
        goToSchemaBrowser();
        selectQuery("targetedms", "generalmoleculechrominfo");
        waitForText("view data");
        clickAndWait(Locator.linkWithText("view data"));
        DataRegionTable drt = new DataRegionTable("query", this);
        drt.setFilter("PeptideId", "Equals", "AGGSSEPVTGLADK");

        // Verify number of expected rows in the filtered grid
        assertEquals("Unexpected number of rows", 4, drt.getDataRowCount());

        // Add the RunId (Skyline document name) column
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("SampleFileId/ReplicateId/RunId");
        _customizeViewsHelper.saveCustomView();

        // Sort the grid by the sample file name
        String columnName = "SampleFileId";
        drt.setSort(columnName, SortDirection.ASC);

        // Verify values in the rows.
        // Sample files 25fmol_Pepmix_spike_SRM_1601_02 and 25fmol_Pepmix_spike_SRM_1601_02
        // are common to the two docs. They hould have only 1 row each since they were imported
        // only from the first document (QC_1.sky.zip).
        verifyRow(drt, 0, "25fmol_Pepmix_spike_SRM_1601_01", QC_1_FILE);
        verifyRow(drt, 1, "25fmol_Pepmix_spike_SRM_1601_02", QC_1_FILE);
        verifyRow(drt, 2, "25fmol_Pepmix_spike_SRM_1601_03", QC_1_FILE);
        verifyRow(drt, 3, "25fmol_Pepmix_spike_SRM_1601_04", QC_2_FILE);
    }

    @Test
    public void testCombinedPlots()
    {
        int count;
        int expectedNumPointsPerSeries = 47;
        String[] legendItemColors = new String[]{"#66C2A5", "#FC8D62", "#8DA0CB", "#E78AC3", "#A6D854", "#FFD92F", "#E5C494"};

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);

        //select "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        //Counts no. of points. Fill color values are taken from 'legend-item' - so this also checks for legend
        //seq. color and trend line points' color match.
        for (int i = 0; i < PRECURSORS.length; i++)
        {
            count = qcPlotsWebPart.getPointElements("fill", legendItemColors[i], false).size();
            assertEquals("Unexpected number of points for " + PRECURSORS[i], expectedNumPointsPerSeries, count);
        }

        //annotation check
        checkForCorrectAnnotations("Combined Plot", qcPlotsWebPart);

        //select "Group X-Axis Values by Date" and count no. of points
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        for (int i = 0; i < PRECURSORS.length; i++)
        {
            count = qcPlotsWebPart.getPointElements("fill", legendItemColors[i], false).size();
            assertEquals("Unexpected number of points for " + PRECURSORS[i], expectedNumPointsPerSeries, count);
        }
        qcPlotsWebPart.setGroupXAxisValuesByDate(false);

        //Check for clickable pdf button for Combined plot
        clickAndWaitForDownload(Locator.css("#combinedPlot-exportToPDFbutton > a"));

        //deselect "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, PRECURSORS.length);

        //Check for no. of pdf buttons for individual plots
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot0-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot1-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot2-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot3-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot4-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot5-exportToPDFbutton"));
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot6-exportToPDFbutton"));
    }

    @Test
    public void testSmallMoleculeQC()
    {
        String subFolderName = "Small Molecule QC Plot Test";
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC); //create a Panorama folder of type QC

        importData(SMALL_MOLECULE);
        clickFolder(subFolderName);
        verifyQcSummary(1, 5, 186);

        QCPlotsWebPart qcPlotsWebPart = new QCPlotsWebPart(this.getWrappedDriver());

        Assert.assertEquals("Unexpected overflow warning text","Limiting display to the first 50 precursors out of 91 total", qcPlotsWebPart.getOverflowWarningText());
        Assert.assertEquals("Unexpected number of plots", 50, qcPlotsWebPart.getPlots().size());

        //select "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        //Check for clickable pdf button for Combined plot
        clickAndWaitForDownload(Locator.css("#combinedPlot-exportToPDFbutton > a"));

        //deselect "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, 50);

        //Check for no. of pdf buttons for individual plots
        assertElementPresent(Locator.id("tiledPlotPanel-2-precursorPlot3-exportToPDFbutton"));
    }

    private void verifyRow(DataRegionTable drt, int row, String sampleName, String skylineDocName)
    {
        assertEquals(sampleName, drt.getDataAsText(row, 3));
        assertEquals(skylineDocName, drt.getDataAsText(row, 6));
    }

    private void createAndInsertAnnotations()
    {
        clickTab("Annotations");

        QCAnnotationWebPart qcAnnotationWebPart = new PanoramaAnnotations(this).getQcAnnotationWebPart();

        qcAnnotationWebPart.startInsert().insert(instrumentChange);
        qcAnnotationWebPart.startInsert().insert(reagentChange);
        qcAnnotationWebPart.startInsert().insert(technicianChange);

        QCAnnotationTypeWebPart qcAnnotationTypeWebPart = new PanoramaAnnotations(this).getQcAnnotationTypeWebPart();

        qcAnnotationTypeWebPart.startInsert().insert(candyChange.getType(), "This happens anytime we get new candies", "808080");

        qcAnnotationWebPart.startInsert().insert(candyChange);
    }

    private void checkForCorrectAnnotations(String plotType, QCPlotsWebPart qcPlotsWebPart)
    {
        List<QCPlot> qcPlots = qcPlotsWebPart.getPlots();
        Bag<QCHelper.Annotation> expectedAnnotations = new HashBag<>();
        expectedAnnotations.add(instrumentChange);
        expectedAnnotations.add(reagentChange);
        expectedAnnotations.add(technicianChange);
        expectedAnnotations.add(candyChange);
        for (QCPlot plot : qcPlots)
        {
            Bag<QCHelper.Annotation> plotAnnotations = new HashBag<>(plot.getAnnotations());
            assertEquals("Wrong annotations in " + plotType + ":" + plot.getPrecursor(), expectedAnnotations, plotAnnotations);
        }
    }
}
