package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

import java.util.List;

public class TargetedMSRunsTable extends DataRegionTable
{
    public TargetedMSRunsTable(BaseWebDriverTest test)
    {
        super("TargetedMSRuns", test);
    }

    public void goToDocumentDetails(String name)
    {
        _test.clickAndWait(Locator.linkWithText(name));
        _test.waitForElement(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Document Versions"));
    }

    public LinkVersionsGrid openDialogForDocuments(List<String> documentNames)
    {
        return openDialogForDocuments(documentNames, documentNames.size());
    }

    public LinkVersionsGrid openDialogForDocuments(List<String> documentNames, int expectedRunCount)
    {
        uncheckAll();
        for (String documentName : documentNames)
            checkCheckbox(getRow("File", documentName));

        clickHeaderButtonByText("Link Versions");

        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(_test);
        linkVersionsGrid.waitForGrid(documentNames, expectedRunCount, true);

        return linkVersionsGrid;
    }

    public void deleteRun(String documentName)
    {
        uncheckAll();
        checkCheckbox(getRow("File", documentName));
        clickHeaderButtonByText("Delete");
        _test.clickButton("Confirm Delete");
    }
}
