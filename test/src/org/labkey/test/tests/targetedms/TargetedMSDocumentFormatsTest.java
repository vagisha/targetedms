/*
 * Copyright (c) 2017-2021 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.Row;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Category({DailyB.class, MS2.class})
public class TargetedMSDocumentFormatsTest extends TargetedMSTest
{
    private static final String SAMPLEDATA_FOLDER = "DocumentFormats/";

    private static int JOB_COUNT;

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSDocumentFormatsTest init = (TargetedMSDocumentFormatsTest) getCurrentTest();
        init.setupFolder(FolderType.Experiment);
    }

    @Test
    public void test36Document()
    {
        goToProjectHome(getProjectName());
        importData(SAMPLEDATA_FOLDER + "DocumentSerializerTest_36.sky.zip", ++JOB_COUNT);
    }

    @Test
    public void testNormalDocument()
    {
        goToProjectHome(getProjectName());
        importData(SAMPLEDATA_FOLDER + "DocumentSerializerTest.sky.zip", ++JOB_COUNT);
    }

    @Test
    public void testCompactDocument()
    {
        goToProjectHome(getProjectName());
        importData(SAMPLEDATA_FOLDER + "DocumentSerializerTest_compact.sky.zip", ++JOB_COUNT);
    }

    @Test
    public void testCrosslinkingDocument() throws IOException, CommandException
    {
        goToProjectHome(getProjectName());
        String fileName = "CrosslinkingExample.sky.zip";
        importData(SAMPLEDATA_FOLDER + fileName, ++JOB_COUNT);

        validateCrossLinked(fileName,
                596,
                "C[+57.021464]C[+57.021464]TKPESER-EKVLTSSAR-[+138.06808@4,2]",
                "p-{4:DSS:p}",
                765.3753,
                765.3753, 765.7096, 766.0435);
        validateCrossLinked(fileName,
                596,
                "AMNFS[+79.966331]GSPGAV-STSPT[+79.966331]QSFM[+15.994915]NTLPR-[-18.010565@11,1]",
                "p-{11:Hydrolysis:b4}",
                921.0515,
                458.5433, 491.2023, 687.3114, 736.2998, 1373.6154, 1471.5923);
    }

    @Test
    public void testCrosslinkingCompactDocument() throws IOException, CommandException
    {
        goToProjectHome(getProjectName());
        String fileName = "CrosslinkingExample_Compact.sky.zip";
        importData(SAMPLEDATA_FOLDER + fileName, ++JOB_COUNT);

        validateCrossLinked(fileName,
                596,
                "C[+57.021464]C[+57.021464]TKPESER-EKVLTSSAR-[+138.06808@4,2]",
                "p-{4:DSS:p}",
                765.3753,
                765.3753, 765.7096, 766.0435);
        validateCrossLinked(fileName,
                596,
                "AMNFS[+79.966331]GSPGAV-STSPT[+79.966331]QSFM[+15.994915]NTLPR-[-18.010565@11,1]",
                "p-{11:Hydrolysis:b4}",
                921.0515,
                458.5433, 491.2023, 687.3114, 736.2998, 1373.6154, 1471.5923);
    }

    @Test
    public void testComplexCrosslinking() throws IOException, CommandException
    {
        goToProjectHome(getProjectName());
        String fileName = "ComplexCrosslinking.sky.zip";
        importData(SAMPLEDATA_FOLDER + fileName, ++JOB_COUNT);

        validateCrossLinked(fileName,
                612,
                "AGKA-LKDAVN-QGGKAR-[-18.010565@4,1][+138.06808@*,2,4]",
                "p-b2-y3",
                435.7495,
                540.8480);
    }

    @Test
    public void testComplexCrosslinkingCompact() throws IOException, CommandException
    {
        goToProjectHome(getProjectName());
        String fileName = "ComplexCrosslinking_Compact.sky.zip";
        importData(SAMPLEDATA_FOLDER + fileName, ++JOB_COUNT);

        validateCrossLinked(fileName,
                612,
                "AGKA-LKDAVN-QGGKAR-[-18.010565@4,1][+138.06808@*,2,4]",
                "p-b2-y3",
                435.7495,
                540.8480);
    }

    private void validateCrossLinked(String file, int countWithAnyIon, String modifiedPeptide, String ion, double precursorMz, double... transitionMz) throws IOException, CommandException
    {
        Connection cn = createDefaultConnection(false);

        Filter fileFilter = new Filter("PrecursorId/PeptideId/PeptideGroupId/RunId/Name", file, Filter.Operator.EQUAL);

        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "transition");
        cmd.setRequiredVersion(9.1);
        cmd.setColumns(Arrays.asList("PrecursorId/PeptideId/PeptideGroupId/Label", "Mz", "PrecursorId/Mz", "Charge", "ComplexFragmentIon", "PrecursorId/PeptideId/PeptideGroupId/RunId", "PrecursorId/PeptideId/PeptideGroupId/RunId/File", "PrecursorId/PeptideId/Sequence", "PrecursorId/PeptideId/PeptideModifiedSequence"));
        cmd.setSorts(Collections.singletonList(new Sort("Mz")));

        cmd.setFilters(Arrays.asList(
                new Filter("ComplexFragmentIon", "", Filter.Operator.NONBLANK),
                fileFilter));

        SelectRowsResponse response = cmd.execute(cn, getCurrentContainerPath());
        Assert.assertEquals("Wrong number of transitions with ComplexFragmentIon values", countWithAnyIon, response.getRowCount().intValue());

        // Do it again with a more restrictive filter
        cmd.setFilters(Arrays.asList(
                fileFilter,
            new Filter("ComplexFragmentIon", ion, Filter.Operator.EQUAL),
            new Filter("PrecursorId/PeptideId/PeptideModifiedSequence", modifiedPeptide, Filter.Operator.EQUAL)));

        response = cmd.execute(cn, getCurrentContainerPath());
        Assert.assertEquals("Wrong number of transitions with ComplexFragmentIon = " + ion, transitionMz.length, response.getRowCount().intValue());

        int index = 0;
        for (Row row : response.getRowset())
        {
            Assert.assertEquals("Wrong Mz for precursor " + index, precursorMz, (Double)row.getValue("PrecursorId/Mz"), 0.0001);
            Assert.assertEquals("Wrong Mz for transition " + index, transitionMz[index++], (Double)row.getValue("Mz"), 0.0001);
        }
    }
}
