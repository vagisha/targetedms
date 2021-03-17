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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.targetedms.parser.speclib.LibSpectrum.RedundantSpectrum;
import org.labkey.targetedms.parser.speclib.LibSpectrum.SpectrumKey;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

// EncyclopeDIA file format documentation: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
@SuppressWarnings("SqlResolve")
public class ElibSpectrumReader extends LibSpectrumReader
{
    @Override
    protected @Nullable ElibSpectrum readSpectrum(Connection conn, SpectrumKey spectrumKey, Path libPath) throws DataFormatException, SQLException
    {
        return readElibSpectrum(conn, spectrumKey, true);
    }

    @Override
    protected @Nullable Path getRedundantLibPath(Container container, Path libPath)
    {
        return libPath; // EncyclopeDIA does not have a separate redundant spectra file
    }

    @Override
    public @Nullable ElibSpectrum readRedundantSpectrum(Connection conn, SpectrumKey spectrumKey) throws SQLException, DataFormatException
    {
        return readElibSpectrum(conn, spectrumKey, false);
    }

    @Override
    protected String getMatchingModSeqLookupSql()
    {
        return "SELECT PeptideModSeq FROM entries WHERE PeptideSeq = ?";
    }

    private ElibSpectrum readElibSpectrum(Connection conn, SpectrumKey spectrumKey, boolean getRedundant) throws SQLException, DataFormatException
    {
        StringBuilder sql = new StringBuilder("SELECT PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score FROM entries")
                         .append(" WHERE PeptideModSeq = ?").append(" AND PrecursorCharge = ?");
            if(spectrumKey.hasSourceFile())
            {
                sql.append(" AND SourceFile = ?");
            }

        List<ElibSpectrum> spectra = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString()))
        {
            stmt.setString(1, spectrumKey.getModifiedPeptide());
            stmt.setInt(2, spectrumKey.getCharge());
            if(spectrumKey.hasSourceFile())
            {
                stmt.setString(3, spectrumKey.getSourceFile());
            }
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    ElibSpectrum spectrum = new ElibSpectrum();
                    spectrum.setPeptideModSeq(spectrumKey.getModifiedPeptide());
                    spectrum.setPrecursorCharge(rs.getInt("PrecursorCharge"));
                    spectrum.setPrecursorMz(rs.getDouble("PrecursorMz"));
                    double rt = rs.getDouble("RTInSeconds");
                    spectrum.setRetentionTime(rt / 60.0);
                    spectrum.setSourceFile(rs.getString("SourceFile"));
                    spectrum.setScore(rs.getDouble("Score"));
                    spectra.add(spectrum);
                }
            }
        }

        if(spectra.size() > 0)
        {
            sortElibSpectra(spectra);
            ElibSpectrum bestSpectrum = spectra.get(0);
            readPeaks(conn, bestSpectrum);

            if(getRedundant)
            {
                AtomicInteger id = new AtomicInteger(1);
                List<RedundantSpectrum> redundantSpectra = spectra.stream()
                        .map(s -> {
                            RedundantSpectrum rSpec = new RedundantSpectrum();
                            rSpec.setBestSpectrum(id.get() == 1);
                            rSpec.setRetentionTime(s.getRetentionTime());
                            rSpec.setSourceFile(s.getSourceFile());
                            rSpec.setRedundantRefSpectrumId(id.getAndIncrement());
                            return rSpec;
                        })
                        .collect(Collectors.toList());
                bestSpectrum.setRedundantSpectrumList(redundantSpectra);
            }

            return bestSpectrum;
        }
        return null;
    }

    private void readPeaks(Connection conn, ElibSpectrum spectrum) throws SQLException, DataFormatException
    {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT MassEncodedLength, MassArray, IntensityEncodedLength, IntensityArray FROM entries " +
                "WHERE PrecursorCharge = ? AND PeptideModSeq = ? AND SourceFile = ?"))
        {
            stmt.setInt(1, spectrum.getPrecursorCharge());
            stmt.setString(2, spectrum.getPeptideModSeq());
            stmt.setString(3, spectrum.getSourceFile());
            try(ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] mzArray = rs.getBytes("MassArray");
                    byte[] intensityArray = rs.getBytes("IntensityArray");

                    double[] peakMzs = extractMassArray(mzArray, rs.getInt("MassEncodedLength"));
                    float[] peakIntensities = extractIntensityArray(intensityArray, rs.getInt("IntensityEncodedLength"));

                    spectrum.setMzAndIntensity(peakMzs, peakIntensities);
                }
            }
        }
    }

    private static double[] extractMassArray(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        // Based on the code provided on the EncyclopeDIA documentation page: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
        byte[] uncompressedData = uncompress(compressedData, uncompressedLength);
        double[] mzArray = new double[uncompressedData.length / 8];
        ByteBuffer bb = ByteBuffer.wrap(uncompressedData);
        bb.order(ByteOrder.BIG_ENDIAN);
        DoubleBuffer buffer = bb.asDoubleBuffer();
        buffer.get(mzArray);
        return mzArray;
    }

    private static float[] extractIntensityArray(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        // Based on the code provided on the EncyclopeDIA documentation page: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
        byte[] uncompressedData = uncompress(compressedData, uncompressedLength);
        float[] intensities = new float[uncompressedData.length / 4];
        ByteBuffer bb = ByteBuffer.wrap(uncompressedData);
        bb.order(ByteOrder.BIG_ENDIAN);
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.get(intensities);
        return intensities;
    }

    private static byte[] uncompress(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        byte[] uncompressed = new byte[uncompressedLength];

        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        inflater.inflate(uncompressed);
        inflater.end();
        return uncompressed;
    }

    private void sortElibSpectra(List<ElibSpectrum> spectra)
    {
        spectra.sort(Comparator.comparing(ElibSpectrum::getPeptideModSeq)
                .thenComparing(ElibSpectrum::getPrecursorCharge)
                .thenComparing(ElibSpectrum::getScore)); // Assuming this is Qvalue; "Score" is not a nullable column;
                                                         // ascending sort will give us the best scoring spectrum
                                                         // for a modified sequence + charge at the top
    }

    @Override
    protected @NotNull List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> readRetentionTimes(Connection conn, String modifiedPeptide, String libPath) throws SQLException
    {
        List<ElibSpectrum> spectra = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement("SELECT PeptideModSeq, PrecursorCharge, RTInSeconds, SourceFile, Score FROM entries WHERE PeptideModSeq = ?"))
        {
            stmt.setString(1, modifiedPeptide);
            try(ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    ElibSpectrum spectrum = new ElibSpectrum();
                    spectrum.setPeptideModSeq(rs.getString("PeptideModSeq"));
                    spectrum.setPrecursorCharge(rs.getInt("PrecursorCharge"));
                    spectrum.setSourceFile(rs.getString("SourceFile"));
                    double rt = rs.getDouble("RTInSeconds");
                    spectrum.setRetentionTime(rt / 60.0);
                    spectrum.setScore(rs.getDouble("Score"));
                    spectra.add(spectrum);
                }
            }
        }

        // Sort the spectra by charge and then by spectrum score (best to worst)
        sortElibSpectra(spectra);
        List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
        int lastCharge = Integer.MAX_VALUE;
        for(var spectrum: spectra)
        {
            LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(spectrum.getSourceFileName(), spectrum.getPeptideModSeq(),
                    spectrum.getPrecursorCharge(), spectrum.getRetentionTime(),
                    spectrum.getPrecursorCharge() != lastCharge // First spectrum for a charge will be the best spectrum for the modified sequence + charge combo
            );
            retentionTimes.add(rtInfo);
            lastCharge = spectrum.getPrecursorCharge();
        }

        return Collections.unmodifiableList(retentionTimes);
    }
}
