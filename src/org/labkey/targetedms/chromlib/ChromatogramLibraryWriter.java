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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.util.FileUtil;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.query.PeptideGroupManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    //Predictor
    private Dao<LibPredictor> _predictorDao;

    // Maps are keyed with the Panorama DB rowId values
    private final Map<Long, LibProtein> _libProteinCache = new LinkedHashMap<>();

    private int _peptideCount;

    private Path _libFile;

    private static final Logger _log = LogManager.getLogger(ChromatogramLibraryWriter.class);

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
                new LibPrecursorRetentionTimeDao(Constants.Table.PrecursorRetentionTime, Constants.Column.PrecursorId, Constants.PrecursorRetentionTimeColumn.values()),
                new LibTransitionDao(new LibTransitionOptimizationDao()));
        _peptideDao = new LibPeptideDao(new LibPeptideStructuralModDao(), precursorDao);
        _proteinDao = new LibProteinDao(_peptideDao);

        _irtLibraryDao = new LibIrtLibraryDao();

        _predictorDao = new LibPredictorDao();
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
        _log.info("Done writing chromatogram library " + FileUtil.pathToString(_libFile));
    }

    private void flushCache() throws SQLException
    {
        if (null != _proteinDao)
        {
            flush(_proteinDao, _libProteinCache.values());
        }
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
        flush(_irtLibraryDao, irtLibraries);
    }

    public void writeProtein(long rowId, LibProtein protein)
    {
        _libProteinCache.put(rowId, protein);
    }

    public void writePeptide(LibPeptide libPeptide, Peptide peptide)
    {
        LibProtein protein = _libProteinCache.computeIfAbsent(peptide.getPeptideGroupId(), (id) ->
                new LibProtein(PeptideGroupManager.get(id)));
        _peptideCount++;
        protein.addChild(libPeptide);
    }

    public void writeMolecule(LibPeptide libMolecule, Molecule molecule)
    {
        LibProtein moleculeList = _libProteinCache.computeIfAbsent(molecule.getPeptideGroupId(), (id) ->
                new LibProtein(PeptideGroupManager.get(id)));
        _peptideCount++;
        moleculeList.addChild(libMolecule);
    }

    public void writePredictor(LibPredictor predictor) throws SQLException
    {
        saveEntry(_predictorDao, predictor);
    }

    public int getProteinCount()
    {
        return _libProteinCache.size();
    }

    public int getPeptideCount()
    {
        return _peptideCount;
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

    private <T> void flush(Dao<T> dao, Collection<T> list) throws SQLException
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
