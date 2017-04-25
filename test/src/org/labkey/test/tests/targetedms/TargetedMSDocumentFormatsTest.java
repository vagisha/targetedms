/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Category({DailyB.class, MS2.class})
public class TargetedMSDocumentFormatsTest extends TargetedMSTest
{
    private static final String SAMPLEDATA_FOLDER = "DocumentFormats/";

    public static final List<String> skyZipFileNames = Collections.unmodifiableList(Arrays.asList(
            "DocumentSerializerTest_36",
            "DocumentSerializerTest",
            "DocumentSerializerTest_compact"));
    @Test
    public void testDocumentSerialization() throws Exception {
        setupFolder(FolderType.Experiment);
        for (String filename : skyZipFileNames) {
            testImportDoc(filename);
        }
    }

    private void testImportDoc(String filename) {
        setupSubfolder(getProjectName(), filename, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + filename + ".sky.zip");
    }
}
