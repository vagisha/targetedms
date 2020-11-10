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

import org.labkey.targetedms.chromlib.Constants.PeptideColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibMoleculeDao extends BaseDaoImpl<LibMolecule>
{
    private final Dao<LibMoleculePrecursor> _precursorDao;

    public LibMoleculeDao(Dao<LibMoleculePrecursor> precursorDao)
    {
        _precursorDao = precursorDao;
    }

    @Override
    public void save(LibMolecule molecule, Connection connection) throws SQLException
    {
        if(molecule != null)
        {
            super.save(molecule, connection);

            if(_precursorDao != null)
            {
                for(LibMoleculePrecursor precursor: molecule.getPrecursors())
                {
                    precursor.setMoleculeId(molecule.getId());
                }
                _precursorDao.saveAll(molecule.getPrecursors(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibMolecule molecule, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setObject(colIndex++, molecule.getMoleculeListId(), Types.INTEGER);
        stmt.setObject(colIndex++, molecule.getIonFormula(), Types.VARCHAR);
        stmt.setObject(colIndex++, molecule.getCustomIonName(), Types.VARCHAR);
        stmt.setObject(colIndex++, molecule.getMassMonoisotopic(), Types.DOUBLE);
        stmt.setObject(colIndex++, molecule.getMassAverage(), Types.DOUBLE);
        stmt.setString(colIndex, molecule.getMoleculeAccession());
    }

    @Override
    public String getTableName()
    {
        return Table.Molecule.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculeColumn.values();
    }

    @Override
    public void saveAll(Collection<LibMolecule> molecules, Connection connection) throws SQLException
    {
        if(molecules != null && molecules.size() > 0)
        {
            super.saveAll(molecules, connection);

            List<LibMoleculePrecursor> precursors = new ArrayList<>();

            if(_precursorDao != null)
            {
                for(LibMolecule peptide: molecules)
                {
                    for(LibMoleculePrecursor precursor: peptide.getPrecursors())
                    {
                        precursor.setMoleculeId(peptide.getId());
                        precursors.add(precursor);
                    }
                }
                _precursorDao.saveAll(precursors, connection);
            }

        }
    }

    @Override
    public List<LibMolecule> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    @Override
    protected List<LibMolecule> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMolecule> molecules = new ArrayList<>();
        while(rs.next())
        {
            LibMolecule molecule = new LibMolecule();
            molecule.setId(rs.getInt(PeptideColumn.Id.baseColumn().name()));
            molecule.setIonFormula(rs.getString(Constants.MoleculeColumn.IonFormula.baseColumn().name()));
            molecule.setCustomIonName(rs.getString(Constants.MoleculeColumn.CustomIonName.baseColumn().name()));
            molecule.setMassMonoisotopic(readDouble(rs, Constants.MoleculeColumn.MassMonoisotopic.baseColumn().name()));
            molecule.setMassAverage(readDouble(rs, Constants.MoleculeColumn.MassAverage.baseColumn().name()));
            molecule.setMoleculeAccession(rs.getString(Constants.MoleculeColumn.MoleculeAccession.baseColumn().name()));

            molecules.add(molecule);
        }
        return molecules;
    }

    public void loadPrecursors(LibMolecule molecule, Connection connection) throws SQLException
    {
        List<LibMoleculePrecursor> precursors = _precursorDao.queryForForeignKey(Constants.MoleculePrecursorColumn.MoleculeId.baseColumn().name(),
                                                                         molecule.getId(),
                                                                         connection);
        for(LibMoleculePrecursor precursor: precursors)
        {
            molecule.addPrecursor(precursor);
        }
    }
}
