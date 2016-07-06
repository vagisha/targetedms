/*
 * Copyright (c) 2015 LabKey Corporation
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
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedList;
import java.util.List;

public class ParetoPlotsWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Pareto Plots";

    public ParetoPlotsWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public enum ChartTypeTicks
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

        ChartTypeTicks(String text)
        {
            _text = text;
        }

        public String toString()
        {
            return _text;
        }

        public static String getChartTypeTick(String value)
        {
            String chartTypeTick = null;

            for(ChartTypeTicks v : values())
            {
                if (value.contains(v.toString()))
                {
                    chartTypeTick = v.toString();
                    break;
                }
            }

            return chartTypeTick;
        }
    }
    public List<String> getTicks(int guideSetNum)
    {
        List<String> ticks = new LinkedList<>();
        int maxIndex = 5;
        int minIndex = 1;

        while(minIndex <= maxIndex)
        {
            String tickText = Locator.css("#paretoPlot-GuideSet-" + guideSetNum +
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

    public boolean isChartTypeTickValid(String chartType)
    {
        return ChartTypeTicks.getChartTypeTick(chartType) != null;
    }

    public int getPlotBarHeight(int guideSetId, int barPlotNum)
    {
       return Integer.parseInt(Locator.css("#paretoPlot-GuideSet-" + guideSetId + "-0 > a:nth-child(" + (barPlotNum+1) + ")").findElement(getDriver()).getText());
    }

    public void clickLeveyJenningsLink(BaseWebDriverTest test)
    {
        Assert.assertTrue(elements().notFound.isDisplayed()); //Check for no guide sets
        test.clickAndWait(elements().leveyJenningsLink); //click on the link to take user to Levey-Jennings plot
    }

    public void waitForTickLoad(int guideSetNum)
    {
        _test.waitForElement(Locator.css("#paretoPlot-GuideSet-" + guideSetNum +
                " > svg > g:nth-child(1) > g.tick-text > a:nth-child(1)"));
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        WebElement notFound = new LazyWebElement(Locator.tagWithClass("div", "tiledPlotPanel").startsWith("Guide Sets not found."), this).withTimeout(1000);
        WebElement leveyJenningsLink = new LazyWebElement(Locator.linkWithText("Levey-Jennings QC Plots"), this);
    }
}
