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
import org.labkey.targetedms.chromlib.Constants.TransitionColumn;

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
public class LibTransitionDao extends BaseDaoImpl<LibTransition>
{
   @Override
    protected void setValuesInStatement(LibTransition transition, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, transition.getPrecursorId());
        stmt.setObject(colIndex++, transition.getMz(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getCharge(), Types.INTEGER);
        stmt.setObject(colIndex++, transition.getNeutralMass(), Types.DOUBLE);
        stmt.setObject(colIndex++, transition.getNeutralLossMass(), Types.DOUBLE);
        stmt.setString(colIndex++, transition.getFragmentType());
        stmt.setObject(colIndex++, transition.getFragmentOrdinal(), Types.INTEGER);
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
        return Table.Transition.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return TransitionColumn.values();
    }

    protected List<LibTransition> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibTransition> transitions = new ArrayList<>();
        while(rs.next())
        {
            LibTransition transition = new LibTransition();
            transition.setId(rs.getInt(TransitionColumn.Id.baseColumn().name()));
            transition.setPrecursorId(rs.getInt(TransitionColumn.PrecursorId.baseColumn().name()));
            transition.setMz(readDouble(rs, TransitionColumn.Mz.baseColumn().name()));
            transition.setCharge(readInteger(rs, TransitionColumn.Charge.baseColumn().name()));
            transition.setNeutralMass(readDouble(rs, TransitionColumn.NeutralMass.baseColumn().name()));
            transition.setNeutralLossMass(readDouble(rs, TransitionColumn.NeutralLossMass.baseColumn().name()));
            transition.setFragmentType(rs.getString(TransitionColumn.FragmentType.baseColumn().name()));
            transition.setFragmentOrdinal(readInteger(rs, TransitionColumn.FragmentOrdinal.baseColumn().name()));
            transition.setMassIndex(readInteger(rs, TransitionColumn.MassIndex.baseColumn().name()));
            transition.setArea(rs.getDouble(TransitionColumn.Area.baseColumn().name()));
            transition.setHeight(rs.getDouble(TransitionColumn.Height.baseColumn().name()));
            transition.setFwhm(rs.getDouble(TransitionColumn.Fwhm.baseColumn().name()));
            transition.setMassErrorPPM(rs.getDouble(TransitionColumn.MassErrorPPM.baseColumn().name()));
            transition.setChromatogramIndex(readInteger(rs, TransitionColumn.ChromatogramIndex.baseColumn().name()));

            transitions.add(transition);
        }
        return transitions;
    }
}
