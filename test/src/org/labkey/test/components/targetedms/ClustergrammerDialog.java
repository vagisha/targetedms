/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.html.Input.Input;

public class ClustergrammerDialog extends Window<ClustergrammerDialog.ElementCache>
{
    private static final String DIALOG_TITLE = "Clustergrammer Heat Map";
    public static final String CG_REDIRECT_URL = "amp.pharm.mssm.edu/";

    public ClustergrammerDialog(WebDriver driver)
    {
        super(DIALOG_TITLE, driver);
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

    public ClustergrammerDialog setReportTitle(String title)
    {
        elementCache().reportTitleEditor.set(title);
        elementCache().reportTitleEditor.blur();
        return this;
    }

    public ClustergrammerDialog setReportDescription(String description)
    {
        elementCache().reportDescriptionEditor.set(description);
        elementCache().reportDescriptionEditor.blur();
        return this;
    }

    public String getReportTitle()
    {
        return elementCache().reportTitleEditor.get();
    }

    public String getReportDescription()
    {
        return elementCache().reportDescriptionEditor.get();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Window.ElementCache
    {
        Input reportTitleEditor = Input(Locator.inputById("reportTitleEditor" + "-inputEl"), getDriver()).findWhenNeeded(this);
        Input reportDescriptionEditor = Input(Locator.textarea("reportDescriptionEditor" + "-inputEl"), getDriver()).findWhenNeeded(this);
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