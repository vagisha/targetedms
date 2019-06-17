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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class GetDataAPITest extends TargetedMSTest
{
    String CLIENT_API_CORE1 = "{"+
"   source: {"+
"		type: 'query',"+
"		schemaName: 'targetedms',"+
"		queryName: 'precursor'"+
"	},"+
"	transforms: ["+
"		{"+
"			type: 'aggregate',"+
"			filters: [ { fieldKey: ['ModifiedSequence'], type: 'startswith', value: 'L' } ],"+
"		},"+
"		{"+
"			type: 'aggregate',"+
"			groupBy: [ [\"PeptideId\", \"Sequence\"] ]"+
"			,aggregates: [ { fieldKey: ['PeptideId'], type: 'MAX', label: 'MyPeptideId' },"+
"                                       { fieldKey: ['Charge'], type: 'MAX', label: 'MyCharge' } ]"+
"			,pivot: { columns: [ [\"MyPeptideId\"] ], by: [\"Charge\"] }"+
"		}],"+
"    success: tableSuccess,"+
"    failure: function(responseData){"+
"        console.log(responseData);"+
"    }"+
"}";

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Experiment);
        importData("MRMer.zip");
        clientApiTest();
    }

    /*
     * Test for the getData client API. The test was built on the test
     * data for the TargetedMS module and therefore was placed here.
     * The test uses a script saved in the file 'getDataTest.html' which is
     * the source input of a wiki page that is run to generate a brief
     * set of output.
     */
    @LogMethod
    protected void clientApiTest()
    {
        runClientAPITestCore(CLIENT_API_CORE1);
        waitForElement(Locator.id("jsonWrapperTest").append("/table"));
        String getDataTable = getText(Locator.id("jsonWrapperTest"));

        assertThat(getDataTable, containsString("Sequence"));
        assertThat(getDataTable, containsString("My Charge"));
        assertThat(getDataTable, containsString("LLPYWQDVIAK"));
        assertThat(getDataTable, containsString("LLSEEALPAIR"));
        assertThat(getDataTable, containsString("LLTTIADAAK"));
        assertThat(getDataTable, containsString("LSVQDLDLK"));
        assertThat(getDataTable, containsString("LTSLNVVAGSDLR"));
        assertThat(getDataTable, containsString("LVEAFQWTDK"));
        assertThat(getDataTable, containsString("LVEDPQVIAPFLGK"));
        assertThat(getDataTable, containsString("LWDVATGETYQR"));
        assertThat(getDataTable, containsString("2"));
    }

    private void runClientAPITestCore(String request_core)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Wiki");

        String scriptText = TestFileUtils.getFileContents(TestFileUtils.getSampleData("api/getDataTest.html"));
        scriptText = scriptText.replace("REPLACEMENT_STRING", request_core);

        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createWikiPage("getDataTest", null, "getData API Test", scriptText, true, null, false);
    }
}
