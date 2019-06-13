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
import org.labkey.targetedms.parser.SignedMz;

import java.io.IOException;

/**
 * Structure for a transition within a ChromGroupHeaderInfo.
 */
public class ChromTransition
{
    double _product;
    private float _extractionWidth;
    private float _driftTime;
    private float _driftTimeExtractionWidth;
    private short _flagBits;
    private short _align1;

    public ChromTransition(CacheFormatVersion cacheFormatVersion, LittleEndianInput dataInputStream)
    {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Four) <= 0) {
            _product = Float.intBitsToFloat(dataInputStream.readInt());
        } else if (cacheFormatVersion.compareTo(CacheFormatVersion.Six) <= 0) {
            _product = dataInputStream.readDouble();
            _extractionWidth = Float.intBitsToFloat(dataInputStream.readInt());
            _flagBits = dataInputStream.readShort();
            _align1 = dataInputStream.readShort();
        } else {
            _product = dataInputStream.readDouble();
            _extractionWidth = Float.intBitsToFloat(dataInputStream.readInt());
            _driftTime = Float.intBitsToFloat(dataInputStream.readInt());
            _driftTimeExtractionWidth = Float.intBitsToFloat(dataInputStream.readInt());
            _flagBits = dataInputStream.readShort();
            _align1 = dataInputStream.readShort();
        }
    }

    public static int getStructSize(CacheFormatVersion cacheFormatVersion) {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Five) < 0) {
            return 4;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Six) <= 0) {
            return 16;
        }
        return 24;
    }

    public SignedMz getProduct(ChromGroupHeaderInfo chromGroupHeaderInfo) {
        if (chromGroupHeaderInfo.isNegativePolarity()) {
            return new SignedMz(Math.abs(_product), true);
        }
        return new SignedMz(Math.abs(_product), false);
    }

    public boolean isMissingMassErrors() {
        return 0 != (_flagBits & 0x04);
    }

    public ChromSource getChromSource() {
        return ChromSource.values()[_flagBits & 0x03];
    }

    public enum ChromSource {
        unknown,
        ms1,
        fragment,
        sim
    }
}
