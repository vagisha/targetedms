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
import org.labkey.targetedms.chromlib.Constants.StructuralModificationColumn;
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
public class LibStructuralModificationDao extends BaseDaoImpl<LibStructuralModification>
{
    private final Dao<LibStructuralModLoss> _modLossDao;

    public LibStructuralModificationDao(Dao<LibStructuralModLoss> modLossDao)
    {
        _modLossDao = modLossDao;
    }

    public void save(LibStructuralModification structuralMod, Connection connection) throws SQLException
    {
        if(structuralMod != null)
        {
            super.save(structuralMod, connection);

            if(_modLossDao != null)
            {
                for(LibStructuralModLoss modLoss: structuralMod.getModLosses())
                {
                    modLoss.setStructuralModId(structuralMod.getId());
                    _modLossDao.save(modLoss, connection);
                }
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibStructuralModification structuralMod, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, structuralMod.getName());
        stmt.setObject(colIndex++, structuralMod.getAminoAcid(), Types.VARCHAR);
        stmt.setObject(colIndex++, structuralMod.getTerminus(), Types.CHAR);
        stmt.setObject(colIndex++, structuralMod.getFormula(), Types.VARCHAR);
        stmt.setObject(colIndex++, structuralMod.getMassDiffMono(), Types.DOUBLE);
        stmt.setObject(colIndex++, structuralMod.getMassDiffAvg(), Types.DOUBLE);
        stmt.setObject(colIndex++, structuralMod.getUnimodId(), Types.INTEGER);
        stmt.setBoolean(colIndex++, structuralMod.getVariable());
        stmt.setObject(colIndex, structuralMod.getExplicitMod(), Types.BOOLEAN);
    }

    @Override
    public String getTableName()
    {
        return Table.StructuralModification.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return StructuralModificationColumn.values();
    }

    @Override
    public List<LibStructuralModification> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibStructuralModification> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibStructuralModification> structuralMods = new ArrayList<>();
        while(rs.next())
        {
            LibStructuralModification structuralModification = new LibStructuralModification();
            structuralModification.setId(rs.getInt(StructuralModificationColumn.Id.baseColumn().name()));
            structuralModification.setName(rs.getString(StructuralModificationColumn.Name.baseColumn().name()));
            structuralModification.setAminoAcid(rs.getString(StructuralModificationColumn.AminoAcid.baseColumn().name()));
            String terminus = rs.getString(StructuralModificationColumn.Terminus.baseColumn().name());
            if(terminus != null) structuralModification.setTerminus(terminus.charAt(0));
            structuralModification.setFormula(rs.getString(StructuralModificationColumn.Formula.baseColumn().name()));
            double massDiffMono = rs.getDouble(StructuralModificationColumn.MassDiffMono.baseColumn().name());
            if(!rs.wasNull())
                structuralModification.setMassDiffMono(massDiffMono);
            double massDiffAvg = rs.getDouble(StructuralModificationColumn.MassDiffAvg.baseColumn().name());
            if(!rs.wasNull())
                structuralModification.setMassDiffAvg(massDiffAvg);
            int unimodId = rs.getInt(StructuralModificationColumn.UnimodId.baseColumn().name());
            if(!rs.wasNull())
                structuralModification.setUnimodId(unimodId);
            structuralModification.setVariable(rs.getBoolean(StructuralModificationColumn.Variable.baseColumn().name()));
            boolean explicitMod = rs.getBoolean(StructuralModificationColumn.ExplicitMod.baseColumn().name());
            if(!rs.wasNull())
                structuralModification.setExplicitMod(explicitMod);
            structuralMods.add(structuralModification);
        }
        return structuralMods;
    }

    public void loadStructuralModLosses(LibStructuralModification strMod, Connection connection) throws SQLException
    {
        if(strMod != null)
        {
            List<LibStructuralModLoss> modLosses = _modLossDao.queryForForeignKey(StructuralModLossColumn.StructuralModId.baseColumn().name(),
                                                                                  strMod.getId(),
                                                                                  connection);
            for(LibStructuralModLoss modLoss: modLosses)
            {
                strMod.addModLoss(modLoss);
            }
        }
    }

    @Override
    public void saveAll(List<LibStructuralModification> structuralModifications, Connection connection)
    {
        throw new UnsupportedOperationException();
    }

}
