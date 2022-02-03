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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSSmallMoleculeLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "SmMolLibA.sky.zip";
    private static final String SKY_FILE2 = "SmMolLibB.sky.zip";

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Library); // Library folder
        importData(SKY_FILE1);
        verifySingleFileLibrary(1);
        importData(SKY_FILE2, 2);
        verifyRevision2();
        verifyAndResolveConflicts();
        verifyRevision3();
        verifyDocumentLibraryView();
        deleteSkyFile(SKY_FILE2);
        verifySingleFileLibrary(4);
    }

    @Override
    protected void selectFolderType(FolderType folderType)
    {
        // Make sure that we're still in the wizard UI
        assertTextPresent("Create Project", "Users / Permissions");
        super.selectFolderType(folderType);
    }

    @LogMethod
    protected void verifySingleFileLibrary(int revision)
    {
        log("Verifying expected counts in library revision 1 after uploading " + SKY_FILE1);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(2, 8, revision, false);

        verifyLibraryMoleculeCount(2);

        // Verify one precursor from some of the molecules in the library.  All are from SKY_FILE1 at this point.
        Map<String, List<String>> precursorMap = new HashMap<>();
        precursorMap.put("120.0655", Arrays.asList("C4H9NO3", SKY_FILE1, "C4H9NO3[M+H]"));
        precursorMap.put("124.0789", Arrays.asList("C4H9NO3", SKY_FILE1, "C4H9NO3[M4C13+H]"));
        precursorMap.put("124.0393", Arrays.asList("NICOTINATE", SKY_FILE1, "[M+]"));

        verifyLibraryPrecursors(precursorMap, 4, 4);
    }

    private void verifyLibraryMoleculeCount(int count)
    {
        clickTab("Molecules");
        log("Verify molecule count in the library");
        DataRegionTable moleculeTable = new DataRegionTable("Molecule" ,getDriver());
        assertEquals("Unexpected number of rows in molecules table", count, moleculeTable.getDataRowCount());
    }

    private void verifyChromatogramLibraryDownloadWebPart(int moleculeCount, int transitionCount, int revision, boolean hasConflict)
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
                moleculeCount + " molecules",
                transitionCount + " ranked transitions");
        assertElementPresent(Locator.lkButton("Download"));
        assertTextPresent("Revision " + revision);
    }


    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected counts in library revision 2 after uploading " + SKY_FILE2);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        // The folder has conflicts so the download link and the numbers should be for the last stable library built
        // before conflicts, which is revision 1.
        verifyChromatogramLibraryDownloadWebPart(2,8, 1, true);

        verifyLibraryMoleculeCount(3);

        // Verify one precursors from some of the molecules in the library.
        Map<String,List<String>> precursorMap = new HashMap<>();
        // Conflicted
        precursorMap.put("120.0655", Arrays.asList("C4H9NO3", SKY_FILE1, "C4H9NO3[M+H]"));
        precursorMap.put("124.0789", Arrays.asList("C4H9NO3", SKY_FILE1, "C4H9NO3[M4C13+H]"));

        // The precursors below are only in SKY_FILE2 so will not result in a conflict.
        precursorMap.put("122.0270", Arrays.asList("CYSTEINE", SKY_FILE2, "[M+]"));
        precursorMap.put("125.0371", Arrays.asList("CYSTEINE", SKY_FILE2, "[M3.01007+]"));

        // Only in SKY_FILE1
        precursorMap.put("124.0393", Arrays.asList("NICOTINATE", SKY_FILE1, "[M+]"));

        verifyLibraryPrecursors(precursorMap, 6, 8);
    }

    private void verifyLibraryPrecursors(Map<String, List<String>> precursorMap, int libraryPrecursorCount, int totalPrecursorCount)
    {
        log("Verify precursors in the library");

        DataRegionTable precursorTable = new DataRegionTable("MoleculePrecursor" ,getDriver());
        if(libraryPrecursorCount > 100)
        {
            precursorTable.getPagingWidget().setPageSize(250, true);
        }

        assertEquals("Unexpected number of rows in precursors table", libraryPrecursorCount, precursorTable.getDataRowCount());

        for(Map.Entry<String, List<String>> entry: precursorMap.entrySet())
        {
            String mz = entry.getKey();
            int idx = precursorTable.getRowIndex("Q1 m/z", mz);
            assertTrue("Expected precursor with mz " + mz + " not found in table", idx != -1);
            List<String> rowValues = precursorTable.getRowDataAsText(idx, "Molecule", "File", "Ion Formula");
            assertEquals("Wrong data for row", entry.getValue(), rowValues);
        }

        // Click the "View All" button to display the default view of the grid, and check the total precursor count displayed.
        // This will include all the precursors from all the documents in the folder.
        precursorTable.clickHeaderButtonAndWait("View All");
        if (precursorTable.getPagingWidget().hasPagingButton(false))
        {
            precursorTable.getPagingWidget().clickShowAll();
        }
        assertEquals("Unexpected number of rows in precursors table (default view)", totalPrecursorCount, precursorTable.getDataRowCount());
    }


    @LogMethod
    private void verifyAndResolveConflicts()
    {
        clickTab("Panorama Dashboard");
        log("Verifying that expected conflicts exist");
        String[] conflictText = new String[] {"The last Skyline document imported in this folder had 1 molecule that were already a part of the library",
                "Please click the link below to resolve conflicts and choose the version of each molecule that should be included in the library",
                "The library cannot be extended until the conflicts are resolved. The download link below is for the last stable version of the library"};
        assertTextPresent(conflictText);
        var resolveConflictsLink = Locator.tagWithClass("div", "labkey-download").descendant(Locator.linkWithText("RESOLVE CONFLICTS"));
        assertElementPresent(resolveConflictsLink);
        clickAndWait(resolveConflictsLink);
        assertTextPresent(
                "Newly Imported Data",
                "Current Library Data",
                "Resolve conflicts for " + SKY_FILE2 + ".");

        int expectedConflictCount = 2;
        assertEquals(expectedConflictCount + 2 /*add header rows*/, getTableRowCount("dataTable"));

        Set<String> expectedConflicts = new HashSet<>();
        expectedConflicts.add("C4H9NO3[M+H] - 120.0655");
        expectedConflicts.add("C4H9NO3[M4C13+H] - 124.0789");

        // Verify rows in the conflicts table
        Locator.XPathLocator table = Locator.id("dataTable");
        for(int i = 0; i < expectedConflictCount; i++)
        {
            String conflict = getTableCellText(table, i, 2);
            assertTrue("Unexpected row in conflicts table " + conflict, expectedConflicts.contains(conflict));
        }

        clickButton("Apply Changes");
    }

    protected void verifyRevision3()
    {
        log("Verifying expected counts in library revision 3 after resolving conflicts. ");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(3, 12, 3, false);

        verifyLibraryMoleculeCount(3);

        Map<String, List<String>> precursorMap = new HashMap<>();
        precursorMap.put("120.0655", Arrays.asList("C4H9NO3", SKY_FILE2, "C4H9NO3[M+H]"));
        precursorMap.put("124.0789", Arrays.asList("C4H9NO3", SKY_FILE2, "C4H9NO3[M4C13+H]"));
        precursorMap.put("122.0270", Arrays.asList("CYSTEINE", SKY_FILE2, "[M+]"));
        precursorMap.put("125.0371", Arrays.asList("CYSTEINE", SKY_FILE2, "[M3.01007+]"));
        precursorMap.put("130.0594", Arrays.asList("NICOTINATE", SKY_FILE1, "[M6.02013+]"));

        verifyLibraryPrecursors(precursorMap, 6, 8);
    }

    private void verifyDocumentLibraryView()
    {
        goToDashboard();
        clickAndWait(Locator.linkContainingText(SKY_FILE1));
        var precursorTable = new DataRegionTable("small_mol_precursors_view" ,getDriver());
        // DataRegionTable methods do not work correctly in a nested grid so we will look for the expected molecule lists
        // and molecules are in the html source.
        assertTextPresentInThisOrder("Formulas", "C4H9NO3", "C4H9NO3", "NamesAndMzs", "NICOTINATE", "NICOTINATE");
        // Switch to the library view. We should see only the molecule lists and molecules that are in the current library.
        precursorTable.goToView("Library Members");
        assertTextPresentInThisOrder("NamesAndMzs", "NICOTINATE", "NICOTINATE");
        assertTextNotPresent("Formulas", "C4H9NO3", "C4H9NO3");
    }
}
