package org.labkey.test.tests;

/**
 * User: tgaluhn
 * Date: 3/24/14
 */

import org.junit.experimental.categories.Category;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class, MS2.class})
public class TargetedMSExperimentIrtTest extends TargetedMSLibraryIrtTest
{
    public TargetedMSExperimentIrtTest()
    {
        super();
    }

    @Override
    protected void doTestSteps() throws Exception
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
