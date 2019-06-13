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
package org.labkey.targetedms.parser;

import com.google.common.primitives.Floats;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.LittleEndianInput;
import org.labkey.targetedms.parser.skyd.proto.ChromatogramGroupDataOuterClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The set of binary formats for the chromatogram data in a skyd file.
 * The ordinal values of these items are persisted in the
 * {@link PrecursorChromInfo#getChromatogramFormat()}.
 */
public enum ChromatogramBinaryFormat
{
    /**
     * Format that was exclusively used prior to skyd file format 12. All of the transitions in a ChromGroupHeader
     * info share one set of times.
     */
    Arrays{
        @Override
        public Chromatogram readChromatogram(byte[] uncompressedBytes, int numPoints, int numTrans)
        {
            LittleEndianInput dataInputStream = new LittleEndianByteArrayInputStream(uncompressedBytes);
            float[] sharedTimes = readFloats(dataInputStream, numPoints);
            List<float[]> transitionIntensities = new ArrayList<>();
            for (int i = 0; i < numTrans; i++) {
                transitionIntensities.add(readFloats(dataInputStream, numPoints));
            }
            // dataInputStream also has mass errors and scan ids, but we ignore them

            List<TimeIntensities> transitionTimeIntensities = transitionIntensities.stream()
                    .map(intensities->new TimeIntensities(sharedTimes, intensities))
                    .collect(Collectors.toList());

            return new Chromatogram(transitionTimeIntensities);
        }
    },
    /**
     * Format which is sometimes used in skyd file format 12 and later when the transitions in a ChromGroupHeaderInfo
     * have separate sets of times. The binary format is a Google Protocol Buffer.
     * @see ChromatogramGroupDataOuterClass
     */
    ChromatogramGroupData {
        @Override
        public Chromatogram readChromatogram(byte[] uncompressedBytes, int numPoints, int numTrans) throws IOException
        {
            ChromatogramGroupDataOuterClass.ChromatogramGroupData chromatogramGroupData
                    = ChromatogramGroupDataOuterClass.ChromatogramGroupData.parseFrom(uncompressedBytes);

            List<float[]> timeLists = chromatogramGroupData.getTimeListsList().stream()
                    .map(timeList->Floats.toArray(timeList.getTimesList()))
                    .collect(Collectors.toList());

            List<TimeIntensities> transitionTimeIntensities = new ArrayList<>();
            for (ChromatogramGroupDataOuterClass.ChromatogramGroupData.Chromatogram chromatogram
                    : chromatogramGroupData.getChromatogramsList()) {
                transitionTimeIntensities.add(new TimeIntensities(timeLists.get(chromatogram.getTimeListIndex() - 1),
                        Floats.toArray(chromatogram.getIntensitiesList())));
            }
            return new Chromatogram(transitionTimeIntensities);
        }

    };
    public abstract Chromatogram readChromatogram(byte[] uncompressedBytes, int numPoints, int numTrans) throws IOException;

    private static float[] readFloats(LittleEndianInput dataInputStream, int count)
    {
        float[] floats = new float[count];
        for (int i = 0; i < count; i++) {
            floats[i] = Float.intBitsToFloat(dataInputStream.readInt());
        }
        return floats;
    }
}
