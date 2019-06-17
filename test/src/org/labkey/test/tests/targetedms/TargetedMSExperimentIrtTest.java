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
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSExperimentIrtTest extends TargetedMSIrtTest
{
    public TargetedMSExperimentIrtTest()
    {
        super();
    }

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);
        // For experiment folders, importing another iRT scale should create another scale and set of iRT Peptide rows. Unlike with library folders,
        // it doesn't matter if the new scale has a different set of standards.
        importData(SKY_FILE_BAD_STANDARDS, 2);
        assertEquals("Incorrect total iRT peptide count.", getRowCount(), PEPTIDE_COUNT * 2 );
        assertEquals("Incorrect number of rows for iRT peptide " + UPDATE_PEPTIDE, getRowsForPeptide(UPDATE_PEPTIDE).size(), 2);
    }
}
