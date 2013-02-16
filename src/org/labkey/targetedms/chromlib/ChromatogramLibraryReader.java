/*
 * Copyright (c) 2013 LabKey Corporation
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
 * Date: 1/14/13
 * Time: 10:40 AM
 */
public class ChromatogramLibraryReader
{
    private final ConnectionSource _connectionSource;

    public ChromatogramLibraryReader(ConnectionSource connectionSource)
    {
        _connectionSource = connectionSource;
    }

    public LibInfo readLibInfo() throws SQLException
    {
        LibInfoDao libInfoDao = new LibInfoDao();
        List<LibInfo> libInfos = queryAll(libInfoDao);

        if(libInfos == null || libInfos.size() == 0)
        {
            return null;
        }

        if(libInfos.size() > 1)
        {
            throw new IllegalStateException("Multiple entries found in LibInfo table. Library file: "+_connectionSource.getLibraryFilePath());
        }

        return libInfos.get(0);
    }

    public List<LibSampleFile> readSampleFiles() throws SQLException
    {
        return queryAll(new LibSampleFileDao());
    }

    public List<LibStructuralModLoss> readStructuralModLosses() throws SQLException
    {
        return queryAll(new LibStructuralModLossDao());
    }

    private <T> List<T> queryAll(Dao<T> dao) throws SQLException
    {
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();
            return dao.queryAll(connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored){}
        }
    }

    public List<LibStructuralModification> readStructuralModifications() throws SQLException
    {
        LibStructuralModLossDao modLossDao = new LibStructuralModLossDao();
        LibStructuralModificationDao modDao = new LibStructuralModificationDao(modLossDao);
        List<LibStructuralModification> structuralMods = queryAll(modDao);

        // Load any mod losses for each structural modifications
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();
            for(LibStructuralModification mod: structuralMods)
            {
                modDao.loadStructuralModLosses(mod, connection);
            }
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }

        return structuralMods;
    }

    public List<LibIsotopeModification> readIsotopeModifications() throws SQLException
    {
        return queryAll(new LibIsotopeModificationDao());
    }

    public List<LibPeptide> readPeptides() throws SQLException
    {
        return queryAll(new LibPeptideDao(null, null));
    }

    public void refreshPeptidePrecursors(LibPeptide peptide) throws SQLException
    {
        LibPrecursorDao precDao = new LibPrecursorDao(null, null, null);
        LibPeptideDao peptideDao = new LibPeptideDao(null, precDao);
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();

            peptideDao.loadPrecursors(peptide, connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }
    }

    public void refreshPeptideStructuralMods(LibPeptide peptide) throws SQLException
    {
        LibPeptideDao peptideDao = new LibPeptideDao(new LibPeptideStructuralModDao(), null);
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();

            peptideDao.loadPeptideStructuralMods(peptide, connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }
    }

    public void refreshPrecursorTransitions(LibPrecursor precursor) throws SQLException
    {
        LibPrecursorDao precDao = new LibPrecursorDao(null, null, new LibTransitionDao());
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();

            precDao.loadTransitions(precursor, connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }
    }

    public void refreshPrecursorIsotopeModifications(LibPrecursor precursor) throws SQLException
    {
        LibPrecursorDao precDao = new LibPrecursorDao(new LibPrecursorIsotopeModificationDao(), null, null);
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();

            precDao.loadPrecursorIsotopeModifications(precursor, connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }
    }

    public void refreshPrecursorRetentionTimes(LibPrecursor precursor) throws SQLException
    {
        LibPrecursorDao precDao = new LibPrecursorDao(null, new LibPrecursorRetentionTimeDao(), null);
        Connection connection = null;
        try
        {
            connection = _connectionSource.getConnection();

            precDao.loadPrecursorRetentionTimes(precursor, connection);
        }
        finally
        {
            if(connection != null) try {connection.close();} catch(SQLException ignored) {}
        }
    }
}
