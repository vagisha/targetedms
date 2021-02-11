package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;

public class TargetedMSExperimentalQCLinkTest extends TargetedMSTest
{
    private static final String SKY_FILE = "PRM_7x5mix_A40010_QEHF_examples_v3.sky.zip";
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
        importData(SKY_FILE);

        log("Creating one test QC folder with same data");
        setUpFolder(QC_FOLDER_1, FolderType.QC);
        importData(SKY_FILE);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(QC_FOLDER_1, afterTest);
        _containerHelper.deleteProject(QC_FOLDER_2, afterTest);
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
        checker().verifyEquals("Invalid QC Folder Name ", QC_FOLDER_1,
                table.getDataAsText(0, "QCFolders"));

        setUpFolder(QC_FOLDER_2, FolderType.QC);
        importData(SKY_FILE);
        goToProjectHome();
        table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        checker().verifyTrue("Instruments Summary webpart is missing",
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Instruments Summary")));
        table = new DataRegionTable("InstrumentSummary", getDriver());
        checker().verifyEquals("Invalid QC Folder Name ", QC_FOLDER_1 + "\n" + QC_FOLDER_2,
                table.getDataAsText(0, "QCFolders"));

        clickAndWait(Locator.linkWithText(QC_FOLDER_1));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_1,getCurrentContainer());
        goBack();

        clickAndWait(Locator.linkWithText(QC_FOLDER_2));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_2,getCurrentContainer());

    }

}
