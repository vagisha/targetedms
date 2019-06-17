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
        List<LibStructuralModLoss> modLosses = new ArrayList<>();
        while(rs.next())
        {
            LibStructuralModLoss modLoss = new LibStructuralModLoss();
            modLoss.setId(rs.getInt(StructuralModLossColumn.Id.baseColumn().name()));
            modLoss.setStructuralModId(rs.getInt(StructuralModLossColumn.StructuralModId.baseColumn().name()));
            modLoss.setFormula(rs.getString(StructuralModLossColumn.Formula.baseColumn().name()));
            double massDiffMono = rs.getDouble(StructuralModLossColumn.MassDiffMono.baseColumn().name());
            if(!rs.wasNull())
                modLoss.setMassDiffMono(massDiffMono);
            double massDiffAvg = rs.getDouble(StructuralModLossColumn.MassDiffAvg.baseColumn().name());
            if(!rs.wasNull())
                modLoss.setMassDiffAvg(massDiffAvg);
            modLosses.add(modLoss);
        }
        return modLosses;
    }

    @Override
    public void saveAll(List<LibStructuralModLoss> structuralModLosses, Connection connection)
    {
        throw new UnsupportedOperationException();
    }
}
