/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
    public List<LibSampleFile> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibSampleFile> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibSampleFile> sampleFiles = new ArrayList<>();
        while(rs.next())
        {
            LibSampleFile sampleFile = new LibSampleFile();
            sampleFile.setId(rs.getInt(SampleFileColumn.Id.baseColumn().name()));
            sampleFile.setFilePath(rs.getString(SampleFileColumn.FilePath.baseColumn().name()));
            sampleFile.setSampleName(rs.getString(SampleFileColumn.SampleName.baseColumn().name()));

            String acquiredTime = rs.getString(SampleFileColumn.AcquiredTime.baseColumn().name());
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

            String modifiedTime = rs.getString(SampleFileColumn.ModifiedTime.baseColumn().name());
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
            sampleFile.setInstrumentIonizationType(rs.getString(SampleFileColumn.InstrumentIonizationType.baseColumn().name()));
            sampleFile.setInstrumentAnalyzer(rs.getString(SampleFileColumn.InstrumentAnalyzer.baseColumn().name()));
            sampleFile.setInstrumentDetector(rs.getString(SampleFileColumn.InstrumentDetector.baseColumn().name()));
            sampleFiles.add(sampleFile);
        }
        return sampleFiles;
    }

    @Override
    public void saveAll(List<LibSampleFile> sampleFiles, Connection connection)
    {
        throw new UnsupportedOperationException();
    }
}
