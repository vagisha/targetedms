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
import org.jetbrains.annotations.Nullable;
import org.labkey.targetedms.parser.ChromatogramBinaryFormat;
import org.labkey.targetedms.parser.SignedMz;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Structure which describes a group of chromatograms.
 */
public class ChromGroupHeaderInfo
{
    private int textIdIndex;
    private int startTransitionIndex;
    private int startPeakIndex;
    private int startScoreIndex;
    private int numPoints;
    private int compressedSize;
    private short flagBits;
    private short fileIndex;
    private short textIdLen;
    private short numTransitions;
    private byte numPeaks;
    private byte maxPeakIndex;
    private byte isProcessedScans;
    private byte align1;
    private short statusId;
    private short statusRank;
    private double precursor;
    private long locationPoints;
    private int uncompressedSize;
    private Float startTime;
    private Float endTime;
    private float collisionalCrossSection;

    public ChromGroupHeaderInfo(CacheFormatVersion cacheFormatVersion, LittleEndianInput dataInputStream)
    {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Five) < 0) {
            precursor = Float.intBitsToFloat(dataInputStream.readInt());
            fileIndex = checkUShort(dataInputStream.readInt());
            numTransitions = checkUShort(dataInputStream.readInt());
            startTransitionIndex = dataInputStream.readInt();
            numPeaks = checkByte(dataInputStream.readInt());
            startPeakIndex = dataInputStream.readInt();
            int maxPeakIndexInt = dataInputStream.readInt();
            maxPeakIndex = maxPeakIndexInt == -1 ? (byte) 0xff : checkByte(maxPeakIndexInt);
            numPoints = dataInputStream.readInt();
            compressedSize = dataInputStream.readInt();
            dataInputStream.readInt(); // ignore these four bytes
            locationPoints = dataInputStream.readLong();
        } else {
            textIdIndex = dataInputStream.readInt();
            startTransitionIndex = dataInputStream.readInt();
            startPeakIndex = dataInputStream.readInt();
            startScoreIndex = dataInputStream.readInt();
            numPoints = dataInputStream.readInt();
            compressedSize = dataInputStream.readInt();
            flagBits = dataInputStream.readShort();
            fileIndex = dataInputStream.readShort();
            textIdLen = dataInputStream.readShort();
            numTransitions = dataInputStream.readShort();
            numPeaks = dataInputStream.readByte();
            maxPeakIndex = dataInputStream.readByte();
            isProcessedScans = dataInputStream.readByte();
            align1 = dataInputStream.readByte();
            statusId = dataInputStream.readShort();
            statusRank = dataInputStream.readShort();
            precursor = dataInputStream.readDouble();
            locationPoints = dataInputStream.readLong();
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Eleven) < 0) {
            uncompressedSize = -1;
        } else {
            uncompressedSize = dataInputStream.readInt();
            startTime = Float.intBitsToFloat(dataInputStream.readInt());
            endTime = Float.intBitsToFloat(dataInputStream.readInt());
            collisionalCrossSection = Float.intBitsToFloat(dataInputStream.readInt());
        }
    }

    private static byte checkByte(int value) {
        if (value > 0xff || value < 0) {
            throw new IllegalArgumentException();
        }
        return (byte) value;
    }

    private static short checkUShort(int value) {
        if (value > 0xffff || value < 0) {
            throw new IllegalArgumentException();
        }
        return (short) value;
    }

    public int getFileIndex()
    {
        return Short.toUnsignedInt(fileIndex);
    }

    public EnumSet<FlagValues> getFlagValues() {
        return EnumFlagValues.enumSetFromFlagValues(FlagValues.class, Short.toUnsignedLong(flagBits));
    }

    public enum FlagValues
    {
        has_mass_errors,
        has_calculated_mzs,
        extracted_base_peak,
        has_ms1_scan_ids,
        has_sim_scan_ids,
        has_frag_scan_ids,
        polarity_negative,
        raw_chromatograms
    }

    public static int getStructSize(CacheFormatVersion cacheFormatVersion) {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Four) <= 0) {
            return 48;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Eleven) < 0) {
            return 56;
        }
        return 72;
    }

    public int getTextIdIndex()
    {
        return textIdIndex;
    }

    public int getStartTransitionIndex()
    {
        return startTransitionIndex;
    }

    public int getStartPeakIndex()
    {
        return startPeakIndex;
    }

    public int getStartScoreIndex()
    {
        return startScoreIndex;
    }

    public int getNumPoints()
    {
        return numPoints;
    }

    public int getCompressedSize()
    {
        return compressedSize;
    }

    public short getTextIdLen()
    {
        return textIdLen;
    }

    public short getNumTransitions()
    {
        return numTransitions;
    }

    public byte getNumPeaks()
    {
        return numPeaks;
    }

    public long getLocationPoints()
    {
        return locationPoints;
    }

    public int getUncompressedSize()
    {
        if (uncompressedSize != -1) {
            return uncompressedSize;
        }
        int sizeArray = (Integer.SIZE / 8)*numPoints;
        int sizeArrayErrors = (Short.SIZE / 8)*numPoints;
        int sizeTotal = sizeArray*(numTransitions + 1);
        EnumSet<FlagValues> flagValues = getFlagValues();
        if (flagValues.contains(FlagValues.has_mass_errors))
            sizeTotal += sizeArrayErrors*numTransitions;
        if (flagValues.contains(FlagValues.has_ms1_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        if (flagValues.contains(FlagValues.has_frag_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        if (flagValues.contains(FlagValues.has_sim_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        return sizeTotal;

    }

    public byte getMaxPeakIndex()
    {
        return maxPeakIndex;
    }

    @Nullable
    public Float getStartTime()
    {
        return startTime;
    }

    @Nullable
    public Float getEndTime()
    {
        return endTime;
    }

    public SignedMz getPrecursor() {
        if (isNegativePolarity()) {
            return new SignedMz(Math.abs(precursor), true);
        }
        return new SignedMz(Math.abs(precursor), false);
    }

    public double getPrecursorMz() {
        return Math.abs(precursor) * (isNegativePolarity() ? -1 : 1);
    }

    public SignedMz toSignedMz(double mz) {
        return new SignedMz(Math.abs(mz), isNegativePolarity());
    }

    public boolean isNegativePolarity() {
        return 0 != (flagBits & (1 << FlagValues.polarity_negative.ordinal()));
    }

    public boolean excludesTime(double time) {
        if (null == startTime || null == endTime) {
            return false;
        }
        return startTime > time || endTime < time;
    }

    public ChromatogramBinaryFormat getChromatogramBinaryFormat() {
        return getFlagValues().contains(FlagValues.raw_chromatograms)
                ? ChromatogramBinaryFormat.ChromatogramGroupData : ChromatogramBinaryFormat.Arrays;
    }
}
