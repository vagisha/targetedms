/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.GuideSetWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class TargetedMSTest extends BaseWebDriverTest
{
    protected static final String SProCoP_FILE = "SProCoPTutorial.zip";
    protected static final String QC_1_FILE = "QC_1.sky.zip";
    protected static final String QC_2_FILE = "QC_2.sky.zip";
    protected static final String QC_3_FILE = "QC_3.sky.zip";
    protected static final String USER = "qcuser@targetedms.test";

    protected enum SvgShapes
    {
        CIRCLE("M0,3A"),
        TRIANGLE("M0,3L"),
        SQUARE("M-3"),
        DIAMOND("M0 3");

        private String _pathPrefix;
        SvgShapes(String pathPrefix)
        {
            _pathPrefix = pathPrefix;
        }

        public String getPathPrefix()
        {
            return _pathPrefix;
        }
    }

    public enum FolderType {
        Experiment, Library, LibraryProtein, QC, Undefined
    }

    public TargetedMSTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @LogMethod
    protected void setupFolder(FolderType folderType)
    {
        _containerHelper.createProject(getProjectName(), "Panorama");
        selectFolderType(folderType);
    }

    protected void setupSubfolder(String projectName, String folderName, FolderType folderType)
    {
        setupSubfolder(projectName, projectName, folderName, folderType);
    }

    @LogMethod
    protected void setupSubfolder(String projectName, String parentFolderName, String folderName, FolderType folderType)
    {
        _containerHelper.createSubfolder(projectName, parentFolderName, folderName, "Panorama", null, false);
        selectFolderType(folderType);
    }

    @LogMethod
    protected void importData(String file)
    {
        importData(file, 1);
    }

    @LogMethod
    protected void importData(String file, int jobCount)
    {
        importData(file, jobCount, true);
    }

    @LogMethod
    protected void importData(String file, int jobCount, boolean uploadIfDoesntExist)
    {
        log("Importing file " + file);
        goToModule("Pipeline");
        clickButton("Process and Import Data");
        if (uploadIfDoesntExist && !isElementPresent(FileBrowserHelper.Locators.gridRow(file)))
            _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("TargetedMS/" + file));
        _fileBrowserHelper.importFile(file, "Import Skyline Results");
        waitForText("Skyline document import");
        waitForPipelineJobsToFinish(jobCount);
    }

    @LogMethod
    protected void verifyRunSummaryCounts(int proteinCount, int peptideCount, int precursorCount, int transitionCount, @Nullable Integer moleculeCount)
    {
        log("Verifying expected summary counts");
        assertElementPresent(Locator.xpath("//tr[td[text()='Protein Count']][td[text()='" + proteinCount + "']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Peptide Count']][td[text()='" + peptideCount + "']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Precursor Count']][td[text()='" + precursorCount + "']]"));
        assertElementPresent(Locator.xpath("//tr[td[text()='Transition Count']][td[text()='" + transitionCount + "']]"));

        if(moleculeCount != null)
            assertElementPresent(Locator.xpath("//tr[td[text()='Small Molecule Count']][td[text()='" + moleculeCount + "']]"));

    }

    @LogMethod
    protected void selectFolderType(FolderType folderType) {
        log("Select Folder Type: " + folderType);
        switch(folderType)
        {
            case Experiment:
                click(Locator.radioButtonById("experimentalData")); // click the first radio button - Experimental Data
                break;
            case Library:
                click(Locator.radioButtonById("chromatogramLibrary")); // click the 2nd radio button - Library
                break;
            case LibraryProtein:
                click(Locator.radioButtonById("chromatogramLibrary")); // click the 2nd radio button - Library
                click(Locator.checkboxByName("precursorNormalized")); // check the normalization checkbox.
                break;
            case QC:
                click(Locator.radioButtonById("QC")); // click the 3rd radio button - QC
                break;
        }
        clickButton("Finish");
    }

    protected void verifyQcSummary(int docCount, int sampleFileCount, int precursorCount)
    {
        verifyQcSummary(0, null, docCount, sampleFileCount, precursorCount);
    }

    protected void verifyQcSummary(int summaryIndex, String folderName)
    {
        verifyQcSummary(summaryIndex, folderName, 0, 0, 0);
    }

    @LogMethod
    protected void verifyQcSummary(int summaryIndex, String folderName, int docCount, int sampleFileCount, int precursorCount)
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        qcSummaryWebPart.readSummary(summaryIndex);

        assertEquals("Wrong number of Skyline documents uploaded for index " + summaryIndex, docCount, qcSummaryWebPart.getDocCount());
        assertEquals("Wrong number sample files for index " + summaryIndex, sampleFileCount, qcSummaryWebPart.getFileCount());
        assertEquals("Wrong number of precursors tracked for index " + summaryIndex, precursorCount, qcSummaryWebPart.getPrecursorCount());

        if (docCount == 0 && sampleFileCount == 0 && precursorCount == 0)
            assertElementPresent(qcSummaryWebPart.getEmptyTextLocator(summaryIndex));
        else
            assertElementNotPresent(qcSummaryWebPart.getEmptyTextLocator(summaryIndex));

        if (folderName != null)
        {
            Locator loc = qcSummaryWebPart.getFolderNameLinkLocator(summaryIndex);
            assertElementPresent(loc);
            assertEquals("Wrong folder name QC Summary tile title", folderName, getText(loc));
        }
    }

    @LogMethod
    protected void createGuideSetFromTable(GuideSet guideSet)
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        GuideSetWebPart guideSetWebPart = new GuideSetWebPart(this, getProjectName());
        GuideSetPage guideSetPage = guideSetWebPart.startInsert();
        guideSetPage.insert(guideSet, null);
    }

    @LogMethod
    protected void removeAllGuideSets()
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        DataRegionTable table = new DataRegionTable("qwp1", this);
        table.checkAll();
        table.clickHeaderButtonByText("Delete");
        assertAlertContains("Are you sure you want to delete the selected row");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteUsersIfPresent(USER);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("targetedms");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
