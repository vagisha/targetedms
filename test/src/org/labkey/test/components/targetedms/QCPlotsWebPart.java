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
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

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
        _test.waitForElement(elements().plot);
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
        FWB("Full Width at Base (FWB)");

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
        _test.waitForElement(elements().plot);
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
        _test.waitForElement(elements().plot);
    }

    public void applyRange()
    {
        WebElement plotPanel = elements().plotPanel.findElement(_test.getDriver());
        WebElement panelChild = Locator.css("*").findElement(plotPanel); // The panel itself doesn't become stale, but its children do
        _test.clickButton("Apply", 0);
        _test.shortWait().until(ExpectedConditions.stalenessOf(panelChild));
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void waitForPlots(Integer plotCount)
    {
        if (plotCount > 0)
            _test.waitForElements(elements().plot, plotCount);
        else
            _test.waitForElement(Locator.id("tiledPlotPanel").withText("There were no records found. The date filter applied may be too restrictive."));
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
        List<WebElement> titleEls = elements().plotTitle.findElements(_test.getDriver());
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
        waitForPlots(expectedPlotCount);
    }

    public int getGuideSetTrainingRectCount()
    {
        return _test.getElementCount(Locator.css("svg rect.training"));
    }

    public int getGuideSetErrorBarPathCount(String cls)
    {
        return _test.getElementCount(Locator.css("svg g g.error-bar path." + cls));
    }

    public List<WebElement> getPointElements()
    {
        return Locator.css("svg g a.point path").findElements(_test.getDriver());
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
    }
}
