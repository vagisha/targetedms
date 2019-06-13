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

import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianInput;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;

/**
 * Data is written at the very end of a skyd file, and which includes the version information and
 * the location of all of the other structures in the skyd file.
 *
 * Future versions of Skyline will add elements to the beginning of this structure, so that the
 * location of existing elements relative to the end of the file will remain constant.
 */
public class CacheHeaderStruct
{
    // Version 12 fields after this point
    int chromPeakSize;
    int chromTransitionSize;
    int chromGroupHeaderSize;
    int cachedFileSize;
    int versionRequired;
    // Version 9 fields after this point
    long locationScanIds;
    // Version 5 fields after this point
    int numScoreTypes;
    int numScores;
    long locationScores;
    int numTextIdBytes;
    long locationTextIdBytes;
    // Version 2 fields after this point
    int formatVersion;
    int numPeaks;
    long locationPeaks;
    int numTransitions;
    long locationTransitions;
    int numChromatograms;
    long locationHeaders;
    int numFiles;
    long locationFiles;

    public CacheHeaderStruct(LittleEndianInput inputStream)
    {
        chromPeakSize = inputStream.readInt();
        chromTransitionSize = inputStream.readInt();
        chromGroupHeaderSize = inputStream.readInt();
        cachedFileSize = inputStream.readInt();
        versionRequired = inputStream.readInt();
        locationScanIds = inputStream.readLong();
        numScoreTypes = inputStream.readInt();
        numScores = inputStream.readInt();
        locationScores = inputStream.readLong();
        numTextIdBytes = inputStream.readInt();
        locationTextIdBytes = inputStream.readLong();
        formatVersion = inputStream.readInt();
        numPeaks = inputStream.readInt();
        locationPeaks = inputStream.readLong();
        numTransitions = inputStream.readInt();
        locationTransitions = inputStream.readLong();
        numChromatograms = inputStream.readInt();
        locationHeaders = inputStream.readLong();
        numFiles = inputStream.readInt();
        locationFiles = inputStream.readLong();
    }

    public int getNumFiles()
    {
        return numFiles;
    }

    public long getLocationFiles()
    {
        return locationFiles;
    }

    public int getNumPeaks()
    {
        return numPeaks;
    }

    public long getLocationPeaks()
    {
        return locationPeaks;
    }

    public int getNumTransitions()
    {
        return numTransitions;
    }

    public long getLocationTransitions()
    {
        return locationTransitions;
    }

    public int getNumChromatograms()
    {
        return numChromatograms;
    }

    public long getLocationHeaders()
    {
        return locationHeaders;
    }

    public int getNumTextIdBytes()
    {
        return numTextIdBytes;
    }

    public long getLocationTextIdBytes()
    {
        return locationTextIdBytes;
    }

    public static int getStructSize(CacheFormatVersion cacheFormatVersion)
    {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Five) < 0)
        {
            return 52;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Nine) < 0)
        {
            return 80;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Twelve) < 0)
        {
            return 88;
        }
        return 108;
    }

    public static StructSerializer<CacheHeaderStruct> getStructSerializer(CacheFormatVersion cacheFormatVersion) {
        return new StructSerializer<CacheHeaderStruct>(
                CacheHeaderStruct.class, getStructSize(CacheFormatVersion.CURRENT), getStructSize(cacheFormatVersion))
        {
            @Override
            public CacheHeaderStruct fromByteArray(byte[] bytes)
            {
                return new CacheHeaderStruct(new LittleEndianByteArrayInputStream(bytes));
            }

            @Override
            protected boolean isPadFromStart()
            {
                return true;
            }
        };
    }

    public static CacheHeaderStruct read(SeekableByteChannel fileChannel) throws IOException
    {
        StructSerializer<CacheHeaderStruct> minimumStructSerializer = getStructSerializer(CacheFormatVersion.Two);
        fileChannel.position(fileChannel.size() - minimumStructSerializer.getItemSizeOnDisk());
        CacheHeaderStruct minimumStruct = minimumStructSerializer.readArray(Channels.newInputStream(fileChannel), 1)[0];
        StructSerializer<CacheHeaderStruct> actualStructSerializer = getStructSerializer(CacheFormatVersion.fromInteger(minimumStruct.formatVersion));
        if (actualStructSerializer.getItemSizeOnDisk() == minimumStructSerializer.getItemSizeOnDisk())
        {
            return minimumStruct;
        }
        fileChannel.position(fileChannel.size() - actualStructSerializer.getItemSizeOnDisk());
        return actualStructSerializer.readArray(Channels.newInputStream(fileChannel), 1)[0];
    }
}
