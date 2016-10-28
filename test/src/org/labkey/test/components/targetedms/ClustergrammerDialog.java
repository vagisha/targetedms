/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ClustergrammerDialog extends Window
{
    private static final String DIALOG_TITLE = "Clustergrammer Heat Map";
    public static final String CG_REDIRECT_URL = "amp.pharm.mssm.edu/";

    private Elements _elements;

    public ClustergrammerDialog(WebDriver driver)
    {
        super(DIALOG_TITLE, driver);
        _elements = new Elements();
    }

    public Confirmation clickSave()
    {
        clickButton("Save", 0);
        return new Confirmation(getWrapper().getDriver());
    }

    public void clickSave(boolean confirm)
    {
        Confirmation confirmation = clickSave();

        if (confirm)
            confirmation.clickYes();
        else
            confirmation.clickNo();
    }

    public void setReportTitle(String title)
    {
        elements().reportTitleEditor.clear();
        elements().reportTitleEditor.sendKeys(title);
    }

    public void setReportDescription(String description)
    {
        elements().reportDescriptionEditor.clear();
        elements().reportDescriptionEditor.sendKeys(description);
    }

    public String getReportTitle()
    {
        return getWrapper().getFormElement(elements().reportTitleEditor);
    }

    public String getReportDescription()
    {
        return getWrapper().getFormElement(elements().reportDescriptionEditor);
    }

    //Hides superclasses' elements()
    protected Elements elements()
    {
        return _elements;
    }

    //Hides superclasses' Elements
    protected class Elements extends Window.Elements
    {
        WebElement reportTitleEditor = new LazyWebElement(Locator.inputById("reportTitleEditor" + "-inputEl"), this);
        WebElement reportDescriptionEditor = new LazyWebElement(Locator.textarea("reportDescriptionEditor" + "-inputEl"), this);
    }

    public class Confirmation extends Window
    {
        private static final String CONFIRMATION_TITLE = "Publish to Clustergrammer";

        public Confirmation(WebDriver driver)
        {
            super(CONFIRMATION_TITLE, driver);
        }

        public void clickYes()
        {
            clickButton("Yes", 30000);
        }

        public void clickNo()
        {
            clickButton("No", 0);
            waitForClose();
        }
    }
}