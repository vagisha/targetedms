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
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.QCAnnotationTypeWebPart;
import org.labkey.test.components.targetedms.QCAnnotationWebPart;
import org.labkey.test.components.targetedms.QCPlot;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaAnnotations;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.targetedms.QCHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category({DailyB.class, MS2.class})
public class TargetedMSQCTest extends TargetedMSTest
{
    private static final String SProCoP_FILE = "SProCoPTutorial.zip";
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

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testQCDashboard()
    {
        List<String> expectedWebParts = Arrays.asList("QC Summary", "QC Plots");

        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);

        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        assertEquals("Wrong number of Skyline documents uploaded", 1, qcSummaryWebPart.getDocCount());
        assertEquals("Wrong number sample files", 47, qcSummaryWebPart.getFileCount());
        assertEquals("Wrong number of precursors tracked", 7, qcSummaryWebPart.getPrecursorCount());

        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        assertEquals("Wrong precursors", Arrays.asList(PRECURSORS), qcPlotsWebPart.getPlotTitles());
    }

    @Test
    public void testQCAnnotations()
    {
        QCHelper.Annotation instrumentChange = new QCHelper.Annotation("Instrumentation Change", "We changed it", "2013-08-22");
        QCHelper.Annotation reagentChange = new QCHelper.Annotation("Reagent Change", "New reagents", "2013-08-10");
        QCHelper.Annotation technicianChange = new QCHelper.Annotation("Technician Change", "New guy on the scene", "2013-08-10");
        QCHelper.Annotation candyChange = new QCHelper.Annotation("Candy Change", "New candies!", "2013-08-21");

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

    @Test
    public void testQCPlots()
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

    @Test
    public void testBadPlotRange()
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
}
