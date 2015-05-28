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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class QCPlotsWebPart extends BodyWebPart
{
    private static final String DEFAULT_TITLE = "QC Plots";

    public QCPlotsWebPart(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public QCPlotsWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, index);
    }

    @Override
    protected void waitForReady()
    {
        _test.waitForElement(elements().plot);
    }

    public static enum Scale
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

    @LogMethod
    public void setScale(Scale scale)
    {
        WebElement plot = elements().plot.findElement(_test.getDriver());
        _test._ext4Helper.selectComboBoxItem(elements().scaleCombo, scale.toString());
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    public Scale getCurrentScale()
    {
        WebElement scaleInput = elements().scaleCombo.append("//input").findElement(_test.getDriver());
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

    public static enum ChartType
    {
        RETENTION("Retention Time"),
        PEAK("Peak Area"),
        FWHM("Full Width at Half Maximum (FWHM)"),
        FWB("Full Width at Base (FWB)"),
        TPAREARATIO("Transition/Precursor Area Ratio");

        private String _text;

        private ChartType(String text)
        {
            _text = text;
        }

        public String toString()
        {
            return _text;
        }

        public static ChartType getEnum(String value)
        {
            for(ChartType v : values())
                if(v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }
    }

    @LogMethod
    public void setChartType(ChartType chartType)
    {
        WebElement plot = elements().plot.findElement(_test.getDriver());
        _test._ext4Helper.selectComboBoxItem(elements().chartTypeCombo, chartType.toString());
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    public ChartType getCurrentChartType()
    {
        WebElement typeInput = elements().chartTypeCombo.append("//input").findElement(_test.getDriver());
        return ChartType.getEnum(typeInput.getAttribute("value"));
    }

    public void setGroupXAxisValuesByDate(boolean check)
    {
        WebElement plot = elements().plot.findElement(_test.getDriver());
        if (check)
            _test._ext4Helper.checkCheckbox(elements().groupedXCheckbox);
        else
            _test._ext4Helper.uncheckCheckbox(elements().groupedXCheckbox);
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForReady();
    }

    public void applyRange()
    {
        WebElement plotPanel = elements().plotPanel.findElement(_test.getDriver());
        WebElement panelChild = Locator.css("*").findElement(plotPanel); // The panel itself doesn't become stale, but its children do
        _test.clickButton("Apply", 0);
        _test.shortWait().until(ExpectedConditions.stalenessOf(panelChild));
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void waitForPlots(Integer plotCount, boolean exact)
    {
        if (plotCount > 0)
        {
            if (exact)
                _test.waitForElements(elements().plot, plotCount);
            else
                waitForReady();
        }
        else
        {
            _test.waitForElement(elements().plotPanel.withText("There were no records found. The date filter applied may be too restrictive."));
        }
    }

    public List<QCPlot> getPlots()
    {
        List<WebElement> plotEls = elements().plot.findElements(_test.getDriver());
        List<QCPlot> plots = new ArrayList<>();

        for (WebElement plotEl : plotEls)
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

    public void filterQCPlotsToInitialData(int expectedPlotCount)
    {
        filterQCPlots("2013-08-09", "2013-08-27", expectedPlotCount);
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

        for (WebElement point : elements().svgPointPath.findElements(_test.getDriver()))
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
        List<WebElement> points = elements().svgPoint.findElements(_test.getDriver());
        for (WebElement p : points)
        {
            if (p.getAttribute("title").startsWith("Acquired: " + dateStr))
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

        List<WebElement> gsButtons = elements().guideSetSvgButton.findElements(_test.getDriver());
        _test.shortWait().until(ExpectedConditions.elementToBeClickable(gsButtons.get(0)));

        Integer brushPointCount = getPointElements("fill", "rgba(20, 204, 201, 1)", false).size();
        assertEquals("Unexpected number of points selected via brushing", guideSet.getBrushSelectedPoints(), brushPointCount);

        WebElement plot = elements().plot.findElement(_test.getDriver());
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

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        Locator.XPathLocator scaleCombo = webPart.append(Locator.id("scale-combo-box"));
        Locator.XPathLocator startDate = webPart.append(Locator.id("start-date-field")).append("//input");
        Locator.XPathLocator endDate = webPart.append(Locator.id("end-date-field")).append("//input");
        Locator.XPathLocator chartTypeCombo = webPart.append(Locator.id("chart-type-field"));
        Locator.XPathLocator groupedXCheckbox = webPart.append(Locator.id("grouped-x-field")).append("//input");

        Locator.XPathLocator plotPanel = webPart.append(Locator.tagWithId("div", "tiledPlotPanel"));
        Locator.XPathLocator plot = plotPanel.append(Locator.tagWithClass("table", "qc-plot-wp"));
        Locator.XPathLocator plotTitle = plot.append(Locator.tagWithClass("span", "qc-plot-wp-title"));

        Locator.XPathLocator extFormDisplay = Locator.tagWithClass("div", "x4-form-display-field");

        Locator.CssLocator guideSetTrainingRect = Locator.css("svg rect.training");
        Locator.CssLocator guideSetSvgButton = Locator.css("svg g.guideset-svg-button text");
        Locator.CssLocator svgPoint = Locator.css("svg g a.point");
        Locator.CssLocator svgPointPath = Locator.css("svg g a.point path");
    }
}
