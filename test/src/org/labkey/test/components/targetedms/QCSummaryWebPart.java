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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QCSummaryWebPart extends BodyWebPart
{
    public static final String DEFAULT_TITLE = "QC Summary";

    private Integer _docCount;
    private Integer _fileCount;
    private Integer _precursorCount;

    public QCSummaryWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public QCSummaryWebPart(BaseWebDriverTest test, int index)
    {
        super(test, DEFAULT_TITLE, index);
    }

    public void clearCache()
    {
        _docCount = null;
        _fileCount = null;
        _precursorCount = null;
    }

    private void readSummary()
    {
        String qcSummary = _test.getText(elements().qcSummary);

        Pattern docPattern = Pattern.compile("(\\d+) Skyline documents");
        Matcher docMatcher = docPattern.matcher(qcSummary);
        Assert.assertTrue(qcSummary, docMatcher.find());
        _docCount = Integer.parseInt(docMatcher.group(1));

        Pattern filePattern = Pattern.compile("(\\d+) sample files");
        Matcher fileMatcher = filePattern.matcher(qcSummary);
        Assert.assertTrue(qcSummary, fileMatcher.find());
        _fileCount = Integer.parseInt(fileMatcher.group(1));

        Pattern precursorPattern = Pattern.compile("(\\d+) precursors");
        Matcher precursorMatcher = precursorPattern.matcher(qcSummary);
        Assert.assertTrue(qcSummary, precursorMatcher.find());
        _precursorCount = Integer.parseInt(precursorMatcher.group(1));
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

    @Override
    protected Elements elements()
    {
        return new Elements();
    }

    private class Elements extends BodyWebPart.Elements
    {
        public Locator.XPathLocator qcSummary = webPart.append(Locator.tagWithId("div", "qcSummary-1"));
    }
}
