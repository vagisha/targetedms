/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.labkey.targetedms.chromlib.Constants.ColumnDef;
import org.labkey.targetedms.chromlib.Constants.IrtLibraryColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 3/19/14
 *
 * Helper class for exporting chromatogram library.
 *
 */
public class LibIrtLibraryDao extends  BaseDaoImpl<LibIrtLibrary>
{

    @Override
    public String getTableName()
    {
        return Table.IrtLibrary.name();
    }

    @Override
    protected List<LibIrtLibrary> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibIrtLibrary> irtLibraries = new ArrayList<>();

        while(rs.next())
        {
            LibIrtLibrary irtLibrary = new LibIrtLibrary();
            irtLibrary.setId(rs.getInt(IrtLibraryColumn.Id.baseColumn().name()));
            irtLibrary.setModifiedSequence(rs.getString(IrtLibraryColumn.PeptideModSeq.baseColumn().name()));
            irtLibrary.setIrtStandard(rs.getBoolean(IrtLibraryColumn.Standard.baseColumn().name()));
            irtLibrary.setIrtValue(rs.getDouble(IrtLibraryColumn.Irt.baseColumn().name()));
            irtLibrary.setTimeSource(rs.getInt(IrtLibraryColumn.TimeSource.baseColumn().name()));

            irtLibraries.add(irtLibrary);
        }

        return irtLibraries;

    }

    @Override
    protected void setValuesInStatement(LibIrtLibrary irtLibrary, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setObject(colIndex++, irtLibrary.getModifiedSequence(), Types.VARCHAR);
        stmt.setObject(colIndex++, irtLibrary.getIrtStandard(), Types.TINYINT);
        stmt.setObject(colIndex++, irtLibrary.getIrtValue(), Types.DOUBLE);
        stmt.setObject(colIndex++, irtLibrary.getTimeSource(), Types.INTEGER);
    }

    @Override
    protected ColumnDef[] getColumns()
    {
        return IrtLibraryColumn.values();
    }
}
