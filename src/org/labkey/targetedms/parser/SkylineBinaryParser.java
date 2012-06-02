package org.labkey.targetedms.parser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.zip.DataFormatException;

/**
 * Based on ChromatogramCache.cs and ChromHeaderInfo.cs from Skyline
 * 
 * User: jeckels
 * Date: Apr 13, 2012
 */
public class SkylineBinaryParser
{
    private final File _file;
    private final long[] _headers = new long[Header.values().length - 1]; 

    private Chromatogram[] _chromatograms;
    private float[] _transitions;

    public static final int FORMAT_VERSION_CACHE = 4;
    public static final int FORMAT_VERSION_CACHE_3 = 3;
    public static final int FORMAT_VERSION_CACHE_2 = 2;
    private CachedFile[] _cacheFiles;

    public SkylineBinaryParser(File file)
    {
        _file = file;
    }

    public Chromatogram getChromatogram(int index)
    {
        return _chromatograms[index];
    }

    public Chromatogram[] getChromatograms()
    {
        return _chromatograms;
    }

    /** File-level header fields */
    private enum Header
    {
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
        RandomAccessFile raf = null;
        try
        {
            raf = new RandomAccessFile(_file, "r");
            long fileSize = _file.length();

            int headerLength = Header.count.ordinal();
            FileChannel channel = raf.getChannel();

            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, fileSize - headerLength * Integer.SIZE / 8, headerLength * Integer.SIZE / 8); // 4 bytes per int
            buffer.order(ByteOrder.LITTLE_ENDIAN);


            for (int i = 0; i < _headers.length; i++)
            {
                _headers[i] = ((long)buffer.getInt() & 0xFFFFFFFFl);
            }
            int version = Header.format_version.getHeaderValueInt(_headers);

            if (version < FORMAT_VERSION_CACHE_2)
            {
                return;
            }

            parseFiles(version, channel);
            parseTransitions(channel);
            parseChromatograms(channel);
        }
        catch (DataFormatException e)
        {
            throw new IOException("Invalid ZIP content", e);
        }
        finally
        {
            if (raf != null) { try { raf.close(); } catch (IOException ignored) {} }
        }
    }

    private static int getFileHeaderCount(int formatVersion)
    {
        switch (formatVersion)
        {
            case FORMAT_VERSION_CACHE:
                return FileHeader.count.ordinal() * 4;
            case (FORMAT_VERSION_CACHE_3):
                return FileHeader.len_instrument_info.ordinal() * 4;
            default:
                return FileHeader.runstart_lo.ordinal() * 4;
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

        count
    }

    public static class CachedFile
    {
        private final String _filePath;
        private final String _instrumentInfo;

        public CachedFile(String filePath, String instrumentInfo)
        {
            _filePath = filePath;
            _instrumentInfo = instrumentInfo;
        }

        public String getFilePath()
        {
            return _filePath;
        }

        public String getInstrumentInfo()
        {
            return _instrumentInfo;
        }
    }

    private void parseFiles(int formatVersion, FileChannel channel) throws IOException
    {
        ByteBuffer buffer;
        int numFiles = Header.num_files.getHeaderValueInt(_headers);
        long filesStart = Header.location_files_lo.getHeaderValueLong(_headers);

        _cacheFiles = new CachedFile[numFiles];
        int countFileHeader = getFileHeaderCount(formatVersion);

        long offset = filesStart;

        for (int i = 0; i < numFiles; i++)
        {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, countFileHeader);
            offset+= countFileHeader;
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            long modifiedBinary = buffer.getLong();
            int lenPath = buffer.getInt();
            long runstartBinary = (isVersionCurrent() ? buffer.getLong() : 0);
            int lenInstrumentInfo = formatVersion > FORMAT_VERSION_CACHE_3 ? buffer.getInt() : -1;

            byte[] filePathBuffer = new byte[lenPath];
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, lenPath);
            offset += lenPath;
            buffer.get(filePathBuffer);
            String filePath = new String(filePathBuffer, 0, lenPath);

            String instrumentInfoStr = null;
            if (lenInstrumentInfo >= 0)
            {
                byte[] instrumentInfoBuffer = new byte[lenInstrumentInfo];
                buffer = channel.map(FileChannel.MapMode.READ_ONLY, offset, lenInstrumentInfo);
                offset += lenInstrumentInfo;
                buffer.get(instrumentInfoBuffer);
                instrumentInfoStr = new String(instrumentInfoBuffer, 0, lenInstrumentInfo, Charset.forName("UTF8"));
            }

//            DateTime modifiedTime = DateTime.FromBinary(modifiedBinary);
//            DateTime? runstartTime = runstartBinary != 0 ? DateTime.FromBinary(runstartBinary) : (DateTime?) null;
//            var instrumentInfoList = InstrumentInfoUtil.GetInstrumentInfo(instrumentInfoStr);
            _cacheFiles[i] = new CachedFile(filePath, instrumentInfoStr);
        }
    }

    public CachedFile[] getCacheFiles()
    {
        return _cacheFiles;
    }

    private void parseTransitions(FileChannel channel) throws IOException
    {
        int numTransitions = Header.num_transitions.getHeaderValueInt(_headers);
        long transitionsStart = Header.location_trans_lo.getHeaderValueLong(_headers);

        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, transitionsStart, Float.SIZE / 8 * numTransitions);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        _transitions = new float[numTransitions];
        for (int i = 0; i< numTransitions; i++)
        {
            _transitions[i] = buffer.getFloat();
        }
    }

    private void parseChromatograms(FileChannel channel) throws IOException, DataFormatException
    {
        int numChrom = Header.num_chromatograms.getHeaderValueInt(_headers);
        long chromatogramStart = Header.location_headers_lo.getHeaderValueLong(_headers);

        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, chromatogramStart, Chromatogram.SIZE * numChrom);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read the chromatogram headers out of the file
        _chromatograms = new Chromatogram[numChrom];
        for (int i = 0; i < _chromatograms.length; i++)
        {
            _chromatograms[i] = new Chromatogram(buffer);
        }

        // Then go and read the chromatograms themselves
        for (Chromatogram chromatogram : _chromatograms)
        {
            chromatogram.read(channel, _cacheFiles, _transitions);
        }
    }

    public boolean isVersionCurrent()
    {
        long version = Header.format_version.getHeaderValueInt(_headers);
        return (version == FORMAT_VERSION_CACHE || version == FORMAT_VERSION_CACHE_3);
    }
}
