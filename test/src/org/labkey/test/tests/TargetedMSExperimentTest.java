/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

import static org.junit.Assert.*;

@Category({DailyB.class, MS2.class})
public class TargetedMSExperimentTest extends TargetedMSTest
{
    public TargetedMSExperimentTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupAndImportData(FolderType.Experiment);
        verifyImportedData();
        verifyModificationSearch();
        verifyProteinSearch();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyImportedData()
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCounts();
        verifyPeptide();
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
}
