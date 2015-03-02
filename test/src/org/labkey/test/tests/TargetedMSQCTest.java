/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.test.tests;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
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

@Category({DailyB.class, MS2.class})
public class TargetedMSQCTest extends TargetedMSTest
{
    private static final String SProCoP_FILE = "SProCoPTutorial.zip";
    private static final String QC_1_FILE = "QC_1.sky.zip";
    private static final String QC_2_FILE = "QC_2.sky.zip";

    private static final String[] PRECURSORS = {
            "ATEEQLK",
            "FFVAPFPEVFGK",
            "GASIVEDK",
            "LVNELTEFAK",
            "VLDALDSIK",
            "VLVLDTDYK",
            "VYVEELKPTPEGDLEILLQK"};

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
        init.importData(SProCoP_FILE);
    }

    @Test
    public void testSteps()
    {
        testQCDashboard();
        testQCAnnotations();
        testQCPlots();
        testBadPlotRange();
        testDocsWithOverlappingSampleFiles();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    private void testQCDashboard()
    {
        List<String> expectedWebParts = Arrays.asList("QC Summary", "QC Plots");

        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);

        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        verifyQcSummary(1, 47, 7);

        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        assertEquals("Wrong precursors", Arrays.asList(PRECURSORS), qcPlotsWebPart.getPlotTitles());
    }

    private void testQCAnnotations()
    {
        QCHelper.Annotation instrumentChange = new QCHelper.Annotation("Instrumentation Change", "We changed it", "2013-08-22 14:43");
        QCHelper.Annotation reagentChange = new QCHelper.Annotation("Reagent Change", "New reagents", "2013-08-10 15:34");
        QCHelper.Annotation technicianChange = new QCHelper.Annotation("Technician Change", "New guy on the scene", "2013-08-10 08:43");
        QCHelper.Annotation candyChange = new QCHelper.Annotation("Candy Change", "New candies!", "2013-08-21 6:57");

        List<String> expectedWebParts = Arrays.asList(QCAnnotationWebPart.DEFAULT_TITLE, QCAnnotationTypeWebPart.DEFAULT_TITLE);

        clickTab("Annotations");

        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        PanoramaAnnotations qcAnnotations = new PanoramaAnnotations(this);

        QCAnnotationWebPart qcAnnotationWebPart = qcAnnotations.getQcAnnotationWebPart();

        qcAnnotationWebPart.startInsert().insert(instrumentChange);
        qcAnnotationWebPart.startInsert().insert(reagentChange);
        qcAnnotationWebPart.startInsert().insert(technicianChange);

        QCAnnotationTypeWebPart qcAnnotationTypeWebPart = qcAnnotations.getQcAnnotationTypeWebPart();

        qcAnnotationTypeWebPart.startInsert().insert(candyChange.getType(), "This happens anytime we get new candies", "808080");

        qcAnnotationWebPart.startInsert().insert(candyChange);

        clickTab("Panorama Dashboard");
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        List<QCPlot> qcPlots = qcPlotsWebPart.getPlots();

        Bag<QCHelper.Annotation> expectedAnnotations = new HashBag<>();
        expectedAnnotations.add(instrumentChange);
        expectedAnnotations.add(reagentChange);
        expectedAnnotations.add(technicianChange);
        expectedAnnotations.add(candyChange);
        for (QCPlot plot : qcPlots)
        {
            Bag<QCHelper.Annotation> plotAnnotations = new HashBag<>(plot.getAnnotations());
            assertEquals("Wrong annotations in plot: " + plot.getPrecursor(), expectedAnnotations, plotAnnotations);
        }
    }

    private void testQCPlots()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);

        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        QCPlotsWebPart.Scale initialScale = qcPlotsWebPart.getCurrentScale();
        QCPlotsWebPart.ChartType initialType = qcPlotsWebPart.getCurrentChartType();
        String initialStartDate = qcPlotsWebPart.getCurrentStartDate();
        String initialEndDate = qcPlotsWebPart.getCurrentEndDate();

        assertEquals(QCPlotsWebPart.Scale.LINEAR, initialScale);
        assertEquals(QCPlotsWebPart.ChartType.RETENTION, initialType);
        assertEquals("2013-08-09", initialStartDate);
        assertEquals("2013-08-27", initialEndDate);

        for (QCPlotsWebPart.Scale scale : QCPlotsWebPart.Scale.values())
        {
            if (scale != qcPlotsWebPart.getCurrentScale())
            {
                String initialSVGText = qcPlotsWebPart.getSVGPlotText("precursorPlot0");
                qcPlotsWebPart.setScale(scale);
                assertFalse(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("precursorPlot0")));
            }
        }

        for (QCPlotsWebPart.ChartType type : QCPlotsWebPart.ChartType.values())
        {
            if (type != qcPlotsWebPart.getCurrentChartType())
            {
                String initialSVGText = qcPlotsWebPart.getSVGPlotText("precursorPlot0");
                qcPlotsWebPart.setChartType(type);
                assertFalse(initialSVGText.equals(qcPlotsWebPart.getSVGPlotText("precursorPlot0")));
            }
        }
    }

    private void testBadPlotRange()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        qcPlotsWebPart.setStartDate("2014-08-09");
        qcPlotsWebPart.setEndDate("2014-08-27");
        qcPlotsWebPart.applyRange();
        qcPlotsWebPart.waitForPlots(0);

        qcPlotsWebPart.setStartDate("2013-08-09");
        qcPlotsWebPart.setEndDate("2013-08-27");
        qcPlotsWebPart.applyRange();
        qcPlotsWebPart.waitForPlots(PRECURSORS.length);
    }

    private void testDocsWithOverlappingSampleFiles()
    {
        // Upload QC_1.sky.zip
        // File has results from 3 sample files.
        importData(QC_1_FILE, 2);
        goToProjectHome();
        verifyQcSummary(2, 50, 9);

        // Upload QC_2.sky.zip
        // File has results from 3 sample files but two of these are the same as the ones in QC_1.sky.zip.
        // Results from these two sample files will not get imported to the QC folder.
        // This is a test for the fix implemented for issue 22455:
        // https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=22455
        // Importing a file containing two or more sample files that had already been imported from an earlier document
        // in a QC folder was causing an exception in the code that calculates area ratios.
        importData(QC_2_FILE, 3);
        goToProjectHome();
        verifyQcSummary(3, 51, 9);

        // Check for the newly added precursors.
        List<String> precursors = new ArrayList<>(Arrays.asList(PRECURSORS));
        precursors.add("AGGSSEPVTGLADK");
        precursors.add("VEATFGVDESANK");
        Collections.sort(precursors);
        QCPlotsWebPart qcPlotsWebPart = new PanoramaDashboard(this).getQcPlotsWebPart();
        assertEquals("Wrong precursors", precursors, qcPlotsWebPart.getPlotTitles());

        // Filter the grid to a single peptide
        goToSchemaBrowser();
        selectQuery("targetedms", "peptidechrominfo");
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
        verifyRow(drt, 0, "25fmol_Pepmix_spike_SRM_1601_01", "QC_1.sky.zip");
        verifyRow(drt, 1, "25fmol_Pepmix_spike_SRM_1601_02", "QC_1.sky.zip");
        verifyRow(drt, 2, "25fmol_Pepmix_spike_SRM_1601_03", "QC_1.sky.zip");
        verifyRow(drt, 3, "25fmol_Pepmix_spike_SRM_1601_04", "QC_2.sky.zip");
    }

    private void verifyRow(DataRegionTable drt, int row, String sampleName, String skylineDocName)
    {
        assertEquals(sampleName, drt.getDataAsText(row, 1));
        assertEquals(skylineDocName, drt.getDataAsText(row, 4));
    }

    private void verifyQcSummary(int docCount, int sampleFileCount, int precursorCount)
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        assertEquals("Wrong number of Skyline documents uploaded", docCount, qcSummaryWebPart.getDocCount());
        assertEquals("Wrong number sample files", sampleFileCount, qcSummaryWebPart.getFileCount());
        assertEquals("Wrong number of precursors tracked", precursorCount, qcSummaryWebPart.getPrecursorCount());
    }
}
