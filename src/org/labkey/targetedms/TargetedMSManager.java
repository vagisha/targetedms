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
import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.AbstractFileXarSource;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarSource;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.User;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.pipeline.TargetedMSImportPipelineJob;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TargetedMSManager
{
    private static final TargetedMSManager _instance = new TargetedMSManager();

    private static Logger _log = Logger.getLogger(TargetedMSManager.class);

    private TargetedMSManager()
    {
        // prevent external construction with a private default constructor
    }

    public static TargetedMSManager get()
    {
        return _instance;
    }

    public String getSchemaName()
    {
        return TargetedMSSchema.SCHEMA_NAME;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(TargetedMSSchema.SCHEMA_NAME);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoRuns()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUNS);
    }

    public static TableInfo getTableInfoTransInstrumentSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_INSTRUMENT_SETTINGS);
    }

    public static TableInfo getTableInfoPredictor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR);
    }

    public static TableInfo getTableInfoPredictorSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR_SETTINGS);
    }

    public static TableInfo getTableInfoReplicate()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE);
    }

    public static TableInfo getTableInfoSampleFile()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE);
    }

    public static TableInfo getTableInfoTransitionChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO);
    }

    public static TableInfo getTableInfoTransitionAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_AREA_RATIO);
    }

    public static TableInfo getTableInfoPeptideGroup()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
    }

    public static TableInfo getTableInfoPeptideGroupAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP_ANNOTATION);
    }

    public static TableInfo getTableInfoPeptide()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE);
    }

    public static TableInfo getTableInfoPeptideAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_ANNOTATION);
    }

    public static TableInfo getTableInfoProtein()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PROTEIN);
    }

    public static TableInfo getTableInfoPrecursor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR);
    }

    public static TableInfo getTableInfoPrecursorAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_ANNOTATION);
    }

    public static TableInfo getTableInfoPrecursorChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO);
    }

    public static TableInfo getTableInfoPrecursorAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_AREA_RATIO);
    }

    public static TableInfo getTableInfoPrecursorChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoPeptideChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_CHROM_INFO);
    }

    public static TableInfo getTableInfoPeptideAreaRatio()
    {
       return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_AREA_RATIO);
    }

    public static TableInfo getTableInfoInstrument()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_INSTRUMENT);
    }

    public static TableInfo getTableInfoIsotopeEnrichment()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_ENRICHMENT);
    }

    public static TableInfo getTableInfoRetentionTimePredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RETENTION_TIME_PREDICTION_SETTINGS);
    }

    public static TableInfo getTableInfoTransitionPredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_PREDICITION_SETTINGS);
    }

    public static TableInfo getTableInfoTransitionFullScanSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_FULL_SCAN_SETTINGS);
    }

    public static TableInfo getTableInfoTransition()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION);
    }

    public static TableInfo getTableInfoTransitionLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_LOSS);
    }

    public static TableInfo getTableInfoModificationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MODIFICATION_SETTINGS);
    }

    public static TableInfo getTableInfoPeptideStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoPeptideIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoIsotopeLabel()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_LABEL);
    }

    public static TableInfo getTableInfoIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MOD_LOSS);
    }

    public static TableInfo getTableInfoRunIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoRunEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ENZYME);
    }

    public static TableInfo getTableInfoRunStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoTransitionAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionOptimization()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_OPTIMIZATION);
    }

    public static TableInfo getTableInfoLibrarySettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SETTINGS);
    }

    public static TableInfo getTableInfoSpectrumLibrary()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SPECTRUM_LIBRARY);
    }

    public static TableInfo getTableInfoEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ENZYME);
    }

    public static TableInfo getTableInfoLibrarySource()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SOURCE);
    }

    public static TableInfo getTableInfoPrecursorLibInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_LIB_INFO);
    }

    public static int addRunToQueue(ViewBackgroundInfo info,
                                     File file,
                                     PipeRoot root, boolean representative) throws SQLException, IOException
    {
        String description = "Skyline document import - " + file.getName();
        XarContext xarContext = new XarContext(description, info.getContainer(), info.getUser());
        User user =  info.getUser();
        Container container = info.getContainer();
        SkylineDocImporter importer = new SkylineDocImporter(user, container, file.getName(), file, null, xarContext, representative);
        SkylineDocImporter.RunInfo runInfo = importer.prepareRun(false);
        TargetedMSImportPipelineJob job = new TargetedMSImportPipelineJob(info, file, runInfo, root, representative);
        try
        {
            PipelineService.get().queueJob(job);
            return PipelineService.get().getJobId(user, container, job.getJobGUID());
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
    }

    public static ExpRun ensureWrapped(TargetedMSRun run, User user) throws ExperimentException
    {
        ExpRun expRun;
        if (run.getExperimentRunLSID() != null)
        {
            expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun != null && expRun.getContainer().equals(run.getContainer()))
            {
                return expRun;
            }
        }
        return wrapRun(run, user);
    }

    private static ExpRun wrapRun(TargetedMSRun run, User user) throws ExperimentException
    {
        try
        {
            ExperimentService.get().getSchema().getScope().ensureTransaction();

            Container container = run.getContainer();

            // Make sure that we have a protocol in this folder
            String protocolPrefix = run.isZipFile() ? TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX :
                                                      TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX;

            Lsid lsid = new Lsid("Protocol.Folder-" + container.getRowId(), protocolPrefix);
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(lsid.toString());
            if (protocol == null)
            {
                protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ProtocolApplication, "Skyline Document Import", lsid.toString());
                protocol.setMaxInputMaterialPerInstance(0);
                protocol = ExperimentService.get().insertSimpleProtocol(protocol, user);
            }

            ExpRun expRun = ExperimentService.get().createExperimentRun(container, run.getDescription());
            expRun.setProtocol(protocol);

            final File skylineFile = new File(run.getPath(), run.getFileName());
            expRun.setFilePathRoot(skylineFile.getParentFile());
            ViewBackgroundInfo info = new ViewBackgroundInfo(container, user, null);

            Map<ExpData, String> inputDatas = new HashMap<ExpData, String>();
            Map<ExpData, String> outputDatas = new HashMap<ExpData, String>();
            XarSource source = new AbstractFileXarSource("Wrap Targeted MS Run", container, user)
            {
                public File getLogFile() throws IOException
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public File getRoot()
                {
                    return skylineFile.getParentFile();
                }

                @Override
                public ExperimentArchiveDocument getDocument() throws XmlException, IOException
                {
                    throw new UnsupportedOperationException();
                }
            };

            ExpData skylineData = ExperimentService.get().getExpDataByURL(skylineFile, container);
            if (skylineData == null)
            {
                skylineData = ExperimentService.get().createData(skylineFile.toURI(), source);
            }
            outputDatas.put(skylineData, "sky");

            expRun = ExperimentService.get().saveSimpleExperimentRun(expRun,
                                                                     Collections.<ExpMaterial, String>emptyMap(),
                                                                     inputDatas,
                                                                     Collections.<ExpMaterial, String>emptyMap(),
                                                                     outputDatas,
                                                                     Collections.<ExpData, String>emptyMap(),
                                                                     info, _log, false);

            run.setExperimentRunLSID(expRun.getLSID());
            TargetedMSManager.updateRun(run, user);

            ExperimentService.get().getSchema().getScope().commitTransaction();
            return expRun;
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
//        catch (URISyntaxException e)
//        {
//            throw new ExperimentException(e);
//        }
        finally
        {
            ExperimentService.get().getSchema().getScope().closeConnection();
        }
    }

    public static TargetedMSRun getRunByFileName(String path, String fileName, Container c)
    {
        TargetedMSRun[] runs = getRuns("LOWER(Path) = LOWER(?) AND LOWER(FileName) = LOWER(?) AND Deleted = ? AND Container = ?", path, fileName, Boolean.FALSE, c.getId());
        if (null == runs || runs.length == 0)
        {
            return null;
        }
        if (runs.length == 1)
        {
            return runs[0];

        }
        throw new IllegalStateException("There is more than one non-deleted Targeted MS Run for " + path + "/" + fileName);
    }

    private static TargetedMSRun[] getRuns(String whereClause, Object... params)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append(whereClause);
        sql.addAll(params);
        return new SqlSelector(getSchema(), sql).getArray(TargetedMSRun.class);
    }

    public static TargetedMSRun getRun(int runId)
    {
        TargetedMSRun run = null;

        TargetedMSRun[] runs = getRuns("Id = ? AND deleted = ?", runId, false);

        if (runs != null && runs.length == 1)
        {
            run = runs[0];
        }

        return run;
    }

    public static void updateRun(TargetedMSRun run, User user) throws SQLException
    {
        Table.update(user, getTableInfoRuns(), run, run.getRunId());
    }

    // For safety, simply mark runs as deleted.  This allows them to be (manually) restored.
    // TODO: Do we really want to hang on to the data of a deleted run?
    public static void markAsDeleted(List<Integer> runIds, Container c, User user)
    {
        if (runIds.isEmpty())
            return;

        // Save these to delete after we've deleted the runs
        List<ExpRun> experimentRunsToDelete = new ArrayList<ExpRun>();

        for (Integer runId : runIds)
        {
            TargetedMSRun run = getRun(runId);
            if (run != null)
            {
                File file = new File(run.getPath(), run.getFileName());
                ExpData data = ExperimentService.get().getExpDataByURL(file, c);
                if (data != null)
                {
                    ExpRun expRun = data.getRun();
                    if (expRun != null)
                    {
                        experimentRunsToDelete.add(expRun);
                    }
                }
            }
        }

        markDeleted(runIds, c);

        for (ExpRun run : experimentRunsToDelete)
        {
            run.delete(user);
        }
    }

    public static void markAsDeleted(Container c, User user)
    {
        try
        {
            Integer[] runIds = Table.executeArray(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Container=?", new Object[]{c.getId()}, Integer.class);
            markAsDeleted(Arrays.asList(runIds), c, user);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    // pulled out into separate method so could be called by itself from data handlers
    public static void markDeleted(List<Integer> runIds, Container c)
    {
        SQLFragment markDeleted = new SQLFragment("UPDATE " + getTableInfoRuns() + " SET ExperimentRunLSID = NULL, Deleted=?, Modified=? ", Boolean.TRUE, new Date());
        SimpleFilter where = new SimpleFilter();
        where.addCondition("Container", c.getId());
        where.addInClause("Id", runIds);
        markDeleted.append(where.getSQLFragment(getSqlDialect()));

        try
        {
            Table.execute(getSchema(), markDeleted);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public static TargetedMSRun getRunForPrecursor(int precursorId)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoPeptide()+" AS pep, "+
                     getTableInfoPrecursor()+" AS pre "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=pep.PeptideGroupId "+
                     "AND pep.Id=pre.PeptideId "+
                     "AND pre.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(precursorId);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for precursor: "+precursorId);
        }
        return run;
    }

    public static TargetedMSRun getRunForPeptide(int peptideId)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoPeptide()+" AS pep "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=pep.PeptideGroupId "+
                     "AND pep.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(peptideId);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for peptide: "+peptideId);
        }
        return run;
    }

    public static boolean runHasProteins(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM ");
        sql.append(getTableInfoPeptideGroup()+" pg ");
        sql.append("WHERE ");
        sql.append("pg.RunId=? ");
        sql.append("AND ");
        sql.append("pg.SequenceID IS NOT NULL");
        sql.add(runId);

        Integer count = new SqlSelector(getSchema(), sql).getObject(Integer.class);

        return count > 0;
    }

    /** Actually delete runs that have been marked as deleted from the database */
    public static void purgeDeletedRuns()
    {
        try
        {
            // Delete from TransitionChromInfoAnnotation
            deleteTransitionChromInfoDependent(getTableInfoTransitionChromInfoAnnotation());
            // Delete from TransitionAreaRatio
            deleteTransitionChromInfoDependent(getTableInfoTransitionAreaRatio());

            // Delete from PrecursorChromInfoAnnotation
            deletePrecursorChromInfoDependent(getTableInfoPrecursorChromInfoAnnotation());
            // Delete from PrecursorAreaRatio
            deletePrecursorChromInfoDependent(getTableInfoPrecursorAreaRatio());

            // Delete from PeptideAreaRatio
            deletePeptideChromInfoDependent(getTableInfoPeptideAreaRatio());

            // Delete from TransitionChromInfo
            deleteTransitionDependent(getTableInfoTransitionChromInfo());
            // Delete from TransitionAnnotation
            deleteTransitionDependent(getTableInfoTransitionAnnotation());
            // Delete from TransitionLoss
            deleteTransitionDependent(getTableInfoTransitionLoss());
            // Delete from TransitionOptimization
            deleteTransitionDependent(getTableInfoTransitionOptimization());

            // Delete from Transition
            deletePrecursorDependent(getTableInfoTransition());
            // Delete from PrecursorChromInfo
            deletePrecursorDependent(getTableInfoPrecursorChromInfo());
            // Delete from PrecursorAnnotation
            deletePrecursorDependent(getTableInfoPrecursorAnnotation());
            // Delete from PrecursorLibInfo
            deletePrecursorDependent(getTableInfoPrecursorLibInfo());


            // Delete from PeptideAnnotation
            deletePeptideDependent(getTableInfoPeptideAnnotation());
            // Delete from Precursor
            deletePeptideDependent(getTableInfoPrecursor());
            // Delete from PeptideChromInfo
            deletePeptideDependent(getTableInfoPeptideChromInfo());
            // Delete from PeptideStructuralModification
            deletePeptideDependent(getTableInfoPeptideStructuralModification());
            // Delete from PeptideIsotopeModification
            deletePeptideDependent(getTableInfoPeptideIsotopeModification());


            // Delete from Peptide
            deletePeptideGroupDependent(getTableInfoPeptide());
            // Delete from Protein
            deletePeptideGroupDependent(getTableInfoProtein());
            // Delete from PeptideGroupAnnotation
            deletePeptideGroupDependent(getTableInfoPeptideGroupAnnotation());


            // Delete from sampleFile
            deleteReplicateDependent(getTableInfoSampleFile());

            // Delete from PredictorSettings and Predictor
            deleteTransitionPredictionSettingsDependent();

            // Delete from PeptideGroup
            deleteRunDependent(getTableInfoPeptideGroup());
            // Delete from Replicate
            deleteRunDependent(getTableInfoReplicate());
            // Delete from TransitionInstrumentSettings
            deleteRunDependent(getTableInfoTransInstrumentSettings());
            // Delete from Instrument
            deleteRunDependent(getTableInfoInstrument());
            // Delete from RetentionTimePredictionSettings
            deleteRunDependent(getTableInfoRetentionTimePredictionSettings());
            // Delete from TransitionPredictionSettings
            deleteRunDependent(getTableInfoTransitionPredictionSettings());
            // Delete from TransitionFullScanSettings
            deleteRunDependent(getTableInfoTransitionFullScanSettings());
            // Delete from IsotopeEnrichment (part of Full Scan settings)
            deleteRunDependent(getTableInfoIsotopeEnrichment());
            // Delete from ModificationSettings
            deleteRunDependent(getTableInfoModificationSettings());
            // Delete from RunStructuralModification
            deleteRunDependent(getTableInfoRunStructuralModification());
            // Delete from RunIsotopeModification
            deleteRunDependent(getTableInfoRunIsotopeModification());
            // Delete from IsotopeLabel
            deleteRunDependent(getTableInfoIsotopeLabel());
            // Delete from LibrarySettings
            deleteRunDependent(getTableInfoLibrarySettings());
            // Delete from SpectrumLibrary
            deleteRunDependent(getTableInfoSpectrumLibrary());
            // Delete from RunEnzyme
            deleteRunDependent(getTableInfoRunEnzyme());


            // Delete from runs
            Table.execute(getSchema(), "DELETE FROM " + getTableInfoRuns() + " WHERE Deleted = ?", true);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public static void deleteTransitionChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
         Table.execute(getSchema(), "DELETE FROM " + tableInfo +
                    " WHERE TransitionChromInfoId IN (SELECT Id FROM " +
                    getTableInfoTransitionChromInfo() + " WHERE TransitionId IN (SELECT Id FROM " +
                    getTableInfoTransition() + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))))))", true);
    }

    public static void deletePrecursorChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PrecursorChromInfoId IN (SELECT Id FROM "
                + getTableInfoPrecursorChromInfo() + " WHERE PrecursorId IN (SELECT Id FROM "
                + getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM "
                + getTableInfoPeptide() + " WHERE " +
                "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                getTableInfoRuns() + " WHERE Deleted = ?)))))", true);
    }

    public static void deletePeptideChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PeptideChromInfoId IN (SELECT Id FROM "
                + getTableInfoPeptideChromInfo() + " WHERE PeptideId IN (SELECT Id FROM "
                + getTableInfoPeptide() + " WHERE " +
                "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                getTableInfoRuns() + " WHERE Deleted = ?))))", true);
    }

    public static void deleteTransitionDependent(TableInfo tableInfo) throws SQLException
    {
         Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE TransitionId IN (SELECT Id FROM " +
                    getTableInfoTransition() + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)))))", true);
    }

    private static void deletePrecursorDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))))", true);
    }

    private static void deletePeptideDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PeptideId IN (SELECT Id FROM "
                    + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)))", true);
    }

    private static void deletePeptideGroupDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteRunDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)", true);
    }

    private static void deleteReplicateDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo+ " WHERE ReplicateId IN (SELECT Id FROM " +
                    getTableInfoReplicate() + " WHERE RunId IN (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteTransitionPredictionSettingsDependent() throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPredictorSettings() + " WHERE PredictorId IN (SELECT Id FROM " +
                    getTableInfoPredictor() + " WHERE " +
                        "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                        "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?))"
                , true, true);

        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPredictor() + " WHERE " +
                        "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                        "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)"
                , true, true);
    }
}