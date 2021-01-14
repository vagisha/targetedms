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
public class LibMoleculeTransitionDao extends BaseDaoImpl<LibMoleculeTransition>
{

    private final Dao<LibMoleculeTransitionOptimization> _moleculeTransitionOptimizationDao;

    public LibMoleculeTransitionDao(Dao<LibMoleculeTransitionOptimization> moleculeTransitionOptimizationDao)
    {
        _moleculeTransitionOptimizationDao = moleculeTransitionOptimizationDao;
    }

   @Override
    protected void setValuesInStatement(LibMoleculeTransition transition, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setLong(colIndex++, transition.getMoleculePrecursorId());
        stmt.setObject(colIndex++, transition.getMz(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getCharge(), Types.INTEGER);
        stmt.setString(colIndex++, transition.getFragmentType());
        stmt.setObject(colIndex++, transition.getMassIndex(), Types.INTEGER);
        stmt.setObject(colIndex++, transition.getArea(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getHeight(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getFwhm(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getMassErrorPPM(), Types.DOUBLE);
        stmt.setObject(colIndex, transition.getChromatogramIndex(), Types.INTEGER);
    }

    @Override
    public String getTableName()
    {
        return Table.MoleculeTransition.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.MoleculeTransitionColumn.values();
    }

    @Override
    protected List<LibMoleculeTransition> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibMoleculeTransition> transitions = new ArrayList<>();
        while(rs.next())
        {
            LibMoleculeTransition transition = new LibMoleculeTransition();
            transition.setId(rs.getInt(Constants.MoleculeTransitionColumn.Id.baseColumn().name()));
            transition.setMoleculePrecursorId(rs.getInt(Constants.MoleculeTransitionColumn.MoleculePrecursorId.baseColumn().name()));
            transition.setMz(readDouble(rs, Constants.MoleculeTransitionColumn.Mz.baseColumn().name()));
            transition.setCharge(readInteger(rs, Constants.MoleculeTransitionColumn.Charge.baseColumn().name()));
            transition.setFragmentType(rs.getString(Constants.MoleculeTransitionColumn.FragmentType.baseColumn().name()));
            transition.setMassIndex(readInteger(rs, Constants.MoleculeTransitionColumn.MassIndex.baseColumn().name()));
            transition.setArea(rs.getDouble(Constants.MoleculeTransitionColumn.Area.baseColumn().name()));
            transition.setHeight(rs.getDouble(Constants.MoleculeTransitionColumn.Height.baseColumn().name()));
            transition.setFwhm(rs.getDouble(Constants.MoleculeTransitionColumn.Fwhm.baseColumn().name()));
            transition.setMassErrorPPM(rs.getDouble(Constants.MoleculeTransitionColumn.MassErrorPPM.baseColumn().name()));
            transition.setChromatogramIndex(readInteger(rs, Constants.MoleculeTransitionColumn.ChromatogramIndex.baseColumn().name()));

            transitions.add(transition);
        }
        return transitions;
    }

    @Override
    public void saveAll(Collection<LibMoleculeTransition> transitions, Connection connection) throws SQLException
    {
        if (transitions.size() > 0)
        {
            super.saveAll(transitions, connection);

            List<LibMoleculeTransitionOptimization> moleculeTransitionOptimizations = new ArrayList<>();

            if (_moleculeTransitionOptimizationDao != null)
            {
                transitions.forEach(transition -> {

                    if (null != transition.getCollisionEnergy())
                    {
                        LibMoleculeTransitionOptimization moleculeTransitionOptimization = new LibMoleculeTransitionOptimization();
                        moleculeTransitionOptimization.setTransitionId(transition.getId());
                        moleculeTransitionOptimization.setOptimizationType("ce");
                        moleculeTransitionOptimization.setOptimizationValue(transition.getCollisionEnergy());
                        moleculeTransitionOptimizations.add(moleculeTransitionOptimization);
                    }
                    if (null != transition.getDeclusteringPotential())
                    {
                        LibMoleculeTransitionOptimization moleculeTransitionOptimization = new LibMoleculeTransitionOptimization();
                        moleculeTransitionOptimization.setTransitionId(transition.getId());
                        moleculeTransitionOptimization.setOptimizationType("dp");
                        moleculeTransitionOptimization.setOptimizationValue(transition.getDeclusteringPotential());
                        moleculeTransitionOptimizations.add(moleculeTransitionOptimization);
                    }
                });

                _moleculeTransitionOptimizationDao.saveAll(moleculeTransitionOptimizations, connection);
            }
        }
    }
}
