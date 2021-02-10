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
public class LibMoleculePrecursorDao extends BaseDaoImpl<LibMoleculePrecursor>
{
    private final Dao<LibPrecursorRetentionTime> _precRetentionTimeDao;
    private final Dao<LibMoleculeTransition> _transitionDao;

    public LibMoleculePrecursorDao(Dao<LibPrecursorRetentionTime> precRetentionTimeDao,
                                   Dao<LibMoleculeTransition> transitionDao)
    {
        _precRetentionTimeDao = precRetentionTimeDao;
        _transitionDao = transitionDao;
    }

    @Override
    public void save(LibMoleculePrecursor precursor, Connection connection) throws SQLException
    {
        if(precursor != null)
        {
            super.save(precursor, connection);

            if(_precRetentionTimeDao != null)
            {
                for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                {
                    precRetentionTime.setPrecursorId(precursor.getId());
                }
                _precRetentionTimeDao.saveAll(precursor.getRetentionTimes(), connection);
            }
            if(_transitionDao != null)
            {
                for(LibMoleculeTransition transition: precursor.getTransitions())
                {
                    transition.setMoleculePrecursorId(precursor.getId());
                }
                _transitionDao.saveAll(precursor.getTransitions(), connection);
            }
        }
    }

    @Override
    protected void setValuesInStatement(LibMoleculePrecursor precursor, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setLong(colIndex++, precursor.getMoleculeId());
        stmt.setString(colIndex++, precursor.getIsotopeLabel());
        stmt.setDouble(colIndex++, precursor.getMz());
        stmt.setInt(colIndex++, precursor.getCharge());
        stmt.setObject(colIndex++, precursor.getCollisionEnergy(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getDeclusteringPotential(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getTotalArea(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getNumTransitions(), Types.INTEGER);
        stmt.setObject(colIndex++, precursor.getNumPoints(), Types.INTEGER);
        stmt.setObject(colIndex++, precursor.getAverageMassErrorPPM(), Types.DOUBLE);
        stmt.setLong(colIndex++, precursor.getSampleFileId());
        stmt.setObject(colIndex++, precursor.getChromatogram(), Types.BLOB);
        stmt.setInt(colIndex++, precursor.getUncompressedSize());
        stmt.setInt(colIndex++, precursor.getChromatogramFormat());

        stmt.setObject(colIndex++, precursor.getMassMonoisotopic(), Types.DOUBLE);
        stmt.setObject(colIndex++, precursor.getMassAverage(), Types.DOUBLE);
        stmt.setString(colIndex++, precursor.getAdduct());

        colIndex = setIonMobilityColumns(stmt, colIndex, precursor);
    }

    @Override
    public String getTableName()
    {
        return Table.MoleculePrecursor.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculePrecursorColumn.values();
    }

    @Override
    public void saveAll(Collection<LibMoleculePrecursor> precursors, Connection connection) throws SQLException
    {
        if(precursors.size() > 0)
        {
            super.saveAll(precursors, connection);

            List<LibPrecursorRetentionTime> precRetentionTimes = new ArrayList<>();
            List<LibMoleculeTransition> transitions = new ArrayList<>();

            if(_precRetentionTimeDao != null)
            {
                for(LibMoleculePrecursor precursor: precursors)
                {
                    for(LibPrecursorRetentionTime precRetentionTime: precursor.getRetentionTimes())
                    {
                        precRetentionTime.setPrecursorId(precursor.getId());
                        precRetentionTimes.add(precRetentionTime);
                    }
                }
                _precRetentionTimeDao.saveAll(precRetentionTimes, connection);
            }
            if(_transitionDao != null)
            {
                for(LibMoleculePrecursor precursor: precursors)
                {
                    for(LibMoleculeTransition transition: precursor.getTransitions())
                    {
                        transition.setMoleculePrecursorId(precursor.getId());
                        transitions.add(transition);
                    }
                }
                _transitionDao.saveAll(transitions, connection);
            }
        }
    }

    @Override
    protected List<LibMoleculePrecursor> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMoleculePrecursor> precursors = new ArrayList<>();
        while(rs.next())
        {
            LibMoleculePrecursor precursor = new LibMoleculePrecursor(rs);
            precursors.add(precursor);
        }
        return precursors;
    }

    public void loadTransitions(LibMoleculePrecursor precursor, Connection connection) throws SQLException
    {
        List<LibMoleculeTransition> transitions = _transitionDao.queryForForeignKey(Constants.TransitionColumn.PrecursorId.baseColumn().name(),
                                                                            precursor.getId(),
                                                                            connection);
        for(LibMoleculeTransition transition: transitions)
        {
            precursor.addTransition(transition);
        }
    }

    public void loadPrecursorRetentionTimes(LibMoleculePrecursor precursor, Connection connection) throws SQLException
    {
        List<LibPrecursorRetentionTime> precRetTimes = _precRetentionTimeDao.queryForForeignKey(Constants.MoleculePrecursorRetentionTimeColumn.MoleculePrecursorId.baseColumn().name(),
                                                                                                precursor.getId(),
                                                                                                connection);
        for(LibPrecursorRetentionTime precRt: precRetTimes)
        {
            precursor.addRetentionTime(precRt);
        }
    }
}
