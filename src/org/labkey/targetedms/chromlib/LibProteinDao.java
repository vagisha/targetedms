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

import org.labkey.targetedms.chromlib.Constants.ProteinColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
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
public class LibProteinDao extends BaseDaoImpl<LibProtein>
{
    private final Dao<LibPeptide> _peptideDao;

    public LibProteinDao(Dao<LibPeptide> peptideDao)
    {
        _peptideDao = peptideDao;
    }

    @Override
    public void save(LibProtein protein, Connection connection) throws SQLException
    {
        if(protein != null)
        {
            super.save(protein, connection);

            if(_peptideDao != null)
            {
                for(LibPeptide peptide: protein.getPeptides())
                {
                    peptide.setProteinId(protein.getId());
                }
                _peptideDao.saveAll(protein.getPeptides(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibProtein protein, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, protein.getName());
        stmt.setString(colIndex++, protein.getDescription());
        stmt.setString(colIndex, protein.getSequence());
    }

    @Override
    public String getTableName()
    {
        return Table.Protein.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return ProteinColumn.values();
    }

    @Override
    public void saveAll(List<LibProtein> proteins, Connection connection) throws SQLException
    {
        if(proteins != null && proteins.size() > 0)
        {
            super.saveAll(proteins, connection);

            List<LibPeptide> peptideList = new ArrayList<>();

            if(_peptideDao != null)
            {
                for(LibProtein protein: proteins)
                {
                    for(LibPeptide peptide: protein.getPeptides())
                    {
                        peptide.setProteinId(protein.getId());
                        peptideList.add(peptide);
                    }
                }
                _peptideDao.saveAll(peptideList, connection);
            }
        }
    }

    @Override
    public List<LibProtein> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibProtein> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibProtein> proteins = new ArrayList<>();
        while(rs.next())
        {
            LibProtein protein = new LibProtein();
            protein.setId(rs.getInt(ProteinColumn.Id.baseColumn().name()));
            protein.setName(rs.getString(ProteinColumn.Name.baseColumn().name()));
            protein.setDescription(rs.getString(ProteinColumn.Description.baseColumn().name()));
            protein.setSequence(rs.getString(ProteinColumn.Sequence.baseColumn().name()));

            proteins.add(protein);
        }
        return proteins;
    }

    public void loadPeptides(LibProtein protein, Connection connection) throws SQLException
    {
        List<LibPeptide> peptides = _peptideDao.queryForForeignKey(Constants.PeptideColumn.ProteinId.baseColumn().name(), protein.getId(), connection);
        for(LibPeptide peptide: peptides)
        {
            protein.addPeptide(peptide);
        }
    }
}
