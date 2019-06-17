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
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 10:15 PM
 */
public class LibPeptideDao extends BaseDaoImpl<LibPeptide>
{
    private final Dao<LibPeptideStructuralModification> _pepStrModDao;
    private final Dao<LibPrecursor> _precursorDao;

    public LibPeptideDao(Dao<LibPeptideStructuralModification> pepStrModDao,
                         Dao<LibPrecursor> precursorDao)
    {
        _pepStrModDao = pepStrModDao;
        _precursorDao = precursorDao;
    }

    @Override
    public void save(LibPeptide peptide, Connection connection) throws SQLException
    {
        if(peptide != null)
        {
            super.save(peptide, connection);

            if(_pepStrModDao != null)
            {
                for(LibPeptideStructuralModification strMod: peptide.getStructuralModifications())
                {
                    strMod.setPeptideId(peptide.getId());
                }
                _pepStrModDao.saveAll(peptide.getStructuralModifications(), connection);
            }
            if(_precursorDao != null)
            {
                for(LibPrecursor precursor: peptide.getPrecursors())
                {
                    precursor.setPeptideId(peptide.getId());
                }
                _precursorDao.saveAll(peptide.getPrecursors(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibPeptide peptide, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setObject(colIndex++, peptide.getProteinId(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getSequence(), Types.VARCHAR);
        stmt.setObject(colIndex++, peptide.getStartIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getEndIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, peptide.getPreviousAa(), Types.CHAR);
        stmt.setObject(colIndex++, peptide.getNextAa(), Types.CHAR);
        stmt.setDouble(colIndex++, peptide.getCalcNeutralMass());
        stmt.setInt(colIndex, peptide.getNumMissedCleavages());
    }

    @Override
    public String getTableName()
    {
        return Table.Peptide.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PeptideColumn.values();
    }

    @Override
    public void saveAll(List<LibPeptide> peptides, Connection connection) throws SQLException
    {
        if(peptides != null && peptides.size() > 0)
        {
            super.saveAll(peptides, connection);

            List<LibPeptideStructuralModification> strModList = new ArrayList<>();
            List<LibPrecursor> precursors = new ArrayList<>();

            if(_pepStrModDao != null)
            {
                for(LibPeptide peptide: peptides)
                {
                    for(LibPeptideStructuralModification pepStrMod: peptide.getStructuralModifications())
                    {
                        pepStrMod.setPeptideId(peptide.getId());
                        strModList.add(pepStrMod);
                    }
                }
                _pepStrModDao.saveAll(strModList, connection);
            }
            if(_precursorDao != null)
            {
                for(LibPeptide peptide: peptides)
                {
                    for(LibPrecursor precursor: peptide.getPrecursors())
                    {
                        precursor.setPeptideId(peptide.getId());
                        precursors.add(precursor);
                    }
                }
                _precursorDao.saveAll(precursors, connection);
            }

        }
    }

    @Override
    public List<LibPeptide> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(getTableName()+" does not have a foreign key");
    }

    protected List<LibPeptide> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPeptide> peptides = new ArrayList<>();
        while(rs.next())
        {
            LibPeptide peptide = new LibPeptide();
            peptide.setId(rs.getInt(PeptideColumn.Id.baseColumn().name()));
            peptide.setProteinId(readInteger(rs, PeptideColumn.ProteinId.baseColumn().name()));
            peptide.setSequence(rs.getString(PeptideColumn.Sequence.baseColumn().name()));
            peptide.setStartIndex(readInteger(rs, PeptideColumn.StartIndex.baseColumn().name()));
            peptide.setEndIndex(readInteger(rs, PeptideColumn.EndIndex.baseColumn().name()));
            peptide.setPreviousAa(readCharacter(rs, PeptideColumn.PreviousAa.baseColumn().name()));
            peptide.setNextAa(readCharacter(rs, PeptideColumn.NextAa.baseColumn().name()));
            peptide.setCalcNeutralMass(rs.getDouble(PeptideColumn.CalcNeutralMass.baseColumn().name()));
            peptide.setNumMissedCleavages(rs.getInt(PeptideColumn.NumMissedCleavages.baseColumn().name()));

            peptides.add(peptide);
        }
        return peptides;
    }

    public void loadPrecursors(LibPeptide peptide, Connection connection) throws SQLException
    {
        List<LibPrecursor> precursors = _precursorDao.queryForForeignKey(Constants.PrecursorColumn.PeptideId.baseColumn().name(),
                                                                         peptide.getId(),
                                                                         connection);
        for(LibPrecursor precursor: precursors)
        {
            peptide.addPrecursor(precursor);
        }
    }

    public void loadPeptideStructuralMods(LibPeptide peptide, Connection connection) throws SQLException
    {
        List<LibPeptideStructuralModification> precStructuralMods = _pepStrModDao.queryForForeignKey(Constants.PeptideStructuralModificationColumn.PeptideId.baseColumn().name(),
                                                                                                     peptide.getId(),
                                                                                                     connection);
        for(LibPeptideStructuralModification precStrMod: precStructuralMods)
        {
            peptide.addStructuralModification(precStrMod);
        }
    }
}
