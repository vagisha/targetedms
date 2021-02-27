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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.Rowset;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.FilesWebPart;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class TargetedMSExperimentTest extends TargetedMSTest
{
    private static final String SKY_FILE = "MRMer.zip";
    private static final String SKY_FILE2 = "MRMer_renamed_protein.zip";

    private static final String SKY_FILE_SMALLMOL_PEP = "smallmol_plus_peptides.sky.zip";
    private static final String SKY_FILE_SKYD_14 = "SampleIdTest.sky.zip";

    private static final String SKY_FILE_AREA_RATIOS = "AreaRatioTestDoc.sky.zip";
    private static final String SKY_FILE_AREA_RATIOS_2 = "quantitativetest_dda.sky.zip";

    @Test
    public void testSteps() throws IOException, CommandException
    {
        setupFolder(FolderType.Experiment);
        int jobCount = 0;
        importData(SKY_FILE, ++jobCount);
        verifyImportedPeptideData();
        verifyModificationSearch();
        importData(SKY_FILE2, ++jobCount);
        verifyProteinSearch();
        verifyQueries();

        //small molecule
        importData(SKY_FILE_SMALLMOL_PEP, ++jobCount);
        verifyImportedSmallMoleculeData();
        verifyAttributeGroupIdCalcs();
        testRawFileLinks(SKY_FILE_SMALLMOL_PEP);

        // SKYD version 14
        importData(SKY_FILE_SKYD_14, ++jobCount);
        verifyInstrumentSerialNumber();

        // Test peak area ratio calculation
        importData(SKY_FILE_AREA_RATIOS, ++jobCount);
        importData(SKY_FILE_AREA_RATIOS_2, ++jobCount);
        verifyAreaRatios();
        verifyBestMassErrorPpm();
    }

    @LogMethod
    protected void verifyAttributeGroupIdCalcs() throws IOException, CommandException
    {
        Connection cn = createDefaultConnection(false);
        // Query for small molecule data
        SelectRowsCommand smallMoleculeCommand = new SelectRowsCommand("targetedms", "generalmoleculechrominfo");
        smallMoleculeCommand.setRequiredVersion(9.1);
        smallMoleculeCommand.setColumns(Arrays.asList("MoleculeId/CustomIonName", "SampleFileId", "PeakCountRatio", "RetentionTime", "ModifiedAreaProportion", "SampleFileId/ReplicateId/RunId", "MoleculeId/AttributeGroupId", "PeptideId/AttributeGroupId"));
        smallMoleculeCommand.addFilter("MoleculeId/AttributeGroupId", "C2X", Filter.Operator.EQUAL);
        smallMoleculeCommand.addFilter("SampleFileId/SampleName", "13417_02_WAA283_3805_071514", Filter.Operator.EQUAL);
        smallMoleculeCommand.setSorts(Collections.singletonList(new Sort("MoleculeId/CustomIonName")));
        SelectRowsResponse smallMoleculesResponse = smallMoleculeCommand.execute(cn, getCurrentContainerPath());
        // Verify proportions are calculated appropriately
        Rowset smallMoleculeRowSet = smallMoleculesResponse.getRowset();
        assertEquals("Wrong number of attribute group ID small molecule rows", 4, smallMoleculeRowSet.getSize());
        Row smallMoleculeRow = smallMoleculeRowSet.iterator().next();
        assertEquals("Wrong first molecule", "PC aa C24:0", smallMoleculeRow.getValue("MoleculeId/CustomIonName"));
        // Round to avoid floating point precision false positives
        assertEquals("Wrong first molecule proportion", 3475, Math.round(((Number)smallMoleculeRow.getValue("ModifiedAreaProportion")).doubleValue() * 10000));

        // Query for peptide data
        SelectRowsCommand peptideCommand = new SelectRowsCommand("targetedms", "generalmoleculechrominfo");
        peptideCommand.setRequiredVersion(9.1);
        peptideCommand.setColumns(Arrays.asList("PeptideId/PeptideModifiedSequence", "SampleFileId", "PeakCountRatio", "RetentionTime", "ModifiedAreaProportion", "PeptideId/AttributeGroupId"));
        peptideCommand.addFilter("PeptideId/AttributeGroupId", "YAL038WPeptide", Filter.Operator.EQUAL);
        peptideCommand.setSorts(Arrays.asList(new Sort("PeptideId/PeptideModifiedSequence")));
        SelectRowsResponse peptideResponse = peptideCommand.execute(cn, getCurrentContainerPath());
        Rowset peptideRowSet = peptideResponse.getRowset();
        // Verify proportions are calculated appropriately
        assertEquals("Wrong number of attribute group ID peptide rows", 3, peptideRowSet.getSize());
        Row peptideRow = peptideRowSet.iterator().next();
        assertEquals("Wrong first peptide", "GVNLPGTDVDLPALSEK", peptideRow.getValue("PeptideId/PeptideModifiedSequence"));
        // Round to avoid floating point precision false positives
        assertEquals("Wrong first peptide proportion", 2160, Math.round(((Number)peptideRow.getValue("ModifiedAreaProportion")).doubleValue() * 10000));
    }

    @LogMethod
    private void verifyInstrumentSerialNumber() throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "samplefile");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("ReplicateId/Name", "FilePath", "AcquiredTime", "InstrumentId", "InstrumentSerialNumber", "SampleId"));
        cmd.addFilter("InstrumentSerialNumber", "6147F", Filter.Operator.EQUAL);
        SelectRowsResponse response = cmd.execute(createDefaultConnection(false), getCurrentContainerPath());

        assertEquals("Matching row count", 1, response.getRowset().getSize());
        assertEquals("Matching FilePath", "E:\\skydata\\20110215_MikeB\\S_1.RAW?centroid_ms1=true", response.getRowset().iterator().next().getValue("FilePath"));
        assertEquals("Matching SampleId", "1:A,1", response.getRowset().iterator().next().getValue("SampleId"));
        assertEquals("Matching replicate name", "S_1", response.getRowset().iterator().next().getValue("ReplicateId/Name"));
    }

    @LogMethod
    protected void verifyImportedPeptideData()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCountsPep(24,44,0, 88,296, 1, 0, 0);
        verifyDocumentDetails(false);
        verifyPeptide();
    }

    @LogMethod
    protected void verifyImportedSmallMoleculeData()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE_SMALLMOL_PEP));
        verifyRunSummaryCountsSmallMol(27, 44, 98, 186, 394, 5, 0, 0); // Number of protein (groups), peptides, precursors, transitions, small molecules
        verifyDocumentDetails(true);
        verifyMolecule();
    }

    @LogMethod
    protected void verifyDocumentDetails(boolean smallMolPresent)
    {
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cycle) or anaerobic (glucose fermentation) respiration");
        // Verify expected peptides/proteins in the nested view
        //Verify that amino acids from peptides are highlighted in blue as expected.
        assertElementPresent(Locator.xpath("//tr//td//a//span[text()='LTSLNVVAGSDL'][span[contains(@style,'font-weight:bold;color:#0000ff;') and text()='R']]"));

        if(smallMolPresent)
        {
           assertTextPresent("Small Molecule Precursor List");
           assertTextPresent("Acylcarnitine");
        }
    }

    @LogMethod
    protected void verifyProteinSearch()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        assertTextPresent("Mass Spec Search", "Protein Search");
        _ext4Helper.clickExt4Tab("Protein Search");
        waitForElement(Locator.name("identifier"));

        // Test fix for issue 18217
        // MRMer.zip contains the protein YAL038W.  MRMer_renamed_protein.zip contains the same protein with the
        // name YAL038W_renamed.  MRMer.zip is imported first and  a new entry is created in prot.sequences with YAL038W
        // as the bestname. MRMer_renamed_protein is imported second, and an entry is created in prot.identifiers
        // for YAL038W_renamed. A search for YAL038W_renamed should return one protein result.
        setFormElement(Locator.name("identifier"), "YAL038W_renamed");
        waitAndClickAndWait(Locator.lkButton("Search"));
        waitForText("Protein Search Results");
        //waitForText("1 - 7 of 7");
        assertTextPresentInThisOrder("Protein Search", "Matching Proteins (1)", "Targeted MS Proteins");

        clickAndWait(Locator.id("expandCollapse-ProteinSearchProteinMatches"), 0); // Search results are hidden by default.
        DataRegionTable potentialProteins = new DataRegionTable("PotentialProteins", this);
        assertEquals(1, potentialProteins.getDataRowCount());
        String bestName = potentialProteins.getDataAsText(0, "BestName");
        assertTrue("Protein name didn't include 'YAL038W' :" + bestName, bestName.contains("YAL038W"));

        DataRegionTable targetedMSMatches = new DataRegionTable("TargetedMSMatches", this);
        List<String> labels = targetedMSMatches.getColumnDataAsText("Protein / Label");
        assertEquals(0, labels.lastIndexOf("YAL038W"));
        assertEquals(1, labels.indexOf("YAL038W_renamed"));
    }

    @LogMethod
    protected void verifyModificationSearch()
    {
        // add modification search webpart and do an initial search by AminoAcid and DeltaMass
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        waitForElement(Locator.id("identifierInput"));
        _ext4Helper.clickExt4Tab("Modification Search");
        waitForElement(Locator.name("aminoAcids"));
        setFormElement(Locator.name("aminoAcids"), "R");
        setFormElement(Locator.name("deltaMass"), "10");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        waitForText("Modification Search Results");
        //waitForText("1 - 13 of 13");
        assertTextPresentInThisOrder("Targeted MS Modification Search", "Targeted MS Peptides");
        assertTextPresent("Amino acids:", "Delta mass:");
        assertEquals(13, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());

        // search for K[+8] modification
        setFormElement(Locator.name("aminoAcids"), "k R, N"); // should be split into just chars
        setFormElement(Locator.name("deltaMass"), "8.01"); // should be rounded to a whole number
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(31, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());

        // test custom name search type
        _ext4Helper.selectRadioButton("Search by:", "Modification name");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("unimodName"));
        assertElementVisible(Locator.name("customName"));
        _ext4Helper.selectRadioButton("Type:", "Names used in imported experiments");
        _ext4Helper.selectComboBoxItem("Custom name:", "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        assertEquals(13, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());
        setFormElement(Locator.name("customName"), "Label:13C(6)15N(2) (C-term K)"); // test timing fix, instead of using _ext4Helper.selectComboBoxItem again
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(31, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());

        // test unimod name search type
        _ext4Helper.selectRadioButton("Type:", "All Unimod modifications");
        assertElementNotVisible(Locator.name("aminoAcids"));
        assertElementNotVisible(Locator.name("deltaMass"));
        assertElementNotVisible(Locator.name("customName"));
        assertElementVisible(Locator.name("unimodName"));
        _ext4Helper.selectComboBoxItem(Ext4Helper.Locators.formItemWithLabelContaining("Unimod name:"), "Label:13C(6)15N(4) (C-term R)");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        //waitForText("1 - 13 of 13");
        assertEquals(13, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());

        // test C-term search using special character (i.e. ] )
        _ext4Helper.selectRadioButton("Search by:", "Delta mass");
        setFormElement(Locator.name("aminoAcids"), "]");
        setFormElement(Locator.name("deltaMass"), "8");
        waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Search"));
        //waitForText("1 - 31 of 31");
        assertEquals(0, Locator.xpath("//td//a//span[contains(@title, 'R[+10.0]')]").findElements(getDriver()).size());
        assertEquals(31, Locator.xpath("//td//a//span[contains(@title, 'K[+8.0]')]").findElements(getDriver()).size());
    }

    @LogMethod
    protected void verifyPeptide()
    {
        // Click on a peptide.
        String targetProtein = "LTSLNVVAGSDLR";
        clickAndWait(Locator.linkContainingText(targetProtein));
        //Verify it’s associated with the right protein and other values from details view.
        //protein name, protein, neutral mass, avg. RT , precursor
        assertTextPresent(targetProtein, "YAL038W", "1343.740", "27.9232", "677.8818++ (heavy)");

        //Verify the spectrum shows up correctly.

        //Verify we get the expected number of chromatogram graphs.
        assertElementPresent(Locator.xpath("//img[contains(@src, 'generalMoleculeChromatogramChart.view')]"), 1);
        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"), 2);
        assertElementPresent(Locator.xpath("//img[contains(@alt, 'Chromatogram')]"), 3);

        //Click on a precursor icon link.
        clickAndWait(Locator.linkWithHref("precursorAllChromatogramsChart.view?"));
        //Verify expected values in detail view. Verify chromatogram.
        assertTextPresentInThisOrder("Precursor Chromatograms: LTSLNVVAGSDLR", "YAL038W", "672.8777");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'Chromatogram')]"));

        goBack();
        clickAndWait(Locator.linkContainingText("YAL038W"));
        //Verify summary info
        assertTextPresent("CDC19 SGDID:S000000036, Chr I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate,",
                "Sequence Coverage", "Peptides", "LTSLNVVAGSDLR", "TNNPETLVALR", "GVNLPGTDVDLPALSEK", "TANDVLTIR",
                "GDLGIEIPAPEVLAVQK", "EPVSDWTDDVEAR", "Peak Areas");

        goBack();
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        waitAndClick(Locator.linkContainingText("transitions"));
        waitForText("Transition List");

        // There are many regions within one transitions_view region -- all with the same region name.
        // Lookup the elements manually and point to a specific region to examine.
        DataRegionTable drt = DataRegion(getDriver()).index(2).find();
        assertEquals("heavy", drt.getDataAsText(5, "Label"));
        assertEquals("1353.7491", drt.getDataAsText(5, "Precursor Neutral Mass"));
        assertEquals("677.8818", drt.getDataAsText(5, "Q1 m/z"));
        assertEquals("y7", drt.getDataAsText(5, "Fragment"));
        assertEquals("727.3972", drt.getDataAsText(5, "Q3 m/z"));
        assertTextPresent("1343.740", "1226.661", "1001.550");

        //Click down arrow next to protein name. Click "Search for other references to this protein"
        WebElement popupArrow = waitForElement(Locator.linkWithText("YAL038W").followingSibling("span").childTag("img"));
        mouseOver(popupArrow);
        waitForText("Search for other references to this protein");
        clickAndWait(Locator.linkContainingText("Search for other references to this protein"));

        //Verify Targeted MS Peptides section of page.
        //Click on Details link.
        //Spot check some values.
        assertTextPresent("Protein Search Results", "Targeted MS Proteins", "YAL038W",
                "I from 71787-73289, Verified ORF, \"Pyruvate kinase, functions as a homotetramer in glycolysis to convert phosphoenolpyruvate to pyruvate, the input for aerobic (TCA cycle",
                "MRMer.zip");
    }

    @LogMethod
    protected void verifyMolecule()
    {
        //click on link 'PC' under Protein/Label
        clickAndWait(Locator.linkContainingText("PC"));

        //Go to SmallMolecules data region
        DataRegionTable drt = new DataRegionTable("SmallMolecules", getDriver());
        assertEquals("PC aa C30:1", drt.getDataAsText(5, "Custom Ion Name"));
        assertEquals("C38H74NO8P", drt.getDataAsText(5, "Ion Formula"));
        assertEquals("703.9755", drt.getDataAsText(5, "Mass Average"));
        assertEquals("703.5152", drt.getDataAsText(5, "Mass Monoisotopic"));
        assertEquals(" ", drt.getDataAsText(5, "Avg. Measured RT"));
        ensureComparisonPlots("PC");


        //Click on a value under Custom Ion Name
        clickAndWait(Locator.linkContainingText("PC aa C26:0").index(0)); //two links with this text, want the first one under Custom Ion Name hence index(0).
        waitForElement(Locator.xpath("//tr[td[text()='Group']][td[a[normalize-space()='PC']]]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Custom Ion Name']][td[normalize-space()='PC aa C26:0']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Ion Formula']][td[normalize-space()='C34H68NO8P']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Mass Average']][td[normalize-space()='649.8845']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Mass Monoisotopic']][td[normalize-space()='649.4683']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Avg. RT']][td[normalize-space()='0.9701']]"));
        assertTextPresent("Molecule Precursors");

        assertElementPresent(Locator.xpath("//img[contains(@src, 'generalMoleculeChromatogramChart.view')]"));
        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"));
        ensureComparisonPlots("PC aa C26:0");

        //Click on Molecule Precursor Chromatogram link
        clickAndWait(Locator.xpath("//a[contains(@href, 'moleculePrecursorAllChromatogramsChart.view')]"));

        assertTextPresent("Molecule Precursor Chromatograms");
        assertTextPresent("Molecule Precursor Summary");
        waitForElement(Locator.xpath("//tr[td[text()='Molecule Group']][td[normalize-space()='PC']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Molecule Precursor']][td[normalize-space()='PC aa C26:0 - C34H68NO8P[M+H]']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Charge']][td[normalize-space()='1']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='m/z']][td[normalize-space()='650.4755']]"));

        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"), 4);
        ensureComparisonPlots("PC aa C26:0");

        //Go back to Document Summary page
        clickAndWait(Locator.linkContainingText(SKY_FILE_SMALLMOL_PEP));

        //Look for Small Molecule Precursor List data region
        drt = DataRegion(getDriver()).withName("small_mol_precursors_view").index(1).find();
        assertEquals("PC aa C30:1", drt.getDataAsText(5, "Custom Ion Name"));
        assertEquals("C38H74NO8P", drt.getDataAsText(5, "Ion Formula"));
        assertEquals("703.9755", drt.getDataAsText(5, "Mass Average"));
        assertEquals("703.5152", drt.getDataAsText(5, "Mass Monoisotopic"));
        assertEquals("PC aa C30:1", drt.getDataAsText(5, "Precursor"));
        assertEquals("1+", drt.getDataAsText(5, "Q1 Z"));
        assertEquals("704.5225", drt.getDataAsText(5, "Q1 m/z"));
        assertEquals("1", drt.getDataAsText(5, "Transition Count"));

        clickAndWait(Locator.linkContainingText("lysoPC a C14:0").index(0));
        assertTextPresent("Small Molecule Summary");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'generalMoleculeChromatogramChart.view')]"));
        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"));
        ensureComparisonPlots("lysoPC a C14:0");
        assertElementPresent(Locator.xpath("//a[contains(@href, 'moleculePrecursorAllChromatogramsChart.view')]"));

        clickAndWait(Locator.linkContainingText(SKY_FILE_SMALLMOL_PEP));

        clickAndWait(Locator.linkContainingText("lysoPC a C14:0").index(1));
        assertTextPresent("Molecule Precursor Chromatograms");
        assertElementPresent(Locator.xpath("//img[contains(@src, 'precursorChromatogramChart.view')]"), 4);
        ensureComparisonPlots("lysoPC a C14:0");

        //Go to Small Molecule Transition List
        clickAndWait(Locator.linkContainingText(SKY_FILE_SMALLMOL_PEP));
        waitAndClick(Locator.linkContainingText("transitions"));
        waitForText("Small Molecule Transition List");

        drt = DataRegion(getDriver()).withName("small_mol_transitions_view").index(1).find();
        assertEquals("PC aa C30:1", drt.getDataAsText(5, "Molecule"));
        assertEquals("C38H74NO8P", drt.getDataAsText(5, "Ion Formula"));
        assertEquals("703.9755", drt.getDataAsText(5, "Mass Average"));
        assertEquals("703.5152", drt.getDataAsText(5, "Mass Monoisotopic"));
        assertEquals("PC aa C30:1", drt.getDataAsText(5, "Precursor"));
        assertEquals("1+", drt.getDataAsText(5, "Q1 Z"));
        assertEquals("704.5225", drt.getDataAsText(5, "Q1 m/z"));
        assertEquals("C5H14NO4P[M+H]", drt.getDataAsText(5, "Fragment"));
        assertEquals("184.0733", drt.getDataAsText(5, "Q3 m/z"));
        assertEquals("1+", drt.getDataAsText(5, "Q3 Z"));
    }

    private void verifyQueries()
    {
        // As part of 16.1, the targetedms schema was updated to support both proteomics and small molecule data import
        // into separate tables (i.e. general table plus specific tables for each of the two types).
        // This test is to check backwards compatibility for SQL queries on the schema prior to 16.1
        // Note: this expects two runs to be imported: SKY_FILE and SKY_FILE2.

        // Test query against targetedms.peptide
        String querySql = "SELECT \n" +
                "Id, PeptideGroupId, Sequence, StartIndex, EndIndex, PreviousAa, NextAa, CalcNeutralMass, \n" +
                "NumMissedCleavages, Rank, RtCalculatorScore, PredictedRetentionTime, \n" +
                "AvgMeasuredRetentionTime, Decoy, Note, PeptideModifiedSequence,\n" +
                "ExplicitRetentionTime, Annotations NoteAnnotations, \n" +
                "ModifiedPeptideDisplayColumn, RepresentivePrecursorCount,\n" +
                "PeptideGroupId.RunId.Folder.Path,\n" +
                "PeptideGroupId.RunId.File,\n" +
                "PeptideGroupId.Label\n" +
                "FROM peptide";
        createQuery(getProjectName(), "query_peptide", "targetedms", querySql, null, false);
        navigateToQuery("targetedms", "query_peptide");
        waitForElement(Locator.paginationText(45));
        DataRegionTable query = new DataRegionTable("query", this);
        query.setFilter("Sequence", "Equals", "TNNPETLVALR");
        query = new DataRegionTable("query", this);
        assertEquals(1, query.getDataRowCount());
        assertEquals("YAL038W", query.getDataAsText(0, "Protein / Label"));
        assertElementPresent(Locator.linkWithText("YAL038W"));
        assertEquals("TNNPETLVALR", query.getDataAsText(0, "Modified Peptide"));
        assertEquals("K", query.getDataAsText(0, "Next Aa"));
        assertEquals(SKY_FILE, query.getDataAsText(0, "File"));
        query.clearFilter("Sequence");

        // Test query against targetedms.precursor
        querySql = "SELECT \n" +
                "Id, PeptideId, IsotopeLabelId,\n" +
                "Mz, Charge, NeutralMass, ModifiedSequence, CollisionEnergy, DeclusteringPotential, \n" +
                "DecoyMassShift, Note, RepresentativeDataState,\n" +
                "ExplicitIonMobility, Annotations, TransitionCount,\n" +
                "ModifiedPrecursorDisplayColumn, NoteAnnotations, \n" +
                "PeptideId.PeptideGroupId.Label, \n" +
                "PeptideId.PeptideGroupId.Description,\n" +
                "PeptideId.PeptideGroupId.NoteAnnotations AS PeptideGroupIdNoteAnnotations,\n" +
                "PeptideId.ModifiedPeptideDisplayColumn, \n" +
                "PeptideId.NoteAnnotations AS PeptideIdNoteAnnotations,\n" +
                "PeptideId.NumMissedCleavages,\n" +
                "PeptideId.CalcNeutralMass,\n" +
                "PeptideId.Rank,\n" +
                "IsotopeLabelId.Name\n" +
                "FROM precursor";
        createQuery(getProjectName(), "query_precursor", "targetedms", querySql, null, false);
        navigateToQuery("targetedms", "query_precursor");
        waitForElement(Locator.paginationText(89));
        query = new DataRegionTable("query", this);
        query.setFilter("ModifiedSequence", "Equals", "LTSLNVVAGSDLR[+10.0]");
        query = new DataRegionTable("query", this);
        assertEquals(1, query.getDataRowCount());
        assertEquals("677.8818", query.getDataAsText(0, "Q1 m/z"));
        assertEquals("YAL038W", query.getDataAsText(0, "Protein / Label"));
        assertElementPresent(Locator.linkWithText("YAL038W"));
        assertEquals("LTSLNVVAGSDLR", query.getDataAsText(0, "Peptide"));
        assertEquals("1343.7409", query.getDataAsText(0, "Peptide Neutral Mass"));
        query.clearFilter("ModifiedSequence");

        // Test query against targetedms.transition
        querySql = "SELECT \n" +
                "Id, PrecursorId, Mz, Charge, NeutralMass, NeutralLossMass, FragmentType, FragmentOrdinal,\n" +
                "CleavageAa, IsotopeDistIndex, IsotopeDistRank,\n" +
                "IsotopeDistProportion, DecoyMassShift, Note, MassIndex, MeasuredIonName,\n" +
                "Annotations, Fragment, NoteAnnotations,\n" +
                "PrecursorId.PeptideId.PeptideGroupId.Label,\n" +
                "PrecursorId.PeptideId.PeptideGroupId.Description,\n" +
                "PrecursorId.PeptideId.PeptideGroupId.Annotations AS PeptideGroupIdAnnotations,\n" +
                "PrecursorId.PeptideId.ModifiedPeptideDisplayColumn,\n" +
                "PrecursorId.PeptideId.Annotations AS PeptideIdAnnotations,\n" +
                "PrecursorId.PeptideId.NumMissedCleavages,\n" +
                "PrecursorId.PeptideId.CalcNeutralMass,\n" +
                "PrecursorId.PeptideId.Rank,\n" +
                "PrecursorId.ModifiedPrecursorDisplayColumn,\n" +
                "PrecursorId.Annotations AS PrecursorIdAnnotations,\n" +
                "PrecursorId.IsotopeLabelId.Name,\n" +
                "PrecursorId.NeutralMass AS PrecursorIdNeutralMass,\n" +
                "PrecursorId.Mz AS PrecursorIdMz,\n" +
                "PrecursorId.Charge AS PrecursorIdCharge\n" +
                "FROM transition";
        createQuery(getProjectName(), "query_transition", "targetedms", querySql, null, false);
        navigateToQuery("targetedms", "query_transition");
        waitForElement(Locator.paginationText(1, 100, 299));
        query = new DataRegionTable("query", this);
        query.setFilter("PrecursorId", "Equals", "LTSLNVVAGSDLR[+10.0]");
        query = new DataRegionTable("query", this);
        assertEquals(3, query.getDataRowCount());
        assertEquals("677.8818", query.getDataAsText(0, "Precursor Id Mz"));
        assertEquals("YAL038W", query.getDataAsText(0, "Protein / Label"));
        assertElementPresent(Locator.linkWithText("YAL038W"));
        assertEquals("LTSLNVVAGSDLR", query.getDataAsText(0, "Peptide"));
        assertEquals("1343.7409", query.getDataAsText(0, "Peptide Neutral Mass"));
        query.clearFilter("PrecursorId");

        // Test query against targetedms.librarydocprecursor
        querySql = "SELECT GeneralMoleculeId.Id AS Id1, \n" +
                "GeneralMoleculeId.Sequence AS Sequence1,\n" +
                "GeneralMoleculeId.PeptideGroupId.Label AS Protein1,\n" +
                "PeptideId.Id AS Id2,\n" +
                "PeptideId.Sequence AS Sequence2,\n" +
                "PeptideId.PeptideGroupId.Label AS Protein2\n" +
                "FROM librarydocprecursor";
        createQuery(getProjectName(), "query_librarydocprecursor", "targetedms", querySql, null, false);
        navigateToQuery("targetedms", "query_librarydocprecursor");
        waitForElement(Locator.paginationText(89));
        query = new DataRegionTable("query", this);
        query.setFilter("Protein1", "Equals", "YAL038W_renamed");
        query = new DataRegionTable("query", this);
        assertEquals(1, query.getDataRowCount());
        assertEquals(query.getDataAsText(0, "Id1"), query.getDataAsText(0, "Id2"));
        assertEquals(query.getDataAsText(0, "Sequence1"), query.getDataAsText(0, "Sequence1"));
        assertEquals(query.getDataAsText(0, "Protein1"), query.getDataAsText(0, "Protein2"));
        query.clearFilter("Protein1");
    }

    @LogMethod
    private void testRawFileLinks(String skyFile)
    {
        String testFileDir = "TargetedMS/Raw Data Test/ForSeleniumTests/";
        String rawZipValid = "13417_02_WAA283_3805_071514.raw.zip";
        String rawZipInvalid = "13418_02_WAA283_3805_071514.raw.zip";
        String rawFileInvalid = "13418_03_WAA283_3805_071514.raw";
        String mzXmlFile = "silac_1_to_4.mzXML";

        String[] files = new String[] {rawZipValid, rawZipInvalid, rawFileInvalid, mzXmlFile};

        clickTab("Raw Data");
        FileBrowserHelper browser = FilesWebPart.getWebPart(getDriver()).fileBrowser();
        for(String file: files)
        {
            browser.uploadFile(TestFileUtils.getSampleData(testFileDir + file));
        }

        // Create a raw data folder. Directory-based raw data gets auto-zipped when uploded through the FilesWebPart
        // but it is possible to upload the directories if copying to a network drive mapped to a LabKey folder.
        String rawDatadir = "13417_03_WAA283_3805_071514.raw";
        browser.createFolder(rawDatadir);
        browser.uploadFile(TestFileUtils.getSampleData(testFileDir + rawDatadir + "/_FUNC001.DAT"));
        browser.moveFile("_FUNC001.DAT", rawDatadir);

        goToDashboard();
        clickAndWait(Locator.linkContainingText(skyFile));
        clickAndWait(Locator.linkContainingText("5 replicates"));

        DataRegionTable drt = new DataRegionTable("Replicate", getDriver());
        int row = 0;
        validateReplicateRow(row, rawZipValid.replace(".zip", ""), "127 bytes", drt);
        validateReplicateRow(++row, rawDatadir, "10 bytes", drt);

        // 13418_02_WAA283_3805_071514.raw.zip is not a valid zip for Waters raw data.  It does not contain a _FUNC*.DAT file
        // But we do not validate zip contents when showing download links.
        validateReplicateRow(++row, rawZipInvalid.replace(".zip", ""), "128 bytes", drt);

        // 13418_03_WAA283_3805_071514.raw is a file whereas the instrument associated with the SampleFile is Waters
        // which has raw data in .raw directories
        validateReplicateRow(++row, rawFileInvalid, null, drt);

        validateReplicateRow(++row, mzXmlFile, "20 bytes", drt);
    }

    private void validateReplicateRow(int row, String fileName, String sizeString, DataRegionTable drt)
    {
        assertEquals(fileName, drt.getDataAsText(row, "File"));
        if(sizeString != null)
        {
            assertTrue("Unexpected download size", drt.getDataAsText(row, "Download").contains(sizeString));
        }
        else
        {
            assertEquals("Not available", drt.getDataAsText(row, "Download"));
        }
    }

    @LogMethod
    private void verifyAreaRatios() throws IOException, CommandException
    {
        // SKY_FILE_AREA_RATIOS: Transitions are explicitly marked as non-quantitative and should not be included calculating total areas
        String docName = SKY_FILE_AREA_RATIOS;
        Rowset rowset = getPeptideAreaRatiosResults(docName);
        assertEquals("Wrong number of PeptideAreaRatio rows returned for " + docName, 2, rowset.getSize());
        Iterator<Row> iterator = rowset.iterator();
        checkAreaRatio(iterator.next(), docName, "A01_acq_01", "ALGS[+79.966331]PTKQLLPC[+57.021464]EMAC[+57.021464]NEK",0.9462725);
        checkAreaRatio(iterator.next(), docName, "A01_acq_01", "VSMPDVELNLKS[+79.966331]PK",0.03584785);

        // SKY_FILE_AREA_RATIOS_2: Full scan acquisition method is set to "DDA" so MS2 transitions should not be included in calculating total areas
        docName = SKY_FILE_AREA_RATIOS_2;
        rowset = getPeptideAreaRatiosResults(docName);
        assertEquals("Wrong number of PeptideAreaRatio rows returned for " + docName, 12, rowset.getSize());
        iterator = rowset.iterator();

        checkAreaRatio(iterator.next(), docName, "2_1-01", "EVELFSR",2.8158202);
        checkAreaRatio(iterator.next(), docName, "2_1-01", "YSPSPLSMK",2.1585193);
        checkAreaRatio(iterator.next(), docName, "2_1-01", "QLLDFGSENAC[+57.021464]ER",0.8570299);
        checkAreaRatio(iterator.next(), docName, "2_1-01", "ALSEFVDTLVK",3.1268802);
        checkAreaRatio(iterator.next(), docName, "2_1-01", "NAPLAGFGYGLPISR",1.1077645);

        checkAreaRatio(iterator.next(), docName, "4_1-01", "EVELFSR",1.5434148);
        checkAreaRatio(iterator.next(), docName, "4_1-01", "YSPSPLSMK",0.44915965);
        checkAreaRatio(iterator.next(), docName, "4_1-01", "QLLDFGSENAC[+57.021464]ER",0.6879811);
        checkAreaRatio(iterator.next(), docName, "4_1-01", "ALSEFVDTLVK",1.3860041);

        checkAreaRatio(iterator.next(), docName, "6_1-01", "EVELFSR",1.0394642);
        checkAreaRatio(iterator.next(), docName, "6_1-01", "QLLDFGSENAC[+57.021464]ER",0.2087488);
        checkAreaRatio(iterator.next(), docName, "6_1-01", "ALSEFVDTLVK",0.9737395);
    }

    @NotNull
    private Rowset getPeptideAreaRatiosResults(String documentName) throws IOException, CommandException
    {
        Connection conn = createDefaultConnection(false);
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "peptidearearatio");
        cmd.setRequiredVersion(9.1);
        cmd.setSorts(List.of(new Sort("PeptideChrominfoId/SampleFileId/ReplicateId"),
                new Sort("PeptideChrominfoId/PeptideId")));
        cmd.setColumns(Arrays.asList("PeptideChrominfoId/SampleFileId/ReplicateId/RunId",
                "PeptideChrominfoId/SampleFileId/ReplicateId",
                "PeptideChrominfoId/PeptideId",
                "AreaRatio"));
        cmd.addFilter("PeptideChrominfoId/SampleFileId/ReplicateId/RunId/FileName", documentName, Filter.Operator.EQUAL);
        SelectRowsResponse response = cmd.execute(conn, getCurrentContainerPath());
        return response.getRowset();
    }

    private void checkAreaRatio(Row row, String file,  String replicate, String peptide, double ratio)
    {
        assertEquals("Wrong filename", file, row.getDisplayValue("PeptideChrominfoId/SampleFileId/ReplicateId/RunId"));
        assertEquals("Wrong replicate", replicate, row.getDisplayValue("PeptideChrominfoId/SampleFileId/ReplicateId"));
        assertEquals("Wrong peptide", peptide, row.getDisplayValue("PeptideChrominfoId/PeptideId"));
        // Round to avoid floating point precision false positives
        long ratioL = Math.round(ratio * 10000);
        assertEquals("Wrong area ratio", ratioL, Math.round(((Number)row.getValue("AreaRatio")).doubleValue() * 10000));
    }

    @LogMethod
    private void verifyBestMassErrorPpm() throws IOException, CommandException
    {
        String docName = SKY_FILE_AREA_RATIOS_2;

        Connection conn = createDefaultConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "precursorchrominfo");
        cmd.setRequiredVersion(9.1);
        cmd.setSorts(List.of(new Sort("SampleFileId/ReplicateId"), new Sort("PrecursorId")));
        cmd.setColumns(Arrays.asList("SampleFileId/ReplicateId/RunId",
                "SampleFileId/ReplicateId",
                "PrecursorId/PeptideId",
                "PrecursorId",
                "AverageMassErrorPPM",
                "BestMassErrorPPM"));
        cmd.addFilter("SampleFileId/ReplicateId/RunId/FileName", docName, Filter.Operator.EQUAL);
        String peptide = "NAPLAGFGYGLPISR";
        cmd.addFilter("PrecursorId/PeptideId/PeptideModifiedSequence", peptide, Filter.Operator.EQUAL);

        SelectRowsResponse response = cmd.execute(conn, getCurrentContainerPath());
        Rowset rowset = response.getRowset();

        assertEquals("Wrong number of PrecursorChromInfo rows returned for " + docName + " and peptide " + peptide, 6, rowset.getSize());
        Iterator<Row> iterator = rowset.iterator();

        checkPrecursorChromInfoRow(iterator.next(), docName, "2_1-01", "NAPLAGFGYGLPISR",1.2232006, 1.8);
        checkPrecursorChromInfoRow(iterator.next(), docName, "2_1-01", "NAPL[+3.0]AGFGYGL[+3.0]PISR", -1.8464184, 0.4);

        checkPrecursorChromInfoRow(iterator.next(), docName, "4_1-01", "NAPLAGFGYGLPISR", 0.9665566, 1.3);
        checkPrecursorChromInfoRow(iterator.next(), docName, "4_1-01", "NAPL[+3.0]AGFGYGL[+3.0]PISR", null, null);

        // BestMassErrorPpm is not set for the precursor in this replicate since none of the quantitative transitions have a mass error
        checkPrecursorChromInfoRow(iterator.next(), docName, "6_1-01", "NAPLAGFGYGLPISR", -2.1, null);
        checkPrecursorChromInfoRow(iterator.next(), docName, "6_1-01", "NAPL[+3.0]AGFGYGL[+3.0]PISR", -3.481539, -6.7);
    }

    private void checkPrecursorChromInfoRow(Row row, String file,  String replicate, String precursor, Double averageMassErrorPpm, Double bestMassErrorPpm)
    {
        assertEquals("Wrong filename", file, row.getDisplayValue("SampleFileId/ReplicateId/RunId"));
        assertEquals("Wrong replicate", replicate, row.getDisplayValue("SampleFileId/ReplicateId"));
        assertEquals("Wrong precursor", precursor, row.getDisplayValue("PrecursorId"));

        if(averageMassErrorPpm == null)
        {
            assertNull("Expected null AverageMassErrorPPM", row.getDisplayValue("AverageMassErrorPPM"));
        }
        else
        {
            // Round to avoid floating point precision false positives
            long massErrorL = Math.round(averageMassErrorPpm * 10000);
            assertEquals("Wrong AverageMassErrorPPM", massErrorL, Math.round(((Number) row.getValue("AverageMassErrorPPM")).doubleValue() * 10000));
        }
        if(bestMassErrorPpm == null)
        {
            assertNull("Expected null BestMassErrorPPM", row.getDisplayValue("BestMassErrorPPM"));
        }
        else
        {
            // Round to avoid floating point precision false positives
            long massErrorL = Math.round(bestMassErrorPpm * 10000);
            assertEquals("Wrong BestMassErrorPPM", massErrorL, Math.round(((Number) row.getValue("BestMassErrorPPM")).doubleValue() * 10000));
        }
    }
}
