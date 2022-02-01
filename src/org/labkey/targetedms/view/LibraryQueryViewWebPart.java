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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.Map;

import static org.labkey.targetedms.TargetedMSSchema.TABLES_LIBRARY_VIEWS;

public class LibraryQueryViewWebPart extends QueryView
{
    private final String _tableName;
    private final boolean _fixedView;
    private final String DEFAULT_VIEW = "";

    // supported table name -> grid title
    private static final CaseInsensitiveHashMap<String> SUPPORTED = new CaseInsensitiveHashMap<>(Map.of(
            TargetedMSSchema.TABLE_PEPTIDE_GROUP, "Proteins",
            TargetedMSSchema.TABLE_PEPTIDE, "Peptides",
            TargetedMSSchema.TABLE_PRECURSOR, "Precursors",
            TargetedMSSchema.TABLE_MOLECULE, "Molecules",
            TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, "Molecule Precursors"));

    @NotNull
    private static LibraryQueryViewWebPart forTable(@NotNull String tableName, @NotNull ViewContext viewContext, boolean allViews)
    {
        if (!SUPPORTED.containsKey(tableName))
        {
            throw new IllegalStateException("Cannot create a view for table: " + tableName);
        }
        return new LibraryQueryViewWebPart(viewContext, tableName, SUPPORTED.get(tableName), allViews);
    }

    @NotNull
    public static LibraryQueryViewWebPart forTable(@NotNull String tableName, @NotNull ViewContext viewContext)
    {
        return forTable(tableName, viewContext, false);
    }

    @NotNull
    public static LibraryQueryViewWebPart forTableAllViews(@NotNull String tableName, @NotNull ViewContext viewContext)
    {
        return forTable(tableName, viewContext, true);
    }

    public static boolean isTableSupported(String tableName)
    {
        return SUPPORTED.containsKey(tableName);
    }

    private LibraryQueryViewWebPart(ViewContext viewContext, String tableName, String title, boolean allViews)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        _tableName = tableName;
        var libraryFolder = TargetedMSManager.isLibraryFolder(viewContext.getContainer());
        var settings = createQuerySettings(viewContext, _tableName, libraryFolder, allViews);
        setSettings(settings);

        ActionURL titleLink = getTitleLink(viewContext);
        titleLink.addParameter(settings.param(QueryParam.viewName), settings.getViewName());
        setTitleHref(titleLink);

        _fixedView = libraryFolder && !allViews && !viewContext.getRequest().getParameterMap().containsKey(settings.param(QueryParam.viewName));
        if (!_fixedView)
        {
            setShowFilterDescription(false);
        }
        setTitleAndHelpPopup(tableName, title, settings);

        setShowDetailsColumn(false);
        setShowBorders(true);
        setShadeAlternatingRows(true);
        setAllowableContainerFilterTypes();
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName, boolean libraryFolder, boolean allViews)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(portalCtx, dataRegionName, _tableName);
        if (libraryFolder && !allViews && !portalCtx.getRequest().getParameterMap().containsKey(settings.param(QueryParam.viewName)))
        {
            var viewName = TABLES_LIBRARY_VIEWS.getOrDefault(_tableName, DEFAULT_VIEW);
            settings.setViewName(viewName);
            settings.setAllowChooseView(false);
        }
        return settings;
    }

    private void setTitleAndHelpPopup(String tableName, String targetType, QuerySettings settings)
    {
        var viewName = settings.getViewName();
        var isLibraryView = viewName != null && viewName.equals(TABLES_LIBRARY_VIEWS.get(tableName));
        var webpartTitle = isLibraryView ? "Library " + targetType : targetType;
        setTitle(webpartTitle);
        if (isLibraryView)
        {
            setTitlePopupHelp(webpartTitle, targetType + " that are in the current library in the folder.");
        }
        else if (viewName == null || DEFAULT_VIEW.equals(viewName))
        {
            setTitlePopupHelp(webpartTitle, targetType + " from all the Skyline documents uploaded to the folder.");
        }
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);
        if (_fixedView)
        {
            addViewAllButton(view, bar);
        }
    }

    private void addViewAllButton(DataView view, ButtonBar bar)
    {
        ActionButton viewAllButton = new ActionButton("View All");
        ActionURL url = getTitleLink(view.getViewContext());
        viewAllButton.setURL(url);
        bar.add(viewAllButton);
    }

    @NotNull
    private ActionURL getTitleLink(ViewContext viewContext)
    {
        ActionURL url = new ActionURL(TargetedMSController.ShowTargetsGridAction.class, viewContext.getContainer());
        url.addParameter(QueryParam.queryName, _tableName);
        return url;
    }
}
