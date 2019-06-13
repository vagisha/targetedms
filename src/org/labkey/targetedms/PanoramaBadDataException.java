/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms;

import org.jetbrains.annotations.NonNls;
import org.labkey.api.util.SkipMothershipLogging;

/**
 * Indicates there was something wrong with an input file.
 * Created by Josh on 10/19/2017.
 */
public class PanoramaBadDataException extends RuntimeException implements SkipMothershipLogging
{
    public PanoramaBadDataException(@NonNls String message)
    {
        super(message);
    }
}
