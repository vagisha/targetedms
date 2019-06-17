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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class TargetedMSLibraryIrtTest extends TargetedMSIrtTest
{
    // One of the sequences has had its value changed in FOR_UPDATE to verify the weighted average calculation.
    // Another one of the sequences has been changed altogether to verify import counts and new inserts.
    private static final String SKY_FILE_UPDATE_SCALE = "iRT Human+Standard Calibrate_FOR_UPDATE.zip";

    private static final String STANDARD_PEPTIDE = "TPVITGAPYEYR";

    // This peptide has been hand edited in the "FOR_UPDATE" test dataset.
    private static final double ORIGINAL_VALUE = -6.9907960408018255;
    private static final double REWEIGHED_VALUE = -3.49539802;

    //  The changed sequence in "FOR_UPDATE"
    private static final String OMITTED_PEPTIDE = "AQYEDIANR";
    private static final String NEW_PEPTIDE = "ZZZZZZ";

    private static final String IGNORE_ONE_STANDARD_MSG = "Calculated iRT regression line by ignoring import value for standard: ADVTPADFSEWSK";
    private static final String CALCULATED_FROM_SHARED_MSG = "Successfully calculated iRT regression line from 705 shared peptides";
    private static final String CALCULATED_FROM_FULL_STANDARD_LIST = "Calculated iRT regression line from full standard list";

    private int goodImport = 0;

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.LibraryProtein);
        importData(SKY_FILE);

        // 1. Quick sanity check
        assertEquals("Imported iRT Peptide count is incorrect.", PEPTIDE_COUNT, getRowCount());
        assertEquals("Imported iRT value is incorrect for peptide " + UPDATE_PEPTIDE, ORIGINAL_VALUE, getIrtValue(UPDATE_PEPTIDE), DELTA);
        goodImport++;

        // 2. Correlation throwing out one standard.
        importData(SKY_FILE_ONE_BAD_STANDARD, 2);
        checkLogMessage(IGNORE_ONE_STANDARD_MSG);
        goodImport++;

        // 2.a. Verify standard peptides were excluded from new insert/update
        assertEquals("More than one row for standard peptide " + STANDARD_PEPTIDE, 1, getRowsForPeptide(STANDARD_PEPTIDE).size());
        assertEquals("Import count should only be 1 for standard peptide " + STANDARD_PEPTIDE, 1, getImportCount(STANDARD_PEPTIDE));

        // 3. Correlation on shared peptides / scale values
        importData(SKY_FILE_DOUBLE_TIMES_NO_STANDARDS, 3);
        checkLogMessage(CALCULATED_FROM_SHARED_MSG);
        assertEquals("Normalized, weighted value is incorrect for peptide  " + UPDATE_PEPTIDE, ORIGINAL_VALUE, getIrtValue(UPDATE_PEPTIDE), DELTA);
        goodImport++;

        // 4. Correlation on all standards / weighted average / new library peptide test. Import another copy which has been modified with a different value for one of the peptides (sign flipped so average should be 0),
        // and has a new peptide added to it.
        importData(SKY_FILE_UPDATE_SCALE, 4);
        checkLogMessage(CALCULATED_FROM_FULL_STANDARD_LIST);
        assertEquals("Reweighed value is incorrect for peptide " + UPDATE_PEPTIDE, REWEIGHED_VALUE, getIrtValue(UPDATE_PEPTIDE), DELTA);
        assertEquals("Import count is incorrect for peptide " + UPDATE_PEPTIDE, 4, getImportCount(UPDATE_PEPTIDE));
        assertEquals("Import count is incorrect for peptide " + OMITTED_PEPTIDE, 3, getImportCount(OMITTED_PEPTIDE));
        assertEquals("Import count is incorrect for peptide " + NEW_PEPTIDE, 1, getImportCount(NEW_PEPTIDE));
        goodImport++;

        // 5. Shared peptide correlation test. Import another copy which doesn't match the same set of standards as the first import.
        // Allowed, as of Issue 32924: Port Skyline changes to allowable iRT discrepancy during import
        importData(SKY_FILE_BAD_STANDARDS, 5);
        checkLogMessage(CALCULATED_FROM_FULL_STANDARD_LIST);
        goodImport++;

        downloadLibraryExport(goodImport);
    }
}
