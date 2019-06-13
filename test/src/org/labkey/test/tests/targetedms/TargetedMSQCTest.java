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

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCAnnotationTypeWebPart;
import org.labkey.test.components.targetedms.QCAnnotationWebPart;
import org.labkey.test.components.targetedms.QCPlot;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaAnnotations;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.targetedms.QCHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.labkey.test.components.targetedms.QCPlotsWebPart.QCPlotType.CUSUMm;
import static org.labkey.test.components.targetedms.QCPlotsWebPart.QCPlotType.LeveyJennings;
import static org.labkey.test.components.targetedms.QCPlotsWebPart.QCPlotType.MovingRange;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 28)
public class TargetedMSQCTest extends TargetedMSTest
{
    private static final String[] PRECURSORS = {
            "ATEEQLK",
            "FFVAPFPEVFGK",
            "GASIVEDK",
            "LVNELTEFAK",
            "VLDALDSIK",
            "VLVLDTDYK",
            "VYVEELKPTPEGDLEILLQK"};
    private static final String[] PRECURSOR_TITLES = {
            "ATEEQLK, 409.7163",
            "FFVAPFPEVFGK, 692.8686",
            "GASIVEDK, 409.7163",
            "LVNELTEFAK, 582.3190",
            "VLDALDSIK, 487.2819",
            "VLVLDTDYK, 533.2950",
            "VYVEELKPTPEGDLEILLQK, 1157.1330"};

    private static final String QCREPLICATE_1 = "25fmol_Pepmix_spike_SRM_1601_01";
    private static final String QCREPLICATE_2 = "25fmol_Pepmix_spike_SRM_1601_02";
    private static final String QCREPLICATE_3 = "25fmol_Pepmix_spike_SRM_1601_03";
    private static final String QCREPLICATE_4 = "25fmol_Pepmix_spike_SRM_1601_04";

    private static QCHelper.Annotation instrumentChange = new QCHelper.Annotation("Instrumentation Change", "We changed it", "2013-08-22 14:43:00");
    private static QCHelper.Annotation reagentChange = new QCHelper.Annotation("Reagent Change", "New reagents", "2013-08-10 15:34:00");
    private static QCHelper.Annotation technicianChange = new QCHelper.Annotation("Technician Change", "New guy on the scene", "2013-08-10 08:43:00");
    private static QCHelper.Annotation candyChange = new QCHelper.Annotation("Candy Change", "New candies!", "2013-08-21 6:57:00");


    private static String longPeptideJSTest =
            "var testVals = {\n" +
                    "        a: {fragment:'', dataType: 'Peptide', result: ''},\n" +
                    "        b: {fragment:'A', dataType: 'Peptide', result: 'A'},\n" +
                    "        c: {fragment:'A', dataType: 'Peptide', result: 'A'}, // duplicate\n" +
                    "        d: {fragment:'AB', dataType: 'Peptide', result: 'AB'},\n" +
                    "        e: {fragment:'ABC', dataType: 'Peptide', result: 'ABC'},\n" +
                    "        f: {fragment:'ABCD', dataType: 'Peptide', result: 'ABCD'},\n" +
                    "        g: {fragment:'ABCDE', dataType: 'Peptide', result: 'ABCDE'},\n" +
                    "        h: {fragment:'ABCDEF', dataType: 'Peptide', result: 'ABCDEF'},\n" +
                    "        i: {fragment:'ABCDEFG', dataType: 'Peptide', result: 'ABCDEFG'},\n" +
                    "        j: {fragment:'ABCDEFGH', dataType: 'Peptide', result: 'ABC\u2026FGH'},\n" +
                    "        k: {fragment:'ABCDEFGHI', dataType: 'Peptide', result: 'ABC\u2026GHI'},\n" +
                    "        l: {fragment:'ABCE', dataType: 'Peptide', result: 'ABCE'},\n" +
                    "        m: {fragment:'ABDEFGHI', dataType: 'Peptide', result: 'ABD\u2026'},\n" +
                    "        n: {fragment:'ABEFGHI', dataType: 'Peptide', result: 'ABEFGHI'},\n" +
                    "        o: {fragment:'ABEFGHIJ', dataType: 'Peptide', result: 'ABE\u2026HIJ'},\n" +
                    "        p: {fragment:'ABEFHI', dataType: 'Peptide', result: 'ABEFHI'},\n" +
                    "        q: {fragment:'ABFFFGHI', dataType: 'Peptide', result: 'ABF(5)'},\n" +
                    "        r: {fragment:'ABFFFFGHI', dataType: 'Peptide', result: 'ABF(6)'},\n" +
                    "        s: {fragment:'ABFFFFAFGHI', dataType: 'Peptide', result: 'ABF\u2026FA\u2026'},\n" +
                    "        t: {fragment:'ABFFFAFFGHI', dataType: 'Peptide', result: 'ABF\u2026A\u2026'},\n" +
                    "        u: {fragment:'ABGAABAABAGHI', dataType: 'Peptide', result: 'ABG\u2026B\u2026B\u2026'},\n" +
                    "        v: {fragment:'ABGAAbAABAGHI', dataType: 'Peptide', result: 'ABG\u2026b\u2026B\u2026'},\n" +
                    "        w: {fragment:'ABGAABAAbAGHI', dataType: 'Peptide', result: 'ABG\u2026B\u2026b\u2026'},\n" +
                    "        x: {fragment:'ABGAAB[80]AAB[99]AGHI', dataType: 'Peptide', result: 'ABG\u2026b\u2026b\u2026'},\n" +
                    "        y: {fragment:'C32:0', dataType: 'ion', result: 'C32:0'},\n" +
                    "        z: {fragment:'C32:1', dataType: 'ion', result: 'C32:1'},\n" +
                    "        aa: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},\n" +
                    "        bb: {fragment:'C32:2', dataType: 'ion', result: 'C32:2'},\n" +
                    "        cc: {fragment:'C30:0', dataType: 'ion', result: 'C30:0'},\n" +
                    "        dd: {fragment:'C[30]:0', dataType: 'ion', result: 'C[30]:0'},\n" +
                    "        ee: {fragment:'C[400]:0', dataType: 'ion', result: 'C[4\u2026'},\n" +
                    "        ff: {fragment:'C12:0 fish breath', dataType: 'ion', result: 'C12\u2026'},\n" +
                    "        gg: {fragment:'C15:0 fish breath', dataType: 'ion', result: 'C15(14)'},\n" +
                    "        hh: {fragment:'C15:0 doggy breath', dataType: 'ion', result: 'C15(15)'},\n" +
                    "        ii: {fragment:'C16:0 fishy breath', dataType: 'ion', result: 'C16\u2026f\u2026'},\n" +
                    "        jj: {fragment:'C16:0 doggy breath', dataType: 'ion', result: 'C16\u2026d\u2026'},\n" +
                    "        kk: {fragment:'C14', dataType: 'ion', result: 'C14'},\n" +
                    "        ll: {fragment:'C14:1', dataType: 'ion', result: 'C14:1'},\n" +
                    "        mm: {fragment:'C14:1-OH', dataType: 'ion', result: 'C14:1\u2026'},\n" +
                    "        nn: {fragment:'C14:2', dataType: 'ion', result: 'C14:2'},\n" +
                    "        oo: {fragment:'C14:2-OH', dataType: 'ion', result: 'C14:2\u2026'},\n" +
                    "    };\n" +
                    "\n" +
                    "    var testLegends = function() {\n" +
                    "        var result = '';\n" +
                    "\t\tvar legendHelper = Ext4.create(\"LABKEY.targetedms.QCPlotLegendHelper\");\n" +
                    "        legendHelper.setupLegendPrefixes(testVals, 3);\n" +
                    "\n" +
                    "        for (var key in testVals) {\n" +
                    "            if (testVals.hasOwnProperty(key)) {\n" +
                    "                var val = legendHelper.getUniquePrefix(testVals[key].fragment, (testVals[key].dataType == 'Peptide'));\n" +
                    "                if(val !== testVals[key].result)\n" +
                    "                    result += \"Incorrect result for \" + testVals[key].fragment + \". Expected: \" + testVals[key].result + \", Actual: \" + val + '\\n';\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        return result;\n" +
                    "    };\n" +
                    "\n" +
                    "    return testLegends();";

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCTest init = (TargetedMSQCTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _userHelper.createUser(USER);
        new ApiPermissionsHelper(this).setUserPermissions(USER, "Reader");
        importData(SProCoP_FILE);
        createAndInsertAnnotations();
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
        assertEquals("Wrong precursors", Arrays.asList(PRECURSOR_TITLES), qcPlotsWebPart.getPlotTitles());
    }

    @Test
    public void testQCAnnotations()
    {
        List<String> expectedWebParts = Arrays.asList(QCAnnotationWebPart.DEFAULT_TITLE, QCAnnotationTypeWebPart.DEFAULT_TITLE);

        clickTab("Annotations");

        PortalHelper portalHelper = new PortalHelper(this);
        assertTrue("Wrong WebParts", portalHelper.getWebPartTitles().containsAll(expectedWebParts));

        clickTab("Panorama Dashboard");
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        checkForCorrectAnnotations("Individual Plots", qcPlotsWebPart);
    }

    @Test
    public void testZipOfFiles()
    {
        File single = TestFileUtils.getSampleData("TargetedMS/first.zipme");

        goToProjectHome();
        clickTab("Raw Data");

        log("Drops the dataTransferItems object");
        dragAndDropFileInDropZone(single);

        log("Verifying if the file is uploaded and zipped");
        waitForElement(Locator.tagWithText("span", "TestZipMeDir.zip"),WAIT_FOR_PAGE);
        assertElementPresent(Locator.tagWithText("span", "TestZipMeDir.zip"));
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

        // test that plot0 changes based on scale
        for (QCPlotsWebPart.Scale scale : QCPlotsWebPart.Scale.values())
        {
            if (scale != qcPlotsWebPart.getCurrentScale())
            {
                initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
                qcPlotsWebPart.setScale(scale);
                String svgPlotText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
                assertFalse(svgPlotText.isEmpty());
                assertFalse(initialSVGText.equals(svgPlotText));
            }
        }
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);
        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("+/-3 x Std Dev", true));

        // test that plot0_plotType_1 (CUSUMm) does not change from linear
        qcPlotsWebPart.checkPlotType(CUSUMm, true);
        qcPlotsWebPart.waitForPlots(2, true);
        initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0_plotType_1");
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LOG);
        assertTrue(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0_plotType_1")));
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.PERCENT_OF_MEAN);
        assertTrue(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0_plotType_1")));
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.STANDARD_DEVIATIONS);
        assertTrue(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0_plotType_1")));

        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LINEAR);

        // test that plot0 changes based on metric type
        for (QCPlotsWebPart.MetricType type : QCPlotsWebPart.MetricType.values())
        {
            if (type != qcPlotsWebPart.getCurrentMetricType())
            {
                log("Verify plot type: " + type);
                initialSVGText = qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0");
                qcPlotsWebPart.setMetricType(type, type.hasData());
                if (type.hasData())
                    assertNotEquals(initialSVGText, qcPlotsWebPart.getSVGPlotText("tiledPlotPanel-2-precursorPlot0"));

                // back to default metric type for baseline comparison of svg plot change
                qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.RETENTION, true, type.hasData());
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
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.PEAK);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.PERCENT_OF_MEAN);
        qcPlotsWebPart.setGroupXAxisValuesByDate(true);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        qcPlotsWebPart.filterQCPlots(testDateStr, testDateStr, 1);
        int count = qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
        assertEquals("Unexpected number of points for '" + testDateStr + "'", 21, count);

        // verify that on refresh, the selections are persisted to the inputs
        refresh();
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, true);
        assertEquals("Metric Type not round tripped as expected", QCPlotsWebPart.MetricType.PEAK, qcPlotsWebPart.getCurrentMetricType());
        assertEquals("Y-Axis Scale not round tripped as expected", QCPlotsWebPart.Scale.PERCENT_OF_MEAN, qcPlotsWebPart.getCurrentScale());
        assertTrue("Group X-Axis not round tripped as expected", qcPlotsWebPart.isGroupXAxisValuesByDateChecked());
        assertTrue("Show All Peptides not round tripped as expected", qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked());
        assertEquals("Date Range Offset not round tripped as expected", QCPlotsWebPart.DateRangeOffset.CUSTOM, qcPlotsWebPart.getCurrentDateRangeOffset());
        assertEquals("Start Date not round tripped as expected", testDateStr, qcPlotsWebPart.getCurrentStartDate());
        assertEquals("End Date not round tripped as expected", testDateStr, qcPlotsWebPart.getCurrentEndDate());
        count = qcPlotsWebPart.getPointElements("d", "M", true).size();
        assertEquals("Unexpected number of points for initial data date range", 21, count);

        // test plot type selection persistence
        qcPlotsWebPart.checkAllPlotTypes(false);
        List<QCPlotsWebPart.QCPlotType> selectedPlotTypes = new ArrayList<>();
        selectedPlotTypes.add(MovingRange);
        selectedPlotTypes.add(CUSUMm);
        qcPlotsWebPart.checkPlotType(selectedPlotTypes.get(0), true);
        qcPlotsWebPart.checkPlotType(selectedPlotTypes.get(1), true);
        qcPlotsWebPart.chooseSmallPlotSize(false);
        qcPlotsWebPart.waitForPlots(2, true);

        // test plot type selection is persisted
        refresh();
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(2, true);
        assertEquals("QC Plot Type not round tripped as expected", true, qcPlotsWebPart.isPlotTypeSelected(selectedPlotTypes.get(0)));
        assertEquals("QC Plot Type not round tripped as expected", true, qcPlotsWebPart.isPlotTypeSelected(selectedPlotTypes.get(1)));
        assertEquals("Plot Size not round tripped as expected", false, qcPlotsWebPart.isSmallPlotSizeSelected());

        // impersonate a different user in this container and verify that initial form fields used
        impersonate(USER);
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, false);
        assertEquals("Metric Type not set to default value", QCPlotsWebPart.MetricType.RETENTION, qcPlotsWebPart.getCurrentMetricType());
        assertEquals("Y-Axis Scale not set to default value", QCPlotsWebPart.Scale.LINEAR, qcPlotsWebPart.getCurrentScale());
        assertFalse("Group X-Axis not set to default value", qcPlotsWebPart.isGroupXAxisValuesByDateChecked());
        assertFalse("Show All Peptides not set to default value", qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked());
        assertEquals("Date Range Offset not set to default value", QCPlotsWebPart.DateRangeOffset.ALL, qcPlotsWebPart.getCurrentDateRangeOffset());

        stopImpersonating();
        goToProjectHome();
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, false);

        // reset plot type selection
        qcPlotsWebPart.resetInitialQCPlotFields();
    }

    @Test
    public void testQCPlotLogMessages()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        qcPlotsWebPart.checkAllPlotTypes(true);

        // if metric has negative values and we pick log y-axis scale, we should revert to linear scale and show message
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.MASSACCURACTY);
        qcPlotsWebPart.setScale(QCPlotsWebPart.Scale.LOG);
        assertEquals("Unexpected number of plots with invalid log scale.", 3, qcPlotsWebPart.getLogScaleInvalidCount());
        assertEquals("Unexpected number of plots with invalid log scale.", 0, qcPlotsWebPart.getLogScaleWarningCount());
        assertEquals("Unexpected number of plots with log scale 0 value replacement warning.", PRECURSORS.length, qcPlotsWebPart.getLogScaleEpsilonWarningCount());

        // if the guide set expected range error bar goes beyond zero, show log plot message about it
        createGuideSetFromTable(new GuideSet("2013-08-09", "2013-08-28", "all initial data points"));
        clickTab("Panorama Dashboard");
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.waitForPlots(1, false);
        assertEquals("Y-axis Scale selection wasn't persisted", QCPlotsWebPart.Scale.LOG, qcPlotsWebPart.getCurrentScale());
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.PEAK);
        assertEquals("Unexpected number of plots with invalid log scale.", 0, qcPlotsWebPart.getLogScaleInvalidCount());
        assertEquals("Unexpected number of plots with invalid log scale.", 1, qcPlotsWebPart.getLogScaleWarningCount());
        assertEquals("Unexpected number of plots with log scale 0 value replacement warning.", PRECURSORS.length, qcPlotsWebPart.getLogScaleEpsilonWarningCount());

        qcPlotsWebPart.resetInitialQCPlotFields();

        removeAllGuideSets();
    }

    @Test
    public void testQCPlotType()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        log("Verify Plot Types and Legends");
        qcPlotsWebPart.checkAllPlotTypes(false);
        qcPlotsWebPart.checkPlotType(LeveyJennings, true);
        qcPlotsWebPart.waitForPlots(PRECURSORS.length, true);
        assertFalse("Plot Size should be disabled with less than 2 plot types selected", qcPlotsWebPart.isPlotSizeRadioEnabled());

        qcPlotsWebPart.checkPlotType(MovingRange, true);
        qcPlotsWebPart.waitForPlots(PRECURSORS.length * 2, true);
        assertTrue("Plot Size should be enabled with at least 2 plot types selected", qcPlotsWebPart.isPlotSizeRadioEnabled());

        assertElementNotPresent(qcPlotsWebPart.getLegendItemLocator("CUSUM Group", true));

        qcPlotsWebPart.checkPlotType(CUSUMm, true);
        qcPlotsWebPart.checkPlotType(QCPlotsWebPart.QCPlotType.CUSUMv, true);
        qcPlotsWebPart.waitForPlots(PRECURSORS.length * 4, true);

        assertElementPresent(qcPlotsWebPart.getLegendItemLocator("CUSUM Group", true));

        log("Verify Small/Large Plot Size");
        if (!qcPlotsWebPart.isSmallPlotSizeSelected())
        {
            qcPlotsWebPart.chooseSmallPlotSize(true);
            qcPlotsWebPart.waitForPlots();
        }
        assertTrue("Plot Size is set to small but plot is rendered in large size", isElementPresent(qcPlotsWebPart.getSmallPlotLoc()));

        qcPlotsWebPart.chooseSmallPlotSize(false);
        qcPlotsWebPart.waitForPlots();
        refresh();
        qcPlotsWebPart.waitForPlots();
        assertFalse("Plot Size is set to large but plot is rendered in small size", isElementPresent(qcPlotsWebPart.getSmallPlotLoc()));

        qcPlotsWebPart.resetInitialQCPlotFields();
    }

    @Test
    public void testMultiSeriesQCPlot()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        qcPlotsWebPart.setMetricType(QCPlotsWebPart.MetricType.TPAREAS);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        for (QCPlotsWebPart.QCPlotType plotType : QCPlotsWebPart.QCPlotType.values())
        {
            qcPlotsWebPart.waitForPlots(1, false);
            if (qcPlotsWebPart.isGroupXAxisValuesByDateChecked())
            {
                qcPlotsWebPart.setGroupXAxisValuesByDate(false);
                qcPlotsWebPart.waitForPlots();
            }
            if (qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked())
            {
                qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, PRECURSORS.length);
                qcPlotsWebPart.waitForPlots();
            }
            qcPlotsWebPart.checkAllPlotTypes(false);
            qcPlotsWebPart.checkPlotType(plotType, true);
            qcPlotsWebPart.waitForPlots(1, false);

            testEachMultiSeriesQCPlot(plotType);
        }
        // reset to avoid test case dependency
        qcPlotsWebPart.resetInitialQCPlotFields();
    }

    private void testEachMultiSeriesQCPlot(QCPlotsWebPart.QCPlotType plotType)
    {
        log("Test plot type " + plotType.getLongLabel());

        String yLeftColor = "#66C2A5";
        String yRightColor = "#FC8D62";

        int pointsPerSeries = 47;
        if (plotType == CUSUMm || plotType == QCPlotsWebPart.QCPlotType.CUSUMv)
            pointsPerSeries *= 2;

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        refresh();
        qcPlotsWebPart.waitForPlots(1, false);
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
        if (plotType == CUSUMm || plotType == QCPlotsWebPart.QCPlotType.CUSUMv)
            assertElementPresent(qcPlotsWebPart.getLegendItemLocator("CUSUM Group", true));
        for (String precursor : PRECURSORS)
        {
            Locator legendItemLoc = qcPlotsWebPart.getLegendItemLocatorByTitle(precursor);
            assertElementPresent("Unexpected number of QC plot legend items found for " + precursor, legendItemLoc, 2);
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

        // reset to avoid test case dependency
        qcPlotsWebPart.resetInitialQCPlotFields();
    }

    @Test
    public void testDocsWithOverlappingSampleFiles()
    {
        List<String> precursors = new ArrayList<>();
        precursors.add("AGGSSEPVTGLADK, 644.8226");
        precursors.add("VEATFGVDESANK, 683.8279");
        Collections.sort(precursors);

        String subFolderName = "OverlappingSampleFiles";
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC); //create a Panorama folder of type QC

        // Upload QC_1.sky.zip
        // File has results from 3 sample files.
        importData(QC_1_FILE, 1);
        clickFolder(subFolderName);
        verifyQcSummary(1, 3, precursors.size());

        // Upload QC_2.sky.zip
        // File has results from 3 sample files but two of these are the same as the ones in QC_1.sky.zip.
        // Results from these two sample files will overwrite the previously uploaded sample files.
        // This is a test for the fix implemented for issue 22455:
        // https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=22455
        // Importing a file containing two or more sample files that had already been imported from an earlier document
        // in a QC folder was causing an exception in the code that calculates area ratios.
        importData(QC_2_FILE, 2);
        clickFolder(subFolderName);
        verifyQcSummary(2, 4, precursors.size());

        // verify if the new start/stop date ranges based on the runs added in this test
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.resetInitialQCPlotFields();
        assertEquals("2015-01-16", qcPlotsWebPart.getCurrentStartDate());
        assertEquals("2015-01-16", qcPlotsWebPart.getCurrentEndDate());

        // Check for the newly added precursors.
        assertEquals("Wrong precursors", precursors, qcPlotsWebPart.getPlotTitles());

        // Filter the grid to a single peptide
        DataRegionTable drt =  getSchemaBrowserDataView("targetedms", "generalmoleculechrominfo");

        drt.setFilter("PeptideId", "Equals", "AGGSSEPVTGLADK");

        // Verify number of expected rows in the filtered grid
        assertEquals("Unexpected number of rows", 4, drt.getDataRowCount());

        // Add the RunId (Skyline document name) column
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("SampleFileId/ReplicateId/RunId");
        _customizeViewsHelper.saveCustomView();

        // Sort the grid by the sample file name
        String columnName = "SampleFileId";
        drt.setSort(columnName, SortDirection.ASC);

        // Verify values in the rows.
        // Sample files 25fmol_Pepmix_spike_SRM_1601_02 and 25fmol_Pepmix_spike_SRM_1601_02
        // are common to the two docs. They should have only 1 row each since they were imported
        // only from the first document (QC_1.sky.zip).
        verifyRow(drt, 0, QCREPLICATE_1, QC_1_FILE);
        verifyRow(drt, 1, QCREPLICATE_2, QC_2_FILE);
        verifyRow(drt, 2, QCREPLICATE_3, QC_2_FILE);
        verifyRow(drt, 3, QCREPLICATE_4, QC_2_FILE);

        goToSchemaBrowser();
        selectQuery("targetedms", "replicateannotation");
        waitAndClickAndWait(Locator.linkWithText("view data"));

        // Ensure samples from QC-1 that exist in QC-2 have been overwritten
        drt = new DataRegionTable("query", this);
        assertEquals(Arrays.asList("QC1A_Annotation", "QC2A_Annotation"), drt.getColumnDataAsText("Name"));
        assertTextNotPresent("QC1B_Annotation");

        // Ensure QC_1 file not erased since one sample file in it is not overwritten
        goToModule("FileContent");
        waitForText("QC_1.sky.zip");

        clickFolder(subFolderName);
        importData(QC_4_FILE, 3);
        clickFolder(subFolderName);
        verifyQcSummary(2, 4, precursors.size());

        // Ensure QC-2 samples have been overwritten by QC-4
        goToSchemaBrowser();
        selectQuery("targetedms", "precursorchrominfo");
        waitAndClickAndWait(Locator.linkWithText("view data"));
        assertTextPresent("42.2525");
        assertTextNotPresent("42.4541");

        // QC_2 should be deleted since all samples have been overwritten, log to remain
        goToModule("FileContent");
        waitForElement(Locator.xpath("//div[contains(@id,'fileContent')]"));
        assertTextNotPresent("QC_2.sky.zip");
    }

    private DataRegionTable getSchemaBrowserDataView(String schemaName, String queryName)
    {
        goToSchemaBrowser();
        return viewQueryData(schemaName, queryName);
    }

    private void verifyCombinedLegend()
    {
        assertTextPresent("ATEEQLK",  // 7 is max length without abbreviation
            "FFV\u2026",
            "VLV\u2026");

        String result = (String)executeScript(longPeptideJSTest);
        assertEquals("", result);
    }

    @Test
    public void testCombinedPlots()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.filterQCPlotsToInitialData(PRECURSORS.length, true);
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        verifyCombinedLegend();

        for (QCPlotsWebPart.QCPlotType plotType : QCPlotsWebPart.QCPlotType.values())
        {
            qcPlotsWebPart.waitForPlots(1, false);

            if (qcPlotsWebPart.isShowAllPeptidesInSinglePlotChecked())
            {
                qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, PRECURSORS.length);
                qcPlotsWebPart.waitForPlots();
            }
            if (qcPlotsWebPart.isGroupXAxisValuesByDateChecked())
            {
                qcPlotsWebPart.setGroupXAxisValuesByDate(false);
                qcPlotsWebPart.waitForPlots();
            }
            qcPlotsWebPart.checkAllPlotTypes(false);
            qcPlotsWebPart.checkPlotType(plotType, true);
            qcPlotsWebPart.waitForPlots(1, false);

            testEachCombinedPlots(plotType);
        }
        // reset to avoid test case dependency
        qcPlotsWebPart.resetInitialQCPlotFields();
    }

    private void testEachCombinedPlots(QCPlotsWebPart.QCPlotType plotType)
    {
        log("Testing combined plot for " + plotType.getLongLabel());
        int count;
        int expectedNumPointsPerSeries = 47;
        if (plotType == CUSUMm || plotType == QCPlotsWebPart.QCPlotType.CUSUMv)
            expectedNumPointsPerSeries *= 2;

        String[] legendItemColors = new String[]{"#66C2A5", "#FC8D62", "#8DA0CB", "#E78AC3", "#A6D854", "#FFD92F", "#E5C494"};

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        refresh();
        qcPlotsWebPart.waitForPlots(1, false);

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

        //Check for clickable pdf and PNG button for Combined plot
        verifyDownloadablePlotIcons(1);

        //deselect "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, PRECURSORS.length);

        //Check for no. of pdf buttons for individual plots
        verifyDownloadablePlotIcons(7);
    }

    private void verifyDownloadablePlotIcons(int expectedPlotCount)
    {
        //Check for clickable pdf and png for Pareto Plot
        assertEquals("Unexpected number of plot export PDF icons", expectedPlotCount, getExportPDFIconCount("chart-render-div"));
        clickExportPDFIcon("chart-render-div", expectedPlotCount - 1);
        assertEquals("Unexpected number of plot export PNG icons", expectedPlotCount, getExportPNGIconCount("chart-render-div"));
        clickExportPNGIcon("chart-render-div", expectedPlotCount - 1);
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
        int currentPagePlotCount = 50;
        qcPlotsWebPart.waitForPlots(currentPagePlotCount, true);
        assertTrue("Unexpected overflow warning text", qcPlotsWebPart.getPaginationText().startsWith("Showing 1 - 50 of 91 precursors"));

        // go to the second page of plots
        qcPlotsWebPart.goToNextPage();
        currentPagePlotCount = 41;
        qcPlotsWebPart.waitForPlots(currentPagePlotCount, true);
        assertTrue("Unexpected overflow warning text", qcPlotsWebPart.getPaginationText().startsWith("Showing 51 - 91 of 91 precursors"));

        //select "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);

        //Check for clickable PDF and PNG export icons for Combined plot
        verifyDownloadablePlotIcons(1);

        //deselect "Show All Peptides in Single Plot"
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(false, currentPagePlotCount);

        //Check for no. of PDF and PNG export icons for individual plots
        verifyDownloadablePlotIcons(currentPagePlotCount);
    }

    @Test
    public void testQCPlotExclusions()
    {
        String[] sampleFileAcquiredDates = new String[]{"2015-01-16 09:12:39", "2015-01-16 12:26:46", "2015-01-16 14:47:30"};
        String subFolderName = "QC Plot Exclusions Test";
        setupSubfolder(getProjectName(), subFolderName, FolderType.QC); //create a Panorama folder of type QC

        importData(QC_1a_FILE);
        clickFolder(subFolderName);
        verifyQcSummary(1, 3, 2);

        //confirm 3 exclusions
        DataRegionTable drt = getSchemaBrowserDataView("targetedms", "qcmetricexclusion");
        assertEquals("Wrong count", 3,drt.getDataRowCount());
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(0,"MetricId").get(0));
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(1,"MetricId").get(0));
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(2,"MetricId").get(0));

        importData(QC_1b_FILE,2);
        clickFolder(subFolderName);
        verifyQcSummary(1, 3, 2);

        drt = getSchemaBrowserDataView("targetedms", "qcmetricexclusion");
        assertEquals("Wrong count", 3,drt.getDataRowCount());
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(0,"MetricId").get(0));
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(1,"MetricId").get(0));
        assertEquals("Wrong metric", " ", drt.getRowDataAsText(2,"MetricId").get(0));

        verifyUploadReport("Replicate 25fmol_Pepmix_spike_SRM_1601_03 has an ignore_in_QC=false annotation " +
                "but there are existing exclusions that were added within Panorama or from a previous import.");

        clickFolder(subFolderName);
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setShowExcludedPoints(true);
        qcPlotsWebPart.waitForPlots(2, true);

        // verify that the plot data points are excluded and then change the state to re-include it
        String acquiredDateStr = getAcquiredDateDisplayStr(sampleFileAcquiredDates[0]);
        verifyExclusionButtonSelection(acquiredDateStr, QCPlotsWebPart.QCPlotExclusionState.ExcludeAll);
        changePointExclusionState(acquiredDateStr, QCPlotsWebPart.QCPlotExclusionState.Include, 2);
        acquiredDateStr = getAcquiredDateDisplayStr(sampleFileAcquiredDates[2]);
        verifyExclusionButtonSelection(acquiredDateStr, QCPlotsWebPart.QCPlotExclusionState.ExcludeAll);
        changePointExclusionState(acquiredDateStr, QCPlotsWebPart.QCPlotExclusionState.Include, 2);

        // verify initial QC summary outlier info
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[0], "no outliers");
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[1], "not included in QC");
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[2], "no outliers");

        // create a guide set and verify updated QC Summary outliers info
        qcPlotsWebPart.createGuideSet(new GuideSet(sampleFileAcquiredDates[0], sampleFileAcquiredDates[1], null, 2), null);
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[2], "10/16 (Levey-Jennings), 10/16 (Moving Range) outliers");

        // change data point to only be excluded for a single metric and verify outliers changed
        changePointExclusionState(getAcquiredDateDisplayStr(sampleFileAcquiredDates[1]), QCPlotsWebPart.QCPlotExclusionState.ExcludeMetric, 2);
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[2], "2/16 (Levey-Jennings), 3/16 (Moving Range) outliers");
        changePointExclusionState(getAcquiredDateDisplayStr(sampleFileAcquiredDates[2]), QCPlotsWebPart.QCPlotExclusionState.ExcludeMetric, 2);
        verifyQCSummarySampleFileOutliers(sampleFileAcquiredDates[2], "1/14 (Moving Range) outliers");
    }

    private void verifyQCSummarySampleFileOutliers(String acquiredDate, String outlierInfo)
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        qcDashboard.getQcSummaryWebPart().waitForRecentSampleFiles(3);
        QCSummaryWebPart.QcSummaryTile qcSummaryTile = qcDashboard.getQcSummaryWebPart().getQcSummaryTiles().get(0);
        assertTrue("Unexpected outlier information for QC summary sample file, expected: "
                + acquiredDate + " - " + outlierInfo, qcSummaryTile.hasRecentSampleFileWithOulierTxt(acquiredDate, outlierInfo));
    }

    private String getAcquiredDateDisplayStr(String acquiredDate)
    {
        return acquiredDate.replaceAll("/","-");
    }

    private void verifyExclusionButtonSelection(String acquiredDate, QCPlotsWebPart.QCPlotExclusionState state)
    {
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        WebElement bubble = qcPlotsWebPart.openExclusionBubble(acquiredDate);
        RadioButton radioButton = RadioButton.RadioButton().withLabel(state.getLabel()).find(bubble);
        assertTrue("QC data point exclusion selection not as expected:" + state.getLabel(), radioButton.isChecked());
        qcPlotsWebPart.closeBubble();
    }

    private void changePointExclusionState(String acquiredDate, QCPlotsWebPart.QCPlotExclusionState state, int waitForPlotCount)
    {
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        WebElement bubble = qcPlotsWebPart.openExclusionBubble(acquiredDate);
        RadioButton radioButton = RadioButton.RadioButton().withLabel(state.getLabel()).find(bubble);
        if (!radioButton.isChecked())
        {
            radioButton.check();
            clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(bubble));
        }
        else
            qcPlotsWebPart.closeBubble();
        qcPlotsWebPart.waitForPlots(waitForPlotCount, true);
    }

    @LogMethod
    private void verifyUploadReport(String... reportText)
    {
        beginAt( getCurrentContainerPath()  + "/pipeline-status-showList.view?");
        waitForRunningPipelineJobs(MAX_WAIT_SECONDS * 1000);

        PipelineStatusTable statusTable = new PipelineStatusTable(this);
        statusTable.clickStatusLink(0);
        assertTextPresent(reportText);
    }

    private void verifyRow(DataRegionTable drt, int row, String sampleName, String skylineDocName)
    {
        assertEquals(sampleName, drt.getDataAsText(row, "Sample File"));
        assertEquals(skylineDocName, drt.getDataAsText(row, "File"));
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
