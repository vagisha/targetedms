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

import org.labkey.targetedms.chromlib.Constants.PeptideStructuralModificationColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

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
public class LibPeptideStructuralModDao extends BaseDaoImpl<LibPeptideStructuralModification>
{
    @Override
    protected void setValuesInStatement(LibPeptideStructuralModification pepStrMod, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, pepStrMod.getPeptideId());
        stmt.setInt(colIndex++, pepStrMod.getStructuralModificationId());
        stmt.setInt(colIndex++, pepStrMod.getIndexAa());
        stmt.setDouble(colIndex, pepStrMod.getMassDiff());
    }

    @Override
    public String getTableName()
    {
        return Table.PeptideStructuralModification.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PeptideStructuralModificationColumn.values();
    }

    protected List<LibPeptideStructuralModification> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPeptideStructuralModification> pepStrMods = new ArrayList<>();
        while(rs.next())
        {
            LibPeptideStructuralModification pepStrMod = new LibPeptideStructuralModification();
            pepStrMod.setId(rs.getInt(PeptideStructuralModificationColumn.Id.baseColumn().name()));
            pepStrMod.setPeptideId(rs.getInt(PeptideStructuralModificationColumn.PeptideId.baseColumn().name()));
            pepStrMod.setStructuralModificationId(rs.getInt(PeptideStructuralModificationColumn.StructuralModId.baseColumn().name()));
            pepStrMod.setIndexAa(rs.getInt(PeptideStructuralModificationColumn.IndexAa.baseColumn().name()));
            pepStrMod.setMassDiff(rs.getDouble(PeptideStructuralModificationColumn.MassDiff.baseColumn().name()));

            pepStrMods.add(pepStrMod);
        }
        return pepStrMods;
    }
}
