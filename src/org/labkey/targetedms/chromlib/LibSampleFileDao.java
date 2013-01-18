package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.ColumnDef;
import org.labkey.targetedms.chromlib.Constants.SampleFileColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 8:30 PM
 */
public class LibSampleFileDao extends BaseDaoImpl<LibSampleFile>
{
    @Override
    protected void setValuesInStatement(LibSampleFile sampleFile, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, sampleFile.getFilePath());
        stmt.setString(colIndex++, sampleFile.getSampleName());
        if(sampleFile.getAcquiredTime() != null)
        {
            stmt.setString(colIndex++, Constants.DATE_FORMAT.format(sampleFile.getAcquiredTime()));
        }
        else
        {
            stmt.setNull(colIndex++, Types.VARCHAR);
        }
        if(sampleFile.getModifiedTime() != null)
        {
            stmt.setString(colIndex++, Constants.DATE_FORMAT.format(sampleFile.getModifiedTime()));
        }
        else
        {
            stmt.setNull(colIndex++, Types.VARCHAR);
        }
        stmt.setString(colIndex++, sampleFile.getInstrumentIonizationType());
        stmt.setString(colIndex++, sampleFile.getInstrumentAnalyzer());
        stmt.setString(colIndex, sampleFile.getInstrumentDetector());
    }

    @Override
    public String getTableName()
    {
        return Table.SampleFile.name();
    }

    @Override
    protected ColumnDef[] getColumns()
    {
        return SampleFileColumn.values();
    }

    @Override
    public List<LibSampleFile> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibSampleFile> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibSampleFile> sampleFiles = new ArrayList<LibSampleFile>();
        while(rs.next())
        {
            LibSampleFile sampleFile = new LibSampleFile();
            sampleFile.setId(rs.getInt(SampleFileColumn.Id.colName()));
            sampleFile.setFilePath(rs.getString(SampleFileColumn.FilePath.colName()));
            sampleFile.setSampleName(rs.getString(SampleFileColumn.SampleName.colName()));

            String acquiredTime = rs.getString(SampleFileColumn.AcquiredTime.colName());
            if(!rs.wasNull())
            {
                try
                {
                    sampleFile.setAcquiredTime(Constants.DATE_FORMAT.parse(acquiredTime));
                }
                catch (ParseException e)
                {
                    throw new RuntimeException("Error parsing AcquiredTime '"+acquiredTime+"' in "+getTableName(), e);
                }
            }

            String modifiedTime = rs.getString(SampleFileColumn.ModifiedTime.colName());
            if(!rs.wasNull())
            {
                try
                {
                    sampleFile.setModifiedTime(Constants.DATE_FORMAT.parse(modifiedTime));
                }
                catch (ParseException e)
                {
                    throw new RuntimeException("Error parsing ModifedTime '"+modifiedTime+"' in "+getTableName(), e);
                }
            }
            sampleFile.setInstrumentIonizationType(rs.getString(SampleFileColumn.InstrumentIonizationType.colName()));
            sampleFile.setInstrumentAnalyzer(rs.getString(SampleFileColumn.InstrumentAnalyzer.colName()));
            sampleFile.setInstrumentDetector(rs.getString(SampleFileColumn.InstrumentDetector.colName()));
            sampleFiles.add(sampleFile);
        }
        return sampleFiles;
    }

    @Override
    public void saveAll(List<LibSampleFile> sampleFiles, Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
