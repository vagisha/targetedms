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
package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.UserManager;
import org.labkey.targetedms.model.ExperimentAnnotations;

import java.util.List;

/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    // called at 13.13->13.14
    @SuppressWarnings({"UnusedDeclaration"})
    public void setContainersToExperimentType(final ModuleContext moduleContext)
    {
        setContainersToExperimentType(ContainerManager.getRoot());
    }

    private void setContainersToExperimentType(Container container)
    {
        // Look for all TargetedMS containers. Need to check the folder type name, since the folder type itself hasn't been registered yet
        if (TargetedMSFolderType.NAME.equals(ContainerManager.getFolderTypeName(container)))
        {
            // If we don't already have a subtype specified, assume it's an Experiment folder
            String currentValue = TargetedMSModule.FOLDER_TYPE_PROPERTY.getValueContainerSpecific(container);
            if (currentValue == null || TargetedMSModule.FolderType.Undefined.toString().equalsIgnoreCase(currentValue))
            {
                TargetedMSModule.FOLDER_TYPE_PROPERTY.saveValue(null, container, TargetedMSModule.FolderType.Experiment.toString());
            }
        }

        // Recurse into the child containers
        for (Container child : container.getChildren())
        {
            setContainersToExperimentType(child);
        }
    }

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
}
