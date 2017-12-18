/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.targetedms.model.ExperimentAnnotations;
import org.labkey.targetedms.model.JournalExperiment;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.SkylineDocumentParser;
import org.labkey.targetedms.query.ExperimentAnnotationsManager;

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

    // Called at 16.10-16.20 (only on SQLServer)
    @SuppressWarnings({"UnusedDeclaration"})
    public void updatePrecursorModifiedSequence(final ModuleContext moduleContext) throws SQLException
    {
        DbSchema schema = TargetedMSSchema.getSchema();
        String updateSql = "UPDATE targetedms.precursor SET ModifiedSequence=? WHERE Id=?";
        TableSelector ts = new TableSelector(TargetedMSManager.getTableInfoPrecursor(),
                                             new SimpleFilter(FieldKey.fromParts("ModifiedSequence"), "[", CompareType.CONTAINS), null);
        ts.forEachBatch(batch -> {
            try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
            {
                ArrayList<Collection<Object>> paramList = new ArrayList<>();
                for (Precursor precursor : batch)
                {
                    String originalSequence = precursor.getModifiedSequence();
                    String newSequence = SkylineDocumentParser.ensureDecimalInModMass(originalSequence);
                    if (!originalSequence.equals(newSequence))
                    {
                        List<Object> modSeqPrecursorId = new ArrayList<>();
                        modSeqPrecursorId.add(newSequence);
                        modSeqPrecursorId.add(precursor.getId());
                        paramList.add(modSeqPrecursorId);
                    }
                }
                Table.batchExecute(schema, updateSql, paramList);
                transaction.commit();
            }
        }, Precursor.class, 1000);
    }

    // Called at 17.12-17.13
    @SuppressWarnings({"UnusedDeclaration"})
    public void updateExperimentAnnotations(final ModuleContext moduleContext) throws SQLException
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
    public void addRawDataTab(final ModuleContext moduleContext) throws SQLException
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
}
