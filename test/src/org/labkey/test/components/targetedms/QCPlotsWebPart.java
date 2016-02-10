/*
 * Copyright (c) 2014-2015 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class QCPlotsWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Levey-Jennings QC Plots";

    public QCPlotsWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public QCPlotsWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    @Override
    protected void waitForReady()
    {
        waitForPlots(1, false);
    }

    public enum Scale
    {
        LINEAR("Linear"),
        LOG("Log");

        private String _text;

        private Scale(String text)
        {
            _text = text;
        }

        public String toString()
        {
            return _text;
        }

        public static Scale getEnum(String value)
        {
            for(Scale v : values())
                if(v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }
    }

    private void waitForNoRecords()
    {
        _test.waitFor(() -> elements().noRecords().size() > 0, 10000);
    }

    @LogMethod
    public void setScale(Scale scale)
    {
        WebElement plot = elements().findPlots().get(0);
        _test._ext4Helper.selectComboBoxItem(elements().scaleCombo, scale.toString());
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    public Scale getCurrentScale()
    {
        WebElement scaleInput = elements().scaleCombo.append("//input").waitForElement(this, 1000);
        return Scale.getEnum(scaleInput.getAttribute("value"));
    }

    public void setStartDate(String startDate)
    {
        _test.setFormElement(elements().startDate, startDate);
    }

    public String getCurrentStartDate()
    {
        return _test.getFormElement(elements().startDate);
    }

    public void setEndDate(String endDate)
    {
        _test.setFormElement(elements().endDate, endDate);
    }

    public String getCurrentEndDate()
    {
        return _test.getFormElement(elements().endDate);
    }

    public enum ChartType
    {
        RETENTION("Retention Time", true),
        PEAK("Peak Area", true),
        FWHM("Full Width at Half Maximum (FWHM)", true),
        FWB("Full Width at Base (FWB)", true),
        LHRATIO("Light/Heavy Ratio", false),
        TPAREARATIO("Transition/Precursor Area Ratio", true),
        TPAREAS("Transition/Precursor Areas", true),
        MASSACCURACTY("Mass Accuracy", true);

        private String _text;
        private boolean _hasData;

        ChartType(String text, boolean hasData)
        {
            _text = text;
            _hasData = hasData;
        }

        public String toString()
        {
            return _text;
        }

        public boolean hasData()
        {
            return _hasData;
        }

        public static ChartType getEnum(String value)
        {
            for(ChartType v : values())
                if(v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }
    }

    public void setChartType(ChartType chartType)
    {
        setChartType(chartType, true, true);
    }

    public void setChartType(ChartType chartType, boolean hasData)
    {
        setChartType(chartType, hasData, true);
    }

    @LogMethod
    public void setChartType(ChartType chartType, boolean hasData, boolean hasExistingPlot)
    {
        WebElement plot = null;
        if (hasExistingPlot)
            plot = elements().findPlots().get(0);

        _test._ext4Helper.selectComboBoxItem(elements().chartTypeCombo, chartType.toString());

        if (hasExistingPlot)
            _test.shortWait().until(ExpectedConditions.stalenessOf(plot));

        if (hasData)
            waitForReady();
        else
            waitForNoRecords();
    }

    public ChartType getCurrentChartType()
    {
        WebElement typeInput = elements().chartTypeCombo.append("//input").waitForElement(this, 1000);
        return ChartType.getEnum(typeInput.getAttribute("value"));
    }

    public void setGroupXAxisValuesByDate(boolean check)
    {
        WebElement plot = elements().findPlots().get(0);
        elements().groupedXCheckbox.set(check);
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    public boolean isGroupXAxisValuesByDateChecked()
    {
        return elements().groupedXCheckbox.isChecked();
    }

    public void setShowAllPeptidesInSinglePlot(boolean check, @Nullable Integer expectedPlotCount)
    {
        WebElement plot = elements().findPlots().get(0);
        elements().singlePlotCheckbox.set(check);
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();

        if (expectedPlotCount != null)
            waitForPlots(expectedPlotCount, true);
        else
            waitForPlots(1, false);
    }

    public boolean isShowAllPeptidesInSinglePlotChecked()
    {
        return elements().singlePlotCheckbox.isChecked();
    }

    public void applyRange()
    {
        WebElement panelChild = Locator.css("svg").findElement(elements().plotPanel); // The panel itself doesn't become stale, but its children do
        _test.clickButton("Apply", 0);
        _test.shortWait().until(ExpectedConditions.stalenessOf(panelChild));
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void waitForPlots(Integer plotCount, boolean exact)
    {
        if (plotCount > 0)
        {
            if (exact)
                _test.waitFor(() -> elements().findPlots().size() == plotCount, WebDriverWrapper.WAIT_FOR_PAGE);
            else
                _test.waitFor(() -> elements().findPlots().size() >= plotCount, WebDriverWrapper.WAIT_FOR_PAGE);
        }
        else
        {
            _test.longWait().until(ExpectedConditions.textToBePresentInElement(elements().plotPanel, "There were no records found. The date filter applied may be too restrictive."));
        }
    }

    public List<QCPlot> getPlots()
    {
        List<QCPlot> plots = new ArrayList<>();

        for (WebElement plotEl : elements().findPlots())
        {
            plots.add(new QCPlot(plotEl));
        }

        return plots;
    }

    public String getSVGPlotText(String id)
    {
        Locator loc = Locator.tagWithId("div", id).withDescendant(Locator.xpath("//*[local-name() = 'svg']"));
        WebElement svg = loc.findElement(_test.getDriver());
        return svg.getText();
    }

    public List<String> getPlotTitles()
    {
        List<String> titles = new ArrayList<>();

        for (QCPlot plot : getPlots())
        {
            titles.add(plot.getPrecursor());
        }

        return titles;
    }

    public void filterQCPlotsToInitialData(int expectedPlotCount, boolean resetForm)
    {
        if (resetForm)
        {
            resetInitialQCPlotFields();
        }

        if (!"2013-08-09".equals(getCurrentStartDate()) || !"2013-08-27".equals(getCurrentEndDate()))
        {
            filterQCPlots("2013-08-09", "2013-08-27", expectedPlotCount);
        }
    }

    public void resetInitialQCPlotFields()
    {
        // revert to the initial form values if any of them have changed
        if (getCurrentChartType() != QCPlotsWebPart.ChartType.RETENTION)
            setChartType(QCPlotsWebPart.ChartType.RETENTION);
        if (getCurrentScale() != QCPlotsWebPart.Scale.LINEAR)
            setScale(QCPlotsWebPart.Scale.LINEAR);
        if (isGroupXAxisValuesByDateChecked())
            setGroupXAxisValuesByDate(false);
        if (isShowAllPeptidesInSinglePlotChecked())
            setShowAllPeptidesInSinglePlot(false, null);
    }

    public void filterQCPlots(String startDate, String endDate, int expectedPlotCount)
    {
        setStartDate(startDate);
        setEndDate(endDate);
        applyRange();
        waitForPlots(expectedPlotCount, true);
    }

    public int getGuideSetTrainingRectCount()
    {
        return _test.getElementCount(elements().guideSetTrainingRect);
    }

    public int getGuideSetErrorBarPathCount(String cls)
    {
        return _test.getElementCount(Locator.css("svg g g.error-bar path." + cls));
    }

    public List<WebElement> getPointElements(String attr, String value, boolean isPrefix)
    {
        List<WebElement> matchingPoints = new ArrayList<>();

        for (WebElement point : elements().svgPointPath.findElements(this))
        {
            if ((isPrefix && point.getAttribute(attr).startsWith(value))
                || (!isPrefix && point.getAttribute(attr).equals(value)))
            {
                matchingPoints.add(point);
            }
        }

        return matchingPoints;
    }

    public WebElement getPointByAcquiredDate(String dateStr)
    {
        dateStr = dateStr.replaceAll("/", "-"); // convert 2013/08/14 -> 2013-08-14
        List<WebElement> points = elements().svgPoint.findElements(this);
        for (WebElement p : points)
        {
            if (p.getAttribute("title").contains("Acquired: " + dateStr))
                return p;
        }

        return null;
    }

    public void createGuideSet(GuideSet guideSet, String expectErrorMsg)
    {
        waitForPlots(1, false);
        _test.clickButton("Create Guide Set", 0);

        WebElement startPoint = getPointByAcquiredDate(guideSet.getStartDate());
        WebElement endPoint = getPointByAcquiredDate(guideSet.getEndDate());

        Actions builder = new Actions(_test.getDriver());
        builder.moveToElement(startPoint, -10, 0).clickAndHold().moveToElement(endPoint, 10, 0).release().perform();

        List<WebElement> gsButtons = elements().guideSetSvgButton.findElements(this);
        _test.shortWait().until(ExpectedConditions.elementToBeClickable(gsButtons.get(0)));

        Integer brushPointCount = getPointElements("fill", "rgba(20, 204, 201, 1)", false).size();
        assertEquals("Unexpected number of points selected via brushing", guideSet.getBrushSelectedPoints(), brushPointCount);

        WebElement plot = elements().findPlots().get(0);
        gsButtons.get(0).click(); // Create button : index 0
        if (guideSet.getBrushSelectedPoints() != null && guideSet.getBrushSelectedPoints() < 5)
            _test._ext4Helper.clickWindowButton("Create Guide Set Warning", "Yes", 0, 0);

        if (expectErrorMsg != null)
        {
            _test.waitForElement(Ext4Helper.Locators.window("Error Creating Guide Set"));
            _test.assertElementPresent(elements().extFormDisplay.withText(expectErrorMsg));
            _test._ext4Helper.clickWindowButton("Error Creating Guide Set", "OK", 0, 0);
            gsButtons.get(1).click(); // Cancel button : index 1
        }
        else
        {
            _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
            waitForReady();
        }
    }

    public int getLogScaleInvalidCount()
    {
        return elements().logScaleInvalid().size();
    }

    public int getLogScaleWarningCount()
    {
        return elements().logScaleWarning().size();
    }

    public Locator getLegendItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elements().legendItem.withText(text);
        else
            return elements().legendItem.containing(text);
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        WebElement startDate = new LazyWebElement(Locator.css("#start-date-field input"), this);
        WebElement endDate = new LazyWebElement(Locator.css("#end-date-field input"), this);
        Locator.XPathLocator scaleCombo = Locator.id("scale-combo-box");
        Locator.XPathLocator chartTypeCombo = Locator.id("chart-type-field");
        Checkbox groupedXCheckbox = new Checkbox(new LazyWebElement(Locator.css("#grouped-x-field input"), this));
        Checkbox singlePlotCheckbox = new Checkbox(new LazyWebElement(Locator.css("#peptides-single-plot input"), this));

        WebElement plotPanel = new LazyWebElement(Locator.css("div.tiledPlotPanel"), this);

        List<WebElement> findPlots() { return Locator.css("table.qc-plot-wp").waitForElements(plotPanel, 20000);}

        List<WebElement> noRecords() { return Locator.tagContainingText("span", "There were no records found.").findElements(plotPanel);}
        List<WebElement> logScaleInvalid() { return Locator.tagContainingText("span", "Log scale invalid for values").findElements(plotPanel);}
        List<WebElement> logScaleWarning() { return Locator.tagContainingText("span", "For log scale, standard deviations below the mean").findElements(plotPanel);}

        Locator extFormDisplay = Locator.css("div.x4-form-display-field");

        Locator.CssLocator guideSetTrainingRect = Locator.css("svg rect.training");
        Locator.CssLocator guideSetSvgButton = Locator.css("svg g.guideset-svg-button text");
        Locator.CssLocator svgPoint = Locator.css("svg g a.point");
        Locator.CssLocator svgPointPath = Locator.css("svg g a.point path");
        Locator.CssLocator legendItem = Locator.css("svg g.legend-item");
    }
}
