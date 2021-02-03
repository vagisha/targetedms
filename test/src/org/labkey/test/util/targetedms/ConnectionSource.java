package org.labkey.test.util.targetedms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionSource
{
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

    public static Connection getConnection(String libraryFilePath) throws SQLException
    {
        String dbURL = "jdbc:sqlite:/" + libraryFilePath;
        return DriverManager.getConnection(dbURL);
    }
}
