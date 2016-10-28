/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.targetedms.model.ExperimentAnnotations;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.SkylineDocumentParser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    // Called at 14.23->14.24
    @SuppressWarnings({"UnusedDeclaration"})
    public void updateExperimentAnnotations(final ModuleContext moduleContext)
    {
        try (DbScope.Transaction transaction = CoreSchema.getInstance().getSchema().getScope().ensureTransaction())
        {
            // Get a list of all the entries in targetedms.ExperimentAnnotations
            List<ExperimentAnnotations> expAnnotations = new TableSelector(TargetedMSManager.getTableInfoExperimentAnnotations()).getArrayList(ExperimentAnnotations.class);

            for(ExperimentAnnotations expAnnot: expAnnotations)
            {
                // Create an entry in exp.experiment
                ExpExperiment experiment = ExperimentService.get().createExpExperiment(expAnnot.getContainer(),expAnnot.getTitle());
                ensureUniqueLSID(experiment);
                experiment.save(UserManager.getUser(expAnnot.getCreatedBy()));
                // Save the rowId
                expAnnot.setExperimentId(experiment.getRowId());
                Table.update(null, TargetedMSManager.getTableInfoExperimentAnnotations(), expAnnot, expAnnot.getId());
            }

            transaction.commit();
        }
    }

    private void ensureUniqueLSID(ExpExperiment experiment)
    {
        String lsid;
        int suffix = 1;
        String name = experiment.getName();
        do
        {
            if(suffix > 1)
            {
                name = experiment.getName() + "_" + suffix;
            }
            suffix++;
            lsid = ExperimentService.get().generateLSID(experiment.getContainer(), ExpExperiment.class, name);
        }
        while (ExperimentService.get().getExpExperiment(lsid) != null);
        experiment.setLSID(lsid);
    }

    // called at 14.30->14.31
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

    // Called at 16.14->16.15 (only on SQLServer)
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
}
