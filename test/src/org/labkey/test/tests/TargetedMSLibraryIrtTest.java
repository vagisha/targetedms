/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.UIContainerHelper;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, MS2.class})
public class TargetedMSLibraryIrtTest extends TargetedMSTest
{
    private static final String SCHEMA = "targetedms";
    private static final String QUERY = "iRTPeptide";

    // All tests use varations of this test dataset.
    protected static final String SKY_FILE = "iRT Human+Standard Calibrate.zip";
    protected static final int PEPTIDE_COUNT = 716;
    private static final double DELTA = 0.00001;

    // One of the sequences for a standard peptide has been changed in DIEFERENT_STANDARDS
    protected static final String SKY_FILE_BAD_STANDARDS = "iRT Human+Standard Calibrate_DIFFERENT_STANDARDS.zip";

    // One of the sequences has had it's value changed in FOR_UPDATE to verify the weighted average calculation.
    // Another one of the sequences has been changed altogether to verify import counts and new inserts.
    private static final String SKY_FILE_UPDATE_SCALE = "iRT Human+Standard Calibrate_FOR_UPDATE.zip";

    // This peptide has been hand edited in the "FOR_UPDATE" test dataset.
    protected static final String UPDATE_PEPTIDE = "ASTEGVAIQGQQGTR";
    private static final double ORIGINAL_VALUE = -6.9907960408018255;
    private static final double REWEIGHED_VALUE = 0.0;

//  The changed sequence in "FOR_UPDATE"
    private static final String OMITTED_PEPTIDE = "AQYEDIANR";
    private static final String NEW_PEPTIDE = "ZZZZZZ";

    public TargetedMSLibraryIrtTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS" + "_iRT Test";
    }

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.LibraryProtein);
        importData(SKY_FILE);

        // Quick sanity check
        assertEquals("Imported iRT Peptide count is incorrect.", getRowCount(), PEPTIDE_COUNT);
        assertEquals("Imported iRT value is incorrect for peptide " + UPDATE_PEPTIDE , getIrtValue(UPDATE_PEPTIDE), ORIGINAL_VALUE, DELTA);

        // Import another copy which has been modified with a different value for one of the peptides (sign flipped so average should be 0),
        // and has a new peptide added to it.
        importData(SKY_FILE_UPDATE_SCALE, 2);
        assertEquals("Reweighed value is incorrect for peptide " + UPDATE_PEPTIDE, getIrtValue(UPDATE_PEPTIDE), REWEIGHED_VALUE, DELTA);
        assertEquals("Import count is incorrect for peptide " + UPDATE_PEPTIDE, getImportCount(UPDATE_PEPTIDE), 2);
        assertEquals("Import count is incorrect for peptide " + OMITTED_PEPTIDE, getImportCount(OMITTED_PEPTIDE), 1);
        assertEquals("Import count is incorrect for peptide " + NEW_PEPTIDE, getImportCount(NEW_PEPTIDE), 1);

        // Import another copy which doesn't match the same set of standards as the first import. For library folders, this is an error condition
        // and aborts the import.
        importData(SKY_FILE_BAD_STANDARDS, 3);
        assertTextPresent("ERROR");
        checkExpectedErrors(1);

        downloadLibraryExport();
    }

    private void downloadLibraryExport()
    {
        // Very basic sanity check- can we get a download file with expected name and a reasonable file length.
        // We're not attempting to verify its contents.
        goToProjectHome();
        final File exportFile = clickAndWaitForDownload(Locator.linkWithText("Download"), 1)[0];
        assertEquals(getProjectName() + "_rev2.clib", exportFile.getName());
        Checker fileSize = new Checker()
        {
            @Override
            public boolean check()
            {
                return exportFile.length() > 1E+6;
            }
        };
        doesElementAppear(fileSize, WAIT_FOR_JAVASCRIPT);
        assertTrue("Chromatogram library export file is too small: " + exportFile.length() + " bytes", fileSize.check());
    }

    protected int getRowCount()
    {
        return executeSelectRowCommand(SCHEMA, QUERY).getRowCount().intValue();
    }

    protected List<Map<String, Object>> getRowsForPeptide(String peptide)
    {
        return executeSelectRowCommand(SCHEMA, QUERY, Collections.singletonList(new Filter("ModifiedSequence", peptide))).getRows();
    }

    protected Map<String, Object> getIrtPeptide(String peptide)
    {
        return getRowsForPeptide(peptide).get(0);
    }

    protected double getIrtValue(String peptide)
    {
        return (double) getIrtPeptide(peptide).get("iRTValue");
    }

    protected int getImportCount(String peptide)
    {
        return (int) getIrtPeptide(peptide).get("ImportCount");
    }
}
