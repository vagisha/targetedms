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
import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

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

        int totalPeptideCount = 55;
        int totalPrecursorCount = 55;
        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(totalPeptideCount, 343, 1);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(totalPeptideCount);

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

        verifyLibraryPrecursors(precursorMap, totalPrecursorCount);
    }

    private void verifyLibraryPeptideCount(int totalPeptideCount)
    {
        clickTab("Peptides");
        log("Verify peptide count in the library");
        DataRegionTable peptidesTable = new DataRegionTable("Peptide",getDriver());
        assertEquals("Unexpected number of rows in peptides table", totalPeptideCount, peptidesTable.getDataRowCount());
    }

    private void verifyLibraryPrecursors(Map<String, Pair<String, String>> precursorMap, int totalPrecursorCount)
    {
        log("Verify precursors in the library");

        // The grid in the "Library Precursors" webpart cannot be customized so click the title of the webpart to display the
        // full, customizable query view.
        var webpart = new PortalHelper(getDriver()).getBodyWebPart("Library Precursors");
        webpart.getComponentElement().click();
        var panelTitleLink = Locators.panelWebpartTitle.withText("Library Precursors").parent("a").withAttribute("href").findElement(getDriver());
        clickAndWait(panelTitleLink);
        DataRegionTable precursorTable = new DataRegionTable("query", this);
        CustomizeView customizeView = precursorTable.openCustomizeGrid();
        customizeView.addColumn("ModifiedSequence");
        customizeView.applyCustomView();
        if(totalPrecursorCount > 100)
        {
            precursorTable.getPagingWidget().setPageSize(250, true);
        }

        List<String> colNames = precursorTable.getColumnNames();
        String x = colNames.get(0);
        assertEquals("Unexpected number of rows in precursors table", totalPrecursorCount, precursorTable.getDataRowCount());

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
    }

    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected counts in library revision 2 after uploading " + SKY_FILE2);

        int totalPeptideCount = 97;
        int totalPrecursorCount = 103;

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        // The folder has conflicts so the download link and the numbers should be for the last stable library built
        // before conflicts, which is revision 1.
        verifyChromatogramLibraryDownloadWebPart(55, 343, 1, true);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(totalPeptideCount);

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

        verifyLibraryPrecursors(precursorMap, totalPrecursorCount);
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

        int totalPeptideCount = 93;
        int totalPrecursorCount = 103;

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(totalPeptideCount, 607, 3);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(totalPeptideCount);

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

        verifyLibraryPrecursors(precursorMap, totalPrecursorCount);
    }

    protected void verifyRevision4()
    {
        log("Verifying expected counts in library revision 4 after deleting " + SKY_FILE2);

        int totalPeptideCount = 55;
        int totalPrecursorCount = 55;
        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(totalPeptideCount, 343, 4);

        // Verify the number of rows in the Peptides table
        verifyLibraryPeptideCount(totalPeptideCount);

        // All are from SKY_FILE1 after deleting SKY_FILE2.
        Map<String, Pair<String, String>> precursorMap = new HashMap<>();
        precursorMap.put("AHHNALER",           new Pair(SKY_FILE1, "MAX"));
        precursorMap.put("ALDFAVGEYNK[+8.0]",  new Pair(SKY_FILE1, "QPrEST_CystC_HPRR5000001"));
        precursorMap.put("QFVYLESDYSK[+8.0]",  new Pair(SKY_FILE1, "HPRR1440042"));
        precursorMap.put("ADVTPADFSEWSK",      new Pair(SKY_FILE1, "iRT-C18 Standard Peptides"));

        verifyLibraryPrecursors(precursorMap, totalPrecursorCount);
    }
}
