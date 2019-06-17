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
