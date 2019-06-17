/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TargetedMSRunsTable extends DataRegionTable
{
    private static final String ALL_VERSIONS = "All Versions";
    private static final String LATEST_VERSIONS = "Latest Versions";

    public TargetedMSRunsTable(WebDriverWrapper test)
    {
        super("TargetedMSRuns", test.getDriver());
    }

    public void goToDocumentDetails(String name)
    {
        getWrapper().clickAndWait(Locator.linkWithText(name));
        getWrapper().waitForElement(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Document Summary"));
        getWrapper().clickAndWait(Locator.linkContainingText(" version"));
        getWrapper().waitForElement(Locator.tagWithClass("span", "labkey-wp-title-text").withText("Document Versions"));
    }

    public LinkVersionsGrid openLinkVersionsDialogForDocuments(List<String> documentNames)
    {
        return openLinkVersionsDialogForDocuments(documentNames, documentNames.size());
    }

    public LinkVersionsGrid openLinkVersionsDialogForDocuments(List<String> documentNames, int expectedCount)
    {
        // If there is a document chain, click "All Versions" to list all Skyline documents in the table
        showAllVersions();

        openDialogForDocuments("Link Versions", documentNames);

        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(getWrapper());
        linkVersionsGrid.waitForGrid(documentNames, expectedCount, true);

        return linkVersionsGrid;
    }

    public void deleteRun(String documentName)
    {
        uncheckAll();
        final int rowIndex = getRowIndex("File", documentName);
        if (rowIndex < 0)
            fail("Unable to find checkbox for non-existent file: " + documentName);
        checkCheckbox(rowIndex);
        clickHeaderButtonByText("Delete");
        getWrapper().clickButton("Confirm Delete");
    }

    public ClustergrammerDialog openClustergrammerDialog(List<String> documents)
    {
        openDialogForDocuments("Clustergrammer Heatmap", documents);
        return new ClustergrammerDialog(getWrapper().getDriver());
    }

    public void openDialogForDocuments(String buttonText, List<String> documentNames)
    {
        uncheckAll();
        for (String documentName : documentNames)
        {
            final int rowIndex = getRowIndex("File", documentName);
            if (rowIndex < 0)
                fail("Unable to find checkbox for non-existent file: " + documentName);
            checkCheckbox(rowIndex);
        }

        clickHeaderButtonByText(buttonText);
    }

    public void showAllVersions()
    {
        showVersions(ALL_VERSIONS);
    }

    private void showLatestVersions()
    {
        showVersions(LATEST_VERSIONS);
    }

    private void showVersions(String text)
    {
        List<WebElement> buttons = getHeaderButtons();
        if(getWrapper().getTexts(buttons).contains(text))
        {
            clickHeaderButtonAndWait(text);
        }
    }

    public void verifyDocumentChain(List<String> latestVersions, int[] verCounts)
    {
        showLatestVersions();
        assertEquals(latestVersions.size(), getDataRowCount());
        assertTrue(getColumnLabels().contains("Versions"));
        assertTrue(getColumnLabels().contains("Replaced By"));

        int i = 0;
        for(String docName: latestVersions)
        {
            int idx = getRowIndex("File",docName);
            assertTrue(idx != -1);
            List<String>verCount = getRowDataAsText(idx, "Versions");
            assertEquals(1, verCount.size());
            assertEquals("Expected version count for " + latestVersions, String.valueOf(verCounts[i]), verCount.get(0));
            i++;
        }
    }

    public void verifyNoChain(int rowCount)
    {
        List<WebElement> buttons = getHeaderButtons();
        assertTrue(!getWrapper().getTexts(buttons).contains(ALL_VERSIONS));
        assertTrue(!getWrapper().getTexts(buttons).contains(LATEST_VERSIONS));
        assertEquals(rowCount, getDataRowCount());
        assertTrue(!getColumnLabels().contains("Versions"));
        assertTrue(!getColumnLabels().contains("Replaced By"));
    }
}
