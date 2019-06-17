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

import org.labkey.targetedms.chromlib.Constants.PrecursorRetentionTimeColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibPrecursorRetentionTimeDao extends BaseDaoImpl<LibPrecursorRetentionTime>
{
    @Override
    protected void setValuesInStatement(LibPrecursorRetentionTime precursorRetentionTime, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, precursorRetentionTime.getPrecursorId());
        stmt.setInt(colIndex++, precursorRetentionTime.getSampleFileId());
        stmt.setObject(colIndex++, precursorRetentionTime.getRetentionTime(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursorRetentionTime.getStartTime(), Types.DOUBLE);
        stmt.setObject(colIndex, precursorRetentionTime.getEndTime(), Types.DOUBLE);
    }

    @Override
    public String getTableName()
    {
        return Table.PrecursorRetentionTime.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PrecursorRetentionTimeColumn.values();
    }

    protected List<LibPrecursorRetentionTime> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPrecursorRetentionTime> precursorRetentionTimes = new ArrayList<>();
        while(rs.next())
        {
            LibPrecursorRetentionTime precRetentionTime = new LibPrecursorRetentionTime();
            precRetentionTime.setId(rs.getInt(PrecursorRetentionTimeColumn.Id.baseColumn().name()));
            precRetentionTime.setPrecursorId(rs.getInt(PrecursorRetentionTimeColumn.PrecursorId.baseColumn().name()));
            precRetentionTime.setSampleFileId(rs.getInt(PrecursorRetentionTimeColumn.SampleFileId.baseColumn().name()));
            precRetentionTime.setRetentionTime(readDouble(rs, PrecursorRetentionTimeColumn.RetentionTime.baseColumn().name()));
            precRetentionTime.setStartTime(readDouble(rs, PrecursorRetentionTimeColumn.StartTime.baseColumn().name()));
            precRetentionTime.setEndTime(readDouble(rs, PrecursorRetentionTimeColumn.EndTime.baseColumn().name()));

            precursorRetentionTimes.add(precRetentionTime);
        }
        return precursorRetentionTimes;
    }
}
