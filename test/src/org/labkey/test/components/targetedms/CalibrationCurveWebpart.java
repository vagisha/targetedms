/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CalibrationCurveWebpart extends BodyWebPart<CalibrationCurveWebpart.ElementCache>
{
    public CalibrationCurveWebpart(WebDriver driver)
    {
        super(driver, "Calibration Curve");
    }

    public List<String> getSvgLegendItems()
    {
        final List<String> texts = getWrapper().getTexts(elementCache().findLegendItems());
        texts.removeAll(Arrays.asList(""));
        return texts;
    }

    public List<WebElement> getSvgPoints()
    {
        return elementCache().findPoints();
    }

    public CalibrationCurveWebpart selectAnyPoint()
    {
        final List<WebElement> svgPoints = getSvgPoints();
        if (svgPoints.isEmpty())
            Assert.fail("SVG has no points to click");

        boolean successful = false;
        for (WebElement point : svgPoints)
        {
            try
            {
                point.click();
                successful = true;
            }
            catch (WebDriverException ex)
            {
                if (ex.getMessage().contains("Other element would receive the click"))
                    continue;
                throw ex;
            }
        }
        if (!successful)
            Assert.fail("Unable to click any point in calibration curve svg");

        return this;
    }

    public WebElement getSvg()
    {
        elementCache().svg.isSelected(); // trigger findWhenNeeded
        return elementCache().svg;
    }

    public File exportToPdf()
    {
        getWrapper().mouseOver(elementCache().svg);
        return getWrapper().doAndWaitForDownload(() -> elementCache().exportToPdf.click());
    }

    public File exportToPng()
    {
        getWrapper().mouseOver(elementCache().svg);
        return getWrapper().doAndWaitForDownload(() -> elementCache().exportToPng.click());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart.ElementCache
    {
        private final WebElement exportToPng = Locator.id("targetedmsCalibrationCurve-png").findWhenNeeded(this);
        private final WebElement exportToPdf = Locator.id("targetedmsCalibrationCurve-pdf").findWhenNeeded(this);
        private final WebElement svg = Locator.css("svg").findWhenNeeded(this);
        private List<WebElement> findPoints()
        {
            return Locator.css("a.point").findElements(svg);
        }
        private List<WebElement> findLegendItems()
        {
            return Locator.css("g.legend > g.legend-item").findElements(svg);
        }
    }
}
