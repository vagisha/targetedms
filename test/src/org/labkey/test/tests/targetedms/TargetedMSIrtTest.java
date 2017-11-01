/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.UIContainerHelper;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class TargetedMSIrtTest extends TargetedMSTest
{
    private static final String SCHEMA = "targetedms";
    private static final String QUERY = "iRTPeptide";

    // All tests use varations of this test dataset.
    protected static final String SKY_FILE = "iRT Human+Standard Calibrate.zip";
    protected static final int PEPTIDE_COUNT = 716;
    protected static final double DELTA = 0.00001;

    // Same iRT standards, but one value has been badly skewed
    protected static final String SKY_FILE_ONE_BAD_STANDARD = "iRT Human+Standard Calibrate_ONE_BAD_STANDARD.zip";

    // No iRT standards, and all retention times multiplied by 2
    protected static final String SKY_FILE_DOUBLE_TIMES_NO_STANDARDS = "iRT Human_ DOUBLE_TIMES_NO_STANDARDS.zip";

    // One of the sequences for a standard peptide has been changed in DIEFERENT_STANDARDS
    protected static final String SKY_FILE_BAD_STANDARDS = "iRT Human+Standard Calibrate_DIFFERENT_STANDARDS.zip";

    // This peptide has been hand edited in the "FOR_UPDATE" test dataset.
    protected static final String UPDATE_PEPTIDE = "ASTEGVAIQGQQGTR";

    @Override
    protected String getProjectName()
    {
        return "TargetedMS" + "_iRT Test";
    }

    protected void downloadLibraryExport(int goodImport)
    {
        // Very basic sanity check- can we get a download file with expected name and a reasonable file length.
        // We're not attempting to verify its contents.
        goToProjectHome();
        final File exportFile = clickAndWaitForDownload(Locator.linkWithText("Download"), 1)[0];
        assertEquals(getProjectName() + "_rev" + goodImport + ".clib", exportFile.getName());
        Supplier<Boolean> fileSize = () -> exportFile.length() > 1E+6;
        waitFor(fileSize, WAIT_FOR_JAVASCRIPT);
        assertTrue("Chromatogram library export file is too small: " + exportFile.length() + " bytes", fileSize.get());
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

    protected void checkLogMessage(String message)
    {
        new PipelineStatusTable(this).clickStatusLink(0);
        assertTextPresent(message);
    }
}