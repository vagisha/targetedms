/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.targetedms;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;

public class PKReportPage extends LabKeyPage<PKReportPage.ElementCache>
{
    private int _totalSubgroupTimeRowCount;

    public PKReportPage(WebDriver driver, int totalSubgroupTimeRowCount)
    {
        super(driver);
        _totalSubgroupTimeRowCount = totalSubgroupTimeRowCount;
        waitForPage();
    }

    @Override
    protected void waitForPage()
    {
        if(_totalSubgroupTimeRowCount > 0)
        {
            waitForElements(elementCache().timeCellLoc, _totalSubgroupTimeRowCount);
        }
        else
        {
            waitForElement(elementCache().timeCellLoc);
        }
    }

    public void setAllSubgroupTimeCheckboxes(String subgroup, int count, boolean check)
    {
        for (int i = 1; i <= count; i++)
        {
            setSubgroupTimeCheckbox(subgroup, true, i, check);
            setSubgroupTimeCheckbox(subgroup, false, i, check);
        }
    }

    public void setSubgroupTimeCheckbox(String subgroup, boolean isC0, int rowIndex, boolean check)
    {
        int colIndex = isC0 ? 2 : 3;
        Locator.XPathLocator loc = Locator.xpath("//*[@id=\"pk-table-input-" + subgroup + "\"]/tbody/tr[" + rowIndex + "]/td[" + colIndex + "]/input");
        if (check)
            checkCheckbox(loc);
        else
            uncheckCheckbox(loc);

        sleep(1000); // each input change persists the settings to the server, so wait a second
    }

    public void setNonIVC0(String subgroup, String newValue)
    {
        Locator inputLoc = Locator.id("nonIVC0-" + subgroup);
        Locator warnLoc = Locator.id("nonIVC0Controls-Warn-" + subgroup);
        Locator btnLoc = Locator.id("btnNonIVC0-" + subgroup);

        if (StringUtils.isEmpty(getFormElement(inputLoc)))
            assertElementVisible(warnLoc);

        setFormElement(inputLoc, newValue);
        click(btnLoc);
        sleep(1000); // each input change persists the settings to the server, so wait a second

        if (!StringUtils.isEmpty(getFormElement(inputLoc)))
            assertElementNotVisible(warnLoc);
    }

    public void verifyTableColumnValues(String table, String subgroup, int colIndex, String expectedValsStr)
    {
        String tableId = "pk-table-" + table + "-" + subgroup;
        String actualVals = columnDataAsString(Locator.id(tableId).findElement(getDriver()), colIndex);
        assertEquals("Incorrect values in data table " + tableId, expectedValsStr, actualVals);
    }

    private String columnDataAsString (WebElement table,int col)
    {
        String retVal = "";
        int size = table.findElements(By.tagName("tr")).size();
        for (int i = 1; i < size ; i++)
            retVal += Locator.xpath("//tbody/tr[" + i + "]/td[" + col + "]").findElement(table).getText() + " ";
        return retVal.trim();
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator timeCellLoc = Locator.tagWithClass("td", "pk-table-time");
    }
}