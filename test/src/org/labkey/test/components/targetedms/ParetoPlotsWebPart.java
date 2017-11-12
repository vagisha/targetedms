/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedList;
import java.util.List;

public class ParetoPlotsWebPart extends BodyWebPart<ParetoPlotsWebPart.ElementCache>
{
    public static final String DEFAULT_TITLE = "Pareto Plots";

    public ParetoPlotsWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public enum MetricTypeTicks
    {
        RETENTION("Retention Time"),
        PEAK("Peak Area"),
        FWHM("Full Width at Half Maximum (FWHM)"),
        FWB("Full Width at Base (FWB)"),
        LHRATIO("Light/Heavy Ratio"),
        TPAREARATIO("Transition/Precursor Area Ratio"),
        PAREA("Precursor Area"),
        TAREA("Transition Area"),
        MASSACCURACY("Mass Accuracy");

        private String _text;

        MetricTypeTicks(String text)
        {
            _text = text;
        }

        public String toString()
        {
            return _text;
        }

        public static String getMetricTypeTick(String value)
        {
            String metricTypeTick = null;

            for(MetricTypeTicks v : values())
            {
                if (value.contains(v.toString()))
                {
                    metricTypeTick = v.toString();
                    break;
                }
            }

            return metricTypeTick;
        }
    }
    public enum ParetoPlotType
    {
        LeveyJennings("Levey-Jennings", ""),
        MovingRange("Moving Range", "_mR"),
        CUSUMm("Mean CUSUM", "_CUSUMm"),
        CUSUMv("Variability CUSUM", "_CUSUMv");

        private String _label;
        private String _suffix;

        ParetoPlotType(String text, String idSuffix)
        {
            _label = text;
            _suffix = idSuffix;
        }

        public String getLabel()
        {
            return _label;
        }

        public String getIdSuffix()
        {
            return _suffix;
        }
    }
    public List<String> getTicks(int guideSetNum)
    {
        return getTicks(guideSetNum, ParetoPlotType.LeveyJennings);
    }
    public List<String> getTicks(int guideSetNum, ParetoPlotType plotType)
    {
        List<String> ticks = new LinkedList<>();
        int maxIndex = 5;
        int minIndex = 1;

        while(minIndex <= maxIndex)
        {
            String tickText = Locator.css("#paretoPlot-GuideSet-" + guideSetNum  + plotType.getIdSuffix() +
                    " > svg > g:nth-child(1) > g.tick-text > a:nth-child(" + minIndex + ")").findElement(getDriver()).getText();
            ticks.add(tickText);
            minIndex++;
        }
        return ticks;
    }

    public int getNumOfParetoPlots()
    {
        return Locator.xpath("//div[contains(@id, 'tiledPlotPanel')]/table[contains(@class, 'labkey-wp pareto-plot-wp')]").findElements(getDriver()).size();
    }

    public boolean isMetricTypeTickValid(String metricType)
    {
        return MetricTypeTicks.getMetricTypeTick(metricType) != null;
    }

    public int getPlotBarHeight(int guideSetId, int barPlotNum)
    {
        return getPlotBarHeight(guideSetId, ParetoPlotType.LeveyJennings, barPlotNum);
    }

    public int getPlotBarHeight(int guideSetId, ParetoPlotType plotType, int barPlotNum)
    {
       return Integer.parseInt(Locator.css("#paretoPlot-GuideSet-" + guideSetId + plotType.getIdSuffix() + "-0" +
               " > a:nth-child(" + (barPlotNum+1) + ")").findElement(getDriver()).getText());
    }

    public String getPlotBarTooltip(int guideSetId, ParetoPlotType plotType, int barPlotNum)
    {
        return Locator.css("#paretoPlot-GuideSet-" + guideSetId + plotType.getIdSuffix() + "-0" +
                " > a:nth-child(" + (barPlotNum+1) + ")").findElement(getDriver()).getText();
    }

    public void clickQCPlotsLink(BaseWebDriverTest test)
    {
        Assert.assertTrue(elementCache().notFound.isDisplayed()); //Check for no guide sets
        test.clickAndWait(elementCache().qcPlotsLink); //click on the link to take user to the QC Plots webpart
    }

    public void waitForTickLoad(int guideSetNum)
    {
        waitForTickLoad(guideSetNum, ParetoPlotType.LeveyJennings);
    }

    public void waitForTickLoad(int guideSetNum, ParetoPlotType plotType)
    {
        getWrapper().waitForElement(Locator.css("#paretoPlot-GuideSet-" + guideSetNum + plotType.getIdSuffix() +
                " > svg > g:nth-child(1) > g.tick-text > a:nth-child(1)"));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebPart.ElementCache
    {
        WebElement notFound = new LazyWebElement(Locator.tagWithClass("div", "tiledPlotPanel").startsWith("No Guide Sets found in this folder."), this).withTimeout(1000);
        WebElement qcPlotsLink = new LazyWebElement(Locator.linkWithText("QC Plots"), this);
    }
}
