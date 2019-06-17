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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Describes the format of a skyd file, including the version number, and the size of the various structures.
 */
public class CacheFormat {
    public static final CacheFormatVersion WithStructSizes = CacheFormatVersion.Twelve;
    private CacheFormatVersion _formatVersion;
    private CacheFormatVersion _versionRequired;
    private int _cachedFileSize;
    private int _chromGroupHeaderSize;
    private int _chromPeakSize;
    private int _chromTransitionSize;

    private CacheFormat() {

    }

    public static CacheFormat fromVersion(CacheFormatVersion formatVersion) {
        if (formatVersion == CacheFormatVersion.UnknownFutureVersion) {
            throw new IllegalArgumentException();
        }
        CacheFormat cacheFormat = new CacheFormat();
        cacheFormat._formatVersion = formatVersion;
        cacheFormat._versionRequired = formatVersion;
        cacheFormat._cachedFileSize = CachedFileHeaderStruct.getStructSize(formatVersion);
        return cacheFormat;
    }

    public CacheFormat(CacheHeaderStruct cacheHeaderStruct) {
        _formatVersion = CacheFormatVersion.fromInteger(cacheHeaderStruct.formatVersion);
        _versionRequired = CacheFormatVersion.fromInteger(cacheHeaderStruct.versionRequired);
        if (_formatVersion.compareTo(WithStructSizes) >= 0) {
            _cachedFileSize = cacheHeaderStruct.cachedFileSize;
            _chromGroupHeaderSize = cacheHeaderStruct.chromGroupHeaderSize;
            _chromTransitionSize = cacheHeaderStruct.chromTransitionSize;
            _chromPeakSize = cacheHeaderStruct.chromPeakSize;
        } else {
            _cachedFileSize = CachedFileHeaderStruct.getStructSize(_formatVersion);
            _chromGroupHeaderSize = ChromGroupHeaderInfo.getStructSize(_formatVersion);
            _chromTransitionSize = ChromTransition.getStructSize(_formatVersion);
            _chromPeakSize = ChromPeak.getStructSize(_formatVersion);

        }
    }

    public Charset getCharset() {
        if (getFormatVersion().compareTo(CacheFormatVersion.Six) > 0) {
            return StandardCharsets.UTF_8;
        }
        return StandardCharsets.ISO_8859_1;
    }

    public CacheFormatVersion getFormatVersion()
    {
        return _formatVersion;
    }

    public CacheFormatVersion getVersionRequired()
    {
        return _versionRequired;
    }

    public StructSerializer<CachedFileHeaderStruct> cachedFileSerializer()
    {
        return new StructSerializer<CachedFileHeaderStruct>(CachedFileHeaderStruct.class,
                CachedFileHeaderStruct.getStructSize(CacheFormatVersion.CURRENT),
                _cachedFileSize)
        {
            @Override
            public CachedFileHeaderStruct fromByteArray(byte[] bytes)
            {
                return new CachedFileHeaderStruct(new LittleEndianByteArrayInputStream(bytes));
            }
        };
    }

    public StructSerializer<ChromGroupHeaderInfo> chromGroupHeaderInfoSerializer() {
        return new StructSerializer<ChromGroupHeaderInfo>(ChromGroupHeaderInfo.class, ChromGroupHeaderInfo.getStructSize(CacheFormatVersion.CURRENT), _chromGroupHeaderSize)
        {
            @Override
            public ChromGroupHeaderInfo fromByteArray(byte[] bytes)
            {
                return new ChromGroupHeaderInfo(_formatVersion, new LittleEndianByteArrayInputStream(bytes));
            }
        };
    }
    public StructSerializer<ChromTransition> chromTransitionSerializer() {
        return new StructSerializer<ChromTransition>(ChromTransition.class, ChromTransition.getStructSize(CacheFormatVersion.CURRENT), _chromTransitionSize)
            {
                @Override
                public ChromTransition fromByteArray(byte[] bytes)
                {
                    return new ChromTransition(_formatVersion, new LittleEndianByteArrayInputStream(bytes));
                }
            };
    }

    public StructSerializer<ChromPeak> chromPeakSerializer() {
        return new StructSerializer<ChromPeak>(ChromPeak.class, ChromPeak.getStructSize(CacheFormatVersion.CURRENT), _chromPeakSize)
        {
            @Override
            public ChromPeak fromByteArray(byte[] bytes)
            {
                return new ChromPeak(new LittleEndianByteArrayInputStream(bytes));
            }
        };
    }
}

