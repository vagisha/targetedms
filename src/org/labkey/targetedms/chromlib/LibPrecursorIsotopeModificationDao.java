package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.PrecursorIsotopeModificationColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibPrecursorIsotopeModificationDao extends BaseDaoImpl<LibPrecursorIsotopeModification>
{
    @Override
    protected void setValuesInStatement(LibPrecursorIsotopeModification precIsotopeMod, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, precIsotopeMod.getPrecursorId());
        stmt.setInt(colIndex++, precIsotopeMod.getIsotopeModificationId());
        stmt.setInt(colIndex++, precIsotopeMod.getIndexAa());
        stmt.setDouble(colIndex, precIsotopeMod.getMassDiff());
    }

    @Override
    public String getTableName()
    {
        return Table.PrecursorIsotopeModification.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PrecursorIsotopeModificationColumn.values();
    }

    protected List<LibPrecursorIsotopeModification> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPrecursorIsotopeModification> precIsotopeModifications = new ArrayList<LibPrecursorIsotopeModification>();
        while(rs.next())
        {
            LibPrecursorIsotopeModification precIsotopeMod = new LibPrecursorIsotopeModification();
            precIsotopeMod.setId(rs.getInt(PrecursorIsotopeModificationColumn.Id.colName()));
            precIsotopeMod.setPrecursorId(rs.getInt(PrecursorIsotopeModificationColumn.PrecursorId.colName()));
            precIsotopeMod.setIsotopeModificationId(rs.getInt(PrecursorIsotopeModificationColumn.IsotopeModId.colName()));
            precIsotopeMod.setIndexAa(rs.getInt(PrecursorIsotopeModificationColumn.IndexAa.colName()));
            precIsotopeMod.setMassDiff(readDouble(rs, PrecursorIsotopeModificationColumn.MassDiff.colName()));

            precIsotopeModifications.add(precIsotopeMod);
        }
        return precIsotopeModifications;
    }
}
