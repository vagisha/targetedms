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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DeferredUpgrade;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.ContextListener;
import org.labkey.api.util.StartupListener;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.pipeline.AreaProportionRecalcJob;

import javax.servlet.ServletContext;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * User: jeckels
 * Date: 5/13/13
 */
public class TargetedMSUpgradeCode implements UpgradeCode
{
    private static final Logger LOG = LogManager.getLogger(TargetedMSUpgradeCode.class);

    // called at every bootstrap to initialize annotation types
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

    @DeferredUpgrade
    public void recalculateAreaProportions(final ModuleContext moduleContext)
    {
        if (!moduleContext.isNewInstall())
        {
            // Wait until post-startup because the Enterprise pipeline won't be initialized until it's done on servers
            // that are using the JMS queue
            ContextListener.addStartupListener(new StartupListener()
            {
                @Override
                public String getName()
                {
                    return "AreaProportionRecalcJob submitter";
                }

                @Override
                public void moduleStartupComplete(ServletContext servletContext)
                {
                    try
                    {
                        LOG.info("Module startup complete, queuing AreaProportionRecalcJob");
                        ViewBackgroundInfo info = new ViewBackgroundInfo(ContainerManager.getRoot(), moduleContext.getUpgradeUser(), null);
                        PipeRoot root = PipelineService.get().findPipelineRoot(ContainerManager.getRoot());
                        PipelineService.get().queueJob(new AreaProportionRecalcJob(info, root));
                    }
                    catch (PipelineValidationException e)
                    {
                        throw UnexpectedException.wrap(e);
                    }
                }
            });
        }
    }
}
