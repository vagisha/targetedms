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
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;

import java.io.IOException;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class TargetedMSMAMTest extends TargetedMSTest
{
    protected static final String SKY_FILE = "iRT Human+Standard Calibrate.zip";

    @Test
    public void testSteps() throws IOException, CommandException
    {
        setupFolder(FolderType.ExperimentMAM);
        importData(SKY_FILE);

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));

        verifyRunSummaryCountsPep(125,158,0, 160,628, 1, 0, 0);

        clickAndWait(Locator.linkContainingText("PTM Report"));

        assertElementPresent("Wrong modification count", Locator.xpath("//td[contains(text(), 'Carbamidomethyl Cysteine')]"), 9);
        assertTextPresentInThisOrder("(K)HDLDLICR(A)", "(K)YLECSALTQR(G)", "(R)YVDIAIPCNNK(G)");
        assertTextPresentInThisOrder("C245", "C157", "C163");
        assertTextPresent("A_D110907_SiRT_HELA_11_nsMRM_150selected_2_30min-5-35", "A_D110907_SiRT_HELA_11_nsMRM_150selected_1_30min-5-35");

        clickAndWait(Locator.linkContainingText("Peptide Map"));
        assertTextPresentInThisOrder("11.3", "14.1", "14.8");
        assertTextPresentInThisOrder("1501.75", "1078.50", "1547.71");
        assertTextPresentInThisOrder("NU205", "NU205", "1433Z");
        assertTextPresentInThisOrder("70-84", "325-333", "28-41");
        assertTextPresentInThisOrder("(K)ASTEGVAIQGQQGTR(L)", "(K)AQYEDIANR(S)", "(K)SVTEQGAELSNEER(N)");
        assertTextPresentInThisOrder("Carbamidomethyl Cysteine @ C156", "Carbamidomethyl Cysteine @ C244", "Carbamidomethyl Cysteine @ C93");
    }
}
