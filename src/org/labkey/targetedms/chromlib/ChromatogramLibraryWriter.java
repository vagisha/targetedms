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
import org.labkey.api.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/18/12
 * Time: 1:31 PM
 */
public class ChromatogramLibraryWriter
{
    private ConnectionSource _connectionSource;

    private Dao<LibInfo> _libInfoDao;
    private Dao<LibSampleFile> _sampleFileDao;
    private Dao<LibStructuralModification> _structuralModificationDao;
    private Dao<LibIsotopeModification> _isotopeModificationDao;
    private Dao<LibProtein> _proteinDao;
    private Dao<LibPeptide> _peptideDao;
    private Dao<LibIrtLibrary> _irtLibraryDao;

    private List<LibPeptide> _libPeptideCache;
    private List<LibProtein> _libProteinCache;
    private int _cacheSize = 0;

    private int _maxCacheSize = 1000;

    private Path _libFile;

    private static final Logger _log = Logger.getLogger(ChromatogramLibraryWriter.class);

    public void setMaxCacheSize(int maxCacheSize)
    {
        _maxCacheSize = maxCacheSize;
    }

    public void openLibrary(Path libFile) throws SQLException
    {
        _log.info("Writing chromatogram library " + FileUtil.getFileName(libFile));
        if(Files.exists(libFile))
        {
            throw new IllegalStateException("Chromatogram library file "+ FileUtil.pathToString(libFile) +" already exists.");
        }

        _libFile = libFile;

        // Setup a connection source
       _connectionSource = new ConnectionSource(_libFile.toAbsolutePath().toString());

        // Create an empty schema
        ChromLibSqliteSchemaCreator schemaCreator = new ChromLibSqliteSchemaCreator();
        schemaCreator.createSchema(getConnection());

        initializeDaos();
    }

    private void initializeDaos()
    {

        _libInfoDao = new LibInfoDao();
        _sampleFileDao = new LibSampleFileDao();
        _structuralModificationDao = new LibStructuralModificationDao(new LibStructuralModLossDao());
        _isotopeModificationDao = new LibIsotopeModificationDao();

        Dao<LibPrecursor> precursorDao = new LibPrecursorDao(new LibPrecursorIsotopeModificationDao(),
                new LibPrecursorRetentionTimeDao(),
                new LibTransitionDao());

        _peptideDao = new LibPeptideDao(new LibPeptideStructuralModDao(), precursorDao);

        _proteinDao = new LibProteinDao(_peptideDao);

        _irtLibraryDao = new LibIrtLibraryDao();
    }

    private Connection getConnection() throws SQLException
    {
        return _connectionSource.getConnection();
    }

    public void closeLibrary() throws SQLException
    {
        // Clear any cached data
        flushCache();

        if(_connectionSource != null)
        {
            _connectionSource.close();
        }
        _log.info("Done writing chromatogram library "+FileUtil.pathToString(_libFile));
    }

    private void flushCache() throws SQLException
    {
        // _log.info("ChromatogramLibraryWriter.flushCache() cache size is " + _cacheSize);
        if(_libPeptideCache != null && _libPeptideCache.size() > 0)
        {
            writePeptides(_libPeptideCache);
            _libPeptideCache.clear();
            _libPeptideCache = null;
        }
        if(_libProteinCache != null && _libProteinCache.size() > 0)
        {
            writeProteins(_libProteinCache);
            _libProteinCache.clear();
            _libProteinCache = null;
        }

        _cacheSize = 0;
    }

    public void writeLibInfo(LibInfo libInfo) throws SQLException
    {
        saveEntry(_libInfoDao, libInfo);
    }

    public void writeSampleFile(LibSampleFile sampleFile) throws SQLException
    {
        saveEntry(_sampleFileDao, sampleFile);
    }

    public void writeStructuralModification(LibStructuralModification strMod) throws SQLException
    {
        saveEntry(_structuralModificationDao, strMod);
    }

    public void writeIsotopeModification(LibIsotopeModification isotopeMod) throws SQLException
    {
        saveEntry(_isotopeModificationDao, isotopeMod);
    }

    public void writeIrtLibrary(List<LibIrtLibrary> irtLibraries) throws SQLException
    {
        saveList(_irtLibraryDao, irtLibraries);
    }

    public void writeProtein(LibProtein protein) throws SQLException
    {
        addToProteinCache(protein);
    }

    public void writePeptide(LibPeptide peptide) throws SQLException
    {
        addToPeptideCache(peptide);
    }

    private void writeProteins(List<LibProtein> proteins) throws SQLException
    {
        saveList(_proteinDao, proteins);
    }

    private void writePeptides(List<LibPeptide> peptides) throws SQLException
    {
        saveList(_peptideDao, peptides);
    }

    private void addToProteinCache(LibProtein libProtein) throws SQLException
    {
        if(_libProteinCache == null)
        {
            _libProteinCache = new ArrayList<>();
        }
        _libProteinCache.add(libProtein);

        updateCacheSize(libProtein);

        if(_cacheSize >= _maxCacheSize)
        {
           flushCache();
        }
    }

    private void addToPeptideCache(LibPeptide libPeptide) throws SQLException
    {
        if(_libPeptideCache == null)
        {
            _libPeptideCache = new ArrayList<>();
        }
        _libPeptideCache.add(libPeptide);

        updateCacheSize(libPeptide);

        if(_cacheSize >= _maxCacheSize)
        {
            flushCache();
        }
    }

    private void updateCacheSize(LibProtein libProtein)
    {
        _cacheSize++;
        _cacheSize += libProtein.getPeptides().size();
        for(LibPeptide libPeptide: libProtein.getPeptides())
        {
            updateCacheSize(libPeptide);
        }
    }

    private void updateCacheSize(LibPeptide libPeptide)
    {
        _cacheSize++;
        _cacheSize += libPeptide.getPrecursors().size();
        _cacheSize += libPeptide.getStructuralModifications().size();

        for(LibPrecursor libPrecursor: libPeptide.getPrecursors())
        {
            _cacheSize += libPrecursor.getTransitions().size();
            _cacheSize += libPrecursor.getIsotopeModifications().size();
            _cacheSize += libPrecursor.getRetentionTimes().size();
        }
    }

    private <T> void saveEntry(Dao<T> dao, T object) throws SQLException
    {
        try (Connection connection = getConnection())
        {
            dao.save(object, connection);
        }
        catch(SQLException e)
        {
            _log.error("Error saving to " + dao.getTableName(), e);
            throw e;
        }
    }

    private <T> void saveList(Dao<T> dao, List<T> list) throws SQLException
    {
        try (Connection connection = getConnection())
        {
            dao.saveAll(list, connection);
        }
        catch(SQLException e)
        {
            _log.error("Error saving list to " + dao.getTableName(), e);
            throw e;
        }
    }
}
