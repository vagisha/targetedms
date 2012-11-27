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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.ProteinService;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.EnumConverter;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.JspView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.pipeline.TargetedMSPipelineProvider;
import org.labkey.targetedms.view.TransitionPeptideSearchViewProvider;
import org.labkey.targetedms.view.TransitionProteinSearchViewProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TargetedMSModule extends SpringModule
{
    public static final String NAME = "TargetedMS";

    // Protocol prefix for importing .sky documents from Skyline
    public static final String IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSky";
    // Protocol prefix for importing .zip archives from Skyline
    public static final String IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSkyZip";

    public static final ExperimentRunType EXP_RUN_TYPE = new TargetedMSExperimentRunType();
    public static final String TARGETED_MS_RUNS_WEBPART_NAME = "Targeted MS Runs";
    public static final String TARGETED_MS_PROTEIN_SEARCH = "Targeted MS Protein Search";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return 12.30;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory runsFactory = new BaseWebPartFactory(TARGETED_MS_RUNS_WEBPART_NAME)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                QueryView gridView = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), EXP_RUN_TYPE);
                gridView.setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
                gridView.setTitleHref(new ActionURL(TargetedMSController.ShowListAction.class, portalCtx.getContainer()));
                VBox vbox = new VBox();
                vbox.addView(new JspView("/org/labkey/targetedms/view/conflictSummary.jsp"));
                vbox.addView(gridView);
                return vbox;
            }
        };

        BaseWebPartFactory proteinSearchFactory = new BaseWebPartFactory(TARGETED_MS_PROTEIN_SEARCH)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                    JspView view = new JspView("/org/labkey/targetedms/view/proteinSearch.jsp");
                view.setTitle("Protein Search");
                return view;
            }
        };

        List<WebPartFactory> webpartFactoryList = new ArrayList<WebPartFactory>(1);
        webpartFactoryList.add(runsFactory);
        webpartFactoryList.add(proteinSearchFactory);
        return webpartFactoryList;
    }

    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(TargetedMSManager.get().getSchemaName());
    }

    @Override
    @NotNull
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(TargetedMSManager.getSchema());
    }

    @Override
    protected void init()
    {
        addController("targetedms", TargetedMSController.class);
        TargetedMSSchema.register();
        EnumConverter.registerEnum(TargetedMSRun.RepresentativeDataState.class);
        EnumConverter.registerEnum(RepresentativeDataState.class);
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new TargetedMSPipelineProvider(this));

        ExperimentService.get().registerExperimentDataHandler(new TargetedMSDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new SkylineBinaryDataHandler());

        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            public Set<ExperimentRunType> getExperimentRunTypes(Container container)
            {
                if (container.getActiveModules().contains(TargetedMSModule.this))
                {
                    return Collections.singleton(EXP_RUN_TYPE);
                }
                return Collections.emptySet();
            }
        });

        //register the Targeted MS folder type
        ModuleLoader.getInstance().registerFolderType(this, new TargetedMSFolderType(this));

        ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
        proteinService.registerProteinSearchView(new TransitionProteinSearchViewProvider());
        proteinService.registerPeptideSearchView(new TransitionPeptideSearchViewProvider());
    }
}