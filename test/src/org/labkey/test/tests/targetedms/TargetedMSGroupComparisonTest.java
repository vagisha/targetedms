/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Group Comparison calculations in Panorama.  Tests uploading a Skyline document which has Group Comparisons
 * defined, and makes sure that the fold change results that are calculated match the files in
 * /SampleData/TargetedMS/Quantification/GroupComparisonScenariosTest.  Those files came from the Skyline unit test
 * "GroupComparisonScenariosTest".
 */
@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSGroupComparisonTest extends TargetedMSTest
{
    private static final String SAMPLEDATA_FOLDER = "Quantification/GroupComparisonScenariosTest/";
    private static final String GROUP_COMPARISON_PREFIX = "Group Comparison: ";
    public static final List<String> scenarioNames = Arrays.asList(
            "Rat_plasma");

    @Test
    public void testGroupComparisonScenarios()
    {
        setupFolder(FolderType.Experiment);
        for (String scenario : scenarioNames)
        {
            runScenario(scenario);
        }
    }

    private void runScenario(String scenario)
    {
        setupSubfolder(getProjectName(), scenario, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + scenario + ".sky.zip");
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));

        waitAndClick(Locator.linkContainingText("precursor"));

        Locator groupComparisonLocator = Locator.xpath("//span[starts-with(text(), '" + GROUP_COMPARISON_PREFIX + "')]");
        List<WebElement> groupComparisonElements = groupComparisonLocator.findElements(getDriver());
        List<String> groupComparisonNames = new ArrayList<>();
        for (WebElement element : groupComparisonElements)
        {
            String elementText = element.getText();
            assertTrue(elementText.startsWith(GROUP_COMPARISON_PREFIX));
            groupComparisonNames.add(elementText.substring(GROUP_COMPARISON_PREFIX.length()));
        }
        for (String groupComparisonName : groupComparisonNames)
        {
            verifyGroupComparison(scenario, groupComparisonName);
        }
    }

    private void verifyGroupComparison(String scenario, String groupComparisonName)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(scenario + ".sky.zip"));
        // TODO(nicksh): this only verifies Peptide fold changes.
        // We need to add some scenarios that involve small molecules.
        waitAndClick(Locator.xpath("//th[span[text() = 'Precursor List']]/span/a/span[contains(@class, 'fa-caret-down')]"));
        click(Locator.linkWithText(GROUP_COMPARISON_PREFIX + groupComparisonName));

        List<Map<String, Object>> expectedResultRows = readScenarioCsv(scenario, groupComparisonName + "_GroupComparisonColumns");
        DataRegionTable groupComparisonsTable = new DataRegionTable("group_comparison", this);
        groupComparisonsTable.showAll();
        TestLogger.log(String.format("Verifying %d rows for Group Comparison '%s'.", expectedResultRows.size(), groupComparisonName));
        List<String> peptideGroupColumn = null;
        List<String> peptideColumn = null;
        for (Map<String, Object> expectedRow : expectedResultRows)
        {
            String protein = Objects.toString(expectedRow.get("Protein"), "");
            String peptide = Objects.toString(expectedRow.get("Peptide Modified Sequence"), "");
            int rowIndex;
            if (peptide.isEmpty())
            {
                if (peptideGroupColumn == null)
                {
                    peptideGroupColumn = groupComparisonsTable.getColumnDataAsText("PeptideGroupId");
                }
                rowIndex = peptideGroupColumn.indexOf(protein);
            }
            else
            {
                if (peptideColumn == null)
                {
                    peptideColumn = groupComparisonsTable.getColumnDataAsText("Peptide");
                }
                rowIndex = peptideColumn.indexOf(peptide.replace("[+57]", "[+57.0]"));
            }
            assertNotEquals(-1, rowIndex);
            double delta = 1E-4;
            double actualLog2FoldChange = Double.parseDouble(groupComparisonsTable.getDataAsText(rowIndex, "Log 2 Fold Change"));
            double expectedFoldChange = Double.parseDouble(expectedRow.get("Log 2 Fold Change").toString());
            assertEquals(expectedFoldChange, actualLog2FoldChange, delta);
        }
    }

    private List<Map<String, Object>> readScenarioCsv(String scenarioName, String reportName)
    {
        File file = TestFileUtils.getSampleData("TargetedMS/" + SAMPLEDATA_FOLDER + scenarioName + "_" + reportName + ".csv");
        try (TabLoader tabLoader = new TabLoader(file, true))
        {
            tabLoader.parseAsCSV();
            return tabLoader.load();
        }
    }
}
