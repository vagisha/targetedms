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
package org.labkey.targetedms;

import org.apache.log4j.Logger;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.targetedms.model.ExperimentAnnotations;
import org.labkey.targetedms.model.JournalExperiment;
import org.labkey.targetedms.query.ExperimentAnnotationsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.labkey.targetedms.TargetedMSController.FolderSetupAction.RAW_FILES_TAB;


/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = Logger.getLogger(TargetedMSUpgradeCode.class);

    // called at 14.30-15.10
    @SuppressWarnings({"UnusedDeclaration"})
    public void populateDefaultAnnotationTypes(final ModuleContext moduleContext)
    {
        insertAnnotationType("Instrumentation Change", "FF0000", moduleContext.getUpgradeUser());
        insertAnnotationType("Reagent Change", "00FF00", moduleContext.getUpgradeUser());
        insertAnnotationType("Technician Change", "0000FF", moduleContext.getUpgradeUser());

        // Enable the module in the /Shared container so that it can be resolved
        Set<Module> activeModules = new HashSet<>(ContainerManager.getSharedContainer().getActiveModules());
        activeModules.add(ModuleLoader.getInstance().getModule(TargetedMSModule.class));
        ContainerManager.getSharedContainer().setActiveModules(activeModules);
    }

    private void insertAnnotationType(String name, String color, User user)
    {
        SQLFragment sql = new SQLFragment("INSERT INTO ");
        sql.append(TargetedMSManager.getTableInfoQCAnnotationType());
        sql.append(" (Container, Created, Modified, CreatedBy, ModifiedBy, Name, Color) VALUES (?, ?, ?, ?, ?, ?, ?)");
        sql.add(ContainerManager.getSharedContainer());
        Date now = new Date();
        sql.add(now);
        sql.add(now);
        sql.add(user.getUserId());
        sql.add(user.getUserId());
        sql.add(name);
        sql.add(color);
        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    // Called at 17.10-17.20
    @SuppressWarnings({"UnusedDeclaration"})
    public void updateExperimentAnnotations(final ModuleContext moduleContext)
    {
        // Populate the sourceExperimentId, sourceExperimentPath and shortUrl columns that were just added.
        // This will be done only for experiments are are journal copies (journalCopy = true).
        DbSchema schema = TargetedMSSchema.getSchema();
        String updateSql = "UPDATE targetedms.ExperimentAnnotations SET sourceExperimentId=?, sourceExperimentPath=?, shortUrl=? WHERE Id=?";
        String updateShortUrlSql = "UPDATE targetedms.ExperimentAnnotations SET shortUrl=? WHERE Id=?";

        TableSelector ts = new TableSelector(TargetedMSManager.getTableInfoExperimentAnnotations(),
                new SimpleFilter(FieldKey.fromParts("journalCopy"), true, CompareType.EQUAL), null);
        ts.forEachBatch(batch -> {
            try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
            {
                ArrayList<Collection<Object>> paramList = new ArrayList<>();
                ArrayList<Collection<Object>> paramList_shortUrlOnly = new ArrayList<>();

                for (ExperimentAnnotations expAnnotations : batch)
                {
                    Container container = expAnnotations.getContainer();
                    ActionURL url = PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(container);
                    // Get any existing short URLs for this container
                    List<ShortURLRecord> shortUrls =  new TableSelector(CoreSchema.getInstance().getTableInfoShortURL(),
                                                                 new SimpleFilter(FieldKey.fromParts("fullurl"), url.toString()),
                                                            null).getArrayList(ShortURLRecord.class);

                    boolean found = false;
                    for(ShortURLRecord shortUrl: shortUrls)
                    {
                        JournalExperiment je = getRecordForShortAccessUrl(shortUrl);
                        if (je != null && je.getCopied() != null)
                        {
                            ExperimentAnnotations sourceExperiment = ExperimentAnnotationsManager.get(je.getExperimentAnnotationsId());
                            if(sourceExperiment != null)
                            {
                                List<Object> params = new ArrayList<>();
                                params.add(sourceExperiment.getId()); // sourceExperimentId
                                params.add(sourceExperiment.getContainer().getPath()); // sourceExperimentPath
                                params.add(shortUrl.getEntityId()); // shortUrl
                                params.add(expAnnotations.getId());

                                paramList.add(params);
                                found = true;
                                break;
                            }
                        }
                    }
                    if(shortUrls.size() == 1 && !found)
                    {
                        // We did not find a matching record in JournalExperiment for any of the short URLs
                        // On panoramaweb.org this means that the source experiment was deleted (along with
                        // the corresponding record in JournalExperiment)
                        // Set the shortURL only in this case
                        List<Object> params = new ArrayList<>();
                        params.add(shortUrls.get(0).getEntityId()); // shortUrl
                        params.add(expAnnotations.getId());

                        paramList_shortUrlOnly.add(params);
                    }
                }
                Table.batchExecute(schema, updateSql, paramList);
                if(paramList_shortUrlOnly.size() > 0)
                {
                    Table.batchExecute(schema, updateShortUrlSql, paramList_shortUrlOnly);
                }
                transaction.commit();
            }
        }, ExperimentAnnotations.class, 1000);
    }

    public JournalExperiment getRecordForShortAccessUrl(ShortURLRecord shortUrl)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("shortAccessUrl"), shortUrl);
        return new TableSelector(TargetedMSManager.getTableInfoJournalExperiment(), filter, null).getObject(JournalExperiment.class);
    }

    // Called at 17.30-17.31
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void addRawDataTab(final ModuleContext moduleContext)
    {
        if(moduleContext.isNewInstall())
        {
            // This is a new install.  There are no "targetedms" folders.
            return;
        }

        Set<Container> containers = ContainerManager.getAllChildrenWithModule(ContainerManager.getRoot(), ModuleLoader.getInstance().getModule(TargetedMSModule.class));

        for(Container container: containers)
        {
            if(Portal.getParts(container, RAW_FILES_TAB).size() != 0)
            {
                continue;
            }

            TargetedMSController.addRawFilesPipelineTab(container);
        }
    }

    // Called at 18.21 - 18.22
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void updateExpSkydData(final ModuleContext moduleContext)
    {
        String updateRunIdSql = "UPDATE exp.data SET runId=? WHERE rowId=?";;
        String updateSql = "UPDATE exp.data SET runId=?, container=?, dataFileUrl=? WHERE rowId=?";

        ExperimentService expService = ExperimentService.get();
        DbSchema expSchema = expService.getSchema();

        TableSelector ts = new TableSelector(TargetedMSManager.getTableInfoRuns(), new SimpleFilter(FieldKey.fromParts("SkydDataId"), null, CompareType.NONBLANK), null);
        ts.forEachBatch(batch -> {
            try (DbScope.Transaction transaction = expSchema.getScope().ensureTransaction())
            {
                ArrayList<Collection<Object>> runIdParamList = new ArrayList<>();
                ArrayList<Collection<Object>> paramList = new ArrayList<>();

                for (TargetedMSRun run : batch)
                {
                    Integer skydDataId = run.getSkydDataId();

                    ExpRun expRun = expService.getExpRun(run.getExperimentRunLSID());
                    ExpData expData = expService.getExpData(run.getSkydDataId());
                    if(expRun != null && expData != null)
                    {
                        List<Object> params = new ArrayList<>();
                        params.add(expRun.getRowId()); // runId

                        if(run.getContainer().equals(expData.getContainer()))
                        {
                            params.add(expData.getRowId());
                            runIdParamList.add(params);
                        }
                        else
                        {
                            // Run has been moved from the original container.  Update container and datafileurl in exp.data
                            params.add(run.getContainer()); // container

                            PipeRoot targetRoot = PipelineService.get().findPipelineRoot(run.getContainer());
                            if(targetRoot != null)
                            {
                                PipeRoot sourceRoot = PipelineService.get().findPipelineRoot(expData.getContainer());
                                if(sourceRoot != null)
                                {
                                    // Issue 35812: TargetedMS upgrade code throws exception if run has been moved from and container whose file root is S3
                                    // Instead of Paths.get() use PipeRoot.resolveToNioPathFromUrl(URL) or PipeRoot.resolveToNioPath(String), because they know how to use the cloud module for S3 paths
                                    Path sourceFilePath = sourceRoot.resolveToNioPathFromUrl(expData.getDataFileUrl());
                                    if (sourceFilePath != null)
                                    {
                                        Path destFile = targetRoot.getRootNioPath().resolve(sourceFilePath.getParent().getFileName()) // Name of the exploded directory
                                                                                   .resolve(sourceFilePath.getFileName());            // Name of the .skyd file
                                        if (Files.exists(destFile))
                                        {
                                            params.add(destFile.toUri().toString()); // dataFileUrl
                                        }
                                        else
                                        {
                                            params.add(expData.getDataFileUrl());
                                            LOG.warn("Target destination file " + destFile.toString() + " does not exist for exp.data rowId " + expData.getRowId());
                                        }
                                    }
                                    else
                                    {
                                        LOG.warn("Cannot resolve dataFileUrl \"" + expData.getDataFileUrl() + "\" for exp.data rowId " + expData.getRowId());
                                    }
                                }
                                else
                                {
                                    LOG.warn("Could not get pipeline root for source container " + expData.getContainer().getPath() +", exp.data rowId " + expData.getRowId());
                                }
                            }
                            else
                            {
                                LOG.warn("Could not get pipeline root for target container " + run.getContainer().getPath() +", targetedms runId " + run.getId());
                            }

                            params.add(expData.getRowId());
                            paramList.add(params);
                        }
                    }
                }
                if(paramList.size() > 0)
                {
                    Table.batchExecute(expSchema, updateSql, paramList);
                }
                if(runIdParamList.size() > 0)
                {
                    Table.batchExecute(expSchema, updateRunIdSql, runIdParamList);
                }

                transaction.commit();
            }
        }, TargetedMSRun.class, 1000);

    }

    // Called at 18.31 - 18.32
    @SuppressWarnings({"UnusedDeclaration"})
    @DeferredUpgrade
    public void addDocumentSize(final ModuleContext moduleContext)
    {
        TableSelector ts = new TableSelector(TargetedMSManager.getTableInfoRuns(), new SimpleFilter(FieldKey.fromParts("status"), null, CompareType.ISBLANK), null);

        final long documentCount = ts.getRowCount();

        LOG.info("Updating targetedms.DocumentSize for " + documentCount + " rows");
        AddDocumentSizeBatchBlock batch = new AddDocumentSizeBatchBlock(TargetedMSSchema.getSchema(), documentCount);
        ts.forEachBatch(batch, TargetedMSRun.class, 500);

        if(batch.getErrorCount() > 0)
        {
            LOG.warn("Could not update targetedms.DocumentSize for " + batch.getErrorCount() + " rows.");
        }
    }

    private class AddDocumentSizeBatchBlock implements Selector.ForEachBatchBlock<TargetedMSRun>
    {
        private final String updateSql = "UPDATE targetedms.runs SET DocumentSize = ? WHERE Id = ?";
        private final DbSchema _schema;
        private final long _totalCount;
        private long _count = 0;
        private long _errorCount = 0;

        public AddDocumentSizeBatchBlock(DbSchema schema, long totalCount)
        {
            _schema = schema;
            _totalCount = totalCount;
        }

        @Override
        public void exec(List<TargetedMSRun> batch) throws SQLException
        {
            try (DbScope.Transaction transaction = _schema.getScope().ensureTransaction())
            {
                ArrayList<Collection<Object>> paramList = new ArrayList<>();

                for (TargetedMSRun run : batch)
                {
                    Path skyDocFile = SkylineFileUtils.getSkylineFile(run.getExperimentRunLSID(), run.getContainer());
                    if (skyDocFile != null && Files.exists(skyDocFile) && !Files.isDirectory(skyDocFile))
                    {
                        try
                        {
                            List<Object> params = new ArrayList<>();
                            params.add(Files.size(skyDocFile));
                            params.add(run.getId());
                            paramList.add(params);
                            _count++;

                            if (_count % 100 == 0)
                            {
                                LOG.info(String.format("Processing targetedms.DocumentSize for %d / %d rows.", _count, _totalCount));
                            }
                        }
                        catch (IOException e)
                        {
                            LOG.warn("Could not get size of Skyline document: " + skyDocFile.toAbsolutePath(), e);
                            _errorCount++;
                        }
                    }
                    else
                    {
                        _errorCount++;
                    }
                }
                if(paramList.size() > 0)
                {
                    Table.batchExecute(_schema, updateSql, paramList);
                }
                transaction.commit();
                LOG.info(String.format("UPDATED targetedms.DocumentSize for %d / %d rows.", _count, _totalCount));
            }
        }

        long getErrorCount()
        {
            return _errorCount;
        }
    }
}
