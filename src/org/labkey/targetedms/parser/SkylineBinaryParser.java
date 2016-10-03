/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.targetedms.parser;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.zip.DataFormatException;

/**
 * Parses the .skyd binary file format, for chromatogram data.
 *
 * Based on ChromatogramCache.cs and ChromHeaderInfo.cs from Skyline
 * 
 * User: jeckels
 * Date: Apr 13, 2012
 */
public class SkylineBinaryParser
{
    private final File _file;
    private final Logger _log;
    private FileChannel _channel;
    private RandomAccessFile _randomAccessFile;
    private final long[] _headers = new long[Header.values().length - 1];

    private Chromatogram[] _chromatograms;
    private ChromatogramTran[] _transitions;
    private float[] _allPeaksRt;
    private int _peakSize = (Float.SIZE * 7 + Integer.SIZE ) / 8;

    public static final int FORMAT_VERSION_CACHE_11 = 11;
    public static final int FORMAT_VERSION_CACHE_10 = 10; // Introduces additional lock mass parameters InstrumentInfoUtil
    public static final int FORMAT_VERSION_CACHE_9 = 9; // Introduces abbreviated scan ids
    public static final int FORMAT_VERSION_CACHE_8 = 8; // Introduces ion mobility data
    public static final int FORMAT_VERSION_CACHE_7 = 7; // Introduces UTF8 character support
    public static final int FORMAT_VERSION_CACHE_6 = 6;
    public static final int FORMAT_VERSION_CACHE_5 = 5;
    public static final int FORMAT_VERSION_CACHE_4 = 4;
    public static final int FORMAT_VERSION_CACHE_3 = 3;
    public static final int FORMAT_VERSION_CACHE_2 = 2;

    /** Newest supported version */
    public static final int FORMAT_VERSION_CACHE = FORMAT_VERSION_CACHE_11;

    private CachedFile[] _cacheFiles;

    public SkylineBinaryParser(File file, Logger log)
    {
        _file = file;
        _log = log;
    }

    public Chromatogram[] getChromatograms()
    {
        return _chromatograms;
    }

    public void close()
    {
        if (_channel != null)
        {
            try { _channel.close(); } catch (IOException ignored) {}
        }
        if (_randomAccessFile != null)
        {
            try { _randomAccessFile.close(); } catch (IOException ignored) {}
        }
    }

    /** File-level header fields */
    // ChromatogramCache.Header in Skyline code
    private enum Header
    {
        // Version 9 header addition
        location_scan_ids_lo,
        location_scan_ids_hi,

        // Version 5 header addition
        num_score_types,
        num_scores,
        location_scores_lo(true),
        location_scores_hi,
        num_seq_bytes,
        location_seq_bytes_lo(true),
        location_seq_bytes_hi,

        format_version,
        num_peaks,
        location_peaks_lo(true),
        location_peaks_hi,
        num_transitions,
        location_trans_lo(true),
        location_trans_hi,
        num_chromatograms,
        location_headers_lo(true),
        location_headers_hi,
        num_files,
        location_files_lo(true),
        location_files_hi,

        count;

        private final boolean _longValue;

        private Header()
        {
            this(false);
        }

        private Header(boolean longValue)
        {
            _longValue = longValue;
        }

        public long getHeaderValueLong(long[] headers)
        {
            if (!_longValue)
            {
                throw new IllegalStateException("Can only request long value from low bit enum values");
            }
            return headers[ordinal()] + (headers[ordinal() + 1] << 32);
        }

        public int getHeaderValueInt(long[] headers)
        {
            long value = headers[ordinal()];
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            {
                throw new IllegalStateException("Value out of range " + value + " for " + this);
            }
            return (int)value;
        }
    }

    public void parse() throws IOException
    {
        try
        {
            _randomAccessFile = new RandomAccessFile(_file, "r");
            long fileSize = _file.length();

            int headerLength = Header.count.ordinal();
            _channel = _randomAccessFile.getChannel();

            ByteBuffer buffer = _channel.map(FileChannel.MapMode.READ_ONLY, fileSize - headerLength * Integer.SIZE / 8, headerLength * Integer.SIZE / 8); // 4 bytes per int
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < _headers.length; i++)
            {
                _headers[i] = ((long)buffer.getInt() & 0xFFFFFFFFL);
            }
            int version = Header.format_version.getHeaderValueInt(_headers);

            if (version < FORMAT_VERSION_CACHE_2)
            {
                _log.warn("Version " + version + " is not supported for .skyd files. The earliest supported version is " + FORMAT_VERSION_CACHE_2 + ". Skipping chromatogram import.");
                return;
            }
            if (version > FORMAT_VERSION_CACHE)
            {
                _log.warn("Version " + version + " is not supported for .skyd files. The newest supported version is " + FORMAT_VERSION_CACHE + ". Skipping chromatogram import.");
                return;
            }

            parseFiles(version);
            parsePeaks();
            parseTransitions(version);
            parseChromatograms(version);
        }
        catch (DataFormatException e)
        {
            throw new IOException("Invalid ZIP content", e);
        }
    }

    private enum FileHeader
    {
        modified_lo,
        modified_hi,
        len_path,
        // Version 3 file header addition
        runstart_lo,
        runstart_hi,
        // Version 4 file header addition
        len_instrument_info,
        // Version 5 file header addition
        flags,
        // Version 6 file header addition
        max_retention_time,
        max_intensity,
        // Version 9 file header addition
        size_scan_ids,
        location_scan_ids_lo,
        location_scan_ids_hi,

        count;

        public static int getSize(int formatVersion)
        {
            switch (formatVersion)
            {
                case FORMAT_VERSION_CACHE_2:
                    return FileHeader.runstart_lo.ordinal() * 4;
                case FORMAT_VERSION_CACHE_3:
                    return FileHeader.len_instrument_info.ordinal() * 4;
                case FORMAT_VERSION_CACHE_4:
                    return FileHeader.flags.ordinal() * 4;
                case FORMAT_VERSION_CACHE_5:
                    return FileHeader.max_retention_time.ordinal() * 4;
                case FORMAT_VERSION_CACHE_6:
                case FORMAT_VERSION_CACHE_7:
                case FORMAT_VERSION_CACHE_8:
                    return FileHeader.size_scan_ids.ordinal() * 4;
                default:
                    return FileHeader.count.ordinal() * 4;
            }
        }
    }

    public static class CachedFile
    {
        private final String _filePath;
        private final String _instrumentInfo;
        private final int _flags;

        public CachedFile(String filePath, String instrumentInfo, int flags)
        {
            _filePath = filePath;
            _instrumentInfo = instrumentInfo;
            _flags = flags;
        }

        public String getFilePath()
        {
            return _filePath;
        }

        public String getInstrumentInfo()
        {
            return _instrumentInfo;
        }

        public boolean IsSingleMatchMz()
        {
            return (_flags & 0x02) != 0;
        }
    }

    private void parseFiles(int formatVersion) throws IOException
    {
        ByteBuffer buffer;
        int numFiles = Header.num_files.getHeaderValueInt(_headers);
        long filesStart = Header.location_files_lo.getHeaderValueLong(_headers);

        _cacheFiles = new CachedFile[numFiles];
        int countFileHeader = FileHeader.getSize(formatVersion);

        long offset = filesStart;

        for (int i = 0; i < numFiles; i++)
        {
            buffer = _channel.map(FileChannel.MapMode.READ_ONLY, offset, countFileHeader);
            offset+= countFileHeader;
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            long modifiedBinary = buffer.getLong();
            int lenPath = buffer.getInt();

            long runstartBinary = (isVersionCurrent() ? buffer.getLong() : 0);
            int lenInstrumentInfo = formatVersion > FORMAT_VERSION_CACHE_3 ? buffer.getInt() : -1;
            int flags = formatVersion > FORMAT_VERSION_CACHE_4 ? buffer.getInt() : 0;

            byte[] filePathBuffer = new byte[lenPath];
            buffer = _channel.map(FileChannel.MapMode.READ_ONLY, offset, lenPath);
            offset += lenPath;
            buffer.get(filePathBuffer);
            String filePath = formatVersion > FORMAT_VERSION_CACHE_6 ?
											  new String(filePathBuffer, 0, lenPath, Charset.forName("UTF8"))
											: new String(filePathBuffer, 0, lenPath);

            String instrumentInfoStr = null;
            if (lenInstrumentInfo >= 0)
            {
                byte[] instrumentInfoBuffer = new byte[lenInstrumentInfo];
                buffer = _channel.map(FileChannel.MapMode.READ_ONLY, offset, lenInstrumentInfo);
                offset += lenInstrumentInfo;
                buffer.get(instrumentInfoBuffer);
                instrumentInfoStr = new String(instrumentInfoBuffer, 0, lenInstrumentInfo, Charset.forName("UTF8"));
            }

            Date modifiedTime = new Date(modifiedBinary);
            Date runstartTime = (runstartBinary != 0 ? new Date(runstartBinary) : null);
//            var instrumentInfoList = InstrumentInfoUtil.GetInstrumentInfo(instrumentInfoStr);
            _cacheFiles[i] = new CachedFile(filePath, instrumentInfoStr, flags);
        }
    }

    private void parsePeaks() throws IOException
    {
        int numPeaks = Header.num_peaks.getHeaderValueInt(_headers);
        long peaksStart = Header.location_peaks_lo.getHeaderValueLong(_headers);

        ByteBuffer buffer = _channel.map(FileChannel.MapMode.READ_ONLY, peaksStart, _peakSize * numPeaks);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        _allPeaksRt = new float[numPeaks];
        for (int i = 0; i< numPeaks; i++)
        {

            float retentionTime = buffer.getFloat();
            buffer.getFloat(); // startTime
            buffer.getFloat(); // endTime
            buffer.getFloat(); // area
            buffer.getFloat(); // backgroundArea
            buffer.getFloat(); // height
            buffer.getFloat(); // fwhm
            buffer.getInt(); // flagBits

            _allPeaksRt[i] = retentionTime;
        }
    }

    private void parseTransitions(int formatVersion) throws IOException
    {
        int numTransitions = Header.num_transitions.getHeaderValueInt(_headers);
        long transitionsStart = Header.location_trans_lo.getHeaderValueLong(_headers);

        ByteBuffer buffer = _channel.map(FileChannel.MapMode.READ_ONLY, transitionsStart, ChromatogramTran.getSize(formatVersion) * numTransitions);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        _transitions = new ChromatogramTran[numTransitions];
        for (int i = 0; i< numTransitions; i++)
        {

            if (formatVersion > FORMAT_VERSION_CACHE_4)
            {
                double product = buffer.getDouble();
                float extractionWidth = buffer.getFloat();
                float ionMobilityValue = 0;
                float ionMobilityExtractionWidth = 0;
                if(formatVersion > FORMAT_VERSION_CACHE_7)
                {
                    ionMobilityValue = buffer.getFloat();
                    ionMobilityExtractionWidth = buffer.getFloat();
                }

                _transitions[i] = new ChromatogramTran(product, extractionWidth,
                                                       ionMobilityValue, ionMobilityExtractionWidth,
                                                       ChromatogramTran.Source.fromBits(buffer.getShort()));
                buffer.getShort();  // read padding
            }
            else
            {
                _transitions[i] = new ChromatogramTran(buffer.getFloat());
            }
        }
    }

    private void parseChromatograms(int formatVersion) throws IOException, DataFormatException
    {
        byte[] seqBytes = null;
        if (formatVersion > FORMAT_VERSION_CACHE_4)
        {
            int numSeqBytes = Header.num_seq_bytes.getHeaderValueInt(_headers);
            long seqBytesStart = Header.location_seq_bytes_lo.getHeaderValueLong(_headers);
            ByteBuffer bufferSb = _channel.map(FileChannel.MapMode.READ_ONLY, seqBytesStart, numSeqBytes);
            seqBytes = new byte[numSeqBytes];
            bufferSb.get(seqBytes);
        }

        int numChrom = Header.num_chromatograms.getHeaderValueInt(_headers);
        long chromatogramStart = Header.location_headers_lo.getHeaderValueLong(_headers);

        ByteBuffer buffer = _channel.map(FileChannel.MapMode.READ_ONLY, chromatogramStart, Chromatogram.getSize(formatVersion) * numChrom);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read the chromatogram headers out of the file
        _chromatograms = new Chromatogram[numChrom];
        for (int i = 0; i < _chromatograms.length; i++)
        {
            _chromatograms[i] = new Chromatogram(formatVersion, buffer, seqBytes, _cacheFiles, _transitions, _allPeaksRt);
        }
    }

    public FileChannel getChannel()
    {
        return _channel;
    }

    public boolean isVersionCurrent()
    {
        long version = Header.format_version.getHeaderValueInt(_headers);
        return (version >= FORMAT_VERSION_CACHE_3 && FORMAT_VERSION_CACHE >= version);
    }

    final int getCacheFileSize()
    {
        return _cacheFiles != null ? _cacheFiles.length : 0;
    }
}
