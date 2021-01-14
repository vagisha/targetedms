package org.labkey.targetedms.chromlib;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibPredictorDao extends BaseDaoImpl<LibPredictor>
{
    @Override
    protected List<LibPredictor> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPredictor> libPredictors = new ArrayList<>();
        while (rs.next())
        {
            LibPredictor libPredictor = new LibPredictor();
            libPredictor.setName(rs.getString(Constants.PredictorColumn.Name.name()));
            libPredictor.setStepSize(rs.getDouble(Constants.PredictorColumn.StepSize.name()));
            libPredictor.setStepCount(rs.getInt(Constants.PredictorColumn.StepCount.name()));

            libPredictors.add(libPredictor);
        }
        return libPredictors;
    }

    @Override
    protected void setValuesInStatement(LibPredictor predictor, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, predictor.getName());
        stmt.setDouble(colIndex++, predictor.getStepSize());
        stmt.setInt(colIndex, predictor.getStepCount());
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.PredictorColumn.values();
    }

    @Override
    public String getTableName()
    {
        return Constants.Table.Predictor.name();
    }
}
