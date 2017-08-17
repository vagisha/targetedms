/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import java.util.List;

import static org.junit.Assert.fail;

public class TargetedMSRunsTable extends DataRegionTable
{
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
}
