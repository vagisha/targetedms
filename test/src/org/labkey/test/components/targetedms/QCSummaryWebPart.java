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

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public final class QCSummaryWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "QC Summary";

    private Integer _docCount;
    private Integer _fileCount;
    private Integer _precursorCount;

    public QCSummaryWebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public QCSummaryWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    public void clearCache()
    {
        _docCount = null;
        _fileCount = null;
        _precursorCount = null;
    }

    private void readSummary()
    {
        readSummary(0);
    }

    public void readSummary(int index)
    {
        String qcSummary = elements().qcSummary.findElement(getDriver()).getText();
        if (index > 0)
            qcSummary = getQCSummaryDetails().get(index).getText();

        Pattern docPattern = Pattern.compile("(\\d+) Skyline document");
        Matcher docMatcher = docPattern.matcher(qcSummary);
        _docCount = docMatcher.find() ? Integer.parseInt(docMatcher.group(1)) : 0;

        Pattern filePattern = Pattern.compile("(\\d+) sample file");
        Matcher fileMatcher = filePattern.matcher(qcSummary);
        _fileCount = fileMatcher.find() ? Integer.parseInt(fileMatcher.group(1)) : 0;

        Pattern precursorPattern = Pattern.compile("(\\d+) precursor");
        Matcher precursorMatcher = precursorPattern.matcher(qcSummary);
        _precursorCount = precursorMatcher.find() ? Integer.parseInt(precursorMatcher.group(1)) : 0;
    }

    public int getDocCount()
    {
        if (_docCount == null)
            readSummary();
        return _docCount;
    }

    public int getFileCount()
    {
        if (_fileCount == null)
            readSummary();
        return _fileCount;
    }

    public int getPrecursorCount()
    {
        if (_precursorCount == null)
            readSummary();
        return _precursorCount;
    }

    public Locator getFolderNameLinkLocator(int index)
    {
        return getQCSummaryIndexLocator(index).append(elements().qcSummaryFolderLink);
    }

    public Locator getEmptyTextLocator(int index)
    {
        return getQCSummaryIndexLocator(index).append(elements().qcSummaryEmptyText);
    }

    private Locator.XPathLocator getQCSummaryIndexLocator(int index)
    {
        return new Locator.XPathLocator("(" + elements().qcSummaryDetails.getPath() + ")[" + (index+1) + "]");
    }

    public List<WebElement> getQCSummaryDetails()
    {
        return elements().qcSummaryDetails.findElements(_test.getDriver());
    }

    public Locator.XPathLocator getAutoQCIcon()
    {
        return  getAutoQCIcon(0);
    }

    public Locator.XPathLocator getAutoQCIcon(int index)
    {
        Locator.XPathLocator autoQCIcon;

        _test.log("Trying to get the " + index + " autoQCIcon.");
        autoQCIcon = elements().qcSummaryAutoQCIcon.index(index);
        _test.assertElementVisible(autoQCIcon);

        return  autoQCIcon;
    }

    public Locator.XPathLocator getBubble()
    {
        return elements().qcSummaryHopscotchBubble;
    }

    public void closeBubble()
    {
        _test.click(elements().qcSummaryHopscotchBubbleClose);
    }

    public String getBubbleText()
    {
        return _test.getText(elements().qcSummaryHopscotchBubbleContent);
    }

    public Locator.XPathLocator getSampleFileDetails(int index)
    {
        return elements().qcSummarySampleFileDetails.index(index);
    }

    public Locator.XPathLocator getSampleFileItem(int detailIndex, int itemIndex)
    {
        return elements().qcSummarySampleFileDetails.index(detailIndex).append(elements().qcSummarySampleFileItem).index(itemIndex);
    }

    public String getSampleFileItemText(int detailIndex, int itemIndex)
    {
        return _test.getText(getSampleFileItem(detailIndex, itemIndex));
    }

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        public Locator.XPathLocator qcSummary = Locator.id(_componentElement.getAttribute("id")).append(Locator.tagWithId("div", "qcSummary-1"));
        public Locator.XPathLocator qcSummaryDetails = qcSummary.append(Locator.tagWithClass("div", "summary-view"));
        public Locator.XPathLocator qcSummaryFolderLink = Locator.tagWithClass("div", "folder-name").append(Locator.tag("a"));
        public Locator.XPathLocator qcSummaryEmptyText = Locator.tagWithClass("div", "item-text").withText("No Skyline documents");
        public Locator.XPathLocator qcSummaryAutoQC = Locator.tagWithClass("div", "auto-qc-ping");
        public Locator.XPathLocator qcSummaryAutoQCIcon = qcSummaryAutoQC.append(Locator.xpath("//span"));
        public Locator.XPathLocator qcSummaryHopscotchBubble = Locator.tagWithClass("div", "hopscotch-bubble-container");
        public Locator.XPathLocator qcSummaryHopscotchBubbleContent = qcSummaryHopscotchBubble.append(Locator.tagWithClass("div", "hopscotch-bubble-content").append(Locator.tagWithClass("div", "hopscotch-content")));
        public Locator.XPathLocator qcSummaryHopscotchBubbleClose = Locator.tagWithClass("a", "hopscotch-bubble-close");
        public Locator.XPathLocator qcSummarySampleFileDetails = Locator.tagWithClass("div", "sample-file-details");
        public Locator.XPathLocator qcSummarySampleFileItem = Locator.tagWithClass("div", "sample-file-item");
    }
}
