package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import static org.junit.Assert.assertTrue;

import java.util.List;

public class LinkVersionsGrid
{
    private BaseWebDriverTest _test;

    public LinkVersionsGrid(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void goToDocumentDetails(String name)
    {
        _test.clickAndWait(Locator.linkWithText(name));
        _test.waitForElement(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Document Versions"));
    }

    public Locator getNoVersionsLocator()
    {
        return Locator.tagWithText("div", "No other versions available.");
    }

    public void openDialogForDocuments(List<String> documentNames)
    {
        openDialogForDocuments(documentNames, documentNames.size());
    }

    public void openDialogForDocuments(List<String> documentNames, int expectedRunCount)
    {
        if (!_test.isElementPresent(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Targeted MS Runs")))
            _test.clickTab("Runs");

        DataRegionTable table = new DataRegionTable("TargetedMSRuns", _test);
        table.uncheckAll();
        for (String documentName : documentNames)
            table.checkCheckbox(table.getRow("File", documentName));

        table.clickHeaderButtonByText("Link Versions");
        waitForGrid(documentNames, expectedRunCount, true);
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

    public Locator getReplaceExistingTextLocator()
    {
        return Locator.tagWithClass("div", "link-version-footer").containing("Saving will replace any existing association.");
    }

    public void removeLinkVersion(int index)
    {
        int initialRemoveIconCount = getRemoveLinkIcons().size();
        getRemoveLinkIcons().get(index).click();
        _test.waitForElement(Locator.tagContainingText("div", "Are you sure you want to remove"));

        _test._ext4Helper.clickWindowButton("Remove Confirmation", "Yes", 0, 0);
        _test.sleep(2000); // TODO: window is closed and reopened, need something better than sleep

        if (initialRemoveIconCount > 2)
        {
            _test.waitForElements(Locator.tagWithClass("span", "fa-times"), initialRemoveIconCount - 1);
            _test.assertElementPresent(getReplaceExistingTextLocator());
        }
        else
        {
            _test.assertElementNotPresent(Locator.tagWithClass("span", "fa-times"));
            _test.assertElementNotPresent(getReplaceExistingTextLocator());
        }
    }

    public List<WebElement> getRemoveLinkIcons()
    {
        return Locator.tagWithClass("span", "fa-times").findElements(_test.getDriver());
    }

    public void reorderVersions(String documentToMove, String documentToMoveAbove)
    {
        // TODO: I don't know why the above isn't working to actually use drag-and-drop UI
        //WebElement gridElementToMove = _test.getElement(Ext4Helper.Locators.getGridRow(documentToMove, 0));
        //WebElement topGridElement = _test.getElement(Ext4Helper.Locators.getGridRow(documentToMoveAbove, 0));
        //int yOffset = topGridElement.getLocation().getY() - gridElementToMove.getLocation().getY();
        //_test.dragAndDrop(Ext4Helper.Locators.getGridRow(documentToMove, 0), -1, yOffset-1);
        //_test.sleep(1000); // give it a sec so the drop animation finishes in the Ext4 grid
        _test.executeScript("LABKEY.targetedms.LinkedVersions.moveGridRow(2, 0);");
    }
}
