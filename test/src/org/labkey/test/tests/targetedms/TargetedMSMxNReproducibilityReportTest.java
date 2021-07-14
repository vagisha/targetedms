package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.targetedms.ConnectionSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.labkey.test.Locator.tag;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSMxNReproducibilityReportTest extends TargetedMSTest
{
    private static final String SKY_FILE = "UW_Cpep_Linearity_Imprecision_trimmed.sky.zip";
    private static final String SKY_FILE_WITH_SINGLE_REPLICATE = "ListTest.sky.zip";
    private static final String SKY_FILE_FOR_CALIBRATION_CURVE = "Quantification/CalibrationScenariosTest/MergedDocuments.sky.zip";
    private static final String SKY_CLIB_FILE = "L-Asp-Hipp-B1-A4-Tau_trimmed.sky.zip";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSMxNReproducibilityReportTest init = (TargetedMSMxNReproducibilityReportTest) getCurrentTest();
        init.setupFolder(FolderType.Library);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS MxN Reproducibility Report Test";
    }

    @Test
    public void testReproducibilityReport() throws IOException, CommandException
    {
        goToProjectHome();
        importData(SKY_FILE);

        goToProjectHome();
        clickTab("Proteins");
        DataRegionTable table = new DataRegionTable("PeptideGroup", this);
        table.setFilter("Label", "Equals", "PEPTIDE_GROUP_0");

        log("Clicking on the protein");
        clickAndWait(reproducibilityReportLink());

        waitForElement(Locator.tagWithName("a", "Protein"));

        checker().verifyTrue("Reproducibility Report is not present", isTextPresent("Reproducibility Report"));
        checker().verifyEquals("Incorrect number of graphs for EAEDLQVGQVE", getPrecursorChromeInfoCount("EAEDLQVGQVE"),
                getGraphCount("EAEDLQVGQVE"));
        checker().verifyEquals("Incorrect number of graphs for EAEDL[+7.0]QVGQVE", getPrecursorChromeInfoCount("EAEDL[+7.0]QVGQVE"),
                getGraphCount("EAEDL[+7.0]QVGQVE"));

        log("Verifying the value type");
        checker().verifyEquals("The Value Type drop-down is not defaulted correct", "Normalized",
                getSelectedOptionText(Locator.name("valueType")));
        selectOptionByText(Locator.name("valueType"), "Calibrated");
        checker().verifyTrue("Missing message when calibration value type is selected",
                isElementPresent(Locator.tagWithId("td", "noCalibratedValuesError")
                        .withoutAttributeContaining("style", "display: none")));
        selectOptionByText(Locator.name("valueType"), "Normalized");

        //TODO : More verifications to add for new sample file.
        checker().verifyTrue("Missing calibration curve webpart",
                isElementPresent(Locator.css("span.labkey-wp-title-text").withText("Calibration Curve")));

        checker().verifyTrue("Missing Figures of Merit  webpart",
                isElementPresent(Locator.css("span.labkey-wp-title-text").withText("Figures of Merit")));

        log("Verifying Calibration Curves link");
        clickTab("Proteins");
        clickAndWait(calibrationCurvesLink());
        checker().verifyTrue("Calibration curve link not working",
                isElementPresent(Locator.css("span.labkey-wp-title-text").withText("Peptide Calibration Curves")));
    }

    @Test
    public void testSingleReplicateFile()
    {
        String subFolderName = "Single Replicate Test Case";
        goToProjectHome();
        setupSubfolder(getProjectName(), subFolderName, FolderType.Library);

        navigateToFolder(getProjectName(), subFolderName);
        importData(SKY_FILE_WITH_SINGLE_REPLICATE);

        navigateToFolder(getProjectName(), subFolderName);
        // Ensure tab didn't get automatically added since we don't have reporting to link to
        assertElementNotPresent(Locator.folderTab("Proteins"));

        // Go to the view via the schema browser to make sure it's not giving the link to the reproducibility report
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "PeptideGroup");
        table.goToView("Library Proteins");
        table = new DataRegionTable("query", this);
        table.setFilter("Label", "Equals", "gi|171455|gb|AAA88712.1|");

        assertElementNotPresent("Shouldn't have reproducibility report link", reproducibilityReportLink());
    }

    @Test
    public void testCalibrationCurve()
    {
        String subfolderName = "Calibration Curve";
        goToProjectHome();
        setupSubfolder(getProjectName(), subfolderName, FolderType.Library);

        navigateToFolder(getProjectName(), subfolderName);
        importData(SKY_FILE_FOR_CALIBRATION_CURVE);

        clickTab("Proteins");

        checker().verifyFalse("Reproducibility report link should not be present", isElementPresent(reproducibilityReportLink()));
        checker().verifyTrue("Calibration curve icon should be present", isElementPresent(calibrationCurvesLink()));

        clickAndWait(calibrationCurvesLink().index(2));

        log("Verifying the FOM values");
        waitForElement(Locator.css("span.labkey-wp-title-text").withText("Figures of Merit"));
        checker().verifyTrue("Incorrect peptide", isElementPresent(Locator.tagWithText("h3", "Calibration Curve: VIFDANAPVAVR")));
        checker().verifyEquals("Incorrect Lower limit of quantitation", "1.0",
                Locator.tagWithId("td", "lloq-stat").findElement(getDriver()).getText());
        checker().verifyEquals("Incorrect Upper limit of quantitation", "10.0",
                Locator.tagWithId("td", "uloq-stat").findElement(getDriver()).getText());
        clickAndWait(Locator.linkWithText("Show Details"));

        log("Verifying only 3 samples for displayed");
        checker().verifyEquals("More then 3 samples displayed",
                "Blank+IS__VIFonly\n" + "Blank+IS__VIFonly (2)\n" + "Cal 1_0_20 ng_mL_VIFonly\n" + "and 22 more",
                Locator.tagWithId("div", "fom-sampleList").findElement(getDriver()).getText());

        log("Verifying the downloaded file");
        File exportedExcel = clickAndWaitForDownload(Locator.linkWithId("targetedms-fom-export"));
        checker().verifyTrue("Wrong file type for export [" + exportedExcel.getName() + "]", exportedExcel.getName().endsWith(".xlsx"));
        checker().verifyTrue("Empty file downloaded", exportedExcel.length() > 0);
    }

    @Test
    public void testClibFileUpdates() throws SQLException
    {
        String subfolderName = "Clib File Folder";
        goToProjectHome();
        setupSubfolder(getProjectName(), subfolderName, FolderType.Library);

        navigateToFolder(getProjectName(), subfolderName);
        importData(SKY_CLIB_FILE);

        clickTab("Panorama Dashboard");
        File downloadedClibFile = doAndWaitForDownload(() -> clickButton("Download", 0));

        checker().verifyTrue("Precursor table is missing qvalue column",
                columnExists(downloadedClibFile, "Precursor", "qvalue"));
        checker().verifyTrue("Transition table is missing Quantitative column",
                columnExists(downloadedClibFile, "Transition", "Quantitative"));
        checker().verifyEquals("Incorrect row data for Precursor", 4, sizeOfTable(downloadedClibFile, "Precursor"));
        checker().verifyEquals("Incorrect row data for Transition", 31, sizeOfTable(downloadedClibFile, "Transition"));
    }

    private Locator.XPathLocator reproducibilityReportLink()
    {
        return tag("a").withChild(tag("i").withAttributeContaining("class", "fa-th")
                .withAttribute("title", "Reproducibility Report"));
    }

    private Locator.XPathLocator calibrationCurvesLink()
    {
        return tag("a").withChild(tag("i").withAttributeContaining("class", "fa-line-chart")
                .withAttribute("title", "Calibration Curves"));
    }


    private int getGraphCount(String sequence)
    {
        pushLocation();
        Locator.XPathLocator locator = Locator.linkWithText(sequence);
        waitForElement(locator);
        locator.findElement(getDriver()).click();
        // graph count
        int count = Locator.tagWithAttributeContaining("span", "id", "chrom").findElements(getDriver()).size();
        popLocation();
        return count;
    }

    private int getPrecursorChromeInfoCount(String sequence) throws IOException, CommandException
    {
        SelectRowsCommand rowsCommand = new SelectRowsCommand("targetedms", "PrecursorChromInfo");
        rowsCommand.addFilter("PrecursorId/ModifiedSequence", sequence, Filter.Operator.EQUAL);
        rowsCommand.setRequiredVersion(9.1);
        SelectRowsResponse rowsResponse = rowsCommand.execute(createDefaultConnection(), getCurrentContainerPath());
        return rowsResponse.getRows().size();
    }

    private boolean columnExists(File clibFile, String tableName, String columnName) throws SQLException
    {
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, tableName, columnName))
            {
                if (rs.next())
                    return true; //Table exists
                else
                    return false;
            }
        }
    }

    private int sizeOfTable(File clibFile, String name) throws SQLException
    {
        int cnt = 0;
        @SuppressWarnings("SqlResolve")
        String sql = "SELECT * FROM " + name;
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            try (ResultSet rs = conn.createStatement().executeQuery(sql))
            {
                while (rs.next())
                    cnt++;
            }
        }
        return cnt;
    }
}
