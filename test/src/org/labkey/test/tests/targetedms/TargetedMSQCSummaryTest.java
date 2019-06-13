/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.tests.targetedms;

import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 12)
public class TargetedMSQCSummaryTest extends TargetedMSTest
{
    private static final String FOLDER_1 = "QC Subfolder 1";
    private static final String FOLDER_2 = "QC Subfolder 2";
    private static final String FOLDER_2A = "QC Subfolder 2a";
    private static final String FOLDER_3 = "NonQC Subfolder 3";
    private static final int QCPING_WAIT = 61000; // Value used for sleep, in milliseconds.
    private static final String QCPING_TIMEOUT = "1"; // Value set in the Module Properties. This is in minutes.
    private static final String BUBBLE_TIME_FORMAT = "yyyy-MM-dd HH:mm";


    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCSummaryTest init = (TargetedMSQCSummaryTest)getCurrentTest();
        init.setupProjectWithSubfolders();
        init.importInitialData();
    }

    private void setupProjectWithSubfolders()
    {
        setupFolder(FolderType.QC);

        setupSubfolder(getProjectName(), FOLDER_1, FolderType.QC);
        setupSubfolder(getProjectName(), FOLDER_2, FolderType.QC);
        setupSubfolder(getProjectName(), FOLDER_3, FolderType.Experiment);

        clickFolder(FOLDER_2);
        setupSubfolder(getProjectName(), FOLDER_2, FOLDER_2A, FolderType.QC);
    }

    private void importInitialData()
    {
        goToProjectHome();
        importData(SProCoP_FILE);

        clickFolder(FOLDER_2);
        importData(QC_1_FILE);

        clickFolder(FOLDER_2A);
        importData(QC_2_FILE);
    }

    private void setAutoQCPingTimeOut(String timeOutLength)
    {
        goToProjectHome();
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Module Properties"));

        List<ModulePropertyValue> values = new ArrayList<>();
        values.add(new ModulePropertyValue("TargetedMS", "/" + getProjectName(), "TargetedMS AutoQCPing Timeout", timeOutLength));

        setModuleProperties(values);

        goToProjectHome();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSubfolders()
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        qcSummaryWebPart.waitForRecentSampleFiles(6);
        List<QCSummaryWebPart.QcSummaryTile> summaryTiles = qcSummaryWebPart.getQcSummaryTiles();

        assertEquals("Unexpected number of QC Summary tiles", 3, summaryTiles.size());
        verifyQcSummary(summaryTiles.get(0), getProjectName(), 47, 7);
        verifyQcSummary(summaryTiles.get(1), FOLDER_1, 0, 0);
        verifyQcSummary(summaryTiles.get(2), FOLDER_2, 3, 2);
    }

    @Test
    public void testPermissions()
    {
        _userHelper.createUser(USER);

        // give user reader permissions to all but FOLDER_1
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, getProjectName() + "/" + FOLDER_2);
        permissionsHelper.addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, getProjectName() + "/" + FOLDER_2 + "/" + FOLDER_2A);
        permissionsHelper.addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, getProjectName() + "/" + FOLDER_3);

        // impersonate user and check that the project QC Summary doesn't include the FOLDER_1 details
        goToProjectHome();
        impersonate(USER);
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        qcSummaryWebPart.waitForRecentSampleFiles(6);
        List<QCSummaryWebPart.QcSummaryTile> qcSummaryTiles = qcSummaryWebPart.getQcSummaryTiles();
        assertEquals("Unexpected number of QC Summary tiles", 2, qcSummaryTiles.size());
        verifyQcSummary(qcSummaryTiles.get(0), getProjectName(), 47, 7);
        verifyQcSummary(qcSummaryTiles.get(1), FOLDER_2, 3, 2);
        stopImpersonating();
    }

    @Test
    public void testSampleFiles()
    {
        int sampleFileCount = 3;

        clickFolder(FOLDER_2A);
        waitForRecentSampleFiles(3);
        verifyQcSummary(1, sampleFileCount, 2);

        // verify the initial set of QC plot points
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        qcPlotsWebPart.setShowAllPeptidesInSinglePlot(true, 1);
        assertEquals("Unexpected number of points", 2 * sampleFileCount, getQCPlotPointCount());

        // remove a sample file
        clickAndWait(Locator.linkWithText(sampleFileCount + " sample files"));
        DataRegionTable table = new DataRegionTable("query", getDriver());
        // Delete the oldest sample (of three), which are sorted in reverse chronological order
        table.checkCheckbox(2);
        doAndWaitForPageToLoad(() -> {
            table.clickHeaderButton("Delete");
            assertAlert("Are you sure you want to delete the selected row?");
        });
        sampleFileCount--;
        PanoramaDashboard panoramaDashboard = goToDashboard();
        panoramaDashboard.getQcSummaryWebPart().waitForRecentSampleFiles(2);
        verifyQcSummary(1, sampleFileCount, 2);
        assertEquals("Unexpected number of points", 2 * sampleFileCount, getQCPlotPointCount());

        log("Validate the recently loaded file content is correct.");
        List<String> tempStringList01 = new ArrayList<>();
        List<List<String>> tempStringList02 = new ArrayList<>();
        tempStringList01.add("2015-01-16 15:08 - no outliers");
        tempStringList01.add("2015-01-16 12:47 - no outliers");
        tempStringList02.add(Arrays.asList("25fmol_Pepmix_spike_SRM_1601_04", "Acquired Date/Time: 2015-01-16 15:08"));
        tempStringList02.add(Arrays.asList("25fmol_Pepmix_spike_SRM_1601_03", "Acquired Date/Time: 2015-01-16 12:47"));
        validateSampleFile(0, tempStringList01, tempStringList02);

        // remove all sample files
        clickAndWait(Locator.linkWithText(sampleFileCount + " sample files"));
        table.checkAllOnPage();
        doAndWaitForPageToLoad(() -> {
            table.clickHeaderButton("Delete");
            assertAlert("Are you sure you want to delete the selected rows?");
        });
        sampleFileCount = 0;
        clickTab("Panorama Dashboard");
        waitForElement(Locator.linkWithText(sampleFileCount + " sample files"));
        assertElementNotPresent(Locator.tagWithClass("div", "sample-file-item"));
        assertElementPresent(Locator.tagContainingText("div", "No data found."));
    }

    @Test
    public void testShowAutoQC()
    {
        String lastPingedDate;
        List<String> tempStringList01 = new ArrayList<>();
        List<List<String>> tempStringList02 = new ArrayList<>();
        final int MAIN_SUMMARY = 0;
        final int SUB_FOLDER01 = 1;
        final int SUB_FOLDER02 = 2;

        // Set the time out length.
        setAutoQCPingTimeOut(QCPING_TIMEOUT);

        waitForElements(Locator.tagWithClass("div", "sample-file-item"), 6);
        tempStringList01.add("2013-08-27 14:45 - no outliers");
        tempStringList01.add("2013-08-27 03:19 - no outliers");
        tempStringList01.add("2013-08-26 04:27 - no outliers");
        tempStringList02.add(Arrays.asList("Q_Exactive_08_23_2013_JGB_58", "Acquired Date/Time: 2013-08-27 14:45"));
        tempStringList02.add(Arrays.asList("Q_Exactive_08_23_2013_JGB_51", "Acquired Date/Time: 2013-08-27 03:19"));
        tempStringList02.add(Arrays.asList("Out of guide set range: no outliers"));
        validateSampleFile(0, tempStringList01, tempStringList02);

        tempStringList01.clear();
        tempStringList01.add("qc-none");
        tempStringList01.add("fa-circle-o");
        validateAutoQCStatus(MAIN_SUMMARY, tempStringList01, "Has never been pinged");

        log("Ping the data.");
        //http://localhost:8080/labkey/TargetedMSQCSummaryTest%20Project/QC%20Subfolder%202/targetedms-autoqcping.view
        lastPingedDate = doAutoQCPing(null);

        log("Need to refresh the page to see the updated status.");
        refresh();
        waitForElements(Locator.tagWithClass("div", "sample-file-item"), 6);

        tempStringList01.clear();
        tempStringList01.add("qc-correct");
        tempStringList01.add("fa-check-circle");
        validateAutoQCStatus(MAIN_SUMMARY, tempStringList01, "Was pinged recently on " + lastPingedDate);

        log("Now wait for ping limit to occur.");
        sleep(QCPING_WAIT);

        log("Again need to refresh the page to see the updated status.");
        refresh();

        tempStringList01.clear();
        tempStringList01.add("qc-error");
        tempStringList01.add("fa-circle");
        validateAutoQCStatus(MAIN_SUMMARY, tempStringList01, "Was pinged on " + lastPingedDate);

        log("Now validate the icon for the sub-folder 1.");
        tempStringList01.clear();
        tempStringList01.add("qc-none");
        tempStringList01.add("fa-circle-o");
        validateAutoQCStatus(SUB_FOLDER01, tempStringList01, "Has never been pinged");

        log("Now validate the icon for the sub-folder 2.");
        tempStringList01.clear();
        tempStringList01.add("qc-none");
        tempStringList01.add("fa-circle-o");
        validateAutoQCStatus(SUB_FOLDER02, tempStringList01, "Has never been pinged");

        log("Ping the data in Subfolder 2.");
        lastPingedDate = doAutoQCPing(FOLDER_2);

        log("Refresh the page.");
        refresh();
        waitForElements(Locator.tagWithClass("div", "sample-file-item"), 6);

        log("Validate the updated icons for the sub-folder 2.");
        tempStringList01.clear();
        tempStringList01.add("qc-correct");
        tempStringList01.add("fa-check-circle");
        validateAutoQCStatus(SUB_FOLDER02, tempStringList01, "Was pinged recently on " + lastPingedDate);

        log("Now wait for ping limit to occur.");
        sleep(QCPING_WAIT);

        log("Again need to refresh the page to see the updated status.");
        refresh();

        log("Validate the ping timeout icons for the sub-folder 2.");
        tempStringList01.clear();
        tempStringList01.add("qc-error");
        tempStringList01.add("fa-circle");
        validateAutoQCStatus(SUB_FOLDER02, tempStringList01, "Was pinged on " + lastPingedDate);

        log("Validate that a guide set updates the file info as expected.");
        GuideSet gs = new GuideSet("2013-08-22 00:00", "2013-08-27 00:04", null);
        createGuideSetFromTable(gs);

        goToProjectHome();
        waitForRecentSampleFiles(6);

        tempStringList01.clear();
        tempStringList01.add("2013-08-27 14:45 - 1/56 (Levey-Jennings), 1/56 (Moving Range)");
        tempStringList01.add("2013-08-27 03:19 - 4/56 (Moving Range) outliers");

        tempStringList02.clear();
        tempStringList02.add(Arrays.asList("Q_Exactive_08_23_2013_JGB_58", "Full Width at Half Maximum (FWHM) 1 1 0 0 0 0"));
        tempStringList02.add(Arrays.asList("Q_Exactive_08_23_2013_JGB_51", "Peak Area 0 2 0 0 0 0"));
        validateSampleFile(0, tempStringList01, tempStringList02);

        removeAllGuideSets();

        // Reset the time out length.
        setAutoQCPingTimeOut("");

    }

    private void validateAutoQCStatus(int webPartIndex, List<String> iconClassValues, String bubbleText)
    {
        String tmpString;

        // Create a reference to the web page and its various parts.
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        WebElement autoQC = qcSummaryWebPart.getQcSummaryTiles().get(webPartIndex).getAutoQCIcon();

        tmpString = autoQC.getAttribute("class");

        for(String classValue : iconClassValues)
        {
            log("Validate that the autoQC icon has a value of '" + classValue + "' in its class property.");
            assertTrue("AutoQC icon not as expected. Class did not contain '" + classValue + "'. Class: '" + tmpString + "'", tmpString.toLowerCase().contains(classValue));
        }

        log("Validate bubble text is '" + bubbleText + "'");
        mouseOver(autoQC);
        waitForElement(qcSummaryWebPart.getBubble());
        waitForElement(qcSummaryWebPart.getBubbleContent().containing(bubbleText));

        // move the mouse off the element to remove the bubble.
        mouseOver(Locator.css(".labkey-page-nav"));

    }

    private void validateSampleFile(int fileDetailIndex, List<String> fileDetails, List<List<String>> bubbleTexts)
    {
        if (fileDetails.size() != bubbleTexts.size())
            throw new IllegalArgumentException("The fileDetails and bubbleTexts list are not of equal length.");

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();

        for(int i = 0; i< fileDetails.size(); i++)
        {
            String fileDetailText = fileDetails.get(i);
            List<String> perBubbleTexts = bubbleTexts.get(i);
            QCSummaryWebPart.QcSummaryTile qcSummaryTile = qcSummaryWebPart.getQcSummaryTiles().get(fileDetailIndex);

            String actualFileDetailText = qcSummaryTile.getRecentSampleFiles().get(i).getText();
            log("Validate that the file detail text is '" + fileDetailText + "'.");
            assertTrue("File detail text not as expected. File detail text: '" + actualFileDetailText + "'" + " Expected: '" + fileDetailText + "'", actualFileDetailText.toLowerCase().contains(fileDetailText.toLowerCase()));

            mouseOver(qcSummaryTile.getRecentSampleFiles().get(i));
            waitForElement(qcSummaryWebPart.getBubble());
            if (perBubbleTexts != null && !perBubbleTexts.isEmpty())
            {
                TextSearcher textSearcher = new TextSearcher(() -> waitForElement(qcSummaryWebPart.getBubbleContent()).getText());
                if (!waitFor(() -> textSearcher.getMissingTexts(perBubbleTexts).isEmpty(), 10000))
                {
                    String actualText = textSearcher.getLastSearchedText();
                    fail("The bubble text for the file detail not as expected. Bubble text: '" + actualText + "' Missing: '" + String.join(",", perBubbleTexts.stream().filter(s -> !actualText.contains(s)).collect(Collectors.toList())) + "'");
                }
            }
            qcSummaryWebPart.closeBubble();
            
            log("Move the mouse to avoid another hopscotch bubble.");
            mouseOver(Locator.css(".labkey-page-nav"));
            waitForElementToDisappear(qcSummaryWebPart.getBubble());
        }

    }

    private void waitForRecentSampleFiles(int count)
    {
        new QCSummaryWebPart(getDriver()).waitForRecentSampleFiles(count);
    }

    private int getQCPlotPointCount()
    {
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        return qcPlotsWebPart.getPointElements("d", SvgShapes.CIRCLE.getPathPrefix(), true).size();
    }

    private String doAutoQCPing(@Nullable String subFolder)
    {
        Connection cn = createDefaultConnection(true);
        AutoQCPing aqcp = new AutoQCPing();
        CommandResponse cr;
        String folderPath = getProjectName();

        if (null != subFolder)
        {
            folderPath = folderPath + "/" + subFolder;
        }

        try
        {
            cr = aqcp.execute(cn, folderPath);
            String lastPingedDate = cr.getProperty("Modified");
            Date date = new SimpleDateFormat(BUBBLE_TIME_FORMAT).parse(lastPingedDate);
            return FastDateFormat.getInstance(BUBBLE_TIME_FORMAT).format(date);
        }
        catch (IOException | CommandException | ParseException e)
        {
            throw new RuntimeException("Error trying to ping.", e);
        }
    }

    public class AutoQCPing extends PostCommand<CommandResponse>
    {
        public AutoQCPing()
        {
            super("targetedms", "autoqcping");
        }
    }
}
