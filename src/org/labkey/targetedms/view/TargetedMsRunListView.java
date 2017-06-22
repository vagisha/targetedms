/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentRunListView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.ExperimentAnnotations;

import static org.labkey.targetedms.TargetedMSController.getExcludeSubfoldersInExperimentURL;
import static org.labkey.targetedms.TargetedMSController.getIncludeSubfoldersInExperimentURL;

/**
 * User: vsharma
 * Date: 12/8/13
 * Time: 2:30 PM
 */
public class TargetedMsRunListView extends ExperimentRunListView
{

    private ExperimentAnnotations _expAnnotations;
    private ViewType _viewType;


    public static enum ViewType
    {
        FOLDER_VIEW,
        EXPERIMENT_VIEW,
        EDITABLE_EXPERIMENT_VIEW

    }

    public TargetedMsRunListView(UserSchema schema, QuerySettings settings)
    {
        super(schema, settings, TargetedMSModule.EXP_RUN_TYPE);
        setShowAddToRunGroupButton(false);
        setShowMoveRunsButton(false);
        addClientDependency(ClientDependency.fromPath("Ext4"));
        addClientDependency(ClientDependency.fromPath("targetedms/js/LinkVersionsDialog.js"));
        addClientDependency(ClientDependency.fromPath("targetedms/css/LinkVersionsDialog.css"));
        addClientDependency(ClientDependency.fromPath("targetedms/js/ClustergrammerDialog.js"));
    }

    private void setExpAnnotations(ExperimentAnnotations expAnnotations)
    {
        _expAnnotations = expAnnotations;
    }

    public void setViewType(ViewType viewType)
    {
        _viewType = viewType;

        if(viewType == ViewType.EXPERIMENT_VIEW)
        {
            setButtonBarPosition(DataRegion.ButtonBarPosition.NONE);
            setShowRecordSelectors(false);
            setShowPagination(false);
        }
        if(viewType == ViewType.EDITABLE_EXPERIMENT_VIEW)
        {
            setShowExportButtons(false);
            setShowPagination(false);
        }
        else
        {
            if(TargetedMSManager.getFolderType(getContainer()) == TargetedMSModule.FolderType.Experiment)
            {
                // We are only allowing runs to be moved between experimental data folders.
                setShowMoveRunsButton(true);
            }
        }
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        addLinkVersionButton(view, bar);
        addClusterGrammerButton(view, bar);

        if(_viewType == ViewType.EDITABLE_EXPERIMENT_VIEW)
            addExperimentDetailsViewButtons(bar);
    }

    private void addLinkVersionButton(DataView view, ButtonBar bar)
    {
        ActionButton versionButton = new ActionButton("Link Versions");
        versionButton.setRequiresSelection(true, 2, null);
        versionButton.setScript("LABKEY.targetedms.LinkedVersions.showDialog()");
        versionButton.setDisplayPermission(UpdatePermission.class);
        bar.add(versionButton);
    }

    private void addClusterGrammerButton(DataView view, ButtonBar bar)
    {
        ActionButton cgButton = new ActionButton("Clustergrammer Heatmap");
        cgButton.setRequiresSelection(true, 1, null);
        cgButton.setScript("LABKEY.targetedms.Clustergrammer.showDialog()");

        cgButton.setDisplayPermission(ReadPermission.class);
        bar.add(cgButton);
    }

    private void addExperimentDetailsViewButtons(ButtonBar bar)
    {
        if(_expAnnotations == null)
            return;

        String buttonText = _expAnnotations.isIncludeSubfolders() ? "Exclude Subfolders" : "Include Subfolders";
        ActionURL url = _expAnnotations.isIncludeSubfolders() ?
                getExcludeSubfoldersInExperimentURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL()) :
                getIncludeSubfoldersInExperimentURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL());

        ActionButton includeSubfoldersBtn = new ActionButton(buttonText, url);
        includeSubfoldersBtn.setDisplayPermission(InsertPermission.class);
        includeSubfoldersBtn.setActionType(ActionButton.Action.POST);
        bar.add(includeSubfoldersBtn);
    }

    public static TargetedMsRunListView createView(ViewContext model)
    {
        return createView(model, null, ViewType.FOLDER_VIEW);
    }

    public static TargetedMsRunListView createView(ViewContext model, ExperimentAnnotations expAnnotations, ViewType viewType)
    {
        UserSchema schema = new TargetedMSSchema(model.getUser(), model.getContainer());
        QuerySettings querySettings = getRunListQuerySettings(schema, model, TargetedMSModule.EXP_RUN_TYPE.getTableName(), true);

        TargetedMsRunListView view = new TargetedMsRunListView(schema, querySettings);
        view.setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
        view.setFrame(FrameType.PORTAL);
        view.setViewType(viewType);

        if(viewType == ViewType.EDITABLE_EXPERIMENT_VIEW || viewType == ViewType.EXPERIMENT_VIEW)
        {
            // If we are looking at the details of an experiment, set the container filter to CurrentAndSubfolders so that
            // runs in subfolders are visible (if the experiment includes subfolders).
            querySettings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());
            view.setExpAnnotations(expAnnotations);
        }
        return view;
    }
}
