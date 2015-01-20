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
        _test._extHelper.selectComboBoxItem(elements().scaleCombo, scale.toString());
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        _test.waitForElement(elements().plot);
    }

    public Scale getCurrentScale()
    {
        WebElement scaleInput = elements().scaleCombo.append("/input").findElement(_test.getDriver());
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
        _test._extHelper.selectComboBoxItem(elements().chartTypeCombo, chartType.toString());
        _test.shortWait().until(ExpectedConditions.stalenessOf(plot));
        _test.waitForElement(elements().plot);
    }

    public ChartType getCurrentChartType()
    {
        WebElement typeInput = elements().chartTypeCombo.append("/input").findElement(_test.getDriver());
        return ChartType.getEnum(typeInput.getAttribute("value"));
    }

    public void applyRange()
    {
        WebElement plotPanel = elements().plotPanel.findElement(_test.getDriver());
        _test.click(Locator.button("Apply"));
        _test.shortWait().until(ExpectedConditions.stalenessOf(plotPanel));
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

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        Locator.XPathLocator scaleCombo = webPart.append(Locator.id("scale-combo-box")).parent();
        Locator.XPathLocator startDate = webPart.append(Locator.id("start-date-field"));
        Locator.XPathLocator endDate = webPart.append(Locator.id("end-date-field"));
        Locator.XPathLocator chartTypeCombo = webPart.append(Locator.id("chart-type-field")).parent();

        Locator.XPathLocator plotPanel = webPart.append(Locator.tagWithId("div", "tiledPlotPanel"));
        Locator.XPathLocator plot = plotPanel.append(Locator.tagWithClass("table", "labkey-wp"));
        Locator.XPathLocator plotTitle = plot.append(Locator.tagWithClass("span", "labkey-wp-title-text"));
    }

    public class QCPlot
    {
        WebElement plot;
        String precursor;

        QCPlot(WebElement plot)
        {
            this.plot = plot;
            this.precursor = elements().precursor.findElement(plot).getText();
        }

        public String getPrecursor()
        {
            return precursor;
        }

        public List<String> getAnnotations()
        {
            List<WebElement> annotations = elements().annotation.findElements(plot);
            List<String> annotationStrings = new ArrayList<>();

            for (WebElement annotation : annotations)
            {
                annotationStrings.add(annotation.getText());
            }

            return annotationStrings;
        }

        public String getSvgText()
        {
            WebElement svg = elements().svg.findElement(plot);
            return svg.getText();
        }

        private Elements elements()
        {
            return new Elements();
        }

        private class Elements
        {
            Locator precursor = Locator.css(".labkey-wp-title-text");
            Locator svg = Locator.css("svg");
            Locator annotation = Locator.css(".annotation");
        }
    }
}
