/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@Category({DailyB.class, MS2.class})
public class TargetedMSLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "Stergachis-SupplementaryData_2_a.sky.zip";
    private static final String SKY_FILE2 = "Stergachis-SupplementaryData_2_b.sky.zip";

    public TargetedMSLibraryTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.LibraryProtein);
        importData(SKY_FILE1);
        verifyRevision1();
        importData(SKY_FILE2, 2);
        verifyRevision2();
        verifyRevision3();
    }

    @Override
    protected void selectFolderType(FolderType folderType)
    {
        // Make sure that we're still in the wizard UI
        assertTextPresent("Create Project", "Users / Permissions");
        super.selectFolderType(folderType);
    }

    @LogMethod
    protected void verifyRevision1()
    {
        log("Verifying expected protein/peptide counts in library revision 1");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(4, 49, 313, 1);

        // Verify proteins in the library
        assertTextPresent("CTCF", "MAX", "TAF11", "iRT-C18 Standard Peptides");

        // Verify the the protein MAX is present in this revision of the library
        assertElementPresent(Locator.xpath("//tr[(td[1]='" + SKY_FILE1 + "') and (td[span[a[text()='MAX']]])]"));

        // Verify protein details page
        verifyProteinDetailsPage();
    }

    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected protein/peptide counts in library revision 2");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(6, 79, 528, 2);

        // Verify proteins in the library
        assertTextPresent("CTCF", "GATA3", "MAX", "TAF11", "TP53", "iRT-C18 Standard Peptides");

        //check MAX is from Stergachis-SupplementaryData_2_a.zip
        assertElementPresent(Locator.xpath("//tr[(td[1]='" + SKY_FILE1 + "') and (td[span[a[text()='MAX']]])]"));

        // "iRT-C18 Standard Peptides" should now be from Stergachis-SupplementaryData_2_b.sky.zip
        assertElementPresent(Locator.xpath("//tr[(td[1]='" + SKY_FILE2 + "') and (td[span[a[text()='iRT-C18 Standard Peptides']]])]"));

        verifyAndResolveConflicts();
    }

    private void verifyChromatogramLibraryDownloadWebPart(int proteinCount, int peptideCount, int transitionCount, int revision)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        assertElementPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
        assertTextPresent(
                proteinCount + " proteins", peptideCount + " ranked peptides",
                transitionCount + " ranked transitions");
        assertElementPresent(Locator.linkWithText("Download"));
        assertTextPresent("Revision " + revision);
    }

    @LogMethod
    private void verifyProteinDetailsPage()
    {
        //Checks that important elements are present.
        //Tests graph attributes, changes width and height, checks if graph is still present.
        log("Verifying that expected elements exist on the protein details page");
        clickAndWait(Locator.linkContainingText("CTCF"));
        assertElementPresent(Locator.xpath("//tr[td[span[a[text()='CTCF']]]]"));
        assertTextPresent("HsCD00078657 (2-D02)");
        assertElementPresent(Locator.xpath("//tr[(td[1] ='Decoy') and (td[2]='false')]"));
        assertElementPresent(Locator.xpath("//tr[(td[1] ='File') and (td[2] = '" + SKY_FILE1 + "')]"));
        assertElementPresent(Locator.xpath("//table[contains(@id, 'peptideMap')]"));
        assertElementPresent(Locator.xpath("//div[@id = 'peakAreasGraphImg']/img"));

        log("Testing chart interactivity");
        WebElement height= getDriver().findElement(By.xpath("//input[contains(@id, 'chartHeight-inputEl')]"));
        height.clear();
        height.sendKeys("200");
        WebElement width= getDriver().findElement(By.xpath("//input[contains(@id, 'chartWidth-inputEl')]"));
        width.clear();
        width.sendKeys("500");
        click(Locator.xpath("//a[contains(@class, 'x4-btn x-unselectable x4-box-item x4-toolbar-item x4-btn-default-small x4-noicon x4-btn-noicon x4-btn-default-small-noicon')]"));
        assertElementPresent(Locator.xpath("//div[@id = 'peakAreasGraphImg']/img"));
    }

    @LogMethod
    private void verifyAndResolveConflicts()
    {
        log("Verifying that expected conflicts exist");
        assertElementPresent(Locator.xpath("//div[contains(text(), \"There are 1 conflicting proteins in this folder.\") and contains(@style, \"color:red; font-weight:bold\")]"));
        assertElementPresent(Locator.xpath("//tr[td[div[a[contains(@style,'color:red; text-decoration:underline;') and text()='Resolve conflicts']]]]"));
        clickAndWait(Locator.linkContainingText("Resolve conflicts"));
        assertTextPresent(
                "Conflicting Proteins in Document",
                "Current Library Proteins",
                "MAX", "Resolve conflicts for " + SKY_FILE2 + ".");

        clickButton("Apply Changes");
    }

    @LogMethod
    private void verifyRevision3()
    {
        log("Verifying expected protein/peptide counts in library revision 3");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(6, 81, 539, 3);

        assertTextPresent("Archived Revisions");

        //check MAX is fromStergachis-SupplementaryData_2_b.zip
        assertElementPresent(Locator.xpath("//tr[(td[1]='" + SKY_FILE2 + "') and (td[span[a[text()='MAX']]])]"));
    }

}