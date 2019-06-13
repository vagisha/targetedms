/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "Stergachis-SupplementaryData_2_a.sky.zip";
    private static final String SKY_FILE2 = "Stergachis-SupplementaryData_2_b.sky.zip";

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
        assertElementPresent(Locator.xpath("//tr[(td[2]='" + SKY_FILE1 + "') and (td[span[a[contains(normalize-space(),'MAX')]]])]"));

        // Verify protein details page
        verifyProteinDetailsPage();
    }

    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected protein/peptide counts in library revision 2");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(6, 79, 528, 2);

        log("Verify proteins in the library");
        DataRegionTable table = new DataRegionTable("PeptideGroup",getDriver());
        HashSet<String> actualProteinList= new HashSet<>(table.getColumnDataAsText("Label"));
        HashSet<String> expectedProteinValue = new HashSet<>(Arrays.asList("CTCF", "GATA3", "MAX", "TAF11", "TP53", "iRT-C18 Standard Peptides"));
        assertEquals("Missing proteins in the library",expectedProteinValue,actualProteinList);


        log("Check MAX is from Stergachis-SupplementaryData_2_a.zip");
        int indexForProtein = table.getRowIndex("Label","MAX");
        assertEquals("MAX is not from Stergachis-SupplementaryData_2_a.zip",SKY_FILE1,table.getDataAsText(indexForProtein,"RunId/File"));

        log("iRT-C18 Standard Peptides should now be from Stergachis-SupplementaryData_2_b.sky.zip");
        indexForProtein = table.getRowIndex("Label","iRT-C18 Standard Peptides");
        assertEquals("iRT-C18 Standard Peptides is not from Stergachis-SupplementaryData_2_b.sky.zip",SKY_FILE2,table.getDataAsText(indexForProtein,"RunId/File"));

        verifyAndResolveConflicts();
    }

    private void verifyChromatogramLibraryDownloadWebPart(int proteinCount, int peptideCount, int transitionCount, int revision)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        assertElementPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
        assertTextPresent(
                proteinCount + " proteins", peptideCount + " ranked peptides",
                transitionCount + " ranked transitions");
        assertElementPresent(Locator.lkButton("Download"));
        assertTextPresent("Revision " + revision);
    }

    @LogMethod
    private void verifyProteinDetailsPage()
    {
        //Checks that important elements are present.
        //Tests graph attributes, changes width and height, checks if graph is still present.
        log("Verifying that expected elements exist on the protein details page");
        clickAndWait(Locator.linkContainingText("CTCF"));
        assertElementPresent(Locator.pageHeader("CTCF"));
        assertTextPresent("HsCD00078657 (2-D02)");
        assertEquals("Wrong decoy value", "false", getText(Locator.tagWithClass("td", "lk-form-label").withText("Decoy:").followingSibling("td")));
        assertEquals("Wrong File", SKY_FILE1, getText(Locator.tagWithClass("td", "lk-form-label").withText("File:").followingSibling("td")));
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
        assertElementPresent(Locator.xpath("//tr[(td[2]='" + SKY_FILE2 + "') and (td[span[a[normalize-space()='MAX']]])]"));
    }

}