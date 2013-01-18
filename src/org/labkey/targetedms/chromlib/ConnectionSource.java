package org.labkey.targetedms.chromlib;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * User: vsharma
 * Date: 1/6/13
 * Time: 3:30 PM
 */
public class ConnectionSource
{
    private String _libraryFilePath;
    private final DataSource _dataSource;
    private GenericObjectPool _connectionPool;

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
        String connectURI = new StringBuilder().append("jdbc:sqlite:/").append(libraryFilePath).toString();
        _dataSource = setupDataSource(connectURI);
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
        if(_connectionPool != null)
        {
            _connectionPool.clear();
            try {_connectionPool.close();} catch(Exception ignored) {}
        }
    }

    private DataSource setupDataSource(String connectURI)
    {
        // Create a ConnectionFactory that the pool will use to create Connections.
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,null);

        // Create an ObjectPool that serves as the pool of connections.
        _connectionPool =  new GenericObjectPool();
        _connectionPool.setMaxActive(5);


        // Create a PoolableConnectionFactory, which wraps the "real" Connections created by the
        // ConnectionFactory with the classes that implement the pooling functionality.
        new PoolableConnectionFactory(connectionFactory,
                                      _connectionPool,
                                      null,
                                      "SELECT 1",  // validationQuery
                                      false, // defaultReadOnly
                                      true); // defaultAutoCommit

        // Create the PoolingDataSource
        return new PoolingDataSource(_connectionPool);
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
