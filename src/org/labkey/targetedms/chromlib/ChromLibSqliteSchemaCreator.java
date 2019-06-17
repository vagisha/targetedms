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
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: vsharma
 * Date: 12/26/12
 * Time: 3:26 PM
 */
public class ChromLibSqliteSchemaCreator
{
    public void createSchema(Connection connection) throws SQLException
    {
        try {
            createTables(connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored){ignored.printStackTrace();}
        }
    }

    private void createTables(Connection conn) throws SQLException
    {
        // Create LibInfo table
        createLibInfoTable(conn);

        // Create StructuralModification table.
        createStructuralModificationTable(conn);

        // Create StructuralModLoss table.
        createStructuralModLossTable(conn);

        // Create IsotopeModification table.
        createIsotopeModificationTable(conn);

        // Create Protein table.
        createProteinTable(conn);

        // Create Peptide table.
        createPeptideTable(conn);

        // Create PeptideStructuralModification table.
        createPeptideStructuralModificationTable(conn);

        // Create SampleFile table.
        createSampleFileTable(conn);

        // Create Precursor table.
        createPrecursorTable(conn);

        // Create PrecursorIsotopeModification table.
        createPrecursorIsotopeModificationTable(conn);

        // Create PrecursorRetentionTime table.
        createPrecursorRetentionTimeTable(conn);

        // Create Transition table.
        createTransitionTable(conn);

        createIrtLibraryTable(conn);
    }

    private void createLibInfoTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.LibInfo, Constants.LibInfoColumn.values());
    }

    private void createStructuralModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.StructuralModification, Constants.StructuralModificationColumn.values());
    }

    private void createStructuralModLossTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.StructuralModLoss, Constants.StructuralModLossColumn.values());
    }

    private void createIsotopeModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.IsotopeModification, Constants.IsotopeModificationColumn.values());
    }

    private void createProteinTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Protein, Constants.ProteinColumn.values());
    }

    private void createPeptideTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Peptide, Constants.PeptideColumn.values());
    }

    private void createPeptideStructuralModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.PeptideStructuralModification, Constants.PeptideStructuralModificationColumn.values());
    }

    private void createPrecursorTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Precursor, Constants.PrecursorColumn.values());
    }

    private void createPrecursorIsotopeModificationTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.PrecursorIsotopeModification, Constants.PrecursorIsotopeModificationColumn.values());
    }

    private void createSampleFileTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.SampleFile, Constants.SampleFileColumn.values());
    }

    private void createPrecursorRetentionTimeTable(Connection conn) throws SQLException
    {
       createTable(conn, Table.PrecursorRetentionTime, Constants.PrecursorRetentionTimeColumn.values());
    }

    private void createTransitionTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.Transition, Constants.TransitionColumn.values());
    }

    private void createIrtLibraryTable(Connection conn) throws SQLException
    {
        createTable(conn, Table.IrtLibrary, Constants.IrtLibraryColumn.values());
    }

    private String getColumnSql(Constants.ColumnDef[] columns)
    {
        StringBuilder columnSql = new StringBuilder();
        for(Constants.ColumnDef column: columns)
        {
            columnSql.append(", ").append(column.baseColumn().name()).append(" ").append(column.definition());

            // Append the foreign key if it has one
            if (column.baseColumn().getFkColumn() != null)
            {
                columnSql.append(" REFERENCES ").
                        append(column.baseColumn().getFkTable()).
                        append("(").
                        append(column.baseColumn().
                                getFkColumn()).append(")");
            }
        }
        columnSql.deleteCharAt(0); // delete first comma
        return columnSql.toString();
    }

    private void createTable(Connection conn, Table tableName, Constants.ColumnDef[] columns) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        sql.append(tableName.name());
        sql.append(" (");
        sql.append(getColumnSql(columns));
        sql.append(" )");

        try (Statement stmt = conn.createStatement())
        {
            stmt.execute(sql.toString());
        }

        for (Constants.ColumnDef column : columns)
        {
            if (column.baseColumn().getFkColumn() != null)
            {
                try (Statement stmt = conn.createStatement())
                {
                    StringBuilder indexSQL = new StringBuilder("CREATE INDEX IDX_");
                    indexSQL.append(tableName);
                    indexSQL.append("_");
                    indexSQL.append(column.baseColumn().name());
                    indexSQL.append(" ON ");
                    indexSQL.append(tableName);
                    indexSQL.append("(");
                    indexSQL.append(column.baseColumn().name());
                    indexSQL.append(")");
                    stmt.execute(indexSQL.toString());
                }
            }
        }
    }
}
