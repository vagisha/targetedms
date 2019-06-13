/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

package org.labkey.targetedms.parser.blib;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingStringKeyCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.blib.BlibSpectrum.RedundantSpectrum;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * User: vsharma
 * Date: 5/6/12
 * Time: 11:36 AM
 */
public class BlibSpectrumReader
{
    private BlibSpectrumReader() {}

    static
    {
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find SQLite driver", e);
        }
    }

    private static final Logger LOG = Logger.getLogger(BlibSpectrumReader.class);

    @Nullable
    public static BlibSpectrum getSpectrum(Container container, Path blibPath,
                                           String modifiedPeptide, int charge)
    {
        String blibFilePath = getLocalBlibPath(container, blibPath);
        if (null == blibFilePath)
            return null;

        // CONSIDER: we are reading directly from Bibliospec SQLite file. Should we store library information in the schema?
        // We know it's local file and string may need encoding to convert to Path
        if(!(new File(blibFilePath).exists()))
            return null;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:/" + blibFilePath))
        {
            BlibSpectrum spectrum = readSpectrum(conn, modifiedPeptide, charge);
            if(spectrum == null)
                return null;
            readSpectrumPeaks(conn, spectrum);

            if(spectrum.getRetentionTime() != null // retentionTime will be null if RetentionTimes table does not exist.
                    && redundantBlibExists(blibPath))
            {
                // Get the redundant spectra IDs
                addRedundantSpectrumInfo(conn, spectrum);
            }

            return spectrum;

        }
        catch(SQLException e)
        {
            // Malformed blib file?
            if (malformedBlibFileError(blibFilePath, e))
            {
                return null;
            }
            throw new RuntimeSQLException(e);
        }
    }

    private static boolean malformedBlibFileError(String blibFilePath, SQLException e)
    {
        if(e.getMessage().contains("no such table") || e.getMessage().contains("no such column") || e.getMessage().contains("File opened that is not a database file"))
        {
            LOG.error("Malformed .blib file " + blibFilePath + ". Error was: " + e.getMessage());
            return true;
        }
        return false;
    }

    private static boolean redundantBlibExists(Path blibPath)
    {
        Path redundantBlibFilePath = redundantBlibPath(blibPath);
        return redundantBlibFilePath != null && Files.exists(redundantBlibFilePath);
    }

    public static Path redundantBlibPath(@Nullable Path blibPath)
    {
        if (null != blibPath)
        {
            String filename = blibPath.getFileName().toString();
            int idx = filename.indexOf(".blib");
            if (idx != -1)
            {
                Path parent = blibPath.getParent();
                return parent.resolve(filename.substring(0, idx) + ".redundant.blib");
            }
        }
        return null;
    }

    @NotNull
    public static List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> getRetentionTimes(Container container, Path blibPath, String modifiedPeptide)
    {
        String blibFilePath = getLocalBlibPath(container, blibPath);
        if (null == blibFilePath)
            return Collections.emptyList();

        // We know it's local file and string may need encoding to convert to Path
        if(!(new File(blibFilePath).exists()))
        {
            LOG.error("File not found: " + blibFilePath);
            return Collections.emptyList();
        }

        String blibPeptide = makePeptideBlibFormat(modifiedPeptide);

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:/" + blibFilePath, config.toProperties()))
        {
            if(!hasValidRtTable(conn))
            {
                return Collections.emptyList();
            }

            blibPeptide = findMatchingModifiedSequence(conn, blibPeptide);

            StringBuilder sql = new StringBuilder("SELECT rt.retentionTime, rt.bestSpectrum, rs.peptideModSeq, rs.precursorCharge, ssf.fileName from RetentionTimes AS rt ");
            sql.append(" INNER JOIN RefSpectra AS rs ON (rt.RefSpectraID = rs.id)");
            sql.append(" INNER JOIN SpectrumSourceFiles ssf ON (rt.SpectrumSourceID = ssf.id)");
            sql.append(" WHERE rs.peptideModSeq='"+blibPeptide+"'");

            try(Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql.toString()))
            {
                List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
                while (rs.next())
                {
                    LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(rs.getString("fileName"),
                            modifiedPeptide,
                            rs.getInt("precursorCharge"),
                            rs.getDouble("retentionTime"),
                            rs.getBoolean("bestSpectrum"));
                    retentionTimes.add(rtInfo);
                }
                return Collections.unmodifiableList(retentionTimes);
            }
        }
        catch(SQLException e)
        {
            if (malformedBlibFileError(blibFilePath, e))
            {
                return Collections.emptyList();
            }
            throw new RuntimeSQLException(e);
        }
    }

    @Nullable
    private static BlibSpectrum readSpectrum(Connection conn, String modifiedPeptide, int charge) throws SQLException
    {
        String blibPeptide = makePeptideBlibFormat(modifiedPeptide);
        blibPeptide = findMatchingModifiedSequence(conn, blibPeptide);
        boolean validRtTable = hasValidRtTable(conn);
        StringBuilder sql;
        if(validRtTable)
        {
            sql = new StringBuilder("SELECT rf.*, ssf.fileName");
            sql.append(", rt.retentionTime AS RT"); // Need an alias since RefSpectra table in non-minimized .blib files have a retentionTime column too.
            sql.append(", rt.SpectrumSourceId FROM RefSpectra AS rf ");
            sql.append("LEFT JOIN RetentionTimes AS rt ON (rt.RefSpectraId = rf.id AND rt.bestSpectrum = 1) ");
            sql.append("LEFT JOIN SpectrumSourceFiles AS ssf ON rt.SpectrumSourceId = ssf.id ");
            sql.append(" WHERE rf.peptideModSeq='").append(blibPeptide).append("'");
            sql.append(" AND rf.precursorCharge=").append(charge);
        }
        else
        {
            sql = new StringBuilder("SELECT * from RefSpectra WHERE peptideModSeq='").append(blibPeptide).append("' AND precursorCharge=").append(charge);
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            // Columns in RefSpectra table: id|peptideSeq|peptideModSeq|precursorCharge|precursorMZ|prevAA|nextAA|copies|numPeaks
            // Columns queried from RetentionTimes table (when present): retentionTime, SpectrumSourceId
            // Columns queried from SpectrumSourceFiles table (when present): fileName
            BlibSpectrum spectrum = null;
            if(rs.next())
            {
                spectrum = new BlibSpectrum();
                spectrum.setBlibId(rs.getInt("id"));
                spectrum.setPeptideSeq(rs.getString("peptideSeq"));
                spectrum.setPeptideModSeq(modifiedPeptide);
                spectrum.setPrecursorCharge(rs.getInt("precursorCharge"));
                spectrum.setPrecursorMz(rs.getDouble("precursorMZ"));
                spectrum.setPrevAa(rs.getString("prevAA"));
                spectrum.setNextAa(rs.getString("nextAA"));
                spectrum.setCopies(rs.getInt("copies"));
                spectrum.setNumPeaks(rs.getInt("numPeaks"));
                if (validRtTable)
                {
                    spectrum.setRetentionTime(rs.getDouble("RT"));
                    spectrum.setFileId(rs.getInt("SpectrumSourceId"));
                    spectrum.setSourceFile(rs.getString("fileName"));
                }
            }
            return spectrum;
        }
    }

    private static String findMatchingModifiedSequence(Connection conn, String modifiedSequence) throws SQLException
    {
        List<Pair<Integer, String>> mods = new ArrayList<>();
        String unmodifiedSequence = Peptide.stripModifications(modifiedSequence, mods);
        if (mods.size() == 0) {
            return modifiedSequence;
        }
        String sql = "SELECT peptideModSeq FROM RefSpectra WHERE peptideSeq = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, unmodifiedSequence);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                {
                    String modSeqCompare = rs.getString(1);
                    if (Peptide.modifiedSequencesMatch(modifiedSequence, modSeqCompare)) {
                        return modSeqCompare;
                    }
                }
            }
        }
        return modifiedSequence;
    }

    private static boolean hasRetentionTimesTable(Connection conn) throws SQLException
    {
        String tableCheckStmt = "SELECT name FROM sqlite_master WHERE type='table' AND name='RetentionTimes'";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(tableCheckStmt))
        {
            if (rs.next())
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValidRtTable(Connection conn) throws SQLException
    {
        if(!hasRetentionTimesTable(conn))
        {
            return false;
        }

        // Check if the RetentionTimes table has all the required columns. Some older versions of .blib
        // files have a RetentionTimes table but not the other columns needed to join with the SpectrumSourceFiles table.
        String tableCheckStmt = "PRAGMA table_info('RetentionTimes')"; // returns one row per column in the table, if it exists

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(tableCheckStmt))
        {
            int foundColumns = 0;
            while (rs.next())
            {
                String name = rs.getString("name");
                if (name.toLowerCase().equals("retentiontime"))
                {
                    foundColumns++;
                }
                else if (name.toLowerCase().equals("spectrumsourceid"))
                {
                    foundColumns++;
                }
                else if (name.toLowerCase().equals("refspectraid"))
                {
                    foundColumns++;
                }
                else if (name.toLowerCase().equals("bestspectrum"))
                {
                    foundColumns++;
                }
            }

            return foundColumns == 4;
        }
    }

    private static String makePeptideBlibFormat(String modifiedPeptide)
    {
        // Modified peptide in Bibliospec always has a decimal point in the modification mass.
        // Example: SSQPLASK[+42.0]QEK
        // Skyline represents this as SSQPLASK[+42]QEK

        return modifiedPeptide.replaceAll("\\[([+|-])(\\d+)\\]", "\\[$1$2\\.0\\]");
    }

    private static void readSpectrumPeaks(Connection conn, BlibSpectrum spectrum) throws SQLException
    {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM RefSpectraPeaks WHERE RefSpectraId="+spectrum.getBlibId()))
        {
            if(rs.next())
            {
                byte[] mzsCompressed = rs.getBytes(2);
                byte[] intensCompressed = rs.getBytes(3);

                double[] peakMzs = uncompressPeakMz(mzsCompressed, spectrum.getNumPeaks());
                float[] peakIntensities = uncompressPeakIntensities(intensCompressed, spectrum.getNumPeaks());

                spectrum.setMzAndIntensity(peakMzs, peakIntensities);
            }
        }
        catch (DataFormatException e)
        {
            throw new IllegalStateException("Error uncompressing peaks for spectrum");
        }
    }

    private static double[] uncompressPeakMz(byte[] compressed, int peakCount) throws DataFormatException
    {
        int sizeOfMz = Double.SIZE / 8;

        int uncompressedLength = peakCount * sizeOfMz;
        byte[] uncompressed;
        uncompressed = getBytes(compressed, uncompressedLength);

        ByteBuffer bbuf = ByteBuffer.wrap(uncompressed);
        bbuf = bbuf.order(ByteOrder.LITTLE_ENDIAN);
        double[] mzs = new double[peakCount];
        for(int i = 0; i < mzs.length; i++)
        {
            mzs[i] = bbuf.getDouble(i * sizeOfMz);
        }
        return mzs;
    }

    private static float[] uncompressPeakIntensities(byte[] compressed, int peakCount) throws DataFormatException
    {
        int sizeOfInten = Float.SIZE / 8;

        int uncompressedLength = peakCount * sizeOfInten;
        byte[] uncompressed;

        uncompressed = getBytes(compressed, uncompressedLength);

        ByteBuffer bbuf = ByteBuffer.wrap(uncompressed);
        bbuf = bbuf.order(ByteOrder.LITTLE_ENDIAN);
        float[] intensities = new float[peakCount];
        for(int i = 0; i < intensities.length; i++)
        {
            intensities[i] = bbuf.getFloat(i * sizeOfInten);
        }
        return intensities;
    }

    private static byte[] getBytes(byte[] compressed, int uncompressedLength) throws DataFormatException
    {
        byte[] uncompressed;
        if(uncompressedLength == compressed.length)
        {
            uncompressed = compressed;
        }
        else
        {
            uncompressed = new byte[uncompressedLength];

            Inflater inflater = new Inflater();
            inflater.setInput(compressed, 0, compressed.length);
            inflater.inflate(uncompressed);
            inflater.end();
        }
        return uncompressed;
    }

    private static void addRedundantSpectrumInfo(Connection conn, BlibSpectrum spectrum)
    {
        StringBuilder sql = new StringBuilder("SELECT rt.*, sf.fileName ");
        sql.append("FROM RetentionTimes AS rt INNER JOIN SpectrumSourceFiles AS sf ON rt.spectrumSourceID = sf.id ");
        sql.append("WHERE RefSpectraID=").append(spectrum.getBlibId());

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            List<RedundantSpectrum> redundantSpectra = new ArrayList<>();

            while (rs.next())
            {
                RedundantSpectrum rSpec = new RedundantSpectrum();
                rSpec.setRedundantRefSpectrumId(rs.getInt("RedundantRefSpectraID"));
                rSpec.setRetentionTime(rs.getDouble("retentionTime"));
                rSpec.setSourceFile(rs.getString("fileName"));
                rSpec.setBestSpectrum(rs.getBoolean("bestSpectrum"));

                if (rSpec.isBestSpectrum())
                {
                    // If this is the reference spectrum keep it in front of the list.
                    redundantSpectra.add(0, rSpec);
                }
                else
                {
                    redundantSpectra.add(rSpec);
                }
            }

            spectrum.setRedundantSpectrumList(redundantSpectra);
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static BlibSpectrum getRedundantSpectrum(Container container, Path redundantBlibPath, int redundantRefSpectrumId)
    {
        String redundantBlibFilePath = getLocalBlibPath(container, redundantBlibPath);

        if (null == redundantBlibFilePath)
            return null;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:/" + redundantBlibFilePath))
        {
            BlibSpectrum spectrum = readRedundantSpectrum(conn, redundantRefSpectrumId);
            if(spectrum == null)
                return null;
            readSpectrumPeaks(conn, spectrum);

            return spectrum;
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static BlibSpectrum readRedundantSpectrum(Connection conn, int redundantRefSpectrumid) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT rf.*, ssf.fileName FROM RefSpectra AS rf ");
        sql.append("LEFT JOIN SpectrumSourceFiles AS ssf ON rf.FileID = ssf.id ");
        sql.append(" WHERE rf.id=").append(redundantRefSpectrumid);

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            BlibSpectrum spectrum = null;

            if (rs.next())
            {
                spectrum = new BlibSpectrum();
                spectrum.setBlibId(rs.getInt("id"));
                spectrum.setPeptideSeq(rs.getString("peptideSeq"));
                spectrum.setPeptideModSeq("peptideModSeq");
                spectrum.setPrecursorCharge(rs.getInt("precursorCharge"));
                spectrum.setPrecursorMz(rs.getDouble("precursorMZ"));
                spectrum.setPrevAa(rs.getString("prevAA"));
                spectrum.setNextAa(rs.getString("nextAA"));
                spectrum.setCopies(rs.getInt("copies"));
                spectrum.setNumPeaks(rs.getInt("numPeaks"));
                spectrum.setRetentionTime(rs.getDouble("retentionTime"));
                spectrum.setFileId(rs.getInt("fileID"));
                spectrum.setSourceFile(rs.getString("fileName"));
            }

            return spectrum;
        }
    }


    private static final int BLIBCACHE_LIMIT = 1000;
    private static final long BLIBCACHE_LIFETIME = CacheManager.DAY;

    private static String getBlibCacheKey(@NotNull Container container, String pathStr)
    {
        return container.getId() + "::" + pathStr;
    }

    private static final CacheLoader<String, String> _blibCacheLoader = (key, arg) ->
    {
        if (null != arg)
        {
            Pair<Container, Path> pair = (Pair<Container, Path>) arg;
            Container container = pair.first;
            Path remotePath = pair.second;
            if (null != container && null != remotePath)
            {
                File file = LocalDirectory.copyToContainerDirectory(container, remotePath, LOG);
                if (null != file)
                    return file.getAbsolutePath();
            }
        }
        return null;
    };

    private static final BlockingStringKeyCache<String> _blibCache =
        new BlockingStringKeyCache<String>(CacheManager.getStringKeyCache(BLIBCACHE_LIMIT, BLIBCACHE_LIFETIME, "BlibCache"), _blibCacheLoader)
        {
            @Override
            public void remove(String key)
            {
                deleteTempFile(key);
                super.remove(key);
            }

            @Override
            public void clear()
            {
                getKeys().forEach(this::deleteTempFile);
                super.clear();
            }

            private void deleteTempFile(String key)
            {
                try
                {
                    String filePathStr = get(key);
                    if (null != filePathStr)
                        Files.deleteIfExists(new File(filePathStr).toPath());
                }
                catch (IOException e)
                {
                    LOG.error("Temp Blib file not removed", e);
                }
            }
        };

    @Nullable
    private static String getLocalBlibPath(Container container, Path blibPath)
    {
        // If blib is in cloud, copy it locally to read
        if (FileUtil.hasCloudScheme(blibPath))
        {
            String localFilePathStr = _blibCache.get(getBlibCacheKey(container, FileUtil.getAbsolutePath(blibPath)), new Pair<>(container, blibPath));
            if (null != localFilePathStr)
            {
                return localFilePathStr;
            }
            else
            {
                LOG.warn("Unable to copy " + blibPath + " to local file.");
                return null;
            }
        }
        return FileUtil.getAbsolutePath(blibPath);
    }

    public static void clearBlibCache(Container container)
    {
        _blibCache.clear();     // TODO: we could clear only keys from this container
    }
}
