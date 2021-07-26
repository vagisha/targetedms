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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.GetQueriesCommand;
import org.labkey.remoteapi.query.GetQueriesResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.MS2;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Daily.class, MS2.class})
public class TargetedMSListTest extends TargetedMSTest
{
    private static final String LIST_SKY_FILE_1 = "ListTest.sky.zip";
    private static final String LIST_SKY_FILE_2 = "ListTestDiffValues.sky.zip";
    private static final String LIST_SKY_FILE_3 = "ListTestDiffProps.sky.zip";

    @Test
    public void testSteps() throws IOException, CommandException
    {
        setupFolder(FolderType.Experiment);
        importData(LIST_SKY_FILE_1);

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(LIST_SKY_FILE_1));
        verifyRunSummaryCountsPep(2,4,0, 5,53, 1, 0, 6);
        clickAndWait(Locator.linkContainingText("6 lists"));
        assertTextPresent("DocumentProperties", "Lorem Ipsum", "Protein Descriptions");
        clickAndWait(Locator.linkContainingText("Protein Descriptions"));

        // Check that the document header remains
        verifyRunSummaryCountsPep(2,4,0, 5,53, 1, 0, 6);
        assertTextPresent("ALBU_BOVIN", "main protein of plasma");

        List<String> queryNames = validateSampleInfo(1, Set.of("Mickey"), 5, 12);

        // Figure out the name of the list's query, which will include the RowId of its run
        Optional<String> samplesName = queryNames.stream().filter((s) -> s.endsWith("_Samples")).findFirst();
        assertTrue("Missing '*_Samples' from: " + queryNames, samplesName.isPresent());

        SelectRowsCommand rowsCommand = new SelectRowsCommand("targetedmslists", samplesName.get());
        rowsCommand.setRequiredVersion(9.1);
        SelectRowsResponse rowsResponse = rowsCommand.execute(createDefaultConnection(false), getCurrentContainerPath());
        assertEquals("Wrong number of rows in 'Samples'", 5, rowsResponse.getRows().size());

        Set<String> sampleNames = new HashSet<>();
        rowsResponse.getRowset().forEach((r) -> sampleNames.add((String)r.getValue("SampleName")));
        assertEquals("Wrong sample names", Set.of("Mickey", "Minnie", "Mighty", "Jerry", "Speedy"), sampleNames);


        // Import a second Skyline document that shares a list design so we can check that rows get unioned
        importData(LIST_SKY_FILE_2, 2);

        validateSampleInfo(2, Set.of("Mickey", "Itchy"), 11, 18);

        // Import a third Skyline document that has a different list design of the same name so we can check that it
        // takes over
        importData(LIST_SKY_FILE_3, 3);

        validateSampleInfo(3, new HashSet<>(Arrays.asList(null, "Mickey")), 5, 24);
    }

    /** @return the list of queries in the targetedmslist schema */
    private List<String> validateSampleInfo(int replicateRowCount, Set<String> expectedReplicateLookupValues, int sampleUnionRowCount, int listQueryCount) throws IOException, CommandException
    {
        // Check that the targetedms.replicate table has the expected rows and points to the right lookup values
        SelectRowsCommand replicateRowsCommand = new SelectRowsCommand("targetedms", "replicate");
        replicateRowsCommand.setRequiredVersion(9.1);
        SelectRowsResponse replicateRowsResponse = replicateRowsCommand.execute(createDefaultConnection(false), getCurrentContainerPath());
        assertEquals("Wrong number of replicate rows", replicateRowCount, replicateRowsResponse.getRowCount().intValue());
        Set<String> replicateSampleLookupValues = new HashSet<>();
        replicateRowsResponse.getRowset().forEach((r) -> replicateSampleLookupValues.add((String)r.getDisplayValue("Sample")));
        assertEquals("Wrong sample names", expectedReplicateLookupValues, replicateSampleLookupValues);

        // Check the contents of the unioned "Samples" lookup query
        SelectRowsCommand sampleRowsUnionedCommand = new SelectRowsCommand("targetedmslists", "All_Samples");
        assertEquals("Wrong number of unioned sample list rows", sampleUnionRowCount, sampleRowsUnionedCommand.execute(createDefaultConnection(false), getCurrentContainerPath()).getRowCount().intValue());

        // Check the schema to make sure it's exposing the expected single-list and unioned-list queries
        GetQueriesCommand command = new GetQueriesCommand("targetedmslists");
        GetQueriesResponse queriesResponse = command.execute(createDefaultConnection(false), getCurrentContainerPath());
        assertEquals("Wrong number of queries", listQueryCount, queriesResponse.getQueryNames().size());
        Set<String> trimmedNames = queriesResponse.getQueryNames().stream().filter((s) -> !s.startsWith("All_")).map((s) -> s.substring(s.indexOf("_") + 1)).collect(Collectors.toSet());
        assertTrue("Missing 'DocumentProperties'", trimmedNames.contains("DocumentProperties"));
        assertTrue("Missing 'Numbers'", trimmedNames.contains("Numbers"));

        return queriesResponse.getQueryNames();
    }
}
