package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.DataRegionTable;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
public class TargetedMSQCSummaryTest extends TargetedMSTest
{
    private static final String FOLDER_1 = "QC Subfolder 1";
    private static final String FOLDER_2 = "QC Subfolder 2";
    private static final String FOLDER_2A = "QC Subfolder 2a";
    private static final String FOLDER_3 = "NonQC Subfolder 3";

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCSummaryTest init = (TargetedMSQCSummaryTest)getCurrentTest();
        init.setupProjectWithSubfolders();
        init.importInitialData();
    }

    private void setupProjectWithSubfolders()
    {
        setupFolder(FolderType.QC);

        setupSubfolder(getProjectName(), FOLDER_1, FolderType.QC);
        setupSubfolder(getProjectName(), FOLDER_2, FolderType.QC);
        setupSubfolder(getProjectName(), FOLDER_3, FolderType.Experiment);

        clickFolder(FOLDER_2);
        setupSubfolder(getProjectName(), FOLDER_2, FOLDER_2A, FolderType.QC);
    }

    private void importInitialData()
    {
        goToProjectHome();
        importData(SProCoP_FILE);

        clickFolder(FOLDER_2);
        importData(QC_1_FILE);

        clickFolder(FOLDER_2A);
        importData(QC_2_FILE);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSubfolders()
    {
        waitForElements(Locator.tagWithClass("div", "item-text").containing("Last update"), 2);
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        assertEquals("Unexpected number of QC Summary tiles", 3, qcSummaryWebPart.getQCSummaryDetails().size());
        verifyQcSummary(0, getProjectName(), 1, 47, 7);
        verifyQcSummary(1, FOLDER_1);
        verifyQcSummary(2, FOLDER_2, 1, 3, 2);
    }

    @Test
    public void testPermissions()
    {
        // give user reader permissions to all but FOLDER_1
        createUserWithPermissions(USER, getProjectName(), "Reader");
        clickButton("Save and Finish");
        for (String folder : new String[]{FOLDER_2, FOLDER_2A, FOLDER_3})
        {
            clickFolder(folder);
            _permissionsHelper.setUserPermissions(USER, "Reader");
            clickButton("Save and Finish");
        }

        // impersonate user and check that the project QC Summary doesn't include the FOLDER_1 details
        goToProjectHome();
        impersonate(USER);
        waitForElements(Locator.tagWithClass("div", "item-text").containing("Last update"), 2);
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        assertEquals("Unexpected number of QC Summary tiles", 2, qcSummaryWebPart.getQCSummaryDetails().size());
        verifyQcSummary(0, getProjectName(), 1, 47, 7);
        verifyQcSummary(1, FOLDER_2, 1, 3, 2);
        assertElementNotPresent(qcSummaryWebPart.getEmptyTextLocator(1));
        stopImpersonating();
    }

    @Test
    public void testSampleFiles()
    {
        int sampleFileCount = 3;

        clickFolder(FOLDER_2A);
        waitForElements(Locator.tagWithClass("div", "item-text").containing("Last update"), 1);
        verifyQcSummary(1, sampleFileCount, 2);

        // verify the initial set of QC plot points
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        assertEquals("Unexpected number of points", 2 * sampleFileCount, getQCPlotPointCount());

        // remove a sample file
        clickAndWait(Locator.linkWithText(sampleFileCount + " sample files"));
        DataRegionTable table = new DataRegionTable("query", this);
        table.checkCheckbox(0);
        table.clickHeaderButtonByText("Delete");
        assertAlert("Are you sure you want to delete the selected row?");
        sampleFileCount--;
        clickTab("Panorama Dashboard");
        waitForElements(Locator.tagWithClass("div", "item-text").containing("Last update"), 1);
        verifyQcSummary(1, sampleFileCount, 2);
        assertEquals("Unexpected number of points", 2 * sampleFileCount, getQCPlotPointCount());

        // remove all sample files
        clickAndWait(Locator.linkWithText(sampleFileCount + " sample files"));
        table.checkAll();
        table.clickHeaderButtonByText("Delete");
        assertAlert("Are you sure you want to delete the selected rows?");
        sampleFileCount = 0;
        clickTab("Panorama Dashboard");
        waitForElements(Locator.tagWithClass("div", "item-text").containing("Last update"), 1);
        assertElementPresent(Locator.linkWithText(sampleFileCount + " sample files"));
        assertElementPresent(Locator.tagContainingText("div", "No data found."));
    }

    private int getQCPlotPointCount()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        return qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
    }
}
