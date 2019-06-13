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
package org.labkey.targetedms.parser;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.labkey.api.exp.api.DataType;
import org.labkey.targetedms.parser.skyd.CacheFormat;
import org.labkey.targetedms.parser.skyd.CacheFormatVersion;
import org.labkey.targetedms.parser.skyd.CacheHeaderStruct;
import org.labkey.targetedms.parser.skyd.CachedFileHeaderStruct;
import org.labkey.targetedms.parser.skyd.ChromGroupHeaderInfo;
import org.labkey.targetedms.parser.skyd.ChromPeak;
import org.labkey.targetedms.parser.skyd.ChromTransition;
import org.labkey.targetedms.parser.skyd.StructSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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
    private CacheFormat _cacheFormat;
    private CacheHeaderStruct _cacheHeaderStruct;

    private ChromGroupHeaderInfo[] _chromatograms;
    private ChromTransition[] _transitions;
    private float[] _allPeaksRt;
    private byte[] _seqBytes;


    /** Newest supported version */
    public static final CacheFormatVersion FORMAT_VERSION_CACHE = CacheFormatVersion.CURRENT;

    public static final DataType DATA_TYPE = new DataType("skyd");

    private CachedFile[] _cacheFiles;

    public SkylineBinaryParser(File file, Logger log)
    {
        _file = file;
        _log = log;
    }

    public ChromGroupHeaderInfo[] getChromatograms()
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

        // TODO: We need to call garbage collection here to free the skyd file for possible deletion. When mapping a file
        // using a FileChannel there is a known Java issue on Windows that prevents the mapped file from being deleted,
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154 and http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4469299
        System.gc();
    }

    public void parse() throws IOException
    {
        _randomAccessFile = new RandomAccessFile(_file, "r");
        _channel = _randomAccessFile.getChannel();
        _cacheHeaderStruct = CacheHeaderStruct.read(_channel);
        _cacheFormat = new CacheFormat(_cacheHeaderStruct);

        if (_cacheFormat.getFormatVersion().compareTo(CacheFormatVersion.Two) < 0) {
            _log.warn("Version " + _cacheFormat.getFormatVersion() + " is not supported for .skyd files. The earliest supported version is " + CacheFormatVersion.Two + ". Skipping chromatogram import.");
            return;
        }
        if (_cacheFormat.getVersionRequired().compareTo(FORMAT_VERSION_CACHE) > 0) {
            _log.warn("Version " + _cacheFormat.getVersionRequired() + " is not supported for .skyd files. The newest supported version is " + FORMAT_VERSION_CACHE + ". Skipping chromatogram import.");
            return;
        }

        parseFiles();
        parsePeaks();
        parseTransitions();
        parseChromatograms();
    }

    public static class CachedFile
    {
        private final String _filePath;
        private final String _instrumentInfo;
        private final EnumSet<CachedFileHeaderStruct.Flags> _flags;

        public CachedFile(String filePath, String instrumentInfo, EnumSet<CachedFileHeaderStruct.Flags> flags)
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
            return _flags.contains(CachedFileHeaderStruct.Flags.single_match_mz);
        }
    }

    private void parseFiles() throws IOException
    {
        CacheFormat cacheFormat = _cacheFormat;
        CacheHeaderStruct cacheHeaderStruct = _cacheHeaderStruct;
        StructSerializer<CachedFileHeaderStruct> cachedFileHeaderSerializer = cacheFormat.cachedFileSerializer();
        _channel.position(cacheHeaderStruct.getLocationFiles());
        InputStream stream = Channels.newInputStream(_channel);
        _cacheFiles = new CachedFile[_cacheHeaderStruct.getNumFiles()];
        for (int i = 0; i < cacheHeaderStruct.getNumFiles(); i++)
        {
            CachedFileHeaderStruct cachedFileStruct = cachedFileHeaderSerializer.readArray(stream, 1)[0];
            byte[] filePathBuffer = new byte[cachedFileStruct.getLenPath()];
            IOUtils.readFully(stream, filePathBuffer);
            String filePath = new String(filePathBuffer, cacheFormat.getCharset());

            String instrumentInfoStr = null;
            if (cachedFileStruct.getLenInstrumentInfo() >= 0)
            {
                byte[] instrumentInfoBuffer = new byte[cachedFileStruct.getLenInstrumentInfo()];
                IOUtils.readFully(stream, instrumentInfoBuffer);
                instrumentInfoStr = new String(instrumentInfoBuffer, cacheFormat.getCharset());
            }

            _cacheFiles[i] = new CachedFile(filePath, instrumentInfoStr, cachedFileStruct.getFlags());
        }
    }

    private void parsePeaks() throws IOException
    {
        _channel.position(_cacheHeaderStruct.getLocationPeaks());
        ChromPeak[] chromPeaks = _cacheFormat.chromPeakSerializer()
                .readArray(Channels.newInputStream(_channel), _cacheHeaderStruct.getNumPeaks());
        _allPeaksRt = new float[chromPeaks.length];

        for (int i = 0; i < chromPeaks.length; i++)
        {
            _allPeaksRt[i] = chromPeaks[i].getRetentionTime();
        }
    }

    private void parseTransitions() throws IOException
    {
        _channel.position(_cacheHeaderStruct.getLocationTransitions());
        _transitions =_cacheFormat.chromTransitionSerializer()
                .readArray(Channels.newInputStream(_channel), _cacheHeaderStruct.getNumTransitions());
    }

    private void parseChromatograms() throws IOException
    {
        if (_cacheFormat.getFormatVersion().compareTo(CacheFormatVersion.Four)> 0)
        {
            _channel.position(_cacheHeaderStruct.getLocationTextIdBytes());
            _seqBytes = new byte[_cacheHeaderStruct.getNumTextIdBytes()];
            IOUtils.readFully(Channels.newInputStream(_channel), _seqBytes);
        }

        _channel.position(_cacheHeaderStruct.getLocationHeaders());

        _chromatograms =_cacheFormat.chromGroupHeaderInfoSerializer().readArray(
                Channels.newInputStream(_channel), _cacheHeaderStruct.getNumChromatograms());
    }

    public SeekableByteChannel getChannel()
    {
        return _channel;
    }

    final int getCacheFileSize()
    {
        return _cacheFiles != null ? _cacheFiles.length : 0;
    }

    public int matchTransitions(ChromGroupHeaderInfo header, List<? extends GeneralTransition> transitions, Double explicitRt, double tolerance, boolean multiMatch)
    {
        int match = 0;

        if (explicitRt != null)
        {
            // We have retention time info, use that in the match
            if (header.excludesTime(explicitRt))
                return match;
        }

        for (GeneralTransition transition : transitions)
        {
            int start = header.getStartTransitionIndex();
            int end = start + header.getNumTransitions();
            for (int i = start; i < end; i++)
            {
                // Do we need to look through all of the transitions from the .skyd file?
                if (header.toSignedMz(transition.getMz()).compareTolerant(_transitions[i].getProduct(header), tolerance) == 0)
                {
                    if (explicitRt == null)
                    {
                        match++;
                        if (!multiMatch)
                        {
                            break;  // only one match per transition
                        }
                    }
                    else
                    {
                        match = multiMatch ? match + 1 : 1; // Examine all RT values even if we're not multimatch
                    }
                }
            }
        }
        return match;
    }

    public byte[] readChromatogramBytes(ChromGroupHeaderInfo header) throws DataFormatException, IOException
    {
        // Get the compressed bytes
        ByteBuffer buffer = ByteBuffer.allocate(header.getCompressedSize());
        getChannel().position(header.getLocationPoints()).read(buffer);
        buffer.position(0);
        byte[] result = new byte[header.getCompressedSize()];
        buffer.get(result);
        // Make sure it uncompresses successfully so that we don't import bad content into the database
        uncompress(result, header.getUncompressedSize());
        return result;
    }

    public static byte[] uncompressStoredBytes(byte[] bytes, Integer uncompressedSize, int numPoints, int numTransitions) throws DataFormatException
    {
        if(uncompressedSize == null)
        {
            // For older data that got saved in the database without a value for uncompressedSize
            uncompressedSize = (Integer.SIZE / 8) * numPoints * (numTransitions + 1);
        }
        return uncompress(bytes, uncompressedSize);
    }

    public static byte[] uncompress(byte[] bytes, Integer uncompressedSize) throws DataFormatException
    {
        if (uncompressedSize != null && uncompressedSize == bytes.length) {
            return bytes;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        byte[] buffer = new byte[65536];
        int bytesRead;
        while (0 != (bytesRead = inflater.inflate(buffer)))
        {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public ChromTransition[] getTransitions(ChromGroupHeaderInfo chromGroupHeaderInfo)
    {
        ChromTransition[] transitions = new ChromTransition[chromGroupHeaderInfo.getNumTransitions()];
        for (int i = 0; i < transitions.length; i++)
        {
            transitions[i] = _transitions[i + chromGroupHeaderInfo.getStartTransitionIndex()];
        }
        return transitions;
    }

    public String getTextId(ChromGroupHeaderInfo chromGroupHeaderInfo) {
        if (0 == chromGroupHeaderInfo.getTextIdLen()) {
            return null;
        }
        return new String(_seqBytes, chromGroupHeaderInfo.getTextIdIndex(), chromGroupHeaderInfo.getTextIdLen(), _cacheFormat.getCharset());
    }

    public String getFilePath(ChromGroupHeaderInfo chromGroupHeaderInfo) {
        return _cacheFiles[chromGroupHeaderInfo.getFileIndex()].getFilePath();
    }
}
