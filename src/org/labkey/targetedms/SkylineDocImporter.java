/*
 * Copyright (c) 2012 LabKey Corporation
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

package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.labkey.api.ProteinService;
import org.labkey.api.collections.CsvSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.XarContext;
import org.labkey.api.security.User;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.writer.ZipUtil;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideChromInfo;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.SkylineDocumentParser;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.TransitionSettings;
import org.labkey.targetedms.query.LibraryManager;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class SkylineDocImporter
{
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    private static final String IMPORT_STARTED = "Importing... (refresh to check status)";
    private static final String IMPORT_SUCCEEDED = "";

    protected User _user;
    protected Container _container;
    protected String _description, _fileName, _path;

    protected int _runId;

    // Use passed in logger for import status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static Logger _systemLog = Logger.getLogger(SkylineDocImporter.class);
    protected final XarContext _context;

    // protected Connection _conn;
    // private static final int BATCH_SIZE = 100;

    public SkylineDocImporter(User user, Container c, String description, File file, Logger log, XarContext context)
    {
        _context = context;
        _user = user;
        _container = c;

        _path = file.getParent();
        _fileName = file.getName();

        if (null != description)
            _description = description;
        else
        {
            int extension = _fileName.lastIndexOf(".");

            if (-1 != extension)
                _description = _fileName.substring(0, extension);
        }

        _log = (null == log ? _systemLog : log);
    }

    public TargetedMSRun importRun(RunInfo runInfo) throws IOException, SQLException, XMLStreamException, DataFormatException
    {
        _runId = runInfo.getRunId();

        TargetedMSRun run = TargetedMSManager.getRun(_runId);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == SkylineDocImporter.STATUS_SUCCESS)
        {
            _log.info(_fileName + " has already been imported so it does not need to be imported again");
            return run;
        }

        try
        {
            updateRunStatus(IMPORT_STARTED);
            importSkylineDoc(run);
        }
        catch (FileNotFoundException fnfe)
        {
            logError("Skyline document import failed due to a missing file.", fnfe);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw fnfe;
        }
        catch (SQLException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (IOException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (XMLStreamException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (RuntimeException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        catch (DataFormatException e)
        {
            logError("Skyline document import failed", e);
            updateRunStatus("Import failed (see pipeline log)", STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }

        updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);
        return TargetedMSManager.getRun(_runId);
    }


    private void importSkylineDoc(TargetedMSRun run) throws XMLStreamException, IOException, SQLException, DataFormatException
    {
        // TODO - Consider if this is too big to fit in a single transaction. If so, need to blow away all existing
        // data for this run before restarting the import in the case of a retry
        TargetedMSManager.getSchema().getScope().ensureTransaction();
        SkylineDocumentParser parser = null;
        File zipDir = null;
        try
        {
            File f = new File(_path + "/" + _fileName);

            NetworkDrive.ensureDrive(f.getPath());

            // If this is a zip file extract the contents to a folder
            String ext = FileUtil.getExtension(f.getName());
            if("zip".equalsIgnoreCase(ext))
            {
                zipDir = new File(f.getParent()+File.separator+FileUtil.getBaseName(f));
                List<File> files = ZipUtil.unzipToDirectory(f, zipDir, _log);
                File skyFile = null;
                for(File file: files)
                {
                    ext = FileUtil.getExtension(file.getName());
                    if("sky".equalsIgnoreCase(ext))
                    {
                        skyFile = file;
                        break;
                    }
                }

                if(skyFile == null)
                {
                    throw new IOException("zip file "+skyFile+" does not contain a .sky file");
                }
                f = skyFile;
            }

            ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);

            parser = new SkylineDocumentParser(f, _log);
            parser.readSettings();

            // Store the document settings
            // 1. Transition settings
            TransitionSettings transSettings = parser.getTransitionSettings();
            TransitionSettings.InstrumentSettings instrumentSettings = transSettings.getInstrumentSettings();
            instrumentSettings.setRunId(_runId);
            Table.insert(_user, TargetedMSManager.getTableInfoTransInstrumentSettings(), instrumentSettings);


            // 2. Replicates and sample files
            Map<String, Integer> skylineIdSampleFileIdMap = new HashMap<String, Integer>();
            Map<String, Integer> filePathSampleFileIdMap = new HashMap<String, Integer>();
            for(Replicate replicate: parser.getReplicates())
            {
                replicate.setRunId(_runId);
                replicate = Table.insert(_user, TargetedMSManager.getTableInfoReplicate(), replicate);

                for(SampleFile sampleFile: replicate.getSampleFileList())
                {
                    sampleFile.setReplicateId(replicate.getId());
                    sampleFile = Table.insert(_user, TargetedMSManager.getTableInfoSampleFile(), sampleFile);
                    // Remember the ids we inserted so we can reference them later
                    skylineIdSampleFileIdMap.put(sampleFile.getSkylineId(), sampleFile.getId());
                    filePathSampleFileIdMap.put(sampleFile.getFilePath(), sampleFile.getId());

                    for (Instrument instrument : sampleFile.getInstrumentList())
                    {
                        instrument.setRunId(_runId);
                        instrument = Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
                    }
                }
            }

            // 3. Peptide settings
            Map<String, Integer> isotopeLabelIdMap = new HashMap<String, Integer>();
            Map<String, Integer> structuralModNameIdMap = new HashMap<String, Integer>();
            Map<String, Integer> isotopeModNameIdMap = new HashMap<String, Integer>();
            PeptideSettings pepSettings = parser.getPeptideSettings();
            PeptideSettings.PeptideModifications modifications = pepSettings.getModifications();
            if(modifications != null)
            {
                // Insert isotope labels
                List<PeptideSettings.IsotopeLabel> isotopeLabels = modifications.getIsotopeLabels();
                for(PeptideSettings.IsotopeLabel isotopeLabel: isotopeLabels)
                {
                    isotopeLabel.setRunId(_runId);
                    isotopeLabel = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeLabel(), isotopeLabel);
                    isotopeLabelIdMap.put(isotopeLabel.getName(), isotopeLabel.getId());
                }

                // Insert peptide modification settings
                PeptideSettings.ModificationSettings modSettings = modifications.getModificationSettings();
                if(modSettings != null)
                {
                    modSettings.setRunId(_runId);
                    Table.insert(_user, TargetedMSManager.getTableInfoModificationSettings(), modSettings);
                }

                // Insert structural modifications
                List<PeptideSettings.RunStructuralModification> structuralMods = modifications.getStructuralModifications();
                for(PeptideSettings.RunStructuralModification mod: structuralMods)
                {
                    // TODO: check if this modification already exists in the database
                    mod = Table.insert(_user, TargetedMSManager.getTableInfoStructuralModification(), mod);
                    structuralModNameIdMap.put(mod.getName(), mod.getId());

                    mod.setRunId(_runId);
                    mod.setStructuralModId(mod.getId());
                    Table.insert(_user, TargetedMSManager.getTableInfoRunStructuralModification(), mod);
                }

                // Insert isotope modifications
                List<PeptideSettings.RunIsotopeModification> isotopeMods = modifications.getIsotopeModifications();
                for(PeptideSettings.RunIsotopeModification mod: isotopeMods)
                {
                    // TODO: check if this modification already exists in the database
                    mod = Table.insert(_user, TargetedMSManager.getTableInfoIsotopeModification(), mod);
                    isotopeModNameIdMap.put(mod.getName(), mod.getId());

                    mod.setRunId(_runId);
                    mod.setIsotopeModId(mod.getId());
                    mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
                    Table.insert(_user, TargetedMSManager.getTableInfoRunIsotopeModification(), mod);
                }
            }

            // Spectrum library settings
            Map<String, Integer> librarySourceTypes = LibraryManager.getLibrarySourceTypes();
            Map<String, Integer> libraryNameIdMap = new HashMap<String, Integer>();
            PeptideSettings.SpectrumLibrarySettings librarySettings = pepSettings.getLibrarySettings();
            if(librarySettings != null)
            {
                librarySettings.setRunId(_runId);
                Table.insert(_user, TargetedMSManager.getTableInfoLibrarySettings(), librarySettings);

                for(PeptideSettings.SpectrumLibrary library: librarySettings.getLibraries())
                {
                    library.setRunId(_runId);
                    if(library.getLibraryType().equals("bibliospec_lite") ||
                       library.getLibraryType().equals("bibliospec"))
                    {
                        library.setLibrarySourceId(librarySourceTypes.get("BiblioSpec"));
                    }
                    else if(library.getLibraryType().equals("hunter"))
                    {
                        library.setLibrarySourceId(librarySourceTypes.get("GPM"));
                    }
                    else if(library.getLibraryType().equals("nist") ||
                            library.getLibraryType().equals("spectrast"))
                    {
                        library.setLibrarySourceId(librarySourceTypes.get("NIST"));
                    }
                    library = Table.insert(_user, TargetedMSManager.getTableInfoSpectrumLibrary(), library);
                    libraryNameIdMap.put(library.getName(), library.getId());
                }
            }

            PeptideSettings.Enzyme enzyme = pepSettings.getEnzyme();
            if (enzyme != null)
            {
                SimpleFilter filter = new SimpleFilter("cut", enzyme.getCut());
                if (enzyme.getNoCut() == null)
                {
                    filter.addCondition(new CompareType.CompareClause("nocut", CompareType.ISBLANK, null));
                }
                else
                {
                    filter.addCondition("nocut", enzyme.getNoCut());
                }
                filter.addCondition("sense", enzyme.getSense());
                filter.addCondition("name", enzyme.getName());
                PeptideSettings.Enzyme existingEnzyme = new TableSelector(TargetedMSManager.getTableInfoEnzyme(), Table.ALL_COLUMNS, filter, null).getObject(PeptideSettings.Enzyme.class);
                if (existingEnzyme == null)
                {
                    enzyme = Table.insert(_user, TargetedMSManager.getTableInfoEnzyme(), enzyme);
                }
                else
                {
                    enzyme = existingEnzyme;
                }

                PeptideSettings.EnzymeDigestionSettings digestSettings = pepSettings.getEnzymeDigestionSettings();
                if (digestSettings == null)
                {
                    digestSettings = new PeptideSettings.EnzymeDigestionSettings();
                }
                digestSettings.setRunId(_runId);
                digestSettings.setEnzymeId(enzyme.getId());
                digestSettings = Table.insert(_user, TargetedMSManager.getTableInfoRunEnzyme(), digestSettings);
            }

            // TODO: bulk insert of precursor, transition, chrom info etc.

            // Store the data
            // 1. peptide group
            while (parser.hasNextPeptideGroup())
            {
                PeptideGroup pepGroup = parser.nextPeptideGroup();
                pepGroup.setRunId(_runId);

                if(pepGroup.isProtein())
                {
                    int seqId = proteinService.ensureProtein(pepGroup.getSequence(), null, pepGroup.getLabel(), pepGroup.getDescription());
                    pepGroup.setSequenceId(seqId);
                }
                pepGroup = Table.insert(_user, TargetedMSManager.getTableInfoPeptideGroup(), pepGroup);

                // 2. peptide
                for(Peptide peptide: pepGroup.getPeptideList())
                {
                    peptide.setPeptideGroupId(pepGroup.getId());
                    peptide = Table.insert(_user, TargetedMSManager.getTableInfoPeptide(), peptide);

                    for(Peptide.StructuralModification mod: peptide.getStructuralMods())
                    {
                        int modId = structuralModNameIdMap.get(mod.getModificationName());
                        mod.setStructuralModId(modId);
                        mod.setPeptideId(peptide.getId());
                        Table.insert(_user, TargetedMSManager.getTableInfoPeptideStructuralModification(), mod);
                    }

                    for(Peptide.IsotopeModification mod: peptide.getIsotopeMods())
                    {
                        int modId = isotopeModNameIdMap.get(mod.getModificationName());
                        mod.setIsotopeModId(modId);
                        mod.setPeptideId(peptide.getId());
                        mod.setIsotopeLabelId(isotopeLabelIdMap.get(mod.getIsotopeLabel()));
                        Table.insert(_user, TargetedMSManager.getTableInfoPeptideIsotopeModification(), mod);
                    }

                    Map<Integer, Integer> sampleFileIdPeptideChromInfoIdMap = new HashMap<Integer, Integer>();

                    for(PeptideChromInfo peptideChromInfo: peptide.getPeptideChromInfoList())
                    {
                        int sampleFileId = skylineIdSampleFileIdMap.get(peptideChromInfo.getSkylineSampleFileId());
                        peptideChromInfo.setPeptideId(peptide.getId());
                        peptideChromInfo.setSampleFileId(sampleFileId);
                        peptideChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPeptideChromInfo(), peptideChromInfo);

                        sampleFileIdPeptideChromInfoIdMap.put(sampleFileId, peptideChromInfo.getId());
                    }

                    // 3. precursor
                    for(Precursor precursor: peptide.getPrecursorList())
                    {
                        precursor.setPeptideId(peptide.getId());
                        precursor.setIsotopeLabelId(isotopeLabelIdMap.get(precursor.getIsotopeLabel()));

                        precursor = Table.insert(_user, TargetedMSManager.getTableInfoPrecursor(), precursor);

                        Map<Integer, Integer> sampleFileIdPrecursorChromInfoIdMap = new HashMap<Integer, Integer>();

                        Precursor.LibraryInfo libInfo = precursor.getLibraryInfo();
                        if(libInfo != null)
                        {
                            libInfo.setPrecursorId(precursor.getId());
                            libInfo.setSpectrumLibraryId(libraryNameIdMap.get(libInfo.getLibraryName()));
                            Table.insert(_user, TargetedMSManager.getTableInfoPrecursorLibInfo(), libInfo);
                        }

                        for (PrecursorChromInfo precursorChromInfo: precursor.getChromInfoList())
                        {
                            int sampleFileId = skylineIdSampleFileIdMap.get(precursorChromInfo.getSkylineSampleFileId());
                            precursorChromInfo.setPrecursorId(precursor.getId());
                            precursorChromInfo.setSampleFileId(sampleFileId);
                            precursorChromInfo.setPeptideChromInfoId(sampleFileIdPeptideChromInfoIdMap.get(sampleFileId));

                            precursorChromInfo = Table.insert(_user, TargetedMSManager.getTableInfoPrecursorChromInfo(), precursorChromInfo);
                            sampleFileIdPrecursorChromInfoIdMap.put(sampleFileId, precursorChromInfo.getId());
                        }

                        // 4. transition
                        for(Transition transition: precursor.getTransitionList())
                        {
                            transition.setPrecursorId(precursor.getId());
                            Table.insert(_user, TargetedMSManager.getTableInfoTransition(), transition);

                            // transition results
                            for(TransitionChromInfo transChromInfo: transition.getChromInfoList())
                            {
                                int sampleFileId = skylineIdSampleFileIdMap.get(transChromInfo.getSkylineSampleFileId());
                                transChromInfo.setTransitionId(transition.getId());
                                transChromInfo.setSampleFileId(sampleFileId);
                                transChromInfo.setPrecursorChromInfoId(sampleFileIdPrecursorChromInfoIdMap.get(sampleFileId));
                                Table.insert(_user, TargetedMSManager.getTableInfoTransitionChromInfo(), transChromInfo);
                            }
                        }
                    }
                }
            }

            run.setPeptideGroupCount(parser.getPeptideGroupCount());
            run.setPeptideCount(parser.getPeptideCount());
            run.setPrecursorCount(parser.getPrecursorCount());
            run.setTransitionCount(parser.getTransitionCount());
            Table.update(_user, TargetedMSManager.getTableInfoRuns(), run, run.getId());
            TargetedMSManager.getSchema().getScope().commitTransaction();
        }
        finally
        {
            TargetedMSManager.getSchema().getScope().closeConnection();
            if (parser != null)
                parser.close();
            // TODO: We are not deleting the directory so that we can query any Bibliospec spectrum library
            //       files contained in the zip file.
            //       Think about adding tables either in the TargetedMS schema or another LabKey schema
            //       to store spectrum libraries.
            // If we uncompressed the contents of a zip file to a directory, delete the directory.
//            if(zipDir != null)
//            {
//                FileUtil.deleteDir(zipDir);
//            }
        }
    }

    protected void updateRunStatus(String status)
    {
        // Default statusId = running
        updateRunStatus(status, STATUS_RUNNING);
    }

    protected void updateRunStatus(String status, int statusId)
    {
        updateRunStatus(_runId, status, statusId);
    }

    protected static void updateRunStatus(int runId, String status, int statusId)
    {
        try
        {
            Table.execute(TargetedMSManager.getSchema(), "UPDATE " + TargetedMSManager.getTableInfoRuns() + " SET Status = ?, StatusId = ? WHERE Id = ?",
                    status, statusId, runId);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private static final Object _schemaLock = new Object();

    public static class RunInfo implements Serializable
    {
        private final int _runId;
        private final boolean _alreadyImported;

        private RunInfo(int runId, boolean alreadyImported)
        {
            _runId = runId;

            _alreadyImported = alreadyImported;
        }

        public int getRunId()
        {
            return _runId;
        }

        public boolean isAlreadyImported()
        {
            return _alreadyImported;
        }
    }

    protected RunInfo prepareRun(boolean restart) throws SQLException
    {
        try
        {
            boolean alreadyImported = false;
            TargetedMSManager.getSchema().getScope().ensureTransaction();

            synchronized (_schemaLock)
            {
                // Don't import if we've already imported this file (undeleted run exists matching this file name)
                _runId = getRun();
                if (_runId != -1)
                {
                    if (!restart)
                    {
                        alreadyImported = true;
                    }
                    else
                    {
                        _log.info("Restarting import from " + _fileName);
                    }
                }
                else
                {
                    _log.info("Starting import from " + _fileName);
                    _runId = createRun();
                }
            }

            TargetedMSManager.getSchema().getScope().commitTransaction();
            return new RunInfo(_runId, alreadyImported);
        }
        finally
        {
            TargetedMSManager.getSchema().getScope().closeConnection();
        }
    }

    protected int getRun() throws SQLException
    {
        SimpleFilter filter = new SimpleFilter("Path", _path);
        filter.addCondition("FileName", _fileName);
        filter.addCondition("Container", _container.getId());
        filter.addCondition("Deleted", Boolean.FALSE);
        ResultSet rs = Table.select(TargetedMSManager.getTableInfoRuns(), new CsvSet("Id,Path,FileName,Container,Deleted"), filter, null);

        int runId = -1;

        if (rs.next())
            runId = rs.getInt("Id");

        rs.close();
        return runId;
    }

    protected int createRun() throws SQLException
    {
        HashMap<String, Object> runMap = new HashMap<String, Object>();

        TargetedMSRun run = TargetedMSManager.getRunByFileName(_path, _fileName, _container);
        if (run != null)
        {
            throw new IllegalStateException("There is already a run for " + _path + "/" + _fileName + " in " + _container.getPath());
        }

        runMap.put("Description", _description);
        runMap.put("Container", _container.getId());
        runMap.put("Path", _path);
        runMap.put("FileName", _fileName);
        runMap.put("Status", IMPORT_STARTED);

        Map returnMap = Table.insert(_user, TargetedMSManager.getTableInfoRuns(), runMap);
        return (Integer)returnMap.get("Id");
    }

    private void close()
    {
        // TODO: close connection and prepared statements used for bulk inserts
//        if (null != _conn)
//            TargetedMSManager.getSchema().getScope().releaseConnection(_conn);
    }

    protected void logError(String message, Exception e)
    {
        _systemLog.error(message, e);
        _log.error(message, e);
    }
}
