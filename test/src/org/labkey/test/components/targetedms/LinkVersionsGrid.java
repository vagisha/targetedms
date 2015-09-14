/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.tests.TargetedMSLinkVersionsTest;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertTrue;

import java.util.List;

public class LinkVersionsGrid extends Component
{
    private BaseWebDriverTest _test;

    public LinkVersionsGrid(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return null;
    }

    public void waitForGrid(List<String> documentNames, boolean isDialog)
    {
        waitForGrid(documentNames, documentNames.size(), isDialog);
    }

    public void waitForGrid(List<String> documentNames, int expectedRunCount, boolean isDialog)
    {
        if (isDialog)
            _test.waitForElement(Ext4Helper.Locators.window("Link Versions"));

        _test.waitForElements(Ext4Helper.Locators.getGridRow(), expectedRunCount);

        Locator prevGridRow = null;
        for (String documentName : documentNames)
        {
            Locator gridRow = Ext4Helper.Locators.getGridRow(documentName, 0);
            _test.assertElementPresent(gridRow);

            // verify that the grid rows are in the expected order
            // (using y location on the page, since they are in a grid and stacked vertically)
            if (prevGridRow != null)
            {
                boolean rowIsAfterPrev = _test.getElement(prevGridRow).getLocation().getY() < _test.getElement(gridRow).getLocation().getY();
                assertTrue("Unexpected document version order in method chain.", rowIsAfterPrev);
            }

            prevGridRow = gridRow;
        }
    }

    public void clickSave()
    {
        _test._ext4Helper.clickWindowButton("Link Versions", "Save", _test.getDefaultWaitForPage(), 0);
    }

    public void clickCancel()
    {
        _test._ext4Helper.clickWindowButton("Link Versions", "Cancel", 0, 0);
    }

    public void removeLinkVersion(int index)
    {
        int initialRemoveIconCount = findRemoveLinkIcons().size();
        findRemoveLinkIcons().get(index).click();
        _test.waitForElement(Elements.removeText);

        _test._ext4Helper.clickWindowButton("Remove Confirmation", "Yes", _test.getDefaultWaitForPage(), 0);

        // reopen the link versions dialog to verify it was removed
        TargetedMSRunsTable runsTable = new TargetedMSRunsTable(_test);
        runsTable.openDialogForDocuments(TargetedMSLinkVersionsTest.QC_DOCUMENT_NAMES);
        if (initialRemoveIconCount > 2)
        {
            _test.waitForElements(Elements.removeIcon, initialRemoveIconCount - 1);
            _test.assertElementPresent(Elements.replaceFooter);
        }
        else
        {
            _test.assertElementNotPresent(Elements.removeIcon);
            _test.assertElementNotPresent(Elements.replaceFooter);
        }
        clickCancel();
    }

    public List<WebElement> findRemoveLinkIcons()
    {
        return Elements.removeIcon.findElements(_test.getDriver());
    }

    public void reorderVersions(int rowIndex, int indexToMoveTo)
    {
        // Using drag-and-drop from the UI isn't very reliable, so I added a workaround to reorder via JS
        _test.executeScript("LABKEY.targetedms.LinkedVersions.moveGridRow(" + rowIndex + ", " + indexToMoveTo + ");");
    }

    public static class Elements
    {
        public static Locator removeIcon = Locator.tagWithClass("span", "fa-times");
        public static Locator removeText = Locator.tagContainingText("div", "Are you sure you want to remove");
        public static Locator replaceFooter = Locator.tagWithClass("div", "link-version-footer").containing("Saving will replace any existing association.");
        public static Locator noVersions = Locator.tagWithText("div", "No other versions available.");
    }
}
