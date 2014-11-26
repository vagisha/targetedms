package org.labkey.test.tests;

import org.apache.commons.collections.ListUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
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
