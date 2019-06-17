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

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class QCSummaryWebPart extends BodyWebPart<QCSummaryWebPart.Elements>
{
    public static final String DEFAULT_TITLE = "QC Summary";

    public QCSummaryWebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public QCSummaryWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
        waitForLoad();
    }

    @Override
    public void clearCache()
    {
        super.clearCache();
    }

    public void waitForLoad()
    {
        Locator.tagWithClass("div", "item-text").waitForElement(this, 15000);
        Locators.recentSampleFilesLoading.waitForElementToDisappear(this, 30000);
    }

    public Locator.XPathLocator getBubble()
    {
        return Locators.hopscotchBubble;
    }

    public void closeBubble()
    {
        getWrapper().click(Locators.hopscotchBubbleClose);
    }

    public Locator.XPathLocator getBubbleContent()
    {
        return Locators.hopscotchBubbleContent;
    }

    public String getBubbleText()
    {
        return Locators.hopscotchBubbleContent.withText().waitForElement(getDriver(), 1000).getText();
    }

    public List<QcSummaryTile> getQcSummaryTiles()
    {
        return elementCache().summaryTiles();
    }

    public void waitForRecentSampleFiles(int count)
    {
        assertEquals("Details for wrong number of sample files in QC Summary.", count, Locators.recentSampleFile.findElements(this).size());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends BodyWebPart.ElementCache
    {
        private List<QcSummaryTile> summaryTiles;
        public List<QcSummaryTile> summaryTiles()
        {
            if (summaryTiles == null)
            {
                summaryTiles = new ArrayList<>();
                int index = 0;
                List<WebElement> els = Locators.summaryTile.findElements(this);
                for (WebElement el : els)
                {
                    summaryTiles.add(new QcSummaryTile(el, index++));
                }
            }
            return summaryTiles;
        }
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator hopscotchBubble = Locator.byClass("hopscotch-bubble-container");
        static final Locator.XPathLocator hopscotchBubbleContent = hopscotchBubble.append(Locator.byClass("hopscotch-bubble-content").append(Locator.byClass("hopscotch-content")));
        static final Locator.XPathLocator hopscotchBubbleClose = Locator.byClass("hopscotch-bubble-close");
        static final Locator summaryTile = Locator.tagWithClass("div", "summary-tile");
        static final Locator recentSampleFilesLoading = Locator.tagWithClass("div", "sample-file-details-loading");
        static final Locator recentSampleFile = Locator.css("div.sample-file-item");
    }

    public class QcSummaryTile extends Component
    {
        private final WebElement _el;

        private final Locator emptyText = Locator.tagWithClass("div", "item-text").withText("No sample files imported");
        private final WebElement folderLink = Locator.css("div.folder-name a").findWhenNeeded(this);
        private final WebElement autoQCIcon = Locator.css("div.auto-qc-ping span").findWhenNeeded(this);
        private List<WebElement> _recentSampleFiles;

        private String _folderName;
        private Integer _fileCount;
        private Integer _precursorCount;
        private final int _index;

        protected QcSummaryTile(WebElement el, int index)
        {
            _el = el;
            _index = index;
        }

        @Override
        public WebElement getComponentElement()
        {
            return _el;
        }

        public int getIndex()
        {
            return _index;
        }

        private void readSummary()
        {
            waitForLoad();
            String qcSummary = getComponentElement().getText();

            Pattern filePattern = Pattern.compile("(\\d+) sample file");
            Matcher fileMatcher = filePattern.matcher(qcSummary);
            _fileCount = fileMatcher.find() ? Integer.parseInt(fileMatcher.group(1)) : 0;

            Pattern precursorPattern = Pattern.compile("(\\d+) precursor");
            Matcher precursorMatcher = precursorPattern.matcher(qcSummary);
            _precursorCount = precursorMatcher.find() ? Integer.parseInt(precursorMatcher.group(1)) : 0;
        }

        public String getFolderName()
        {
            if (_folderName == null)
                _folderName = folderLink.getText();
            return _folderName;
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

        public boolean hasNoSkylineDocuments()
        {
            return !emptyText.findElements(this).isEmpty();
        }

        public WebElement getAutoQCIcon()
        {
            assertTrue("AutoQC Icon not visible for " + getFolderName(), autoQCIcon.isDisplayed());
            return autoQCIcon;
        }

        public List<WebElement> getRecentSampleFiles()
        {
            if (_recentSampleFiles == null)
            {
                _recentSampleFiles = new ArrayList<>();
                _recentSampleFiles.addAll(Locators.recentSampleFile.findElements(this));
            }
            return _recentSampleFiles;
        }

        public boolean hasRecentSampleFileWithOulierTxt(String acquiredDate, String outlierStr)
        {
            acquiredDate = acquiredDate.substring(0, acquiredDate.lastIndexOf(":")); // Sample file listing doesn't include seconds
            return Locators.recentSampleFile.withText(" " + acquiredDate + " - " + outlierStr).findElements(this).size() == 1;
        }
    }
}
