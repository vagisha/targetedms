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

import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.GuideSetWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.components.targetedms.TargetedMSRunsTable;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ConfiguresSite;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DefaultSiteConfigurer;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.ReflectionUtils;
import org.labkey.test.util.UIContainerHelper;
import org.openqa.selenium.WebElement;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class TargetedMSTest extends BaseWebDriverTest
{
    protected static final String SProCoP_FILE = "SProCoPTutorial.zip";
    protected static final String SProCoP_FILE_ANNOTATED = "SProCoPTutorial_withAnnotations.zip";
    protected static final String QC_1_FILE = "QC_1.sky.zip";
    protected static final String QC_1a_FILE = "QC_1a.sky.zip";
    protected static final String QC_1b_FILE = "QC_1b.sky.zip";
    protected static final String QC_2_FILE = "QC_2.sky.zip";
    protected static final String QC_3_FILE = "QC_3.sky.zip";
    protected static final String QC_4_FILE = "QC_4.sky.zip";
    protected static final String SKY_FILE_SMALLMOL_PEP = "smallmol_plus_peptides.sky.zip";
    protected static final String USER = "qcuser@targetedms.test";
    private static ConfiguresSite siteConfigurer;

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
        Experiment
                {
                    @Override
                    public void chooseFolderType(TargetedMSTest test)
                    {
                        test.click(Locator.radioButtonById("experimentalData")); // click the first radio button - Experimental Data
                    }
                },
        ExperimentMAM
                {
                    @Override
                    public void chooseFolderType(TargetedMSTest test)
                    {
                        test.click(Locator.radioButtonById("multiAttributeMethod")); // click the second radio button - Experimental Data
                    }
                }, Library
                {
                    @Override
                    public void chooseFolderType(TargetedMSTest test)
                    {
                        test.click(Locator.radioButtonById("chromatogramLibrary")); // click the 3rd radio button - Library
                    }
                }, LibraryProtein
                {
                    @Override
                    public void chooseFolderType(TargetedMSTest test)
                    {
                        test.click(Locator.radioButtonById("chromatogramLibrary")); // click the 3rd radio button - Library
                        test.click(Locator.checkboxByName("precursorNormalized")); // check the normalization checkbox.
                    }
                }, QC
                {
                    @Override
                    public void chooseFolderType(TargetedMSTest test)
                    {
                        test.click(Locator.radioButtonById("QC")); // click the 4th radio button - QC
                    }
                };

        public abstract void chooseFolderType(TargetedMSTest test);
    }

    public TargetedMSTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        // The SQLite driver for the Chromatogram library code chokes on paths with certain characters on OSX. We don't have
        // any real deployments on OSX, so just avoid using those characters on dev machines and rely on TeamCity
        // to keep things happy on the platforms we actually use on production
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isMacOs = osName.startsWith("mac os x");
        if (isMacOs)
        {
            return "TargetedMSProject";
        }
        return "TargetedMSProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @BeforeClass
    public static void initPipeline()
    {
        TargetedMSTest init = (TargetedMSTest)getCurrentTest();

        init.doInitPipeline();
    }

    protected ConfiguresSite getSiteConfigurer()
    {
        if (siteConfigurer == null)
        {
            if (TestProperties.isCloudPipelineEnabled())
                siteConfigurer = ReflectionUtils.getSiteConfigurerOrDefault("org.labkey.test.util.cloud.S3Configurer", this);
            else
                siteConfigurer = new DefaultSiteConfigurer();
        }
        else
        {
            siteConfigurer.setWrapper(this);
        }

        return siteConfigurer;
    }

    private void doInitPipeline()
    {
        getSiteConfigurer().configureSite();
    }

    protected void setupFolder(FolderType folderType)
    {
       setUpFolder(getProjectName(),folderType);
    }

    protected void setUpFolder(String folderName, FolderType folderType )
    {
        _containerHelper.createProject(folderName, "Panorama");
        waitForElement(Locator.linkContainingText("Save"));
        clickAndWait(Locator.linkContainingText("Next"));
        selectFolderType(folderType);
        getSiteConfigurer().configureProject(getProjectName());
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

    protected void deleteSkyFile(String skyFile)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        TargetedMSRunsTable runsTable = new TargetedMSRunsTable(this);
        runsTable.deleteRun(skyFile);
    }

    protected void importData(String file)
    {
        importData(file, 1);
    }

    @LogMethod
    protected void importData(@LoggedParam String file, int jobCount)
    {
        importData(file, jobCount, false);
    }

    @LogMethod
    protected void importData(@LoggedParam String file, int jobCount, boolean expectError)
    {
        Locator.XPathLocator importButtonLoc = Locator.lkButton("Process and Import Data");
        WebElement importButton = importButtonLoc.findElementOrNull(getDriver());
        if (null == importButton)
        {
            goToModule("Pipeline");
            importButton = importButtonLoc.findElement(getDriver());
        }
        clickAndWait(importButton);
        String fileName = Paths.get(file).getFileName().toString();
        if (!_fileBrowserHelper.fileIsPresent(fileName))
            _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("TargetedMS/" + file));
        _fileBrowserHelper.importFile(fileName, "Import Skyline Results");
        waitForText("Skyline document import");
        waitForPipelineJobsToComplete(jobCount, file, expectError);
    }

    protected void verifyRunSummaryCountsSmallMol(int proteinCount, int peptideCount, int moleculeCount, int precursorCount, int transitionCount, int replicateCount, int calibrationCount, int listCount)
    {
        verifyRunSummaryCounts(proteinCount, peptideCount, moleculeCount, precursorCount, transitionCount, replicateCount, calibrationCount, listCount, "molecule lists");
    }

    protected void verifyRunSummaryCountsPep(int proteinCount, int peptideCount, int moleculeCount, int precursorCount, int transitionCount, int replicateCount, int calibrationCount, int listCount)
    {
        verifyRunSummaryCounts(proteinCount, peptideCount, moleculeCount, precursorCount, transitionCount, replicateCount, calibrationCount, listCount, "proteins");
    }

    @LogMethod
    protected void verifyRunSummaryCounts(int proteinCount, int peptideCount, int moleculeCount, int precursorCount, int transitionCount,
                                          int replicateCount, int calibrationCount, int listCount, String peptideGroupLabel)
    {
        log("Verifying expected summary counts");
        waitForElement(Locator.linkContainingText(proteinCount + " " + peptideGroupLabel));
        if (peptideCount > 0)
        {
            assertElementPresent(Locator.linkContainingText(peptideCount + " peptides"));
        }
        else
        {
            assertElementNotPresent(Locator.linkContainingText(" peptides"));
        }
        if (moleculeCount > 0)
        {
            assertElementPresent(Locator.linkContainingText(moleculeCount + " small molecules"));
        }
        else
        {
            assertElementNotPresent(Locator.linkContainingText(" small molecules"));
        }
        assertElementPresent(Locator.linkContainingText(precursorCount + " precursors"));
        assertElementPresent(Locator.linkContainingText(transitionCount + " transitions"));
        assertElementPresent(Locator.linkContainingText(replicateCount + (replicateCount == 1 ? " replicate" : " replicates")));
        if (calibrationCount > 0)
        {
            assertElementPresent(Locator.linkContainingText(calibrationCount + " calibration curves"));
        }
        else
        {
            assertElementNotPresent(Locator.linkContainingText(" calibration curve" + (precursorCount > 1 ? "s" : "")));
        }

        if (listCount > 0)
        {
            assertElementPresent(Locator.linkContainingText(listCount + " list" + (listCount > 1 ? "s" : "")));
        }
        // At the moment, we use "list" to refer to both small molecule groups and Skyline lists, so don't explicitly
        // check for absence here
    }

    @LogMethod
    protected void selectFolderType(FolderType folderType) {
        log("Select Folder Type: " + folderType);
        folderType.chooseFolderType(this);
        clickButton("Finish");
    }

    /** Verify that the comparison plots have been AJAX'd into place */
    protected void ensureComparisonPlots(String title)
    {
        waitForElement(Locator.xpath("//div[@id ='peakAreasGraph']/div[normalize-space()='" + title + "']"));
        waitForElement(Locator.xpath("//div[@id ='retentionTimesGraph']/div[normalize-space()='" + title + "']"));
    }

    protected void verifyQcSummary(int docCount, int sampleFileCount, int precursorCount)
    {
        QCSummaryWebPart qcSummaryWebPart = new PanoramaDashboard(this).getQcSummaryWebPart();
        verifyQcSummary(qcSummaryWebPart.getQcSummaryTiles().get(0), null, sampleFileCount, precursorCount);
    }

    @LogMethod
    protected void verifyQcSummary(QCSummaryWebPart.QcSummaryTile tile, String folderName, int sampleFileCount, int precursorCount)
    {
        String actualFolderName;
        if (folderName != null)
        {
            actualFolderName = tile.getFolderName();
            assertEquals("Wrong folder name for QC Summary tile", folderName, actualFolderName);
        }
        else
        {
            actualFolderName = "tile " + tile.getIndex();
        }

        assertEquals("Wrong number sample files for " + actualFolderName, sampleFileCount, tile.getFileCount());
        assertEquals("Wrong number of precursors tracked for " + actualFolderName, precursorCount, tile.getPrecursorCount());

        if (sampleFileCount == 0 && precursorCount == 0)
            assertTrue("Expected no documents for " + actualFolderName, tile.hasNoSkylineDocuments());
        else
            assertFalse("Unexpected lack of skyline documents for " + actualFolderName, tile.hasNoSkylineDocuments());
    }

    @LogMethod
    public int createGuideSetFromTable(GuideSet guideSet)
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        GuideSetWebPart guideSetWebPart = new GuideSetWebPart(this, getProjectName());
        GuideSetPage guideSetPage = guideSetWebPart.startInsert();
        guideSetPage.insert(guideSet, null);

        return guideSet.getRowId();
    }

    @LogMethod
    protected void removeAllGuideSets()
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        DataRegionTable table = new DataRegionTable("qwp1", getDriver());
        table.checkAllOnPage();
        table.deleteSelectedRows();
    }

    public PanoramaDashboard goToDashboard()
    {
        clickTab("Panorama Dashboard");
        return new PanoramaDashboard(this);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);
        
        _userHelper.deleteUsers(false, USER);
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
