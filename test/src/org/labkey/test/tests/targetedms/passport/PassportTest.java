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
package org.labkey.test.tests.targetedms.passport;


import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

import java.util.List;

@Category({Daily.class})
public class PassportTest  extends PassportTestPart
{
    public PassportTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }


    @Test
    public void testSteps()
    {
        APIUserHelper h = new APIUserHelper(this);
        h.createUser(user);

        setupProject();
        testAsSuperAdmin();
        testAsNormalUser();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void testAsSuperAdmin()
    {
        signIn();
        clickProject(getProjectName());
        testNormalStuff();
    }

    private void testNormalStuff()
    {
        // assert side bar and pipeline tab not visible... only passport tab should be seen by anproteiny user
        assertElementNotPresent(Locator.xpath("//a[@id='PipelineTab']"));
        assertTextNotPresent("Pages");
        // enter Haptoglobin

        click(Locator.xpath("//tr[contains(@class,'labkey-alternate-row')]//a[@class='labkey-text-link'][contains(text(),'PASSPORT VIEW')]"));
        assertTextPresent("Haptoglobin", "data1.sky.zip");
        assertElementPresent(Locator.xpath("//div[@id='rangesliderdeg']"));
        assertElementPresent(Locator.xpath("//div[@id='rangesliderlength']"));
        assertElementPresent(Locator.xpath("//button[@id='formreset']"));
        assertElementPresent(Locator.xpath("//button[@id='formreset']"));
        assertElementPresent(Locator.xpath("//a[contains(text(),'P00738')]"));
        assertElementPresent(Locator.xpath("//div[@id='chart']"));
        String[] peptidesOrderIntensity = {"VGYVSGWGR", "GSFPWQAK","YVMLPVADQDQCIR","QLVEIEK","SCAVAEYGVYVK","TEGDGVYTLNNEK",
                "VMPICLPSK","VTSIQDWVQK","TEGDGVYTLNDK","DYAEVGR","ILGGHLDAK","HYEGSTVPEK","DIAPTLTLYVGK","SPVGVQPILNEHTFCAGMSK",
                "LPECEAVCGKPK","VVLHPNYSQVDIGLIK","NPANPVQR","NLFLNHSENATAK","MVSHHNLTTGATLINEQWLLTTAK"};
        for(int i = 0; i < peptidesOrderIntensity.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderIntensity[i]);
        }
        click(Locator.xpath("//select[@id='peptideSort']")); // click sort peptide dropdown
        click(Locator.xpath("//option[@value='sequencelocation']"));
        String[] peptidesOrderLocation = {"TEGDGVYTLNDK","TEGDGVYTLNNEK","LPECEAVCGKPK","NPANPVQR","ILGGHLDAK",
                "GSFPWQAK","MVSHHNLTTGATLINEQWLLTTAK","NLFLNHSENATAK","DIAPTLTLYVGK","QLVEIEK","VVLHPNYSQVDIGLIK","VMPICLPSK",
                "DYAEVGR","VGYVSGWGR","YVMLPVADQDQCIR","HYEGSTVPEK","SPVGVQPILNEHTFCAGMSK","SCAVAEYGVYVK","VTSIQDWVQK"};
        for(int i = 0; i < peptidesOrderLocation.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderLocation[i]);
        }
        click(Locator.xpath("//button[@id='formreset']")); // reset form check reset works
        for(int i = 0; i < peptidesOrderIntensity.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderIntensity[i]);
        }
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "19");

        dragAndDrop(Locator.xpath("//div[@id='rangesliderdeg']//span[1]"), 50, 0);
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "15");
        click(Locator.xpath("//button[@id='formreset']")); // reset form check reset works
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "19");

        //features
        assertElementPresent(Locator.xpath("//td[contains(@class, 'feature-sequencevariant')]"), 5);
        assertElementPresent(Locator.xpath("//td[contains(@class, 'feature-glycosylationsite')]"), 4);
        assertElementPresent(Locator.xpath("//td[contains(@class, 'feature-helix')]"), 7);
        assertElementPresent(Locator.xpath("//td[contains(@class, 'feature-turn')]"), 6);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void testAsNormalUser()
    {
        impersonate("normaluser@gmail.com");
        clickProject(getProjectName());
        testNormalStuff();

        // Log back in as a site admin
        stopImpersonating();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
