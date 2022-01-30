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
package org.labkey.targetedms.view;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Map;

public class LibraryQueryViewWebPart extends QueryView
{
    public static final Map<String, String> TABLES_LIBRARY_VIEWS = Map.of(
            TargetedMSSchema.TABLE_PEPTIDE_GROUP, "LibraryProteins",
            TargetedMSSchema.TABLE_PEPTIDE, "LibraryPeptides",
            TargetedMSSchema.TABLE_MOLECULE, "LibraryMolecules",
            TargetedMSSchema.TABLE_PRECURSOR, "LibraryPrecursors",
            TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, "LibraryPrecursors"
    );

    private final String _tableName;
    private final String _viewName;
    private final boolean _allViews;
    private final boolean _libraryFolder;
    private final String DEFAULT_VIEW = "";

    public LibraryQueryViewWebPart(ViewContext viewContext, String tableName, String title)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        _tableName = tableName;
        _allViews = viewContext.getRequest().getParameterMap().containsKey("allViews");
        _libraryFolder = TargetedMSManager.isLibraryFolder(viewContext.getContainer());

        _viewName = isFixedView() ? TABLES_LIBRARY_VIEWS.getOrDefault(tableName, DEFAULT_VIEW) : DEFAULT_VIEW;

        var settings = createQuerySettings(viewContext, _tableName);
        setSettings(settings);

        ActionURL titleLink = PageFlowUtil.urlProvider(QueryUrls.class).urlExecuteQuery(viewContext.getContainer(), TargetedMSSchema.SCHEMA_NAME, _tableName);
        titleLink.addParameter("query.viewName", _viewName);
        setTitleHref(titleLink);

        boolean libraryView = isLibraryViewSelected(getViewContext(), tableName, settings);
        String webpartTitle = (libraryView ? "Library " : "") + title;
        setTitle(webpartTitle);
        String help = title + (libraryView ? " that are in the current library in the folder." : " from all the Skyline documents uploaded to the folder.");
        setTitlePopupHelp(webpartTitle, help);

        setShowDetailsColumn(false);
        if (isFixedView())
        {
            setShowFilterDescription(false);
        }

        setShowBorders(true);
        setShadeAlternatingRows(true);
    }

    private boolean isFixedView()
    {
        return _libraryFolder && !_allViews;
    }

    private boolean isLibraryViewSelected(ViewContext viewContext, String tableName, QuerySettings settings)
    {
        var settingsViewName = settings.getViewName();
        if (_viewName.equals(TABLES_LIBRARY_VIEWS.get(tableName)))
        {
            return true;
        }
        else if (viewContext.getRequest().getParameterMap().containsKey(settings.param(QueryParam.viewName)))
        {
            var queryParam = viewContext.getRequest().getParameterMap().get(settings.param(QueryParam.viewName))[0];
            return queryParam != null && queryParam.equals(TABLES_LIBRARY_VIEWS.get(tableName));
        }
        return false;
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(portalCtx, dataRegionName, _tableName);
        if (isFixedView() && !portalCtx.getRequest().getParameterMap().containsKey(settings.param(QueryParam.viewName)))
        {
            settings.setViewName(_viewName);
            settings.setAllowChooseView(false);
        }
        return settings;
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);
        if (isFixedView())
        {
            addViewAllButton(view, bar);
        }
    }

    private void addViewAllButton(DataView view, ButtonBar bar)
    {
        ActionButton viewAllButton = new ActionButton("View All");
        ActionURL url = view.getViewContext().getActionURL().clone();
        url.replaceParameter("allViews", "true");
        viewAllButton.setURL(url);
        bar.add(viewAllButton);
    }
}
