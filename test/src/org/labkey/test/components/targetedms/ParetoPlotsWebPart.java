package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;

import java.util.LinkedList;
import java.util.List;

public class ParetoPlotsWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Pareto Plots";

    public ParetoPlotsWebPart(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public enum ChartTypeTicks
    {
        TPAREARATIO("T/PA Ratio"),
        RETENTION("RT"),
        PEAK("PA"),
        FWHM("FWHM)"),
        FWB("FWB");

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
            String tickText = _test.getElement(Locator.css("#paretoPlot-GuideSet-"+ guideSetNum +
                    " > svg > g:nth-child(1) > g.tick-text > a:nth-child("+ minIndex+")")).getText();
            ticks.add(tickText);
            minIndex++;
        }
        return ticks;
    }

    public int getNumOfParetoPlots()
    {
        return _test.getElementCount(Locator.xpath("//div[contains(@id, 'tiledPlotPanel')]/table[contains(@class, 'labkey-wp pareto-plot-wp')]"));
    }

    public boolean isChartTypeTickValid(String chartType)
    {
        return ChartTypeTicks.getChartTypeTick(chartType) != null;

    }

    public int getPlotBarHeight(int guideSetId, int barPlotNum)
    {
       return Integer.parseInt(_test.getText(Locator.css("#paretoPlot-GuideSet-" + guideSetId + "-" + barPlotNum + " > a:nth-child(1)")));
    }

    public void clickLeveyJenningsLink(BaseWebDriverTest test)
    {
        test.assertElementPresent(elements().notFound); //Check for no guide sets
        test.clickAndWait(elements().leveyJenningsLink); //click on the link to take user to Levey-Jennings plot
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        Locator.XPathLocator notFound = webPart.append(Locator.id("tiledPlotPanel").startsWith("Guide Sets not found."));
        Locator.XPathLocator leveyJenningsLink = webPart.append(Locator.linkWithText("Levey-Jennings QC Plots"));
    }
}
