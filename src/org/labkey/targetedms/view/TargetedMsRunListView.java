/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.List;

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
            setShowDeleteButton(true);
            setShowExportButtons(false);
            setShowRecordSelectors(true);
            setShowPagination(false);
        }
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        if(_viewType == ViewType.EDITABLE_EXPERIMENT_VIEW)
            addExperimentDetailsViewButtons(bar);
    }

    private void addExperimentDetailsViewButtons(ButtonBar bar)
    {
        if(_expAnnotations == null)
            return;

        String buttonText = _expAnnotations.isIncludeSubfolders() ? "Exclude Subfolders" : "Include Subfolders";
        ActionURL url = _expAnnotations.isIncludeSubfolders() ?
                TargetedMSController.getExcludeSubfoldersInExperimentURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL()) :
                TargetedMSController.getIncludeSubfoldersInExperimentURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL());

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

            List<FieldKey> fieldKeys = new ArrayList<>();
            fieldKeys.add(FieldKey.fromParts("Flag"));
            fieldKeys.add(FieldKey.fromParts("File"));

            fieldKeys.add(FieldKey.fromParts("File", "Created"));

            fieldKeys.add(FieldKey.fromParts("File", "PeptideGroupCount"));
            fieldKeys.add(FieldKey.fromParts("File", "PeptideCount"));
            fieldKeys.add(FieldKey.fromParts("File", "PrecursorCount"));
            fieldKeys.add(FieldKey.fromParts("File", "TransitionCount"));
            // Show the "Container" column.
            fieldKeys.add(FieldKey.fromParts("Container"));
            fieldKeys.add(FieldKey.fromParts("File", "Download"));
            querySettings.setFieldKeys(fieldKeys);

            view.setExpAnnotations(expAnnotations);
        }
        return view;
    }
}
