package org.labkey.targetedms.chromlib;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class LibMoleculeTransitionOptimizationDao extends BaseDaoImpl<LibMoleculeTransitionOptimization>
{
    @Override
    protected List<LibMoleculeTransitionOptimization> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMoleculeTransitionOptimization> moleculeTransitionOptimizations = new ArrayList<>();
        while (rs.next())
        {
            LibMoleculeTransitionOptimization moleculeTransitionOptimization = new LibMoleculeTransitionOptimization();
            moleculeTransitionOptimization.setTransitionId(rs.getInt(Constants.TransitionOptimizationColumn.TransitionId.name()));
            moleculeTransitionOptimization.setOptimizationValue(readDouble(rs, Constants.TransitionOptimizationColumn.OptimizationValue.name()));
            moleculeTransitionOptimization.setOptimizationType(rs.getString(Constants.TransitionOptimizationColumn.OptimizationType.name()));

            moleculeTransitionOptimizations.add(moleculeTransitionOptimization);
        }
        return moleculeTransitionOptimizations;
    }

    @Override
    protected void setValuesInStatement(LibMoleculeTransitionOptimization libMoleculeTransitionOptimization, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, libMoleculeTransitionOptimization.getTransitionId());
        stmt.setString(colIndex++, libMoleculeTransitionOptimization.getOptimizationType());
        stmt.setObject(colIndex, libMoleculeTransitionOptimization.getOptimizationValue(), Types.DOUBLE);
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculeTransitionOptimizationColumn.values();
    }

    @Override
    public String getTableName()
    {
        return Constants.Table.MoleculeTransitionOptimization.name();
    }
}
