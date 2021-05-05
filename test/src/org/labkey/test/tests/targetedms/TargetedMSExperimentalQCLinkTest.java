package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class TargetedMSExperimentalQCLinkTest extends TargetedMSTest
{
    private static final String SKY_FILE_EXPERIMENT = "SProCoPTutorial.zip";
    private static final String SKY_FILE_QC = "SProCoPTutorial-QCFolderData.zip";
    private static final String QC_FOLDER_1 = "Test Project QC Folder 1";
    private static final String QC_FOLDER_2 = "Test Project QC Folder 2";


    @BeforeClass
    public static void initProject()
    {
        TargetedMSExperimentalQCLinkTest init = (TargetedMSExperimentalQCLinkTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE_EXPERIMENT);

        log("Creating one test QC folder with same data");
        setUpFolder(QC_FOLDER_1, FolderType.QC);
        importData(SKY_FILE_QC);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);
        apiContainerHelper.deleteProject(QC_FOLDER_1, afterTest);
        apiContainerHelper.deleteProject(QC_FOLDER_2, afterTest);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "TargetedMS Experimental QC Link Test";
    }

    @Test
    public void testInstrumentSummaryPage()
    {
        goToProjectHome();
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        checker().verifyTrue("Instruments Summary webpart is missing",
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Instruments Summary")));
        table = new DataRegionTable("InstrumentSummary", getDriver());
        checker().verifyTrue("Invalid QC Folder Name ", table.getDataAsText(0, "QCFolders").contains(QC_FOLDER_1));

        setUpFolder(QC_FOLDER_2, FolderType.QC);
        importData(SKY_FILE_QC);
        goToProjectHome();
        table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        checker().verifyTrue("Instruments Summary webpart is missing",
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Instruments Summary")));
        table = new DataRegionTable("InstrumentSummary", getDriver());
        checker().verifyEquals("Invalid Instrument serial number", "Exactive Series slot #2384",
                table.getDataAsText(0, "SerialNumber"));
        checker().verifyTrue("Invalid QC Folder Name ",
                table.getDataAsText(0, "QCFolders").contains(QC_FOLDER_1 + "\n" + QC_FOLDER_2));

        clickAndWait(Locator.linkWithText(QC_FOLDER_1));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_1, getCurrentContainer());
        goBack();

        clickAndWait(Locator.linkWithText(QC_FOLDER_2));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_2, getCurrentContainer());

    }

    @Test
    public void testLinkExperimentalQC()
    {
        String expRange = "Skyline File: " + SKY_FILE_EXPERIMENT + ", " +
                "Start: 2013-08-09 11:39:00, " +
                "End: 2013-08-27 14:45:49, " +
                "Mean: 14.669, Std Dev: 0.501, " +
                "%CV: 3.415";

        goToProjectHome(QC_FOLDER_1);
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        qcDashboard.getQcPlotsWebPart(); // this causes the next line to wait till the sample files are loaded

        createGuideSetFromTable(new GuideSet("2013/08/03 00:00", "2013/08/09 23:59", "First"));
        createGuideSetFromTable(new GuideSet("2013/08/19 00:00", "2013/08/21 23:59", "Second"));

        List<String> guideSetTitle = Arrays.asList("Guide Set ID: " + getGuideSetRowId("First") + ", " +
                        "Start: 2013-08-03 00:00:00, " +
                        "End: 2013-08-09 23:59:00, " +
                        "# Runs: 5, " +
                        "Mean: 15.427, " +
                        "Std Dev: 0.973, " +
                        "%CV: 6.307",
                "Guide Set ID: " + getGuideSetRowId("Second") + ", " +
                        "Start: 2013-08-19 00:00:00, " +
                        "End: 2013-08-21 23:59:00, " +
                        "# Runs: 17, " +
                        "Mean: 14.687, " +
                        "Std Dev: 0.588, " +
                        "%CV: 4.004");

        goToProjectHome();
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));
        clickAndWait(Locator.linkWithText(QC_FOLDER_1));

        qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        log("Verify show reference guide set is selected by default");
        checker().verifyTrue("Show reference guide set is not checked", qcPlotsWebPart.isShowReferenceGuideSetChecked());

        log("Verify experiment toolbar is present");
        checker().verifyTrue("Experiment date range toolbar is not present",
                isElementPresent(Locator.linkContainingText(SKY_FILE_EXPERIMENT)));
        clickAndWait(Locator.linkContainingText(SKY_FILE_EXPERIMENT));
        checker().verifyEquals("Did not navigate to experimental folder", getProjectName(), getCurrentContainer());
        goBack();

        qcDashboard = new PanoramaDashboard(this);
        qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();

        checker().verifyEquals("Incorrect expRange information", expRange, qcPlotsWebPart.getExperimentRangeRectTitle());
        checker().verifyEquals("Incorrect guideSet information", guideSetTitle, qcPlotsWebPart.getGuideSetTrainingRectTitle(2));

        String testStartDate = "2013-08-19";
        String testEndDate = "2013-08-27";
        qcPlotsWebPart.filterQCPlots(testStartDate, testEndDate, 7);

        checker().verifyTrue("The graph is not divided by line separator",
                isElementPresent(Locator.tagWithAttribute("line", "class", "separator")));
        checker().verifyTrue("One of the 5 points to left of start is missing",
                isElementPresent(Locator.tagWithAttributeContaining("a", "id", "2013-08-18 11:28:48")));
        checker().verifyTrue("One of the 5 points to left of start is missing",
                isElementPresent(Locator.tagWithAttributeContaining("a", "id", "2013-08-17 16:01:37")));
        checker().verifyTrue("One of the 5 points to left of start is missing",
                isElementPresent(Locator.tagWithAttributeContaining("a", "id", "2013-08-17 06:10:02")));
        checker().verifyTrue("One of the 5 points to left of start is missing",
                isElementPresent(Locator.tagWithAttributeContaining("a", "id", "2013-08-16 20:26:28")));
        checker().verifyTrue("One of the 5 points to left of start is missing",
                isElementPresent(Locator.tagWithAttributeContaining("a", "id", "2013-08-16 10:38:57")));
    }

    public String getGuideSetRowId(String comment)
    {
        goToProjectHome(QC_FOLDER_1);
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        qcDashboard.getQcPlotsWebPart(); // this causes the next line to wait till the sample files are loaded
        goToSchemaBrowser();
        DataRegionTable gsTable = viewQueryData("targetedms", "GuideSet");
        CustomizeView view = gsTable.openCustomizeGrid();
        view.showHiddenItems();
        view.addColumn("RowId");
        view.applyCustomView();

        gsTable.setFilter("Comment", "Equals", comment);
        return gsTable.getDataAsText(0, "RowId");
    }
}
