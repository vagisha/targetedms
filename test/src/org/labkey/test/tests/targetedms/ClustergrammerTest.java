/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
        init.importData(SProCoP_FILE, true);
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
        assertElementPresent(Locator.tagWithClass("a", "labkey-disabled-button").withText(BUTTON_TEXT));

        //check button enabled on selection
        TargetedMSRunsTable table = new TargetedMSRunsTable(this);
        table.checkCheckbox(0);
        assertElementPresent(Locator.lkButton(BUTTON_TEXT));
        ClustergrammerDialog dialog = table.openClustergrammerDialog(Arrays.asList(SProCoP_FILE));

        //Verify default title and description contain filename
        Assert.assertTrue(dialog.getReportTitle().contains(SProCoP_FILE));
        Assert.assertTrue(dialog.getReportDescription().contains(SProCoP_FILE));

        String declineConfirmation = "Test: Decline confirmation";
        dialog.setReportTitle(declineConfirmation);
        dialog.setReportDescription(declineConfirmation);
        dialog.clickSave(false);

        String acceptConfirmation = "Test: Acceptance";
        dialog.setReportTitle(acceptConfirmation);
        dialog.setReportDescription(acceptConfirmation);
        ClustergrammerDialog.Confirmation confirmation = dialog.clickSave();
        confirmation.clickYes();
        Assert.assertTrue("Was not redirected to Clustergrammer as expected",
                getURL().toString().contains(ClustergrammerDialog.CG_REDIRECT_URL));

        //Verify LinkReport was created
        navigateToRunsTab();
        goToManageViews();
        waitForElement(Locator.linkWithText(acceptConfirmation));
        assertElementNotPresent(Locator.linkWithText(declineConfirmation));

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
