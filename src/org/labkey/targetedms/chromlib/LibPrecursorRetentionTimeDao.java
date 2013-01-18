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
        List<LibPrecursorRetentionTime> precursorRetentionTimes = new ArrayList<LibPrecursorRetentionTime>();
        while(rs.next())
        {
            LibPrecursorRetentionTime precRetentionTime = new LibPrecursorRetentionTime();
            precRetentionTime.setId(rs.getInt(PrecursorRetentionTimeColumn.Id.colName()));
            precRetentionTime.setPrecursorId(rs.getInt(PrecursorRetentionTimeColumn.PrecursorId.colName()));
            precRetentionTime.setSampleFileId(rs.getInt(PrecursorRetentionTimeColumn.SampleFileId.colName()));
            precRetentionTime.setRetentionTime(readDouble(rs, PrecursorRetentionTimeColumn.RetentionTime.colName()));
            precRetentionTime.setStartTime(readDouble(rs, PrecursorRetentionTimeColumn.StartTime.colName()));
            precRetentionTime.setEndTime(readDouble(rs, PrecursorRetentionTimeColumn.EndTime.colName()));

            precursorRetentionTimes.add(precRetentionTime);
        }
        return precursorRetentionTimes;
    }
}
