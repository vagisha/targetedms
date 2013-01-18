package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.chromlib.Constants.Table;
import org.labkey.targetedms.chromlib.Constants.TransitionColumn;

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
public class LibTransitionDao extends BaseDaoImpl<LibTransition>
{
   @Override
    protected void setValuesInStatement(LibTransition transition, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, transition.getPrecursorId());
        stmt.setObject(colIndex++, transition.getMz(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getCharge(), Types.INTEGER);
        stmt.setObject(colIndex++, transition.getNeutralMass(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getNeutralLossMass(), Types.DOUBLE);
        stmt.setString(colIndex++, transition.getFragmentType());
        stmt.setObject(colIndex++, transition.getFragmentOrdinal(), Types.INTEGER);
        stmt.setObject(colIndex++, transition.getMassIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, transition.getArea(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getHeight(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getFwhm(), Types.DOUBLE);
        stmt.setObject(colIndex, transition.getChromatogramIndex(), Types.INTEGER);
    }

    @Override
    public String getTableName()
    {
        return Table.Transition.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return TransitionColumn.values();
    }

    protected List<LibTransition> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibTransition> transitions = new ArrayList<LibTransition>();
        while(rs.next())
        {
            LibTransition transition = new LibTransition();
            transition.setId(rs.getInt(TransitionColumn.Id.colName()));
            transition.setPrecursorId(rs.getInt(TransitionColumn.PrecursorId.colName()));
            transition.setMz(readDouble(rs, TransitionColumn.Mz.colName()));
            transition.setCharge(readInteger(rs, TransitionColumn.Charge.colName()));
            transition.setNeutralMass(readDouble(rs, TransitionColumn.NeutralMass.colName()));
            transition.setNeutralLossMass(readDouble(rs, TransitionColumn.NeutralLossMass.colName()));
            transition.setFragmentType(rs.getString(TransitionColumn.FragmentType.colName()));
            transition.setFragmentOrdinal(readInteger(rs, TransitionColumn.FragmentOrdinal.colName()));
            transition.setMassIndex(readInteger(rs, TransitionColumn.MassIndex.colName()));
            transition.setArea(rs.getDouble(TransitionColumn.Area.colName()));
            transition.setHeight(rs.getDouble(TransitionColumn.Height.colName()));
            transition.setFwhm(rs.getDouble(TransitionColumn.Fwhm.colName()));
            transition.setChromatogramIndex(readInteger(rs, TransitionColumn.ChromatogramIndex.colName()));

            transitions.add(transition);
        }
        return transitions;
    }
}
