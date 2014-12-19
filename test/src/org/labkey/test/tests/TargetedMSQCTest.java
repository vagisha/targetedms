/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.QCAnnotationTypeWebPart;
import org.labkey.test.components.targetedms.QCAnnotationWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaAnnotations;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.pages.targetedms.PanoramaInsertAnnotation;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

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

        RReportHelper rReportHelper = new RReportHelper(init);
        rReportHelper.ensureRConfig();

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
        List<String> expectedWebParts = Arrays.asList(QCAnnotationWebPart.DEFAULT_TITLE, QCAnnotationTypeWebPart.DEFAULT_TITLE);

        click(Locator.linkContainingText("Annotations"));

        PortalHelper portalHelper = new PortalHelper(this);
        assertEquals("Wrong WebParts", expectedWebParts, portalHelper.getWebPartTitles());

        PanoramaAnnotations qcAnnotations = new PanoramaAnnotations(this);

        QCAnnotationWebPart qcAnnotationWebPart = qcAnnotations.getQcAnnotationWebPart();

        qcAnnotationWebPart.getInsertPage().insert(PanoramaInsertAnnotation.INSTRUMENT_CHANGE, "We changed it", "2013-08-22");
        qcAnnotationWebPart.getInsertPage().insert(PanoramaInsertAnnotation.REAGENT_CHANGE, "New reagents", "2013-08-10");
        qcAnnotationWebPart.getInsertPage().insert(PanoramaInsertAnnotation.TECHNICIAN_CHANGE, "New guy on the scene", "2013-08-10");

        QCAnnotationTypeWebPart qcAnnotationTypeWebPart = qcAnnotations.getQcAnnotationTypeWebPart();

        qcAnnotationTypeWebPart.getInsertPage().insert("Candy Change", "This happens anytime we get new candies", "0F0F0F");

        qcAnnotationWebPart.getInsertPage().insert("Candy Change", "New candies!", "2013-08-21");

        // TODO: we need to add more validation of the plots after we switch over to the labkey viz api
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
            long initialCRC = qcPlotsWebPart.getPlotImageCRC();
            qcPlotsWebPart.setScale(scale);
            assertFalse(initialCRC == qcPlotsWebPart.getPlotImageCRC());
        }

        for (QCPlotsWebPart.ChartType type : QCPlotsWebPart.ChartType.values())
        {
            long initialCRC = qcPlotsWebPart.getPlotImageCRC();
            qcPlotsWebPart.setChartType(type);
            assertFalse(initialCRC == qcPlotsWebPart.getPlotImageCRC());
        }
    }
}
