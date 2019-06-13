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

import org.labkey.targetedms.chromlib.Constants.IsotopeModificationColumn;
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
public class LibIsotopeModificationDao extends BaseDaoImpl<LibIsotopeModification>
{
    @Override
    protected void setValuesInStatement(LibIsotopeModification isotopeModification, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, isotopeModification.getName());
        stmt.setString(colIndex++, isotopeModification.getIsotopeLabel());
        stmt.setObject(colIndex++, isotopeModification.getAminoAcid(), Types.CHAR);
        stmt.setObject(colIndex++, isotopeModification.getTerminus(), Types.CHAR);
        stmt.setObject(colIndex++, isotopeModification.getFormula(), Types.VARCHAR);
        stmt.setObject(colIndex++, isotopeModification.getMassDiffMono(), Types.DOUBLE);
        stmt.setObject(colIndex++, isotopeModification.getMassDiffAvg(), Types.DOUBLE);
        stmt.setObject(colIndex++, isotopeModification.getLabel13C(), Types.BOOLEAN);
        stmt.setObject(colIndex++, isotopeModification.getLabel15N(), Types.BOOLEAN);
        stmt.setObject(colIndex++, isotopeModification.getLabel18O(), Types.BOOLEAN);
        stmt.setObject(colIndex++, isotopeModification.getLabel2H(), Types.BOOLEAN);
        stmt.setObject(colIndex, isotopeModification.getUnimodId(), Types.INTEGER);
    }

    @Override
    public String getTableName()
    {
        return Table.IsotopeModification.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return IsotopeModificationColumn.values();
    }

    @Override
    public List<LibIsotopeModification> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibIsotopeModification> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibIsotopeModification> isotopeMods = new ArrayList<>();
        while(rs.next())
        {
            LibIsotopeModification isotopeModification = new LibIsotopeModification();
            isotopeModification.setId(rs.getInt(IsotopeModificationColumn.Id.name()));
            isotopeModification.setName(rs.getString(IsotopeModificationColumn.Name.name()));
            isotopeModification.setIsotopeLabel(rs.getString(IsotopeModificationColumn.IsotopeLabel.name()));
            isotopeModification.setAminoAcid(rs.getString(IsotopeModificationColumn.AminoAcid.name()));
            String terminus = rs.getString(IsotopeModificationColumn.Terminus.name());
            if(terminus != null) isotopeModification.setTerminus(terminus.charAt(0));
            isotopeModification.setFormula(rs.getString(IsotopeModificationColumn.Formula.name()));
            double massDiffMono = rs.getDouble(IsotopeModificationColumn.MassDiffMono.name());
            if(!rs.wasNull())
                isotopeModification.setMassDiffMono(massDiffMono);
            double massDiffAvg = rs.getDouble(IsotopeModificationColumn.MassDiffAvg.name());
            if(!rs.wasNull())
                isotopeModification.setMassDiffAvg(massDiffAvg);
            int unimodId = rs.getInt(IsotopeModificationColumn.UnimodId.name());
            if(!rs.wasNull())
                isotopeModification.setUnimodId(unimodId);
            boolean label13C = rs.getBoolean(IsotopeModificationColumn.Label13C.name());
            if(!rs.wasNull())
                isotopeModification.setLabel13C(label13C);
            boolean label15N = rs.getBoolean(IsotopeModificationColumn.Label15N.name());
            if(!rs.wasNull())
                isotopeModification.setLabel15N(label15N);
            boolean label18O = rs.getBoolean(IsotopeModificationColumn.Label18O.name());
            if(!rs.wasNull())
                isotopeModification.setLabel18O(label18O);
            boolean label2H = rs.getBoolean(IsotopeModificationColumn.Label2H.name());
            if(!rs.wasNull())
                isotopeModification.setLabel2H(label2H);

            isotopeMods.add(isotopeModification);
        }
        return isotopeMods;
    }

    @Override
    public void saveAll(List<LibIsotopeModification> isotopeModifications, Connection connection)
    {
        throw new UnsupportedOperationException();
    }
}
