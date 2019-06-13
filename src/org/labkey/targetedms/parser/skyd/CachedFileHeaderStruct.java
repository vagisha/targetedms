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

import org.apache.poi.util.LittleEndianInput;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Created by nicksh on 2/27/2017.
 */
public class CachedFileHeaderStruct
{
    long modified;
    int lenPath;
    // Version 3 file header addition
    long runStart;
    // Version 4 file header addition
    int lenInstrumentInfo;
    // Version 5 file header addition
    int flags;
    // Version 6 file header addition
    float maxRetentionTime;
    float maxIntensity;
    // Version 9 file header addition
    int sizeScanIds;
    long locationScanIds;
    float ticArea;

    public CachedFileHeaderStruct(LittleEndianInput dataInputStream)
    {
        modified = dataInputStream.readLong();
        lenPath = dataInputStream.readInt();
        runStart = dataInputStream.readLong();
        lenInstrumentInfo = dataInputStream.readInt();
        flags = dataInputStream.readInt();
        maxRetentionTime = Float.intBitsToFloat(dataInputStream.readInt());
        maxIntensity = Float.intBitsToFloat(dataInputStream.readInt());
        sizeScanIds = dataInputStream.readInt();
        locationScanIds = dataInputStream.readLong();
        ticArea = Float.intBitsToFloat(dataInputStream.readInt());
    }

    public static int getStructSize(CacheFormatVersion formatVersion) {
        if (formatVersion.compareTo(CacheFormatVersion.Thirteen) >= 0) {
            return 52;
        }
        if (formatVersion.compareTo(CacheFormatVersion.Eight) > 0) {
            return 48;
        }
        if (formatVersion.compareTo(CacheFormatVersion.Five) > 0) {
            return 36;
        }
        if (formatVersion.compareTo(CacheFormatVersion.Four) > 0) {
            return 28;
        }
        if (formatVersion.compareTo(CacheFormatVersion.Three) > 0) {
            return 24;
        }
        if (formatVersion.compareTo(CacheFormatVersion.Two) > 0) {
            return 20;
        }
        return 12;
    }

    public EnumSet<Flags> getFlags()
    {
        return Arrays.stream(Flags.values())
                .filter(flag -> 0 != (flags & (1 << flag.ordinal())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Flags.class)));
    }

    public long getModified()
    {
        return modified;
    }

    public int getLenPath()
    {
        return lenPath;
    }

    public long getRunStart()
    {
        return runStart;
    }

    public int getLenInstrumentInfo()
    {
        return lenInstrumentInfo;
    }

    public float getMaxRetentionTime()
    {
        return maxRetentionTime;
    }

    public float getMaxIntensity()
    {
        return maxIntensity;
    }

    public int getSizeScanIds()
    {
        return sizeScanIds;
    }

    public long getLocationScanIds()
    {
        return locationScanIds;
    }

    public enum Flags {
        single_match_mz_known,
        single_match_mz,
        has_midas_spectra;
    }
}
