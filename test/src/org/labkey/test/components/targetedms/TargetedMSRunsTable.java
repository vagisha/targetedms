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

    public LinkVersionsGrid openLinkVersionsDialogForDocuments(List<String> documentNames)
    {
        return openLinkVersionsDialogForDocuments(documentNames, documentNames.size());
    }

    public LinkVersionsGrid openLinkVersionsDialogForDocuments(List<String> documentNames, int expectedCount)
    {
        openDialogForDocuments("Link Versions", documentNames);

        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(_test);
        linkVersionsGrid.waitForGrid(documentNames, expectedCount, true);

        return linkVersionsGrid;
    }

    public void deleteRun(String documentName)
    {
        uncheckAll();
        checkCheckbox(getRow("File", documentName));
        clickHeaderButtonByText("Delete");
        _test.clickButton("Confirm Delete");
    }

    public ClustergrammerDialog openClustergrammerDialog(List<String> documents)
    {
        openDialogForDocuments("Clustergrammer Heatmap", documents);

        ClustergrammerDialog dialog = new ClustergrammerDialog(_test.getDriver());
        dialog.waitForDialog();

        return dialog;
    }

    public void openDialogForDocuments(String buttonText, List<String> documentNames)
    {
        uncheckAll();
        for (String documentName : documentNames)
            checkCheckbox(getRow("File", documentName));

        clickHeaderButtonByText(buttonText);
    }
}
