/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentRunListView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private boolean _hasDocVersions;
    private boolean _showAllVersions;
    private static final boolean DEFAULT_SHOW_ALL_VERSIONS = true;
    private static final String DEFAULT_VERSIONS_PARAM = DEFAULT_SHOW_ALL_VERSIONS ? "latestVersions" : "allVersions";

    public static enum ViewType
    {
        FOLDER_VIEW,
        EXPERIMENT_VIEW,
        EDITABLE_EXPERIMENT_VIEW

    }

    public TargetedMsRunListView(UserSchema schema, QuerySettings settings, boolean hasDocVersions, boolean showAllVersions)
    {
        super(schema, settings, TargetedMSModule.EXP_RUN_TYPE);
        _hasDocVersions = hasDocVersions;
        _showAllVersions = showAllVersions;
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

    private void setViewType(ViewType viewType)
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

    private boolean isShowAllVersions()
    {
        return _showAllVersions;
    }

    private boolean canChangeDocVersionCols()
    {
        return _hasDocVersions && getCustomView() == null;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        addLinkVersionButton(view, bar);
        addClusterGrammerButton(view, bar);

        if(_viewType == ViewType.EDITABLE_EXPERIMENT_VIEW)
            addExperimentDetailsViewButtons(bar);

        if(canChangeDocVersionCols())
        {
            // Add the view toggle button if this container has a document version chain AND
            // we are not looking at a custom view.
            addDocVersionsButton(view, bar);
        }
    }

    private void addDocVersionsButton(DataView view, ButtonBar bar)
    {
        String txt = _showAllVersions ? "Latest Versions" : "All Versions";
        ActionURL url = view.getViewContext().getActionURL().clone();
        boolean removeParam = DEFAULT_SHOW_ALL_VERSIONS != _showAllVersions;
        if(removeParam)
        {
            url.deleteParameter(DEFAULT_VERSIONS_PARAM);
        }
        else
        {
            url.replaceParameter(DEFAULT_VERSIONS_PARAM, "true");
        }
        ActionButton button = new ActionButton(txt, url);
        button.setDisplayPermission(ReadPermission.class);
        bar.add(button);
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

    public List<DisplayColumn> getDisplayColumns()
    {
        if(!canChangeDocVersionCols())
        {
            return super.getDisplayColumns();
        }

        TableInfo table = getTable();
        if (table == null)
            return Collections.emptyList();

        List<FieldKey> cols = new ArrayList<>(table.getDefaultVisibleColumns());
        if (!_showAllVersions)
        {
            cols.remove(FieldKey.fromParts("ReplacedByRun"));
        }

        List<DisplayColumn> displayCols = new ArrayList<>();
        for (ColumnInfo col : QueryService.get().getColumns(table, cols).values())
        {
            DisplayColumn displayCol = col.getRenderer();
            displayCols.add(displayCol);
        }
        return displayCols;
    }

    public static TargetedMsRunListView createView(ViewContext model)
    {
        return createView(model, null, ViewType.FOLDER_VIEW);
    }

    public static TargetedMsRunListView createView(ViewContext model, ExperimentAnnotations expAnnotations, ViewType viewType)
    {
        UserSchema schema = new TargetedMSSchema(model.getUser(), model.getContainer());
        QuerySettings querySettings = getRunListQuerySettings(schema, model, TargetedMSModule.EXP_RUN_TYPE.getTableName(), true);

        TargetedMsRunListView view = new TargetedMsRunListView(schema, querySettings,
                TargetedMSManager.containerHasDocVersions(model.getContainer()),
                isShowAllVersions(model));
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

        if(view.canChangeDocVersionCols() && !view.isShowAllVersions())
        {
            // Display only the latest version of a Skyline document.
            querySettings.setBaseFilter(new SimpleFilter(FieldKey.fromString("ReplacedByRun"), null, CompareType.ISBLANK));
        }
        return view;
    }

    private static boolean isShowAllVersions(ViewContext context)
    {
        String param = context.getActionURL().getParameter(DEFAULT_VERSIONS_PARAM);
        boolean paramSet = param != null && !("false").equalsIgnoreCase(param);
        // (default show all) false  (param: showAllVersions)false  -> false (show all)
        // (default show all) false  (param: showAllVersions)true   -> true (show all)
        // (default show all) true   (param: showLatest) false      -> true (show all)
        // (default show all) true   (param: showLatest) true       -> false (show all)
        return DEFAULT_SHOW_ALL_VERSIONS != paramSet;
    }
}
