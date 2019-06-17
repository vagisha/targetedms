/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyd;

/**
 * Version numbers of skyd files.
 */
public enum CacheFormatVersion
{
    Zero,
    One,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,// Introduces UTF8 character support
    Eight,// Introduces ion mobility data
    Nine,// Introduces abbreviated scan ids
    Ten,// Introduces waters lockmass correction in MSDataFileUri syntax
    Eleven,// Adds chromatogram start, stop times, and uncompressed size info, and new flag bit for SignedMz
    Twelve,// Adds structure sizes to CacheHeaderStruct
    Thirteen, // Adds total ion current to CachedFileHeaderStruct
    UnknownFutureVersion;
    public static CacheFormatVersion fromInteger(int i) {
        if (i <= Zero.ordinal()) {
            return Zero;
        }
        if (i >= UnknownFutureVersion.ordinal()) {
            return UnknownFutureVersion;
        }
        return values()[i];
    }
    public static final CacheFormatVersion CURRENT = Thirteen;
}
