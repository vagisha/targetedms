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

package org.labkey.targetedms.parser.speclib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.targetedms.BlibSourceFile;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.util.FileUtil;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.speclib.LibSpectrum.RedundantSpectrum;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * User: vsharma
 * Date: 5/6/12
 * Time: 11:36 AM
 */
@SuppressWarnings("SqlResolve")
public class BlibSpectrumReader extends LibSpectrumReader
{
    private static final Logger LOG = LogManager.getLogger(BlibSpectrumReader.class);

    @Override
    protected @Nullable BlibSpectrum readSpectrum(Connection conn, LibSpectrum.SpectrumKey spectrumKey, Path blibPath) throws DataFormatException, SQLException
    {
        try
        {
            BlibSpectrum spectrum = readBlibSpectrum(conn, spectrumKey.getModifiedPeptide(), spectrumKey.getCharge());
            if(spectrum == null)
                return null;
            // Make sure that the Bibliospec spectrum has peaks.  Minimized libraries in Skyline can have
            // library entries with no spectrum peaks.  This should be fixed in Skyline.
            if(spectrum.getNumPeaks() == 0)
            {
                return null;
            }

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
            if (malformedBlibFileError(FileUtil.getFileName(blibPath), e))
            {
                return null;
            }
            throw e;
        }
    }

    @Override
    protected @Nullable Path getRedundantLibPath(Container container, Path libPath)
    {
        return redundantBlibPath(libPath);
    }

    @Override
    protected @Nullable LibSpectrum readRedundantSpectrum(Connection conn, LibSpectrum.SpectrumKey spectrumKey) throws DataFormatException, SQLException
    {
        // Returns a BlibSpectrum from the redundant library (.redundant.blib)
        // redundantRefSpectrumId is the database id of the redundant spectrum match in .redundant.blib SQLite file
        BlibSpectrum spectrum = readRedundantSpectrum(conn, spectrumKey.getRedundantRefSpectrumId());
        if(spectrum == null)
            return null;
        readSpectrumPeaks(conn, spectrum);

        return spectrum;
    }

    @Override
    protected LibSpectrum.SpectrumKey getMatchingModSeqSpecKey(Connection conn, LibSpectrum.SpectrumKey key) throws SQLException
    {
        if(key.forRedundantSpectrum())
        {
            // When querying a redundant spectrum for Bibliospec we do not use the peptide modified sequence
            // since we will have the database id of the spectrum, so no need to do any additional work here.
            return key;
        }
        else
        {
            String blibPeptide = makePeptideBlibFormat(key.getModifiedPeptide());
            String matchingPeptide = findMatchingModifiedSequence(conn, blibPeptide, getMatchingModSeqLookupSql());
            return new LibSpectrum.SpectrumKey(matchingPeptide, key.getCharge(), key.getSourceFile(), key.getRedundantRefSpectrumId());
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
    @Override
    protected List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> readRetentionTimes(Connection conn, String modifiedPeptide, String blibFilePath) throws SQLException
    {
        try
        {
            if(!hasValidRtTable(conn))
            {
                return Collections.emptyList();
            }

            StringBuilder sql = new StringBuilder("SELECT rt.retentionTime, rt.bestSpectrum, rs.peptideModSeq, rs.precursorCharge, ssf.fileName from RetentionTimes AS rt ");
            sql.append(" INNER JOIN RefSpectra AS rs ON (rt.RefSpectraID = rs.id)");
            sql.append(" INNER JOIN SpectrumSourceFiles ssf ON (rt.SpectrumSourceID = ssf.id)");
            sql.append(" WHERE rs.peptideModSeq = ?");

            try(PreparedStatement stmt = conn.prepareStatement(sql.toString()))
            {
                stmt.setString(1, modifiedPeptide);
                List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery())
                {
                    while (rs.next())
                    {
                        LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(rs.getString("fileName"),
                                modifiedPeptide,
                                rs.getInt("precursorCharge"),
                                rs.getDouble("retentionTime"),
                                rs.getBoolean("bestSpectrum"));
                        retentionTimes.add(rtInfo);
                    }
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
            throw e;
        }
    }

    @Nullable
    private BlibSpectrum readBlibSpectrum(Connection conn, String modifiedPeptide, int charge) throws SQLException
    {
        boolean validRtTable = hasValidRtTable(conn);
        StringBuilder sql;
        if(validRtTable)
        {
            sql = new StringBuilder("SELECT rf.*, ssf.fileName");
            sql.append(", rt.retentionTime AS RT"); // Need an alias since RefSpectra table in non-minimized .blib files have a retentionTime column too.
            sql.append(", rt.SpectrumSourceId FROM RefSpectra AS rf ");
            sql.append("LEFT JOIN RetentionTimes AS rt ON (rt.RefSpectraId = rf.id AND rt.bestSpectrum = 1) ");
            sql.append("LEFT JOIN SpectrumSourceFiles AS ssf ON rt.SpectrumSourceId = ssf.id ");
            sql.append(" WHERE rf.peptideModSeq = ?");
            sql.append(" AND rf.precursorCharge = ?");
        }
        else
        {
            sql = new StringBuilder("SELECT * from RefSpectra WHERE peptideModSeq = ? AND precursorCharge = ?");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString()))
        {
            stmt.setString(1, modifiedPeptide);
            stmt.setInt(2, charge);
            try (ResultSet rs = stmt.executeQuery())
            {
                // Columns in RefSpectra table: id|peptideSeq|peptideModSeq|precursorCharge|precursorMZ|prevAA|nextAA|copies|numPeaks
                // Columns queried from RetentionTimes table (when present): retentionTime, SpectrumSourceId
                // Columns queried from SpectrumSourceFiles table (when present): fileName
                BlibSpectrum spectrum = null;
                if (rs.next())
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
    }

    @Override
    String getMatchingModSeqLookupSql()
    {
        return "SELECT peptideModSeq FROM RefSpectra WHERE peptideSeq = ?";
    }

    private static boolean hasTable(Connection conn, String tableName) throws SQLException
    {
        String tableCheckStmt = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";

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
        if(!hasTable(conn, "RetentionTimes"))
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

    private static void readSpectrumPeaks(Connection conn, BlibSpectrum spectrum) throws SQLException, DataFormatException
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

    private static void addRedundantSpectrumInfo(Connection conn, BlibSpectrum spectrum) throws SQLException
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

    // Return a map of blib file -> BlibSourceFiles
    public static Map<String, List<BlibSourceFile>> readBlibSourceFiles(ITargetedMSRun run)
    {
        Map<PeptideSettings.SpectrumLibrary, Path> libs = LibraryManager.getLibraryFilePaths(run.getId());

        Map<String, List<BlibSourceFile>> m = new TreeMap<>();
        for (Map.Entry<PeptideSettings.SpectrumLibrary, Path> entry : libs.entrySet())
        {
            if (!entry.getKey().getLibraryType().contains("bibliospec_lite"))
                continue;

            Path path = entry.getValue();
            String blibFilePath = getNonEmptyLocalLibPath(run.getContainer(), path);
            if (null == blibFilePath)
                continue;

            List<BlibSourceFile> blibSourceFiles = new ArrayList<>();
            try (Connection conn = getLibConnection(blibFilePath))
            {
                if (!hasTable(conn, "SpectrumSourceFiles"))
                {
                    continue;
                }
                Map<Integer, Set<String>> scoreTypes = new HashMap<>(); // file id -> score types
                if(hasTable(conn, "ScoreTypes")) // Older .blib files do not have a ScoreTypes table
                {
                    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT r.fileID, s.scoreType FROM RefSpectra as r JOIN ScoreTypes s ON r.scoreType = s.id"))
                    {
                        while (rs.next())
                        {
                            int fileId = rs.getInt(1);
                            String scoreType = rs.getString(2);
                            scoreTypes.putIfAbsent(fileId, new HashSet<>());
                            scoreTypes.get(fileId).add(scoreType);
                        }
                    }
                }
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM SpectrumSourceFiles"))
                {
                    int idColumn = -1;
                    int fileNameColumn = -1;
                    int idFileNameColumn = -1;
                    ResultSetMetaData metadata = rs.getMetaData();
                    for (int i = 1; i <= metadata.getColumnCount(); i++)
                    {
                        switch (metadata.getColumnName(i).toLowerCase())
                        {
                            case "id":
                                idColumn = i;
                                break;
                            case "filename":
                                fileNameColumn = i;
                                break;
                            case "idfilename":
                                idFileNameColumn = i;
                                break;
                        }
                    }
                    while (rs.next())
                    {
                        Integer id = rs.getInt(idColumn);
                        String fileName = rs.getString(fileNameColumn);
                        String idFileName = idFileNameColumn >= 0 ? rs.getString(idFileNameColumn) : null;
                        blibSourceFiles.add(new BlibSourceFile(fileName, idFileName, scoreTypes.getOrDefault(id, null)));
                    }
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
            m.put(path.getFileName().toString(), blibSourceFiles);
        }
        return m;
    }
}
