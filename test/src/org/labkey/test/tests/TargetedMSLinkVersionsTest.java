package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.components.targetedms.LinkVersionsGrid;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.FileBrowserHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
public class TargetedMSLinkVersionsTest extends TargetedMSTest
{
    private static int PIPELINE_JOB_COUNTER = 0;

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSLinkVersionsTest init = (TargetedMSLinkVersionsTest)getCurrentTest();
        init.setupFolder(FolderType.QC);

        // pre-upload the files to the pipeline root so that all of the @Test don't have to worry about it
        init.goToModule("Pipeline");
        init.clickButton("Process and Import Data");
        init._fileBrowserHelper.waitForFileGridReady();
        for (String file : init.getQcDocumentNames())
            init._fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("TargetedMS/" + file));
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickTab("Runs");

        // since importing one of these runs is really quick, delete and re-import runs
        // for each @Test so that we can assure the Created date ordering of the runs
        deleteExistingQCRuns();
        importData(QC_1_FILE, (PIPELINE_JOB_COUNTER < 3 ? ++PIPELINE_JOB_COUNTER : 3), false);
        importData(QC_2_FILE, (PIPELINE_JOB_COUNTER < 3 ? ++PIPELINE_JOB_COUNTER : 3), false);
        importData(QC_3_FILE, (PIPELINE_JOB_COUNTER < 3 ? ++PIPELINE_JOB_COUNTER : 3), false);
        clickTab("Runs");
    }

    public List<String> getQcDocumentNames()
    {
        return Arrays.asList(QC_1_FILE, QC_2_FILE, QC_3_FILE);
    }

    private void deleteExistingQCRuns()
    {
        boolean hasRunsToDelete = false;
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", this);

        for (String documentName : getQcDocumentNames())
        {
            if (table.getRow("File", documentName) > -1)
            {
                table.checkCheckbox(table.getRow("File", documentName));
                hasRunsToDelete = true;
            }
        }

        if (hasRunsToDelete)
        {
            table.clickHeaderButtonByText("Delete");
            clickButton("Confirm Delete");
        }
    }

    @Test
    public void testReorderMethodChain()
    {
        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(this);

        log("verify originally no linked versions");
        linkVersionsGrid.goToDocumentDetails(QC_1_FILE);
        waitForElement(linkVersionsGrid.getNoVersionsLocator());

        log("link two versions together");
        linkVersionsGrid.openDialogForDocuments(Arrays.asList(QC_1_FILE, QC_2_FILE));
        assertElementNotPresent(linkVersionsGrid.getReplaceExistingTextLocator());
        linkVersionsGrid.clickSave();
        verifyDocumentDetailsChain(Arrays.asList(QC_1_FILE, QC_2_FILE), 0);

        log("add third document to version chain");
        linkVersionsGrid.openDialogForDocuments(Arrays.asList(QC_1_FILE, QC_3_FILE), 3);
        linkVersionsGrid.waitForGrid(getQcDocumentNames(), true); // verify QC_2 document is pulled in by association
        assertElementPresent(linkVersionsGrid.getReplaceExistingTextLocator());
        assertEquals(2, linkVersionsGrid.getRemoveLinkIcons().size());
        linkVersionsGrid.clickSave();
        verifyDocumentDetailsChain(getQcDocumentNames(), 2);

        log("re-order documents in the existing chain");
        linkVersionsGrid.openDialogForDocuments(getQcDocumentNames());
        assertEquals(3, linkVersionsGrid.getRemoveLinkIcons().size());
        linkVersionsGrid.reorderVersions(2, 0);
        linkVersionsGrid.clickSave();
        verifyDocumentDetailsChain(Arrays.asList(QC_3_FILE, QC_1_FILE, QC_2_FILE), 1);
    }

    @Test
    public void testRemoveFromMethodChain()
    {
        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(this);

        log("setup chain for the 3 runs");
        linkVersionsGrid.openDialogForDocuments(getQcDocumentNames());
        linkVersionsGrid.clickSave();

        log("remove link version from middle of chain");
        linkVersionsGrid.openDialogForDocuments(getQcDocumentNames());
        assertEquals(3, linkVersionsGrid.getRemoveLinkIcons().size());
        linkVersionsGrid.removeLinkVersion(1);
        verifyDocumentDetailsChain(Arrays.asList(QC_1_FILE, QC_3_FILE), 0);

        log("remove link version from end of chain");
        linkVersionsGrid.openDialogForDocuments(Arrays.asList(QC_1_FILE, QC_3_FILE));
        assertEquals(2, linkVersionsGrid.getRemoveLinkIcons().size());
        linkVersionsGrid.removeLinkVersion(1);
        linkVersionsGrid.goToDocumentDetails(QC_1_FILE);
        waitForElement(linkVersionsGrid.getNoVersionsLocator());
    }

    @Test
    public void testDeleteRunWhenInChain()
    {
        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(this);

        log("setup chain for the 3 runs");
        linkVersionsGrid.openDialogForDocuments(getQcDocumentNames());
        linkVersionsGrid.clickSave();
        verifyDocumentDetailsChain(getQcDocumentNames(), 0);

        log("delete the run which is in a chain");
        clickTab("Runs");
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", this);
        table.uncheckAll();
        table.checkCheckbox(table.getRow("File", QC_2_FILE));
        table.clickHeaderButtonByText("Delete");
        clickButton("Confirm Delete");

        log("verify that the chain was updated correctly for remaining runs");
        linkVersionsGrid.goToDocumentDetails(QC_1_FILE);
        waitForElement(linkVersionsGrid.getNoVersionsLocator());
        clickTab("Runs");
        linkVersionsGrid.goToDocumentDetails(QC_3_FILE);
        waitForElement(linkVersionsGrid.getNoVersionsLocator());
    }

    private void verifyDocumentDetailsChain(List<String> documentNames, int index)
    {
        LinkVersionsGrid linkVersionsGrid = new LinkVersionsGrid(this);
        linkVersionsGrid.goToDocumentDetails(documentNames.get(index));
        linkVersionsGrid.waitForGrid(documentNames, false);
    }
}
