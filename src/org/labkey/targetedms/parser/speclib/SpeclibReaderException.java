/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

package org.labkey.targetedms.parser.speclib;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.util.FileUtil;
import org.labkey.targetedms.parser.PeptideSettings;

import java.nio.file.Path;

public class SpeclibReaderException extends Exception
{
    public SpeclibReaderException(@NotNull Throwable cause, @NotNull PeptideSettings.SpectrumLibrary library, @NotNull Path libFilePath)
    {
        super(buildMessage(cause, library, libFilePath), cause);
    }

    private static String buildMessage(@NotNull Throwable cause, @NotNull PeptideSettings.SpectrumLibrary library, @NotNull Path libFilePath)
    {
        String libName = library.getName();
        String libFileName = FileUtil.getFileName(libFilePath);
        StringBuilder err = new StringBuilder("Error reading from spectrum library").append(" ").append(libFileName);
        if(!FileUtil.getBaseName(libFileName).equals(libName))
        {
            // Append the library name if it is different from the base library file name
            err.append(" (").append(libName).append(")");
        }
        if(cause.getMessage() != null)
        {
            err.append(". Error was: ").append(cause.getMessage());
        }
        return err.toString();
    }
}
