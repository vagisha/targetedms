package org.labkey.test.components.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;

import java.util.HashMap;
import java.util.Map;

public class ParetoPlotsWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "Pareto Plots";

    public ParetoPlotsWebPart(BaseWebDriverTest test)
    {
        super(test, DEFAULT_TITLE);
    }

    public static enum ChartTypeTicks
    {
        TPAREARATIO("T/PA Ratio"),
        RETENTION("RT"),
        PEAK("PA"),
        FWHM("FWHM)"),
        FWB("FWB");

        private String _text;

        private ChartTypeTicks(String text)
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
    public Map<Locator, String> getTicks(int guideSetNum, BaseWebDriverTest test)
    {
        Map<Locator, String> tickMap = new HashMap<>();
        int maxIndex = 5;
        int minIndex = 1;

        while(minIndex <= maxIndex)
        {
            Locator key = Locator.css("#paretoPlot-GuideSet-"+ guideSetNum +" > svg > g:nth-child(1) > g.tick-text > a:nth-child("+ minIndex+")");
            String val = test.getElement(key).getText();
            tickMap.put(key, val);
            minIndex++;
        }
        return tickMap;
    }

    public int getNumOfParetoPlots(BaseWebDriverTest test)
    {
        return test.getElementCount(Locator.xpath("//div[contains(@id, 'tiledPlotPanel')]/table[contains(@class, 'labkey-wp pareto-plot-wp')]"));
    }

    public boolean isChartTypeTickValid(String chartType)
    {
        if(ChartTypeTicks.getChartTypeTick(chartType) != null)
            return true;

        return false;
    }

    public int getPlotBarHeight(BaseWebDriverTest test, int guideSetId, int barPlotNum)
    {
       return Integer.valueOf(test.getText(Locator.css("#paretoPlot-GuideSet-" + guideSetId + "-" + barPlotNum + " > a:nth-child(1)")));
    }
}
