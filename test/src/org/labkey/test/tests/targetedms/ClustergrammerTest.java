package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.ClustergrammerDialog;
import org.labkey.test.components.targetedms.TargetedMSRunsTable;

import java.util.Arrays;


/**
 * Created by iansigmon on 4/19/16.
 */
@Category({DailyB.class, MS2.class})
public class ClustergrammerTest extends TargetedMSTest
{
    private static final String BUTTON_TEXT = "Clustergrammer Heatmap";

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        ClustergrammerTest init = (ClustergrammerTest)getCurrentTest();
        init.setupFolder(FolderType.QC);
        init.importData(SProCoP_FILE);
        init.importData(QC_1_FILE,2);
        init.importData(QC_2_FILE,3);
        init.importData(QC_3_FILE,4);
    }

    @Before
    public void preTest()
    {
        navigateToRunsTab();
    }

    @Test
    public void testClustergrammerIntegration()
    {
        //Check button present but disabled (since no files selected)
        assertElementPresent(Locator.lkButtonDisabled(BUTTON_TEXT));
        TargetedMSRunsTable table = new TargetedMSRunsTable(this);

        //check button enabled on selection
        table.checkCheckbox(1);
        assertElementPresent(Locator.lkButtonContainingText(BUTTON_TEXT));
        ClustergrammerDialog dialog = table.openClustergrammerDialog(Arrays.asList(SProCoP_FILE));

        //Verify default title and description contain filename
        Assert.assertTrue(dialog.getTitle().contains(SProCoP_FILE));
        Assert.assertTrue(dialog.getDescription().contains(SProCoP_FILE));

        String declineConfirmation = "Test: Decline confirmation";
        dialog.setTitle(declineConfirmation);
        dialog.setDescription(declineConfirmation);
        dialog.clickSave(false);

        String acceptConfirmation = "Test: Acceptance";
        dialog.setTitle(acceptConfirmation);
        dialog.setDescription(acceptConfirmation);
        dialog.clickSave(true);
        Assert.assertTrue("Was not redirected to Clustergrammer as expected",
                getURL().toString().contains(ClustergrammerDialog.CG_REDIRECT_URL));

        //Verify LinkReport was created
        navigateToRunsTab();
        goToManageViews();
        waitForElement(Locator.linkWithText(acceptConfirmation));
        this.assertElementNotPresent(Locator.linkWithText(declineConfirmation));
        this.assertElementPresent(Locator.linkWithText(acceptConfirmation));
        clickAndWait(Locator.linkWithText(acceptConfirmation), 10000);

        //Verify link navigates to Clustergrammer
        Assert.assertTrue("LinkReport [" + acceptConfirmation + "] to Clustergrammer was broken",
                getURL().toString().contains(ClustergrammerDialog.CG_REDIRECT_URL));

        //TODO: Waiting to exercise the query more explicitly as it is likely to change in the near future.
    }

    public void navigateToRunsTab()
    {
        goToProjectHome();
        clickTab("Runs");
    }
}
