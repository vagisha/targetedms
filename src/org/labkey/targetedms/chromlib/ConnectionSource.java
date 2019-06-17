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

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * User: vsharma
 * Date: 1/6/13
 * Time: 3:30 PM
 */
public class ConnectionSource implements AutoCloseable
{
    private final String _libraryFilePath;
    private final DataSource _dataSource;
    private final @NotNull GenericObjectPool _connectionPool;

    static
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    public ConnectionSource(String libraryFilePath)
    {
        _libraryFilePath = libraryFilePath;

        // Create an ObjectPool that serves as the pool of connections.
        _connectionPool = new GenericObjectPool();
        _connectionPool.setMaxActive(5);

        // Create a ConnectionFactory that the pool will use to create Connections.
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:sqlite:/" + libraryFilePath, null);

        // Create a PoolableConnectionFactory, which wraps the "real" Connections created by the
        // ConnectionFactory with the classes that implement the pooling functionality.
        new PoolableConnectionFactory(connectionFactory,
                _connectionPool,
                null,
                "SELECT 1",  // validationQuery
                false, // defaultReadOnly
                true); // defaultAutoCommit

        // Create the PoolingDataSource
        _dataSource = new PoolingDataSource(_connectionPool);
    }

    public String getLibraryFilePath()
    {
       return _libraryFilePath;
    }

    public Connection getConnection() throws SQLException
    {
        return _dataSource.getConnection();
    }

    public void close()
    {
        _connectionPool.clear();
        try {_connectionPool.close();} catch(Exception ignored) {}
    }

    /**
     * Prints connection pool status.
     */
//    private void printStatus() {
//        System.out.println("Max   : " + _connectionPool.getMaxActive() + "; " +
//            "Active: " + _connectionPool.getNumActive() + "; " +
//            "Idle  : " + _connectionPool.getNumIdle());
//    }
}
