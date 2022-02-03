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
import org.labkey.test.categories.Daily;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineStatusTable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "Stergachis-SupplementaryData_2_a.sky.zip";
    private static final String SKY_FILE2 = "Stergachis-SupplementaryData_2_b.sky.zip";
    private static final String SKY_FILE3 = "QC_1.sky.zip"; // NOTE: do not use MRMer.zip in this test. The document format is too old - no "modified_sequence" attributes for peptides;
    private static final String SKY_FILE4 = "MRMer_renamed_protein.zip"; // Does not have any replicates; should fail to import

    @Test
    public void testSteps()
    {
        int serverErrorCount = getServerErrorCount();
        setupFolder(FolderType.LibraryProtein);
        importData(SKY_FILE1);
        verifyRevision1();
        importData(SKY_FILE2, 2);
        testImportFailAfterConflict();
        verifyRevision2();
        verifyAndResolveConflicts();
        verifyRevision3();
        testImportSucceedAfterConflictResolved();
        testImportFailSkyDocWithoutReplicates();
        checkExpectedErrors(serverErrorCount + 3); // Clear the errors to keep the test from failing
    }

    private void testImportFailAfterConflict()
    {
        // Imports in a library folder with conflicts should fail
        importData(SKY_FILE3, 3, true);
        PipelineStatusTable statusTable = new PipelineStatusTable(getDriver());
        var status = statusTable.clickStatusLink("Skyline document import - QC_1.sky.zip");
        status.assertLogTextContains("The library folder has conflicts.", "New Skyline documents can be added to the folder after the conflicts have been resolved");
    }

    private void testImportSucceedAfterConflictResolved()
    {
        // Delete the failed job first otherwise the test fails
        deletePipelineJob("Skyline document import - QC_1.sky.zip", false);
        importData(SKY_FILE3, 3);
    }

    private void testImportFailSkyDocWithoutReplicates()
    {
        // Importing a Skyline document without any replicates should fail in a library folder
        importData(SKY_FILE4, 4, true);
        PipelineStatusTable statusTable = new PipelineStatusTable(getDriver());
        var status = statusTable.clickStatusLink("Skyline document import - MRMer_renamed_protein.zip");
        status.assertLogTextContains("The Skyline document does not have any replicates. Documents imported to a chromatogram library folder must have chromatograms.");
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
        log("Verifying expected protein/peptide counts in library revision 1 after uploading " + SKY_FILE1);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(7, 55, 343, 1);

        clickTab("Proteins");

        // Verify proteins in the library
        // Proteins in SKY_FILE1: "CTCF", "TAF11", "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "DifferentProteinSameLabel",  "iRT-C18 Standard Peptides"
        List<String> proteins  = Arrays.asList("CTCF", "TAF11", "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "DifferentProteinSameLabel",  "iRT-C18 Standard Peptides");
        List<String> files = new ArrayList();
        for (int i = 0; i < proteins.size(); i++)
        {
            files.add(SKY_FILE1); // All proteins are from the same file in version 1.
        }
        verifyLibraryProteins(proteins, files, 7);

        // Verify protein details page
        verifyProteinDetailsPage();
    }

    private void verifyLibraryProteins(List<String> proteins, List<String> files, int totalProteinCount)
    {
        log("Verify proteins in the library");

        DataRegionTable proteinsTable = new DataRegionTable("PeptideGroup",getDriver());
        assertEquals("Unexpected number of rows in proteins table (library view)", proteins.size(), proteinsTable.getDataRowCount());

        int i = 0;
        for(String protein: proteins)
        {
            int idx = proteinsTable.getRowIndex("Label", protein);
            assertTrue("Expected protein " + protein + " not found in table", idx != -1);
            List<String>fileName = proteinsTable.getRowDataAsText(idx, "File");
            assertEquals(1, fileName.size());
            assertEquals("Unexpected file name for " + protein, files.get(i), fileName.get(0));
            i++;
        }

        proteinsTable.clickHeaderButtonAndWait("View All");
        assertEquals("Unexpected number of rows in proteins table (default view)", totalProteinCount, proteinsTable.getDataRowCount());
    }

    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected protein/peptide counts in library revision 2 after uploading " + SKY_FILE2);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        // The folder has conflicts so the download link and the numbers should be for the last stable library built
        // before conflicts, which is revision 1.
        verifyChromatogramLibraryDownloadWebPart(7, 55, 343, 1, true);

        // Proteins in SKY_FILE1: "CTCF", "TAF11", "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "DifferentProteinSameLabel",  "iRT-C18 Standard Peptides"
        // Proteins in SKY_FILE2: "TP53", "GATA3", "MAX", "HPRR350065", "HPRR1440042", "DifferentProteinSameLabel", "HPRR5000001", "iRT-C18 Standard Peptides"
        // Expected proteins in library:
        //  CTCF                        -> SKY_FILE1 (no conflict)
        //  TAF11                       -> SKY_FILE1 (no conflict)
        //  MAX                         -> SKY_FILE1 (in a conflicted state in SKY_FILE2; same sequence and label)
        //  HPRR1440042                 -> SKY_FILE1 (in a conflicted state in SKY_FILE2; same label, no sequence)
        //  QPrEST_CystC_HPRR5000001    -> SKY_FILE1 (in a conflicted state in SKY_FILE2; same sequence, different label HPRR5000001)
        //  DifferentProteinSameLabel   -> SKY_FILE1 (in a conflicted state in SKY_FILE2; different sequence, same label)
        //  TP53                        -> SKY_FILE2 (no conflict)
        //  GATA3                       -> SKY_FILE2 (no conflict)
        //  HPRR350065                  -> SKY_FILE2 (no conflict)
        //  iRT-C18 Standard Peptides   -> SKY_FILE2 (no conflict; new versions of iRT and standard proteins are always added to the library)

        clickTab("Proteins");
        List<String> proteins = Arrays.asList( /*Proteins from SKY_FILE1*/ "CTCF", "TAF11", "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "DifferentProteinSameLabel",
                                               /*Proteins from SKY_FILE2*/ "TP53", "GATA3", "HPRR350065",
                                               /*iRT peptides; always from the latest file (SKY_FILE2)*/"iRT-C18 Standard Peptides");
        List<String> files = Arrays.asList(SKY_FILE1, SKY_FILE1, SKY_FILE1, SKY_FILE1, SKY_FILE1, SKY_FILE1,
                                           SKY_FILE2, SKY_FILE2, SKY_FILE2,
                                           SKY_FILE2);

        verifyLibraryProteins(proteins, files, 15);
    }

    private void verifyChromatogramLibraryDownloadWebPart(int proteinCount, int peptideCount, int transitionCount, int revision)
    {
        verifyChromatogramLibraryDownloadWebPart(proteinCount, peptideCount, transitionCount, revision, false);
    }

    private void verifyChromatogramLibraryDownloadWebPart(int proteinCount, int peptideCount, int transitionCount, int revision, boolean hasConflict)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        if(!hasConflict)
        {
            assertElementPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
        }
        else
        {
            // The library stats graph is not displayed if the folder has conflicts.
            assertElementNotPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
            assertTextPresent("The library cannot be extended until the conflicts are resolved", "The download link below is for the last stable version of the library");
        }
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
        assertEquals("Wrong decoy value", "false", getText(Locator.tagWithClass("td", "labkey-form-label").withText("Decoy").followingSibling("td")));
        assertEquals("Wrong File", SKY_FILE1 + "    584 KB ", getText(Locator.tagWithClass("td", "labkey-form-label").withText("File").followingSibling("td")));
        assertElementPresent(Locator.xpath("//table[contains(@id, 'peptideMap')]"));
        ensureComparisonPlots("CTCF");

        log("Testing chart interactivity");
        WebElement height= getDriver().findElement(By.xpath("//input[contains(@id, 'chartHeight-inputEl')]"));
        height.clear();
        height.sendKeys("200");
        WebElement width= getDriver().findElement(By.xpath("//input[contains(@id, 'chartWidth-inputEl')]"));
        width.clear();
        width.sendKeys("500");
        clickButton("Update",0);
        ensureComparisonPlots("CTCF");
    }

    @LogMethod
    private void verifyAndResolveConflicts()
    {
        clickTab("Panorama Dashboard");
        log("Verifying that expected conflicts exist");
        String[] conflictText = new String[] {"The last Skyline document imported in this folder had 4 proteins that were already a part of the library",
                "Please click the link below to resolve conflicts and choose the version of each protein that should be included in the library",
                "The library cannot be extended until the conflicts are resolved. The download link below is for the last stable version of the library"};
        assertTextPresent(conflictText);

        var resolveConflictsLink = Locator.tagWithClass("div", "labkey-download").descendant(Locator.linkWithText("RESOLVE CONFLICTS"));
        assertElementPresent(resolveConflictsLink);

        verifyConflictsAsReadOnlyUser();

        clickAndWait(resolveConflictsLink);
        assertTextPresent(
                "Conflicting Proteins in Document",
                "Current Library Proteins",
                "Resolve conflicts for " + SKY_FILE2 + ".");

        int expectedConflictCount = 4;
        assertEquals(expectedConflictCount + 2 /*add header rows*/, getTableRowCount("dataTable"));

        var expectedRows = new HashMap<String, List<String>>();
        expectedRows.put("MAX", Arrays.asList(SKY_FILE2, "MAX", SKY_FILE1));
        expectedRows.put("HPRR1440042", Arrays.asList(SKY_FILE2, "HPRR1440042", SKY_FILE1));
        expectedRows.put("DifferentProteinSameLabel", Arrays.asList(SKY_FILE2, "DifferentProteinSameLabel", SKY_FILE1));
        expectedRows.put("HPRR5000001", Arrays.asList(SKY_FILE2, "QPrEST_CystC_HPRR5000001", SKY_FILE1));

        // Verify rows in the conflicts table
        Locator.XPathLocator table = Locator.id("dataTable");
        for(int i = 0; i < expectedConflictCount; i++)
        {
            String proteinInDocument = getTableCellText(table, i, 2);
            List<String> expectedRowValues = expectedRows.get(proteinInDocument);
            assertNotNull("Unexpected row for protein " + proteinInDocument, expectedRowValues);
            assertEquals("Unexpected document name for conflicting protein " + proteinInDocument, expectedRowValues.get(0), getTableCellText(table, i, 3));
            assertEquals("Unexpected current library protein name", expectedRowValues.get(1), getTableCellText(table, i, 6));
            assertEquals("Unexpected document name for current library protein " + expectedRowValues.get(1), expectedRowValues.get(2), getTableCellText(table, i, 7));
        }

        // Select a protein from the older document to keep in the library. Test that checking / unchecking checkboxes works
        var proteinName = "MAX";
        var oldProteinCb = getCheckBox(proteinName, true).waitForElement(shortWait());
        var newProteinCb = getCheckBox(proteinName, false).waitForElement(shortWait());
        assertTrue(isChecked(newProteinCb));   // checkbox for the protein in the new document should be checked
        assertFalse(isChecked(oldProteinCb));  // checkbox for the protein in the old document should be unchecked
        // check, uncheck and check again (See Issue 44424)
        changeChecked(oldProteinCb, newProteinCb , true, proteinName);
        changeChecked(oldProteinCb, newProteinCb , false, proteinName);
        changeChecked(oldProteinCb, newProteinCb , true, proteinName);

        clickButton("Apply Changes");
    }

    private void changeChecked(WebElement oldProteinCb, WebElement newProteinCb, boolean selectOld, String proteinName)
    {
        setCheckbox(oldProteinCb, selectOld);
        if (selectOld)
        {
            assertFalse("Expected " + proteinName + " from the new document to be unchecked", isChecked(newProteinCb));  // new protein unchecked
            assertTrue("Expected " + proteinName + " from the old document to be checked", isChecked(oldProteinCb));   // old protein checked
        }
        else
        {
            assertTrue("Expected " + proteinName + " from the new document to be checked", isChecked(newProteinCb));  // new protein checked
            assertFalse("Expected " + proteinName + " from the old document to be unchecked", isChecked(oldProteinCb));   // old protein unchecked
        }
    }

    private Locator getCheckBox(String proteinName, boolean oldProtein)
    {
        var cls = oldProtein ? "oldProtein" : "newProtein";
        // Example xpath:
        // //td[contains(@class, 'oldProtein')][span[contains(text(), 'MAX')]]/preceding-sibling::td[contains(@class, 'oldProtein')][input[@type='checkbox']]/input[contains(@name, 'selectedVals')]
        return Locator.tag("td").withClass(cls).withChild(Locator.XPathLocator.tag("span").withText(proteinName))
                      .precedingSibling("td").withClass(cls).withChild(Locator.XPathLocator.checkbox())
                      .child(Locator.XPathLocator.checkbox().withAttribute("name", "selectedVals"));
    }

    private void verifyConflictsAsReadOnlyUser()
    {
        impersonateRole("Reader");
        String[] conflictText = new String[] {"The chromatogram library in this folder is in a conflicted state and is awaiting action from a folder administrator to resolve the conflicts",
                "The download link below is for the last stable version of the library."};
        assertTextPresent(conflictText);
        stopImpersonating(false);
    }

    @LogMethod
    private void verifyRevision3()
    {
        log("Verifying expected protein/peptide counts in library revision 3 after resolving conflicts");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(10, 89, 586, 3);

        assertTextPresent("Archived Revisions");

        // Expected proteins in library:
        //  CTCF                        -> SKY_FILE1
        //  TAF11                       -> SKY_FILE1
        //  MAX                         -> SKY_FILE1 (conflict resolved; protein from older document was selected to keep in the library)
        //  HPRR1440042                 -> SKY_FILE2 (conflict resolved)
        //  QPrEST_CystC_HPRR5000001    -> SKY_FILE2 (conflict resolved)
        //  DifferentProteinSameLabel   -> SKY_FILE2 (conflict resolved)
        //  TP53                        -> SKY_FILE2
        //  GATA3                       -> SKY_FILE2
        //  HPRR350065                  -> SKY_FILE2
        //  iRT-C18 Standard Peptides   -> SKY_FILE2

        clickTab("Proteins");
        List<String> proteins = Arrays.asList( /*Proteins from SKY_FILE1*/ "CTCF", "TAF11","MAX",
                /*Proteins from SKY_FILE2*/ "TP53", "GATA3", "HPRR350065", "HPRR1440042", "HPRR5000001", "DifferentProteinSameLabel",
                /*iRT peptides; always from the latest file (SKY_FILE2)*/"iRT-C18 Standard Peptides");
        List<String> files = Arrays.asList(SKY_FILE1, SKY_FILE1, SKY_FILE1,
                SKY_FILE2, SKY_FILE2, SKY_FILE2, SKY_FILE2, SKY_FILE2, SKY_FILE2,
                SKY_FILE2);

        verifyLibraryProteins(proteins, files, 15);
    }
}
