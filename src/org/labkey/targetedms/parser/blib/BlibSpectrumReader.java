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

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

        ByteBuffer bbuf = ByteBuffer.wrap(uncompressed);
        bbuf = bbuf.order(ByteOrder.LITTLE_ENDIAN);
        float[] intensities = new float[peakCount];
        for(int i = 0; i < intensities.length; i++)
        {
            intensities[i] = bbuf.getFloat(i * sizeOfInten);
        }
        return intensities;
    }
}
