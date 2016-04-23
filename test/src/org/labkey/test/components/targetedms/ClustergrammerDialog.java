package org.labkey.test.components.targetedms;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by iansigmon on 4/19/16.
 */
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

    public void clickSave(boolean confirm)
    {
        clickButton("Save", 0);
        Confirmation confirmation = new Confirmation(getWrapper().getDriver());
        confirmation.waitForDialog();

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

    public void waitForDialog()
    {
        getWrapper().waitForElement(Ext4Helper.Locators.window(DIALOG_TITLE));
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
            waitForClose();
        }

        public void clickNo()
        {
            clickButton("No", 0);
            waitForClose();
        }

        public void waitForDialog()
        {
            getWrapper().waitForElement(Ext4Helper.Locators.window(CONFIRMATION_TITLE));
        }
    }
}