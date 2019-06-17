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

import org.labkey.targetedms.chromlib.Constants.PrecursorIsotopeModificationColumn;
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
public class LibPrecursorIsotopeModificationDao extends BaseDaoImpl<LibPrecursorIsotopeModification>
{
    @Override
    protected void setValuesInStatement(LibPrecursorIsotopeModification precIsotopeMod, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, precIsotopeMod.getPrecursorId());
        stmt.setInt(colIndex++, precIsotopeMod.getIsotopeModificationId());
        stmt.setInt(colIndex++, precIsotopeMod.getIndexAa());
        stmt.setDouble(colIndex, precIsotopeMod.getMassDiff());
    }

    @Override
    public String getTableName()
    {
        return Table.PrecursorIsotopeModification.name();
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return PrecursorIsotopeModificationColumn.values();
    }

    protected List<LibPrecursorIsotopeModification> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPrecursorIsotopeModification> precIsotopeModifications = new ArrayList<>();
        while(rs.next())
        {
            LibPrecursorIsotopeModification precIsotopeMod = new LibPrecursorIsotopeModification();
            precIsotopeMod.setId(rs.getInt(PrecursorIsotopeModificationColumn.Id.baseColumn().name()));
            precIsotopeMod.setPrecursorId(rs.getInt(PrecursorIsotopeModificationColumn.PrecursorId.baseColumn().name()));
            precIsotopeMod.setIsotopeModificationId(rs.getInt(PrecursorIsotopeModificationColumn.IsotopeModId.baseColumn().name()));
            precIsotopeMod.setIndexAa(rs.getInt(PrecursorIsotopeModificationColumn.IndexAa.baseColumn().name()));
            precIsotopeMod.setMassDiff(readDouble(rs, PrecursorIsotopeModificationColumn.MassDiff.baseColumn().name()));

            precIsotopeModifications.add(precIsotopeMod);
        }
        return precIsotopeModifications;
    }
}
