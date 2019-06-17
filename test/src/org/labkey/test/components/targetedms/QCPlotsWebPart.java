/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.Window.Window;

public final class QCPlotsWebPart extends BodyWebPart<QCPlotsWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "QC Plots";

    public QCPlotsWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public QCPlotsWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    public enum Scale
    {
        LINEAR("Linear"),
        LOG("Log"),
        PERCENT_OF_MEAN("Percent of Mean"),
        STANDARD_DEVIATIONS("Standard Deviations");

        private String _text;

        Scale(String text)
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

    public enum DateRangeOffset
    {
        ALL(0, "All dates"),
        CUSTOM(-1, "Custom range");

        private Integer _offset;
        private String _label;

        DateRangeOffset(Integer offset, String label)
        {
            _offset = offset;
            _label = label;
        }

        public Integer getOffset()
        {
            return _offset;
        }

        public String toString()
        {
            return _label;
        }

        public static DateRangeOffset getEnum(String value)
        {
            for (DateRangeOffset v : values())
                if (v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }
    }

    public enum QCPlotType
    {
        LeveyJennings("Levey-Jennings", "Levey-Jennings", ""),
        MovingRange("Moving Range", "Moving Range", "_mR"),
        CUSUMm("CUSUMm", "Mean CUSUM", "_CUSUMm"),
        CUSUMv("CUSUMv", "Variability CUSUM", "_CUSUMv");

        private String _label;
        private String _labellong;
        private String _suffix;

        QCPlotType(String shortlabel, String longlabel, String idSuffix)
        {
            _label = shortlabel;
            _labellong = longlabel;
            _suffix = idSuffix;
        }

        public String getLabel()
        {
            return _label;
        }

        public String getLongLabel()
        {
            return _labellong;
        }

        public String getIdSuffix()
        {
            return _suffix;
        }
    }

    public enum QCPlotExclusionState
    {
        Include("Include in QC"),
        ExcludeMetric("Exclude sample from QC for this metric"),
        ExcludeAll("Exclude sample from QC for all metrics");

        private String _label;

        QCPlotExclusionState(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }
    }


    private void waitForNoRecords()
    {
        WebDriverWrapper.waitFor(() -> elementCache().noRecords().size() > 0, 10000);
    }

    @LogMethod(quiet = true)
    public void setScale(@LoggedParam Scale scale)
    {
        WebElement plot = elementCache().findPlots().get(0);
        getWrapper()._ext4Helper.selectComboBoxItem(elementCache().scaleCombo, scale.toString());
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForPlots();
    }

    public Scale getCurrentScale()
    {
        WebElement scaleInput = elementCache().scaleCombo.append("//input").waitForElement(this, 1000);
        return Scale.getEnum(scaleInput.getAttribute("value"));
    }

    @LogMethod(quiet = true)
    public void setDateRangeOffset(@LoggedParam DateRangeOffset dateRangeOffset)
    {
        if (dateRangeOffset == null)
            dateRangeOffset = DateRangeOffset.ALL;
        getWrapper()._ext4Helper.selectComboBoxItem(elementCache().dateRangeCombo, dateRangeOffset.toString());
    }

    public DateRangeOffset getCurrentDateRangeOffset()
    {
        WebElement scaleInput = elementCache().dateRangeCombo.append("//input").waitForElement(this, 1000);
        return DateRangeOffset.getEnum(scaleInput.getAttribute("value"));
    }

    @LogMethod(quiet = true)
    public void setStartDate(@LoggedParam String startDate)
    {
        getWrapper().setFormElement(elementCache().startDate, startDate);
    }

    public String getCurrentStartDate()
    {
        return getWrapper().getFormElement(elementCache().startDate);
    }

    @LogMethod(quiet = true)
    public void setEndDate(@LoggedParam String endDate)
    {
        getWrapper().setFormElement(elementCache().endDate, endDate);
    }

    public String getCurrentEndDate()
    {
        return getWrapper().getFormElement(elementCache().endDate);
    }

    public enum MetricType
    {
        RETENTION("Retention Time", true),
        PEAK("Peak Area", true),
        FWHM("Full Width at Half Maximum (FWHM)", true),
        FWB("Full Width at Base (FWB)", true),
        LHRATIO("Light/Heavy Ratio", false),
        TPAREARATIO("Transition/Precursor Area Ratio", true),
        TPAREAS("Transition & Precursor Areas", true),
        MASSACCURACTY("Mass Accuracy", true);

        private String _text;
        private boolean _hasData;

        MetricType(String text, boolean hasData)
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

        public static MetricType getEnum(String value)
        {
            for(MetricType v : values())
                if(v.toString().equalsIgnoreCase(value))
                    return v;
            throw new IllegalArgumentException();
        }
    }

    public void setMetricType(MetricType metricType)
    {
        setMetricType(metricType, true, true);
    }

    public void setMetricType(MetricType metricType, boolean hasData)
    {
        setMetricType(metricType, hasData, true);
    }

    @LogMethod
    public void setMetricType(@LoggedParam MetricType metricType, boolean hasData, boolean hasExistingPlot)
    {
        WebElement plot = null;
        if (hasExistingPlot)
            plot = elementCache().findPlots().get(0);

        getWrapper()._ext4Helper.selectComboBoxItem(elementCache().metricTypeCombo, metricType.toString());

        if (hasExistingPlot)
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(plot));

        if (hasData)
            waitForPlots();
        else
            waitForNoRecords();
    }

    public List<String> getMetricTypeOptions()
    {
        return getWrapper()._ext4Helper.getComboBoxOptions(elementCache().metricTypeCombo);
    }

    public MetricType getCurrentMetricType()
    {
        WebElement typeInput = elementCache().metricTypeCombo.append("//input").waitForElement(this, 1000);
        return MetricType.getEnum(typeInput.getAttribute("value"));
    }

    public void setGroupXAxisValuesByDate(boolean check)
    {
        WebElement plot = elementCache().findPlots().get(0);
        elementCache().groupedXCheckbox.set(check);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForPlots();
    }

    public boolean isGroupXAxisValuesByDateChecked()
    {
        return elementCache().groupedXCheckbox.isChecked();
    }

    public void chooseSmallPlotSize(boolean small)
    {
        String label = "Small";
        if (!small)
            label = "Large";
        RadioButton plotSizeButton = RadioButton.RadioButton().withLabel(label).find(this);
        plotSizeButton.check();
    }

    public boolean isSmallPlotSizeSelected()
    {
        RadioButton plotSizeButton = RadioButton.RadioButton().withLabel("Small").find(this);
        return plotSizeButton.isChecked();
    }

    public boolean isPlotSizeRadioEnabled()
    {
        RadioButton plotSizeButton = RadioButton.RadioButton().withLabel("Small").find(this);
        return plotSizeButton.isEnabled();
    }

    /**
     * This should be called only when a plot is visible.
     */
    public void setShowAllPeptidesInSinglePlot(boolean check, @Nullable Integer expectedPlotCount)
    {
        WebElement plot = elementCache().findPlots().get(0);
        elementCache().singlePlotCheckbox.set(check);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(plot));
        waitForPlots();

        if (expectedPlotCount != null)
            waitForPlots(expectedPlotCount, true);
        else
            waitForPlots(1, false);
    }

    public void setShowExcludedPoints(boolean check)
    {
        elementCache().showExcludedCheckbox.set(check);
    }

    public boolean isShowAllPeptidesInSinglePlotChecked()
    {
        return elementCache().singlePlotCheckbox.isChecked();
    }

    public void applyRange()
    {
        WebElement panelChild = Locator.css("svg").findElement(elementCache().plotPanel); // The panel itself doesn't become stale, but its children do
        getWrapper().clickButton("Apply", 0);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(panelChild));
        getWrapper()._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void waitForPlots()
    {
        waitForPlots(1, false);
    }

    public void waitForPlots(Integer plotCount, boolean exact)
    {
        if (plotCount > 0)
        {
            if (exact)
                WebDriverWrapper.waitFor(() -> elementCache().findPlots().size() == plotCount, WebDriverWrapper.WAIT_FOR_PAGE);
            else
                WebDriverWrapper.waitFor(() -> elementCache().findPlots().size() >= plotCount, WebDriverWrapper.WAIT_FOR_PAGE);
        }
        else
        {
            getWrapper().longWait().until(ExpectedConditions.textToBePresentInElement(elementCache().plotPanel, "There were no records found. The date filter applied may be too restrictive."));
        }
    }

    public List<QCPlot> getPlots()
    {
        List<QCPlot> plots = new ArrayList<>();

        for (WebElement plotEl : elementCache().findPlots())
        {
            plots.add(new QCPlot(plotEl));
        }

        return plots;
    }

    public String getSVGPlotText(String id)
    {
        Locator loc = Locator.tagWithId("div", id).withDescendant(Locator.xpath("//*[local-name() = 'svg']"));
        WebElement svg = loc.findElement(getWrapper().getDriver());
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

        filterQCPlots("2013-08-09", "2013-08-27", expectedPlotCount);
    }

    @LogMethod
    public void resetInitialQCPlotFields()
    {
        // revert to the initial form values if any of them have changed
        if (getCurrentMetricType() != MetricType.RETENTION)
            setMetricType(MetricType.RETENTION);
        if (getCurrentDateRangeOffset() != DateRangeOffset.ALL)
            setDateRangeOffset(DateRangeOffset.ALL);
        if (isPlotTypeSelected(QCPlotType.MovingRange) || isPlotTypeSelected(QCPlotType.CUSUMm) || isPlotTypeSelected(QCPlotType.CUSUMv))
        {
            checkAllPlotTypes(false);
            checkPlotType(QCPlotsWebPart.QCPlotType.LeveyJennings, true);
            waitForPlots();
        }
        if (isPlotSizeRadioEnabled())
        {
            chooseSmallPlotSize(true);
            waitForPlots();
        }
        if (getCurrentScale() != QCPlotsWebPart.Scale.LINEAR)
        {
            setScale(QCPlotsWebPart.Scale.LINEAR);
            waitForPlots();
        }
        else
        {
            // work around to close Plot Type popup
            setScale(Scale.LOG);
            waitForPlots();
            setScale(Scale.LINEAR);
            waitForPlots();
        }
        if (isGroupXAxisValuesByDateChecked())
        {
            setGroupXAxisValuesByDate(false);
            waitForPlots();
        }
        if (isShowAllPeptidesInSinglePlotChecked())
            setShowAllPeptidesInSinglePlot(false, null);

        waitForPlots();
    }

    @LogMethod
    public void filterQCPlots(@LoggedParam String startDate, @LoggedParam String endDate, int expectedPlotCount)
    {
        setDateRangeOffset(DateRangeOffset.CUSTOM);
        setStartDate(startDate);
        setEndDate(endDate);
        applyRange();
        waitForPlots(expectedPlotCount, true);
    }

    public int getGuideSetTrainingRectCount()
    {
        return elementCache().guideSetTrainingRect.findElements(getDriver()).size();
    }

    public int getGuideSetErrorBarPathCount(String cls)
    {
        return Locator.css("svg g g.error-bar path." + cls).findElements(getDriver()).size();
    }

    public List<WebElement> getPointElements(String attr, String value, boolean isPrefix)
    {
        Locator.tag("svg").waitForElement(this, WAIT_FOR_JAVASCRIPT);
        List<WebElement> matchingPoints = new ArrayList<>();

        for (WebElement point : elementCache().svgPointPath.findElements(this))
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
        WebElement point = elementCache().svgPoint.attributeStartsWith("id", dateStr).findElementOrNull(this);
        if (point == null)
        {
            throw new NoSuchElementException("Unable to find svg point with with acquired date: " + dateStr);
        }
        return point;
    }

    public WebElement openExclusionBubble(String acquiredDate)
    {
        getWrapper().shortWait().ignoring(StaleElementReferenceException.class).withMessage("Exclusion pop-up for Acquired Date = " + acquiredDate)
                .until(input -> {
                    getWrapper().mouseOver(getPointByAcquiredDate(acquiredDate));
                    return getWrapper().isElementPresent(Locator.tagWithClass("div", "x4-form-display-field").withText(acquiredDate));
                });
        return elementCache().hopscotchBubble.findElement(getDriver());
    }

    @LogMethod
    public void createGuideSet(@LoggedParam GuideSet guideSet, String expectErrorMsg)
    {
        waitForPlots(1, false);
        getWrapper().clickButton("Create Guide Set", 0);

        WebElement startPoint;
        WebElement endPoint;
        int xStartOffset, yStartOffset;
        int xEndOffset, yEndOffset;
        yStartOffset = 10;
        yEndOffset = 10;

        // If StartDate is empty use the far left of the svg as the starting point.
        if(!guideSet.getStartDate().trim().isEmpty())
        {
            startPoint = getPointByAcquiredDate(guideSet.getStartDate());
            xStartOffset = -10;
        }
        else
        {
            startPoint = elementCache().svgBackgrounds.findElements(this).get(0);
            xStartOffset = -1 * (Integer.parseInt(startPoint.getAttribute("width")) / 2);
        }

        // If EndDate is empty use the far right of the svg as the ending point.
        if(!guideSet.getEndDate().trim().isEmpty())
        {
            endPoint = getPointByAcquiredDate(guideSet.getEndDate());
            xEndOffset = 10;
        }
        else
        {
            endPoint = elementCache().svgBackgrounds.findElements(this).get(0);
            xEndOffset = (Integer.parseInt(endPoint.getAttribute("width")) / 2) - 1;;
        }

        getWrapper().scrollIntoView(startPoint);

        Actions builder = new Actions(getWrapper().getDriver());

        builder.moveToElement(startPoint, xStartOffset, yStartOffset).clickAndHold().moveToElement(endPoint, xEndOffset, yEndOffset).release().perform();

        List<WebElement> gsButtons = elementCache().guideSetSvgButton.findElements(this);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(gsButtons.get(0)));

        Integer brushPointCount = getPointElements("fill", "rgba(20, 204, 201, 1)", false).size();
        assertEquals("Unexpected number of points selected via brushing", guideSet.getBrushSelectedPoints(), brushPointCount);

        boolean expectPageReload = expectErrorMsg == null;
        if (guideSet.getBrushSelectedPoints() != null && guideSet.getBrushSelectedPoints() < 5)
        {
            gsButtons.get(0).click(); // Create button : index 0
            Window warning = Window(getDriver()).withTitle("Create Guide Set Warning").waitFor();
            if (expectPageReload)
                warning.clickButton("Yes");
            else
                warning.clickButton("Yes", false);
        }
        else if (expectPageReload)
        {
            getWrapper().clickAndWait(gsButtons.get(0)); // Create button : index 0
        }
        else
        {
            gsButtons.get(0).click(); // Create button : index 0
        }

        if (expectErrorMsg != null)
        {
            Window error = Window(getDriver()).withTitle("Error Creating Guide Set").waitFor();
            getWrapper().assertElementPresent(elementCache().extFormDisplay.withText(expectErrorMsg));
            error.clickButton("OK", true);
            gsButtons.get(1).click(); // Cancel button : index 1
        }
    }

    public int getLogScaleInvalidCount()
    {
        return elementCache().logScaleInvalid().size();
    }

    public int getLogScaleWarningCount()
    {
        return elementCache().logScaleWarning().size();
    }

    public int getLogScaleEpsilonWarningCount()
    {
        return elementCache().logScaleEpsilonWarning().size();
    }

    public Locator getLegendItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elementCache().legendItem.withText(text);
        else
            return elementCache().legendItem.containing(text);
    }

    public Locator getLegendItemLocatorByTitle(String text)
    {
        return elementCache().legendItemTitle.withText(text);
    }

    public Locator getLegendPopupItemLocator(String text, boolean exactMatch)
    {
        if (exactMatch)
            return elementCache().legendItemPopup.withText(text);
        else
            return elementCache().legendItemPopup.containing(text);
    }

    public Locator getSmallPlotLoc()
    {
        return elementCache().smallPlotLayoutDiv;
    }

    public String getPaginationText()
    {
        return elementCache().paginationPanel.getText();
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public void openLegendPopup()
    {
        getWrapper().waitAndClick(Locator.tagWithText("span", "View Legend"));
        Window(getDriver()).withTitle("Legends").waitFor();
    }

    public void checkPlotType(QCPlotType plotType, boolean checked)
    {
        Checkbox checkbox = elementCache().findQCPlotTypeCheckbox(plotType);
        getWrapper().scrollIntoView(checkbox.getComponentElement());
        checkbox.set(checked);
        dismissTooltip();
    }

    private void dismissTooltip()
    {
        int halfWidth = elementCache().webPartTitle.getSize().getWidth() / 2;
        int xOffset = elementCache().webPartTitle.getLocation().getX() + halfWidth; // distance to edge of window from center of element
        new Actions(getDriver())
                .moveToElement(elementCache().webPartTitle) // Start at the center of the title
                .moveByOffset(-xOffset, 0) // Move all the way to the left edge of the window
                .perform(); // Should dismiss hover tooltips
        WebElement closeHopscotch = Locator.byClass("hopscotch-close").findElementOrNull(getDriver());
        if (closeHopscotch != null && closeHopscotch.isDisplayed())
            closeHopscotch.click();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfElementLocated(Locator.byClass("hopscotch-callout")));
    }

    public boolean isPlotTypeSelected(QCPlotType plotType)
    {
        return elementCache().findQCPlotTypeCheckbox(plotType).isChecked();
    }

    public void checkAllPlotTypes(boolean selected)
    {
        for (QCPlotsWebPart.QCPlotType plotType : QCPlotsWebPart.QCPlotType.values())
        {
            checkPlotType(plotType, selected);
        }
    }

    public List<QCPlotType> getSelectedPlotTypes()
    {
        List<QCPlotType> selected = new ArrayList<>();
        for (QCPlotsWebPart.QCPlotType plotType : QCPlotsWebPart.QCPlotType.values())
        {
            if (isPlotTypeSelected(plotType))
                selected.add(plotType);
        }
        return selected;
    }

    public void closeBubble()
    {
        WebElement closeButton = elementCache().hopscotchBubbleClose.findElement(getDriver());
        closeButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(closeButton));
    }

    public void goToPreviousPage()
    {
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationPrevBtn.findElement(this).click());
    }

    public void goToNextPage()
    {
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().paginationNextBtn.findElement(this).click());
    }

    public class Elements extends BodyWebPart.ElementCache
    {
        WebElement startDate = new LazyWebElement(Locator.css("#start-date-field input"), this);
        WebElement endDate = new LazyWebElement(Locator.css("#end-date-field input"), this);
        Locator.XPathLocator scaleCombo = Locator.id("scale-combo-box");
        Locator.XPathLocator dateRangeCombo = Locator.id("daterange-combo-box");
        Locator.XPathLocator metricTypeCombo = Locator.id("metric-type-field");
        Checkbox groupedXCheckbox = new Checkbox(Locator.css("#grouped-x-field input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox singlePlotCheckbox = new Checkbox(Locator.css("#peptides-single-plot input")
                .findWhenNeeded( this).withTimeout(WAIT_FOR_JAVASCRIPT));
        Checkbox showExcludedCheckbox = new Checkbox(Locator.css("#show-excluded-points input")
                .findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT));

        WebElement plotPanel = new LazyWebElement(Locator.css("div.tiledPlotPanel"), this);
        WebElement paginationPanel = new LazyWebElement(Locator.css("div.plotPaginationHeaderPanel"), this);

        List<WebElement> findPlots() { return Locator.css("table.qc-plot-wp").waitForElements(plotPanel, 20000);}

        List<WebElement> noRecords() { return Locator.tagContainingText("span", "There were no records found.").findElements(plotPanel);}
        List<WebElement> logScaleInvalid() { return Locator.tagContainingText("span", "Log scale invalid for values").findElements(plotPanel);}
        List<WebElement> logScaleWarning() { return Locator.tagContainingText("span", "For log scale, standard deviations below the mean").findElements(plotPanel);}
        List<WebElement> logScaleEpsilonWarning() { return Locator.tagContainingText("span", "Values that are 0 have been replaced").findElements(plotPanel);}

        Locator extFormDisplay = Locator.css("div.x4-form-display-field");

        Locator.CssLocator guideSetTrainingRect = Locator.css("svg rect.training");
        Locator.CssLocator guideSetSvgButton = Locator.css("svg g.guideset-svg-button text");
        Locator.CssLocator svgPoint = Locator.css("svg g a.point");
        Locator.CssLocator svgPointPath = Locator.css("svg g a.point path");
        Locator.CssLocator legendItem = Locator.css("svg g.legend-item");
        Locator.CssLocator legendItemTitle = Locator.css("svg g.legend-item title");
        Locator.CssLocator legendItemPopup = Locator.css(".headerlegendpopup svg g.legend-item");
        Locator.CssLocator smallPlotLayoutDiv = Locator.css(".plot-small-layout");
        Locator.CssLocator paginationPrevBtn = Locator.css(".qc-paging-prev");
        Locator.CssLocator paginationNextBtn = Locator.css(".qc-paging-next");
        Locator.CssLocator svgBackgrounds = Locator.css("svg g.brush rect.background");

        Locator.XPathLocator hopscotchBubble = Locator.byClass("hopscotch-bubble-container");
        Locator.XPathLocator hopscotchBubbleClose = Locator.byClass("hopscotch-bubble-close");

        private Map<QCPlotType, Checkbox> plotTypeCheckboxes = new HashMap<>();
        protected Checkbox findQCPlotTypeCheckbox(QCPlotType plotType)
        {
            if (!plotTypeCheckboxes.containsKey(plotType))
                plotTypeCheckboxes.put(plotType, Ext4Checkbox().withLabel(plotType.getLabel()).waitFor(this));
            return plotTypeCheckboxes.get(plotType);
        }
    }
}
