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

import org.apache.log4j.Logger;
import org.labkey.targetedms.chromlib.Constants.ColumnDef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/3/13
 * Time: 11:22 AM
 */
public abstract class BaseDaoImpl<T extends ObjectWithId> implements Dao<T>
{
    private static final Logger _log = Logger.getLogger(BaseDaoImpl.class);

    public PreparedStatement getPreparedStatement(Connection connection, String sql) throws SQLException
    {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    public void save(T t, Connection connection) throws SQLException
    {
        if(t != null)
        {
            String sql = getInsertSql();

            try (PreparedStatement stmt = getPreparedStatement(connection, sql))
            {
                setValuesInStatement(t, stmt);

                int id = insertAndReturnId(stmt);
                t.setId(id);
            }
        }
    }

    public void saveAll(List<T> list, Connection connection) throws SQLException
    {
        _log.info("Batch insert of "+list.size()+" objects");
        if(list != null && list.size() > 0)
        {
            String sql = getInsertSql();

            try (PreparedStatement stmt = getPreparedStatement(connection, sql))
            {
                connection.setAutoCommit(false);
                for(T t: list)
                {
                    setValuesInStatement(t, stmt);
                    stmt.addBatch();
                }

                int[] ids = insertAndReturnIds(stmt, list.size());
                int index = 0;
                for(T t: list)
                {
                    t.setId(ids[index++]);
                }
                connection.commit();
                connection.setAutoCommit(true);
            }
        }
    }

    private int insertAndReturnId(PreparedStatement statement) throws SQLException
    {
        int rowCount = statement.executeUpdate();

        if (rowCount == 0)
        {
            throw new SQLException("Inserting in "+getTableName()+" failed. No rows were inserted.");
        }

        try (ResultSet generatedKeys = statement.getGeneratedKeys())
        {
            if (generatedKeys.next())
            {
                return generatedKeys.getInt(1);
            }
            else
            {
                throw new SQLException("Inserting in " + getTableName() + " failed. No keys were generated.");
            }
        }
    }

    private int[] insertAndReturnIds(PreparedStatement statement, int numInserts) throws SQLException
    {
        int[] rowCounts = statement.executeBatch();
        if (rowCounts.length != numInserts)
        {
            throw new SQLException("Incorrect number of rows inserted in table "+getTableName()
                                   +". Expected "+numInserts+", got "+rowCounts.length);
        }
        for (int i = 0; i < rowCounts.length; i++)
        {
            if(rowCounts[i] != 1)
            {
                throw new SQLException("Error inserting row at index "+i
                                       +". Status was "+rowCounts[i]
                                       +". Table "+getTableName());
            }
        }

        try (ResultSet generatedKeys = statement.getGeneratedKeys())
        {
            int lastInsertId;

            if (generatedKeys.next())
            {
                lastInsertId = generatedKeys.getInt(1);
            }
            else
            {
                throw new SQLException("Batch insertions failed for table " + getTableName() + ". Could not get last inserted ID.");
            }

            int[] ids = new int[numInserts];
            for (int i = ids.length - 1; i >= 0; i--)
            {
                ids[i] = lastInsertId--;
            }
            return ids;
        }
    }

    public List<T> queryAll(Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());

        return query(connection, sql);
    }

    public T queryForId(int id, Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());
        sql.append(" WHERE Id = ").append(id);

        List<T> results = query(connection, sql);
        if(results == null || results.size() == 0)
        {
            return null;
        }
        if(results.size() != 1)
        {
            throw new SQLException("More than one entries found in "+getTableName()+" for Id "+id);
        }
        return results.get(0);
    }

    public List<T> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ");
        sql.append(getTableName());
        sql.append(" WHERE ").append(foreignKeyColumn).append(" = ").append(foreignKeyValue);
        sql.append(" ORDER BY Id");

         return query(connection, sql);
    }

    private List<T> query(Connection connection, StringBuilder sql) throws SQLException
    {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            return parseQueryResult(rs);
        }
    }

    public Integer readInteger(ResultSet rs, String columnName) throws SQLException
    {
        int value = rs.getInt(columnName);
        if(!rs.wasNull())
        {
            return value;
        }
        else
        {
            return null;
        }
    }

    public Double readDouble(ResultSet rs, String columnName) throws SQLException
    {
        double value = rs.getDouble(columnName);
        if(!rs.wasNull())
        {
            return value;
        }
        else
        {
            return null;
        }
    }

    public Character readCharacter(ResultSet rs, String columnName) throws SQLException
    {
        String value = rs.getString(columnName);
        if(value != null && value.length() > 0)
        {
            return value.charAt(0);
        }
        else
        {
            return null;
        }
    }

    private String getInsertSql()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(getTableName());
        sql.append(" (");
        int startIdx = hasAutoGeneratedIdColumn() ? 1 : 0;
        ColumnDef[] colnames = getColumns();
        for(int i = startIdx; i < colnames.length; i++)
        {
            if(i > startIdx)
                sql.append(", ");
            sql.append(colnames[i].baseColumn().name());
        }

        sql.append(")");
        sql.append(" VALUES (");
        for(int i = startIdx; i < colnames.length; i++)
        {
            if(i > startIdx) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    protected boolean hasAutoGeneratedIdColumn()
    {
        return true;
    }

    protected abstract List<T> parseQueryResult(ResultSet rs) throws SQLException;

    protected abstract void setValuesInStatement(T t, PreparedStatement stmt) throws SQLException;

    protected abstract ColumnDef[] getColumns();

}
