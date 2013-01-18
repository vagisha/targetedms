package org.labkey.targetedms.chromlib;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * User: vsharma
 * Date: 1/2/13
 * Time: 2:12 PM
 */
public interface Dao<T extends Object>
{
    public void save(T t, Connection connection) throws SQLException;

    public void saveAll(List<T> list, Connection connection) throws SQLException;

    public String getTableName();

    public T queryForId(int id, Connection connection) throws SQLException;

    public List<T> queryForForeignKey(String foreignKeyColumn, int foreignKeyValue, Connection connection) throws SQLException;

    public List<T> queryAll(Connection connection) throws SQLException;
}
