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
import java.util.Collection;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibMoleculeListDao extends BaseDaoImpl<LibMoleculeList>
{
    private final Dao<LibMolecule> _moleculeDao;

    public LibMoleculeListDao(Dao<LibMolecule> dao)
    {
        _moleculeDao = dao;
    }

    @Override
    public void save(LibMoleculeList list, Connection connection) throws SQLException
    {
        if (list != null)
        {
            super.save(list, connection);

            if(_moleculeDao != null)
            {
                for(LibMolecule molecule: list.getChildren())
                {
                    molecule.setMoleculeListId(list.getId());
                }
                _moleculeDao.saveAll(list.getChildren(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibMoleculeList list, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, list.getName());
        stmt.setString(colIndex++, list.getDescription());
    }

    @Override
    public String getTableName()
    {
        return Table.MoleculeList.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculeListColumn.values();
    }

    @Override
    public void saveAll(Collection<LibMoleculeList> lists, Connection connection) throws SQLException
    {
        if(lists != null && lists.size() > 0)
        {
            super.saveAll(lists, connection);

            List<LibMolecule> molecules = new ArrayList<>();

            if(_moleculeDao != null)
            {
                for(LibMoleculeList list: lists)
                {
                    for(LibMolecule molecule: list.getChildren())
                    {
                        molecule.setMoleculeListId(list.getId());
                        molecules.add(molecule);
                    }
                }
                _moleculeDao.saveAll(molecules, connection);
            }
        }
    }

    @Override
    public List<LibMoleculeList> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    @Override
    protected List<LibMoleculeList> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMoleculeList> lists = new ArrayList<>();
        while(rs.next())
        {
            LibMoleculeList list = new LibMoleculeList();
            list.setId(rs.getInt(Constants.MoleculeListColumn.Id.baseColumn().name()));
            list.setName(rs.getString(ProteinColumn.Name.baseColumn().name()));
            list.setDescription(rs.getString(ProteinColumn.Description.baseColumn().name()));

            lists.add(list);
        }
        return lists;
    }

    public void loadMolecules(LibMoleculeList list, Connection connection) throws SQLException
    {
        List<LibMolecule> molecules = _moleculeDao.queryForForeignKey(Constants.MoleculeColumn.MoleculeListId.baseColumn().name(), list.getId(), connection);
        for(LibMolecule molecule: molecules)
        {
            list.addChild(molecule);
        }
    }
}
