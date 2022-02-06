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

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSPeptideLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "Stergachis-SupplementaryData_2_a.sky.zip";
    private static final String SKY_FILE2 = "Stergachis-SupplementaryData_2_b.sky.zip";

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Library); // Peptide library folder
        importData(SKY_FILE1);
        verifyRevision1();
        importData(SKY_FILE2, 2);
        verifyRevision2();
        verifyAndResolveConflicts();
        verifyRevision3();
        verifyDocumentLibraryView();
        deleteSkyFile(SKY_FILE2);
        verifyRevision4();
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
        log("Verifying expected counts in library revision 1 after uploading " + SKY_FILE1);

        int libraryPeptideCount = 55;
        int libraryPrecursorCount = 55;
        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(libraryPeptideCount, 343, 1);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(libraryPeptideCount);

        // Verify one precursor from some of the proteins in the library.  All are from SKY_FILE1 at this point.
        Map<String, Pair<String, String>> precursorMap = new HashMap<>();
        precursorMap.put("AHHNALER",           new Pair(SKY_FILE1, "MAX"));
        precursorMap.put("ALDFAVGEYNK[+8.0]",  new Pair(SKY_FILE1, "QPrEST_CystC_HPRR5000001"));
        precursorMap.put("QFVYLESDYSK[+8.0]",  new Pair(SKY_FILE1, "HPRR1440042"));
        precursorMap.put("ADVTPADFSEWSK",      new Pair(SKY_FILE1, "iRT-C18 Standard Peptides"));
        // The precursors below are only in SKY_FILE1 so will not result in a conflict. We can leave them out otherwise the test takes too long to run.
        // precursorMap.put("DPDYQPPAK",          new Pair(SKY_FILE1, "CTCF"));
        // precursorMap.put("EDSSLLNPAAK",        new Pair(SKY_FILE1, "TAF11"));
        // precursorMap.put("GEPGEGAYVYR[+10.0]", new Pair(SKY_FILE1, "DifferentProteinSameLabel"));

        verifyLibraryPrecursors(precursorMap, libraryPrecursorCount,
                libraryPrecursorCount // total precursor count is the same as the library precursor count since there is only one document in the folder.
        );
    }

    private void verifyLibraryPeptideCount(int totalPeptideCount)
    {
        clickTab("Peptides");
        log("Verify peptide count in the library");
        DataRegionTable peptidesTable = new DataRegionTable("Peptide",getDriver());
        assertEquals("Unexpected number of rows in peptides table", totalPeptideCount, peptidesTable.getDataRowCount());
    }

    private void verifyLibraryPrecursors(Map<String, Pair<String, String>> precursorMap, int libraryPrecursorCount, int totalPrecursorCount)
    {
        log("Verify precursors in the library");

        DataRegionTable precursorTable = new DataRegionTable("Precursor",getDriver());
        if (precursorTable.getPagingWidget().hasPagingButton(false))
        {
            precursorTable.getPagingWidget().clickShowAll();
        }
        // The library view is displayed, so the number of rows should be equal to the library precursor count.
        assertEquals("Unexpected number of rows in the precursors table (library view)", libraryPrecursorCount, precursorTable.getDataRowCount());

        // The grid cannot be customized until the "View All" button is clicked, or the title is clicked.
        precursorTable.clickHeaderButtonAndWait("View All");
        // After the button is clicked the "default" view for the grid will be displayed.
        if (precursorTable.getPagingWidget().hasPagingButton(false))
        {
            precursorTable.getPagingWidget().clickShowAll();
        }
        // In the default view, all the precursors from all the documents are displayed.
        assertEquals("Unexpected number of rows in the precursors table (default view)", totalPrecursorCount, precursorTable.getDataRowCount());

        // Switch to the "Library Precursors" view
        precursorTable.goToView("Library Precursors");
        CustomizeView customizeView = precursorTable.openCustomizeGrid();
        customizeView.addColumn("ModifiedSequence");
        customizeView.applyCustomView();
        if(libraryPrecursorCount > 100)
        {
            precursorTable.getPagingWidget().setPageSize(250, true);
        }

        List<String> colNames = precursorTable.getColumnNames();
        String x = colNames.get(0);
        assertEquals("Unexpected number of rows in the precursors table (library view)", libraryPrecursorCount, precursorTable.getDataRowCount());

        for(Map.Entry<String, Pair<String, String>> entry: precursorMap.entrySet())
        {
            String precursor = entry.getKey();
            int idx = precursorTable.getRowIndex("Modified Precursor", precursor);
            assertTrue("Expected precursor " + precursor + " not found in table", idx != -1);
            List<String>fileNameAndProtein = precursorTable.getRowDataAsText(idx, "File", "Protein / Label");
            assertEquals(2, fileNameAndProtein.size());
            assertEquals("Unexpected file name for " + precursor, entry.getValue().first, fileNameAndProtein.get(0));
            assertEquals("Unexpected protein name for " + precursor, entry.getValue().second, fileNameAndProtein.get(1));
        }

        // After the "Library Precursors" view is modified, its name in the menu appears as "LibraryPrecursors" (no space) so the
        // next time this method is called the test will fail because it will not be able to find the "Library Precursors" view.
        // Revert changes to the view.
        precursorTable.getCustomizeView().revertUnsavedViewGridClosed();
    }

    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected counts in library revision 2 after uploading " + SKY_FILE2);

        int libraryPeptideCount = 97;
        int libraryPrecursorCount = 103;

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        // The folder has conflicts so the download link and the numbers should be for the last stable library built
        // before conflicts, which is revision 1.
        verifyChromatogramLibraryDownloadWebPart(55, 343, 1, true);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(libraryPeptideCount);

        // Verify one precursor from some of the proteins in the library.
        // From SKY_FILE1
        Map<String, Pair<String, String>> precursorMap = new HashMap<>();
        precursorMap.put("AHHNALER",           new Pair(SKY_FILE1, "MAX")); // Conflicted in SKY_FILE2
        precursorMap.put("ALDFAVGEYNK[+8.0]",  new Pair(SKY_FILE1, "QPrEST_CystC_HPRR5000001")); // Conflicted in SKY_FILE2
        precursorMap.put("QFVYLESDYSK[+8.0]",  new Pair(SKY_FILE1, "HPRR1440042")); // Conflicted in SKY_FILE2


        // The precursors below are only in SKY_FILE2 so will not result in a conflict. We can leave them out otherwise the test takes too long to run.
        // FROM SKY_FILE2
        // precursorMap.put("FLVSLALR",           new Pair(SKY_FILE2, "DifferentProteinSameLabel"));
        // precursorMap.put("ALGSHHTASPWNLSPFSK", new Pair(SKY_FILE2, "GATA3"));
        // precursorMap.put("AGTLDLSLTVQGK",      new Pair(SKY_FILE2, "HPRR350065"));
        // precursorMap.put("AHSSHLK",            new Pair(SKY_FILE2, "TP53"));

        precursorMap.put("ADVTPADFSEWSK",      new Pair(SKY_FILE2, "iRT-C18 Standard Peptides")); // iRT peptide should always be from the new file

        // SKY_FILE2 has both the heavy and light versions of QFVYLESDYSK.
        // heavy version (QFVYLESDYSK[+8.0]) is conflicted as it also in SKY_FILE1
        // There are two entries for the peptide QFVYLESDYSK in the library at this point
        // QFVYLESDYSK -> QFVYLESDYSK[+8.0] -> heavy precursor from SKY_FILE1
        // QFVYLESDYSK -> QFVYLESDYSK       -> light precursor from SKY_FILE2
        precursorMap.put("QFVYLESDYSK",        new Pair(SKY_FILE2, "HPRR1440042"));

        verifyLibraryPrecursors(precursorMap, libraryPrecursorCount,
                124 // total precursor count includes all the precursors from all the documents in the folder
        );
    }

    private void verifyChromatogramLibraryDownloadWebPart(int peptideCount, int transitionCount, int revision)
    {
        verifyChromatogramLibraryDownloadWebPart(peptideCount, transitionCount, revision, false);
    }

    private void verifyChromatogramLibraryDownloadWebPart(int peptideCount, int transitionCount, int revision, boolean hasConflict)
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
                peptideCount + " peptides",
                transitionCount + " ranked transitions");
        assertElementPresent(Locator.lkButton("Download"));
        assertTextPresent("Revision " + revision);
    }

    @LogMethod
    private void verifyAndResolveConflicts()
    {
        clickTab("Panorama Dashboard");
        log("Verifying that expected conflicts exist");
        String[] conflictText = new String[] {"The last Skyline document imported in this folder had 10 peptides that were already a part of the library",
                "Please click the link below to resolve conflicts and choose the version of each peptide that should be included in the library",
                "The library cannot be extended until the conflicts are resolved. The download link below is for the last stable version of the library"};
        assertTextPresent(conflictText);
        var resolveConflictsLink = Locator.tagWithClass("div", "labkey-download").descendant(Locator.linkWithText("RESOLVE CONFLICTS"));
        assertElementPresent(resolveConflictsLink);
        clickAndWait(resolveConflictsLink);
        assertTextPresent(
                "Newly Imported Data",
                "Current Library Data",
                "Resolve conflicts for " + SKY_FILE2 + ".");

        int expectedConflictCount = 10;
        assertEquals(expectedConflictCount + 2 /*add header rows*/, getTableRowCount("dataTable"));

        Set<String> expectedConflicts = new HashSet<>();
        expectedConflicts.add("MSDNDDIEVESDADK++");
        expectedConflicts.add("AHHNALER++");
        expectedConflicts.add("DSVPSLQGEK++");
        expectedConflicts.add("ATEYIQYMR++");
        expectedConflicts.add("QNALLEQQVR++");
        expectedConflicts.add("SSAQLQTNYPSSDNSLYTNAK++");
        expectedConflicts.add("SVNASNYGLSPDR+++");
        expectedConflicts.add("QFVYLESDYSK++");
        expectedConflicts.add("YSYTATYYIYDLSNGEFVR+++");
        expectedConflicts.add("ALDFAVGEYNK++");

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

        int libraryPeptideCount = 93;
        int libraryPrecursorCount = 103;

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(libraryPeptideCount, 607, 3);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(libraryPeptideCount);

        // Verify one precursor from each protein in the library.
        // FROM SKY_FILE1
        Map<String, Pair<String, String>> precursorMap = new HashMap<>();

        // After resolving conflicts
        precursorMap.put("AHHNALER",           new Pair(SKY_FILE2, "MAX"));         // Now from SKY_FILE2
        precursorMap.put("MSDNDDIEVESDADK",    new Pair(SKY_FILE2, "MAX"));         // Now from SKY_FILE2
        // Both heavy and light precursors of the peptides ALDFAVGEYNK and QFVYLESDYSK are now from SKY_FILE2
        precursorMap.put("ALDFAVGEYNK[+8.0]",  new Pair(SKY_FILE2, "HPRR5000001")); // Now from SKY_FILE2
        precursorMap.put("ALDFAVGEYNK",        new Pair(SKY_FILE2, "HPRR5000001"));
        precursorMap.put("QFVYLESDYSK[+8.0]",  new Pair(SKY_FILE2, "HPRR1440042")); // Now from SKY_FILE2
        precursorMap.put("QFVYLESDYSK",        new Pair(SKY_FILE2, "HPRR1440042"));

        precursorMap.put("ADVTPADFSEWSK",      new Pair(SKY_FILE2, "iRT-C18 Standard Peptides")); // iRT peptide should always be from the new file

        verifyLibraryPrecursors(precursorMap, libraryPrecursorCount,
                124 // total precursor count includes all the precursors from all the documents in the folder
        );
    }

    protected void verifyRevision4()
    {
        log("Verifying expected counts in library revision 4 after deleting " + SKY_FILE2);

        int libraryPeptideCount = 55;
        int libraryPrecursorCount = 55;
        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(libraryPeptideCount, 343, 4);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(libraryPeptideCount);

        // All are from SKY_FILE1 after deleting SKY_FILE2.
        Map<String, Pair<String, String>> precursorMap = new HashMap<>();
        precursorMap.put("AHHNALER",           new Pair(SKY_FILE1, "MAX"));
        precursorMap.put("ALDFAVGEYNK[+8.0]",  new Pair(SKY_FILE1, "QPrEST_CystC_HPRR5000001"));
        precursorMap.put("QFVYLESDYSK[+8.0]",  new Pair(SKY_FILE1, "HPRR1440042"));
        precursorMap.put("ADVTPADFSEWSK",      new Pair(SKY_FILE1, "iRT-C18 Standard Peptides"));

        verifyLibraryPrecursors(precursorMap, libraryPrecursorCount,
                libraryPrecursorCount // total precursor count is the same as the library precursor count since there is only one document in the folder
        );
    }

    private void verifyDocumentLibraryView()
    {
        goToDashboard();
        clickAndWait(Locator.linkContainingText(SKY_FILE1));
        var precursorTable = new DataRegionTable("precursors_view" ,getDriver());
        var peptideGroups = getPeptideGroupsInGrid(precursorTable);
        assertEquals(List.of("CTCF", "TAF11", "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "DifferentProteinSameLabel", "iRT-C18 Standard Peptides"), peptideGroups);

        // Switch to the library view. We should only see the proteins from this document that are in the current library.
        precursorTable.goToView("Library Members");
        peptideGroups = getPeptideGroupsInGrid(precursorTable);
        // "MAX", "QPrEST_CystC_HPRR5000001", "HPRR1440042", "iRT-C18 Standard Peptides" in the library are not from this document anymore
        // so they will not be displayed in the "Library Members" view.
        assertEquals(List.of("CTCF", "TAF11", "DifferentProteinSameLabel"), peptideGroups);
    }

    @NotNull
    private List<String> getPeptideGroupsInGrid(DataRegionTable precursorTable)
    {
        // TODO: Delete this method when there is a DataRegionTable for nested grids.
        // DataRegionTable methods do not work correctly in a nested grid so we will iterate through the rows and get
        // the text in the "Protein / Label" column of the grid.
        var peptideGroups = new ArrayList<String>();
        int incr = 2;
        for (var i = 0; i < precursorTable.getDataRowCount(); i+=incr, incr = incr == 2 ? 1 : 2)
        {
            // Each protein row has a nested row. Rows with class "labkey-alternate-row" have a nested row with the same class.
            // However, rows with class "labkey-row" have a nested row without a class attribute (TODO: add class attribute to nested rows)
            // so they don't get counted in DataRegionTable.getDataRows().
            // To get the protein names, we have to skip every other nested row. The very first row in the grid has the "labkey-alternate-row" class.
            peptideGroups.add(precursorTable.getDataAsText(i, 0));
        }
        return peptideGroups;
    }
}
