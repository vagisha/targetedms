package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.StructuralModLossColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
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
public class LibStructuralModLossDao extends BaseDaoImpl<LibStructuralModLoss>
{
    @Override
    protected void setValuesInStatement(LibStructuralModLoss modLoss, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, modLoss.getStructuralModId());
        stmt.setString(colIndex++, modLoss.getFormula());
        stmt.setObject(colIndex++, modLoss.getMassDiffMono(), Types.DOUBLE);
        stmt.setObject(colIndex, modLoss.getMassDiffAvg(), Types.DOUBLE);
    }

    @Override
    public String getTableName()
    {
        return Table.StructuralModLoss.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return StructuralModLossColumn.values();
    }

    protected List<LibStructuralModLoss> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibStructuralModLoss> modLosses = new ArrayList<LibStructuralModLoss>();
        while(rs.next())
        {
            LibStructuralModLoss modLoss = new LibStructuralModLoss();
            modLoss.setId(rs.getInt(StructuralModLossColumn.Id.colName()));
            modLoss.setStructuralModId(rs.getInt(StructuralModLossColumn.StructuralModId.colName()));
            modLoss.setFormula(rs.getString(StructuralModLossColumn.Formula.colName()));
            double massDiffMono = rs.getDouble(StructuralModLossColumn.MassDiffMono.colName());
            if(!rs.wasNull())
                modLoss.setMassDiffMono(massDiffMono);
            double massDiffAvg = rs.getDouble(StructuralModLossColumn.MassDiffAvg.colName());
            if(!rs.wasNull())
                modLoss.setMassDiffAvg(massDiffAvg);
            modLosses.add(modLoss);
        }
        return modLosses;
    }

    @Override
    public void saveAll(List<LibStructuralModLoss> structuralModLosses, Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
