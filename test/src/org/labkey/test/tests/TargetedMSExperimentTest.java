/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

import static org.junit.Assert.*;

@Category({DailyB.class, MS2.class})
public class TargetedMSExperimentTest extends TargetedMSTest
{
    private static final String SKY_FILE = "MRMer.zip";
    private static final String SKY_FILE2 = "MRMer_renamed_protein.zip";

    public TargetedMSExperimentTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);
        verifyImportedData();
        verifyModificationSearch();
        importData(SKY_FILE2, 2);
        verifyProteinSearch();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyImportedData()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCounts(24,44,88,296); // Number of protein, peptides, precursors, transitions
        verifyDocumentDetails();
        verifyPeptide();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyDocumentDetails()
    {
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cycle) or anaerobic (glucose fermentation) respiration");
        // Verify expected peptides/proteins in the nested view
        //Verify that amino acids from peptides are highlighted in blue as expected.
        assertElementPresent(Locator.xpath("//tr//td//a[span[text()='LTSLNVVAGSDL'][span[contains(@style,'font-weight:bold;color:#0000ff;') and text()='R']]]"));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyProteinSearch()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        assertTextPresent("Mass Spec Search");
        assertTextPresent("Protein Search");
        _ext4Helper.clickExt4Tab("Protein Search");
        waitForElement(Locator.name("identifier"));

        // Test fix for issue 18217
        // MRMer.zip contains the protein YAL038W.  MRMer_renamed_protein.zip contains the same protein with the
        // name YAL038W_renamed.  MRMer.zip is imported first and  a new entry is created in prot.sequences with YAL038W
        // as the bestname. MRMer_renamed_protein is imported second, and an entry is created in prot.identifiers
        // for YAL038W_renamed. A search for YAL038W_renamed should return one protein result.
        setFormElement(Locator.name("identifier"), "YAL038W_renamed");
        waitAndClickAndWait(Locator.navButton("Search"));
        waitForText("Protein Search Results");
        //waitForText("1 - 7 of 7");
        assertTextPresentInThisOrder("Protein Search", "Matching Proteins (1)", "Targeted MS Peptides");
        assertEquals(1, getElementCount(Locator.xpath("id('dataregion_PotentialProteins')/tbody/tr/td/a[contains(text(),'YAL038W')]")));
        assertEquals(7, getElementCount( Locator.xpath("//td/span/a[contains(text(), 'YAL038W')]")));
        assertEquals(1, getElementCount(Locator.xpath("//td/span/a[contains(text(), 'YAL038W_renamed')]")));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyModificationSearch()
    {
        // add modificaiton search webpart and do an initial search by AminoAcid and DeltaMass
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        waitForElement(Locator.id("identifierInput"));
        _ext4Helper.clickExt4Tab("Modification Search");
        waitForElement(Locator.name("aminoAcids"));
        setFormElement(Locator.name("aminoAcids"), "R");
        setFormElement(Locator.name("deltaMass"), "10");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        waitForText("Modification Search Results");
        //waitForText("1 - 13 of 13");
        assertTextPresentInThisOrder("Targeted MS Modification Search", "Targeted MS Peptides");
        assertTextPresent("Amino Acids:", "Delta Mass:");
        assertEquals(13, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));

        // search for K[+8] modification
        setFormElement(Locator.name("aminoAcids"), "k R, N"); // should be split into just chars
        setFormElement(Locator.name("deltaMass"), "8.01"); // should be rounded to a whole number
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(31, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));

        // test custom name search type
        _ext4Helper.selectRadioButton("Search By:", "Modification Name");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("unimodName"));
        assertElementVisible(Locator.name("customName"));
        _ext4Helper.selectRadioButton("Type:", "Names used in imported experiments");
        _ext4Helper.selectComboBoxItem("Custom Name:", "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        assertEquals(13, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));
        _ext4Helper.selectComboBoxItem("Custom Name:", "Label:13C(6)15N(2) (C-term K)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(31, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));

        // test unimod name search type
        _ext4Helper.selectRadioButton("Type:", "All Unimod modifications");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("customName"));
        assertElementVisible(Locator.name("unimodName"));
        _ext4Helper.selectComboBoxItem(Ext4Helper.Locators.formItemWithLabelContaining("Unimod Name:"), "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        assertEquals(13, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));

        // test C-term search using special character (i.e. ] )
        _ext4Helper.selectRadioButton("Search By:", "Delta Mass");
        setFormElement(Locator.name("aminoAcids"), "]");
        setFormElement(Locator.name("deltaMass"), "8");
        waitAndClickAndWait(Locator.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'R[+10]')]")));
        assertEquals(31, getElementCount( Locator.xpath("//td//a/span[contains(@title, 'K[+8]')]")));
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyPeptide()
    {
        // Click on a peptide.
        String targetProtein  = "LTSLNVVAGSDLR";
        clickAndWait(Locator.linkContainingText(targetProtein));
        //Verify it’s associated with the right protein and other values from details view.
        //protein name, portien, neutral mass, avg. RT , precursor
        assertTextPresent(targetProtein, "YAL038W", "1343.7408", "27.9232", "677.8818++ (heavy)");

        //Verify the spectrum shows up correctly.

        //Verify we get the expected number of chromatogram graphs.
        assertElementPresent(Locator.xpath("//img[contains(@src, 'peptideChromatogramChart.view')]"),1);
        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"),2);
        assertElementPresent(Locator.xpath("//img[contains(@alt, 'Chromatogram')]"), 3);

        //Click on a precursor icon link.
        clickAndWait(Locator.linkWithHref("precursorAllChromatogramsChart.view?"));
        //Verify expected values in detail view. Verify chromatogram.
        assertTextPresentInThisOrder("Precursor Chromatograms", "YAL038W",  "LTSLNVVAGSDLR", "672.8777");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"));

        goBack();
        clickAndWait(Locator.linkContainingText("YAL038W"));
        //Verify summary info
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate,");

        assertTextPresent("Sequence Coverage", "Peptides", "LTSLNVVAGSDLR", "TNNPETLVALR", "GVNLPGTDVDLPALSEK", "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR",
                "Peak Areas");

        goBack();
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        //Toggle to Transition view (click on down arrow in Precursor List webpart header)
        click(Locator.xpath("//th[span[contains(text(), 'Precursor List')]]/span/a/img"));
        clickAndWait(Locator.tagContainingText("span","Transition List"));
        waitForText("Transition List");
        DataRegionTable drt = new DataRegionTable("transitions_view", this);
        drt.getDataAsText(5, "Label");
        assertEquals("heavy", drt.getDataAsText(5, "Label"));
        assertEquals("1353.7491", drt.getDataAsText(5, "Precursor Neutral Mass"));
        assertEquals("677.8818", drt.getDataAsText(5, "Q1 m/z"));
        assertEquals("y7", drt.getDataAsText(5, "Fragment"));
        assertEquals("727.3973", drt.getDataAsText(5, "Q3 m/z"));
        // We don't find these values based on their column headers because DataRegionTable gets confused with the
        // nested data regions having the same id in the HTML. The checks above happen to work because
        // they correspond to columns that aren't in the parent table, so the XPath flips to the second table with
        // that id, which has enough columns to satisfy the Locator
        assertTextPresent("1343.7408", "1226.6619", "1001.5505");

        //Click down arrow next to protein name. Click "Search for other references to this protein"
        Locator l = Locator.xpath("//span[a[text()='YAL038W']]/span/img");
        waitForElement(l);
        mouseOver(l);
        waitForText("Search for other references to this protein");
        clickAndWait(Locator.linkContainingText("Search for other references to this protein"));

        //Verify Targeted MS Peptides section of page.
        //Click on Details link.
        //Spot check some values.
        assertTextPresent("Protein Search Results", "Targeted MS Peptides","LTSLNVVAGSDLR",
               "TNNPETLVALR",  "GVNLPGTDVDLPALSEK",  "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR");
        click(Locator.imageWithSrc("plus.gif", true));
        assertTextPresent("I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cyc...");
    }
}
