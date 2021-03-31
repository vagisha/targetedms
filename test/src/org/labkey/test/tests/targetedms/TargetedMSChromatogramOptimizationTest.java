package org.labkey.test.tests.targetedms;


import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.targetedms.ConnectionSource;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSChromatogramOptimizationTest extends TargetedMSTest
{
    private static final String SKY_FILE = "SmallMoleculeLibrary3.sky.zip";
    private static final String SKY_FILE2 = "EnergyMet.sky.zip";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSChromatogramOptimizationTest init = (TargetedMSChromatogramOptimizationTest) getCurrentTest();
        init.setupFolder(FolderType.Library);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS Chromatogram Optimization Test";
    }

    @Test
    public void testUpload() throws Exception
    {
        goToProjectHome();
        importData(SKY_FILE);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        File downloadedClibFile = doAndWaitForDownload(() -> clickButton("Download", 0));

        log("Verifying table exists");
        List<String> tablesToVerify = new LinkedList<String>(Arrays.asList("TransitionOptimization", "Transition", "Peptide", "Protein", "Precursor", "PrecursorRetentionTime"));
        List<String> tablesNotPresent = tableExists(tablesToVerify, downloadedClibFile);
        if (tablesNotPresent.size() != 0)
            checker().verifyTrue("Some of the tables do not exists in SQLITE file" + Arrays.toString(tablesNotPresent.toArray()), false);

        log("Verifying the SampleFile modifications");
        checker().verifyTrue("Sample File does not have CePredictorId",
                columnExists(downloadedClibFile, "SampleFile", "CePredictorId"));
        checker().verifyTrue("Sample File does not have DpPredictorId",
                columnExists(downloadedClibFile, "SampleFile", "DpPredictorId"));

        log("Verifying the rows counts");
        checker().verifyEquals("Invalid number of rows in transition optimization", 0,
                sizeOfTable(downloadedClibFile, "TransitionOptimization"));
        checker().verifyEquals("Invalid number of rows in Peptide", getServerTableRowCount("Molecule", "Library Molecules"),
                sizeOfTable(downloadedClibFile, "Peptide"));
        checker().verifyEquals("Invalid number of rows in Precursor", getServerTableRowCount("MoleculePrecursor", null),
                sizeOfTable(downloadedClibFile, "Precursor"));
        checker().verifyEquals("Invalid number of rows in Transition", getServerTableRowCount("MoleculeTransition", null),
                sizeOfTable(downloadedClibFile, "Transition"));

        // Import a file that includes optimization info
        importData(SKY_FILE2, 2);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        File downloadedClibFile2 = doAndWaitForDownload(() -> clickButton("Download", 0));

        checker().verifyEquals("Invalid number of rows in Peptide", getServerTableRowCount("Molecule", "Library Molecules"),
                sizeOfTable(downloadedClibFile2, "Peptide"));
        checker().verifyEquals("Invalid number of rows in TransitionOptimization", getServerTableRowCount("TransitionOptimization", null),
                sizeOfTable(downloadedClibFile2, "TransitionOptimization"));
        checker().verifyEquals("Invalid number of rows in TransitionOptimization", 28,
                sizeOfTable(downloadedClibFile2, "TransitionOptimization"));
    }

    private int getServerTableRowCount(String tableName, @Nullable String viewName)
    {
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", tableName);
        table.rowSelector().showAll();
        if(viewName != null)
            table.goToView(viewName);
        return table.getDataRowCount();
    }

    private int sizeOfTable(File clibFile, String name) throws SQLException
    {
        int cnt = 0;
        @SuppressWarnings("SqlResolve")
        String sql = "SELECT * FROM " + name;
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            try(ResultSet rs = conn.createStatement().executeQuery(sql))
            {
                while (rs.next())
                    cnt++;
            }
        }
        return cnt;
    }

    private boolean columnExists(File clibFile, String tableName, String columnName) throws SQLException
    {
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            DatabaseMetaData md = conn.getMetaData();
            try(ResultSet rs = md.getColumns(null, null, tableName, columnName))
            {
                if (rs.next())
                    return true; //Table exists
                else
                    return false;
            }
        }
    }

    private List<String> tableExists(List<String> tables, File clibFile) throws SQLException
    {
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            DatabaseMetaData md = conn.getMetaData();
            for (Iterator<String> i = tables.iterator(); i.hasNext(); )
            {
                String tableName = i.next();
                try(ResultSet rs = md.getTables(null, null, tableName, null))
                {
                    if (rs.next())
                        i.remove();
                }
            }

        }
        return tables;
    }
}