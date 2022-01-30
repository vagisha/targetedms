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
import org.labkey.api.data.Container;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

import static org.labkey.api.targetedms.TargetedMSService.FolderType.Library;
import static org.labkey.api.targetedms.TargetedMSService.FolderType.LibraryProtein;

public class LibraryQueryViewWebPart extends QueryView
{
    private final String _tableName;
    private final String _viewName;
    private final String _title;
    private final String DEFAULT_VIEW = "";

    public LibraryQueryViewWebPart(ViewContext viewContext, String tableName, String title)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        _tableName = tableName;
        _title = title;
        _viewName = defaultViewNameForContainer(viewContext.getContainer(), tableName);
        setSettings(createQuerySettings(viewContext, _tableName));
        ActionURL link = getQueryLink(viewContext);
        link.addParameter("query.viewName", _viewName);
        setTitleHref(link);
        setTitle((!DEFAULT_VIEW.equals(_viewName) ? "Library " : "") + title);
        setShowDetailsColumn(false);
        setShowFilterDescription(false);

        setShowBorders(true);
        setShadeAlternatingRows(true);
    }

    private ActionURL getQueryLink(ViewContext viewContext)
    {
        return PageFlowUtil.urlProvider(QueryUrls.class).urlExecuteQuery(viewContext.getContainer(), TargetedMSSchema.SCHEMA_NAME, _tableName);
    }

    private String defaultViewNameForContainer(Container container, String tableName)
    {
        var folderType = TargetedMSService.get().getFolderType(container);
        boolean proteinLibFolder = folderType == LibraryProtein;
        boolean libraryFolder = proteinLibFolder || folderType == Library;
        return switch (tableName)
            {
                case TargetedMSSchema.TABLE_PEPTIDE_GROUP -> libraryFolder ? "LibraryProteins" : DEFAULT_VIEW;
                case TargetedMSSchema.TABLE_PEPTIDE -> libraryFolder ? "LibraryPeptides" : DEFAULT_VIEW;
                case TargetedMSSchema.TABLE_MOLECULE -> libraryFolder ? "LibraryMolecules" : DEFAULT_VIEW;
                case TargetedMSSchema.TABLE_PRECURSOR, TargetedMSSchema.TABLE_MOLECULE_PRECURSOR -> libraryFolder ? "LibraryPrecursors" : DEFAULT_VIEW;
                default -> DEFAULT_VIEW;
            };
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(portalCtx, dataRegionName, _tableName);
        if (!portalCtx.getRequest().getParameterMap().containsKey(QueryParam.viewName) && !DEFAULT_VIEW.equals(_viewName))
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
        if (!DEFAULT_VIEW.equals(_viewName))
        {
            addViewAllButton(view, bar);
        }
    }

    private void addViewAllButton(DataView view, ButtonBar bar)
    {
        ActionButton viewAllButton = new ActionButton("View All " + _title);
        viewAllButton.setURL(getQueryLink(view.getViewContext()));
        bar.add(viewAllButton);
    }
}
