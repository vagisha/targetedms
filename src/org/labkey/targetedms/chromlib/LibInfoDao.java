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

import org.labkey.targetedms.chromlib.Constants.ColumnDef;
import org.labkey.targetedms.chromlib.Constants.LibInfoColumn;
import org.labkey.targetedms.chromlib.Constants.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/29/12
 * Time: 9:25 PM
 */
public class LibInfoDao implements Dao<LibInfo>
{
    public void save(LibInfo libInfo, Connection connection) throws SQLException
    {
        if(libInfo != null)
        {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ");
            sql.append(Table.LibInfo);
            sql.append(" (");
            sql.append(getInsertColumnSql());
            sql.append(")");
            sql.append(" VALUES (?,?,?,?,?,?,?,?,?);");

            try (PreparedStatement stmt = connection.prepareStatement(sql.toString()))
            {
                int colIndex = 1;
                stmt.setString(colIndex++, libInfo.getPanoramaServer());
                stmt.setString(colIndex++, libInfo.getContainer());
                stmt.setString(colIndex++, Constants.DATE_FORMAT.format(libInfo.getCreated()));
                stmt.setString(colIndex++, libInfo.getSchemaVersion());
                stmt.setInt(colIndex++, libInfo.getLibraryRevision());
                stmt.setInt(colIndex++, libInfo.getProteins());
                stmt.setInt(colIndex++, libInfo.getPeptides());
                stmt.setInt(colIndex++, libInfo.getPrecursors());
                stmt.setInt(colIndex, libInfo.getTransitions());
                stmt.executeUpdate();
            }
        }
    }

    public List<LibInfo> queryAll(Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(Table.LibInfo);

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString());)
        {
            List<LibInfo> libInfos = new ArrayList<>();
            while (rs.next())
            {
                LibInfo libInfo = new LibInfo();
                libInfo.setPanoramaServer(rs.getString(LibInfoColumn.PanoramaServer.baseColumn().name()));
                libInfo.setContainer(rs.getString(LibInfoColumn.Container.baseColumn().name()));
                String created = rs.getString(LibInfoColumn.Created.baseColumn().name());
                try
                {
                    libInfo.setCreated(Constants.DATE_FORMAT.parse(created));
                }
                catch (ParseException e)
                {
                    throw new RuntimeException("Error parsing date '"+created+"' in "+Table.LibInfo, e);
                }
                libInfo.setSchemaVersion(rs.getString(LibInfoColumn.SchemaVersion.baseColumn().name()));
                libInfo.setLibraryRevision(rs.getInt(LibInfoColumn.LibraryRevision.baseColumn().name()));
                libInfo.setProteins(rs.getInt(LibInfoColumn.Proteins.baseColumn().name()));
                libInfo.setPeptides(rs.getInt(LibInfoColumn.Peptides.baseColumn().name()));
                libInfo.setPrecursors(rs.getInt(LibInfoColumn.Precursors.baseColumn().name()));
                libInfo.setTransitions(rs.getInt(LibInfoColumn.Transitions.baseColumn().name()));
                libInfos.add(libInfo);
            }
            return libInfos;
        }
    }

    @Override
    public LibInfo queryForId(int id, Connection connection)
    {
        throw new UnsupportedOperationException(Table.LibInfo+" does not have an Id field.");
    }

    @Override
    public List<LibInfo> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection)
    {
        throw new UnsupportedOperationException(Table.LibInfo+" does not have a foreign key");
    }

    @Override
    public void saveAll(List<LibInfo> libInfos, Connection connection)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTableName()
    {
        return Table.LibInfo.name();
    }

    public String getInsertColumnSql()
    {
        StringBuilder columnSql = new StringBuilder();
        for(ColumnDef column: LibInfoColumn.values())
        {
            columnSql.append(", ").append(column.baseColumn().name());
        }
        columnSql.deleteCharAt(0); // delete first comma
        return columnSql.toString();
    }
}
