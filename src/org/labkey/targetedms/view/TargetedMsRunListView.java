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
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.ExperimentRunListView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.ExperimentAnnotations;
import org.labkey.targetedms.query.ExperimentAnnotationsManager;

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
    private ButtonBarType _buttonBarType;


    public static enum ButtonBarType
    {
        FOLDER_VIEW,
        EXPERIMENT_DETAILS_VIEW,
        ALL_AVAILABLE_RUNS_VIEW
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

    public void setButtonBarType(ButtonBarType buttonBarType)
    {
        _buttonBarType = buttonBarType;

        if(_buttonBarType == ButtonBarType.EXPERIMENT_DETAILS_VIEW || _buttonBarType == ButtonBarType.ALL_AVAILABLE_RUNS_VIEW)
        {
            setShowDeleteButton(false);
        }
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        switch(_buttonBarType)
        {
            case FOLDER_VIEW:
                addFolderViewButtons(view, bar);
                break;
            case EXPERIMENT_DETAILS_VIEW:
                addExperimentDetailsViewButtons(view, bar);
                break;
            case ALL_AVAILABLE_RUNS_VIEW:
                addAllAvailableRunsViewButtons(view, bar);
                break;
        }
    }

    private void addAllAvailableRunsViewButtons(DataView view, ButtonBar bar)
    {
        ActionURL url = new ActionURL(TargetedMSController.AddSelectedRunsAction.class, getViewContext().getContainer());
        url.addParameter("expAnnotId", _expAnnotations.getId());
        ActionButton addToExperimentButton = new ActionButton(url,"Add to Experiment");
        bar.add(addToExperimentButton);
    }

    private void addExperimentDetailsViewButtons(DataView view, ButtonBar bar)
    {
        ActionURL removeRunUrl = new ActionURL(TargetedMSController.RemoveSelectedRunsAction.class, getViewContext().getContainer());
        removeRunUrl.addParameter("expAnnotId", _expAnnotations.getId());
        ActionButton removeRunAction = new ActionButton(removeRunUrl,"Remove");
        removeRunAction.setActionType(ActionButton.Action.POST);
        removeRunAction.setRequiresSelection(true);
        removeRunAction.setDisplayPermission(DeletePermission.class);
        bar.add(removeRunAction);

        MenuButton addRunsButton = new MenuButton("Add Runs to Experiment");
        addRunsButton.setDisplayPermission(InsertPermission.class);
        bar.add(addRunsButton);

        ActionURL addRunsInFolderUrl = TargetedMSController.getAddAllRunsToExperimentURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL());
        addRunsButton.addMenuItem("Add all runs in folder", null, createScript(view.getDataRegion(), addRunsInFolderUrl));

        ActionURL selectRunsToAddUrl = TargetedMSController.getShowAvailableRunsURL(_expAnnotations.getId(), getViewContext().getContainer(), getReturnURL());
        addRunsButton.addMenuItem("Select and add runs", selectRunsToAddUrl);
    }

    private void addFolderViewButtons(DataView view, ButtonBar bar)
    {
        MenuButton addToExperimentButton = new MenuButton("Add to experiment");
        addToExperimentButton.setRequiresSelection(true);
        addToExperimentButton.setDisplayPermission(InsertPermission.class);

        ActionURL url = TargetedMSController.getNewExperimentAnnotationURL(getViewContext().getContainer(), getReturnURL(), true);
        addToExperimentButton.addMenuItem("Create new experiment", null, createScript(view.getDataRegion(), url));

        List<ExperimentAnnotations> exptAnnotations = ExperimentAnnotationsManager.get(getViewContext().getContainer());
        if(exptAnnotations.size() > 0)
        {
            addToExperimentButton.addSeparator();
        }

        for(ExperimentAnnotations exptAnnotation: exptAnnotations)
        {
            ActionURL addRunUrl = new ActionURL(TargetedMSController.AddSelectedRunsAction.class, getViewContext().getContainer());
            addRunUrl.addParameter("expAnnotId", exptAnnotation.getId());
            addToExperimentButton.addMenuItem(exptAnnotation.getTitle(), null, getVerifySelectedScript(view, addRunUrl));
        }

        bar.add(addToExperimentButton);
    }

    private static String getVerifySelectedScript(DataView view, ActionURL url)
    {
        StringBuilder script = new StringBuilder("javascript: ");
        script.append("if (verifySelected(").append(view.getDataRegion().getJavascriptFormReference(false));
        script.append( ", \"").append(url.getLocalURIString());
        script.append("\", \"post\", \"run\")) { ").append(view.getDataRegion().getJavascriptFormReference(false));
        script.append(".submit(); }");
        return script.toString();
    }

    private static String createScript(DataRegion dataRegion, ActionURL url)
    {
        StringBuilder script = new StringBuilder("javascript: ");
        script.append(dataRegion.getJavascriptFormReference(false)).append(".method = \"POST\";\n ");
        script.append(dataRegion.getJavascriptFormReference(false)).append(".action = ").append(PageFlowUtil.jsString(url.getLocalURIString())).append(";\n ");
        script.append(dataRegion.getJavascriptFormReference(false)).append(".submit();");
        return script.toString();
    }

    public static TargetedMsRunListView createView(ViewContext model)
    {
        return createView(model, null, ButtonBarType.FOLDER_VIEW);
    }

    public static TargetedMsRunListView createView(ViewContext model, ExperimentAnnotations expAnnotations, ButtonBarType buttonBarType)
    {
        UserSchema schema = new TargetedMSSchema(model.getUser(), model.getContainer());
        QuerySettings querySettings = getRunListQuerySettings(schema, model, TargetedMSModule.EXP_RUN_TYPE.getTableName(), true);

        TargetedMsRunListView view = new TargetedMsRunListView(schema, querySettings);
        view.setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
        view.setFrame(FrameType.PORTAL);
        view.setButtonBarType(buttonBarType);

        if(expAnnotations != null)
        {
            // If we are looking at the details of an experiment, show a list of Skyline documents in the current
            // folder and all its subfolders.  Also, show the "Container" column.
            querySettings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());
            List<FieldKey> fieldKeys = new ArrayList<>();
            fieldKeys.addAll(querySettings.getTable(schema).getDefaultVisibleColumns());
            fieldKeys.add(FieldKey.fromParts("Container"));
            querySettings.setFieldKeys(fieldKeys);

            if(buttonBarType == ButtonBarType.ALL_AVAILABLE_RUNS_VIEW)
            {
                // Filter to runs not already part of the experiment
                SQLFragment sql = new SQLFragment("File NOT IN  (SELECT DISTINCT RunId FROM ");
                sql.append(TargetedMSManager.getTableInfoExperimentAnnotationsRun(), "tmsexpruns");
                sql.append(" ");
                sql.append(" WHERE tmsexpruns.experimentannotationsid = ?)");
                sql.add(expAnnotations.getId());
                querySettings.getBaseFilter().addWhereClause(sql.toString(), null);
            }
            view.setExpAnnotations(expAnnotations);
        }

        return view;
    }
}
