package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.ColumnDef;
import org.labkey.targetedms.chromlib.Constants.IrtLibraryColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 3/19/14
 *
 * Helper class for exporting chromatogram library.
 *
 */
public class LibIrtLibraryDao extends  BaseDaoImpl<LibIrtLibrary>
{

    @Override
    public String getTableName()
    {
        return Table.IrtLibrary.name();
    }

    @Override
    protected List<LibIrtLibrary> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibIrtLibrary> irtLibraries = new ArrayList<>();

        while(rs.next())
        {
            LibIrtLibrary irtLibrary = new LibIrtLibrary();
            irtLibrary.setId(rs.getInt(IrtLibraryColumn.Id.baseColumn().name()));
            irtLibrary.setModifiedSequence(rs.getString(IrtLibraryColumn.IrtStandard.baseColumn().name()));
            irtLibrary.setIrtStandard(rs.getBoolean(IrtLibraryColumn.IrtStandard.baseColumn().name()));
            irtLibrary.setIrtValue(rs.getDouble(IrtLibraryColumn.IrtValue.baseColumn().name()));

            irtLibraries.add(irtLibrary);
        }

        return irtLibraries;

    }

    @Override
    protected void setValuesInStatement(LibIrtLibrary irtLibrary, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setObject(colIndex++, irtLibrary.getModifiedSequence(), Types.VARCHAR);
        stmt.setObject(colIndex++, irtLibrary.getIrtStandard(), Types.TINYINT);
        stmt.setObject(colIndex, irtLibrary.getIrtValue(), Types.DOUBLE);
    }

    @Override
    protected ColumnDef[] getColumns()
    {
        return IrtLibraryColumn.values();
    }
}
