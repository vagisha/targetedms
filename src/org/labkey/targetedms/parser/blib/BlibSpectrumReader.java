/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
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

    private static final Logger LOG = Logger.getLogger(TargetedMSController.class);

    public static BlibSpectrum getSpectrum(String blibFilePath, String modifiedPeptide, int charge)
    {
        // CONSIDER: we are reading directly from Bibliospec SQLite file. Should we store library information in the schema?

        if(!(new File(blibFilePath)).exists())
            return null;

        Connection conn = null;

        try {
            conn = DriverManager.getConnection("jdbc:sqlite:/" + blibFilePath);

            BlibSpectrum spectrum = readSpectrum(conn, modifiedPeptide, charge);
            if(spectrum == null)
                return null;
            readSpectrumPeaks(conn, spectrum);

            return spectrum;

        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if(conn != null) try {conn.close();} catch(SQLException ignored){}
        }
    }

    public static List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> getRetentionTimes(String blibFilePath, String modifiedPeptide)
    {
        String blibPeptide = makePeptideBlibFormat(modifiedPeptide);

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        try {
            conn = DriverManager.getConnection("jdbc:sqlite:/" + blibFilePath, config.toProperties());
            stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder("SELECT rt.retentionTime, rs.peptideModSeq, rs.precursorCharge, ssf.fileName from RetentionTimes AS rt ");
            sql.append(" INNER JOIN RefSpectra AS rs ON (rt.RefSpectraID = rs.id)");
            sql.append(" INNER JOIN SpectrumSourceFiles ssf ON (rt.SpectrumSourceID = ssf.id)");
            sql.append(" WHERE rs.peptideModSeq='"+blibPeptide+"'");

            rs = stmt.executeQuery(sql.toString());

            List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
            while(rs.next())
            {
                LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(rs.getString("fileName"),
                        modifiedPeptide,
                        rs.getInt("precursorCharge"),
                        rs.getDouble("retentionTime"));
                retentionTimes.add(rtInfo);
            }
            return Collections.unmodifiableList(retentionTimes);
        }
        catch(SQLException e)
        {
            // Older versions of blib databases don't have a RetentionTimes table.
            if(e.getMessage().contains("no such table: RetentionTimes"))
            {
                LOG.info("Missing RetentionTimes table in blib file " + blibFilePath);
                return Collections.emptyList();
            }
            throw new RuntimeException(e);
        }
        finally
        {
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
            if(stmt != null) try {stmt.close();} catch(SQLException ignored){}
            if(conn != null) try {conn.close();} catch(SQLException ignored){}
        }
    }

    private static BlibSpectrum readSpectrum(Connection conn, String modifiedPeptide, int charge) throws SQLException
    {
        String blibPeptide = makePeptideBlibFormat(modifiedPeptide);

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * from RefSpectra WHERE peptideModSeq='"+blibPeptide+"' AND precursorCharge="+charge);

            // libLSID|createTime|numSpecs|majorVersion|minorVersion
            // id|peptideSeq|peptideModSeq|precursorCharge|precursorMZ|prevAA|nextAA|copies|numPeaks
            BlibSpectrum spectrum = null;
            if(rs.next())
            {
                spectrum = new BlibSpectrum();
                spectrum.setBlibId(rs.getInt(1));
                spectrum.setPeptideSeq(rs.getString(2));
                spectrum.setPeptideModSeq(modifiedPeptide);
                spectrum.setPrecursorCharge(rs.getInt(4));
                spectrum.setPrecursorMz(rs.getDouble(5));
                spectrum.setPrevAa(rs.getString(6));
                spectrum.setNextAa(rs.getString(7));
                spectrum.setCopies(rs.getInt(8));
                spectrum.setNumPeaks(rs.getInt(9));
            }
            return spectrum;
        }
        finally
        {
            if(stmt != null) try {stmt.close();} catch(SQLException ignored){}
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
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
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT * FROM RefSpectraPeaks WHERE RefSpectraId="+spectrum.getBlibId());
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
        finally
        {
            if(stmt != null) try {stmt.close();} catch(SQLException ignored){}
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
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
}
