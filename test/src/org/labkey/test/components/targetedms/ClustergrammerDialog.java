package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 4/19/16.
 */
public class ClustergrammerDialog extends Component
{
    private static final String DIALOG_TITLE = "Clustergrammer Heat Map";
    private static final String CONFIRMATION_TITLE = "Publish to Clustergrammer";
    private static final String SUCCESS_CONFIRMATION = "Heat Map Generation Successful";
    public static final String CG_REDIRECT_URL = "amp.pharm.mssm.edu/";

    private BaseWebDriverTest _test;

    public ClustergrammerDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return null;
    }

    public void clickSave(boolean confirm)
    {
        _test._ext4Helper.clickWindowButton(DIALOG_TITLE, "Save", 0, 0);

        if (confirm)
        {
            _test._ext4Helper.clickWindowButton(CONFIRMATION_TITLE, "Yes", 30000, 0);
        }
        else
            _test._ext4Helper.clickWindowButton(CONFIRMATION_TITLE, "No", 0, 0);
    }

    public void clickCancel()
    {
        _test._ext4Helper.clickWindowButton(DIALOG_TITLE, "Cancel", 0, 0);
    }

    public void setTitle(String title)
    {
        _test.setFormElement(Elements.titleEditor, title);
    }

    public void setDescription(String description)
    {
        _test.setFormElement(Elements.descriptionEditor, description);
    }

    public String getTitle()
    {
        return _test.getFormElement(Elements.titleEditor);
    }

    public String getDescription()
    {
        return _test.getFormElement(Elements.descriptionEditor);
    }

    public void waitForDialog()
    {
        _test.waitForElement(Ext4Helper.Locators.window(DIALOG_TITLE));
    }

    public void waitForSuccessMessage()
    {
        _test.waitForElement(Ext4Helper.Locators.window(SUCCESS_CONFIRMATION), 30000);
    }

    private static class Elements
    {
        public static Locator titleEditor = Locator.inputById("reportTitleEditor" + "-inputEl");
        public static Locator descriptionEditor = Locator.textarea("reportDescriptionEditor" + "-inputEl");

    }

}