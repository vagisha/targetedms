/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QueryViewProvider;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.ModifiedSequenceDisplayColumn;
import org.labkey.targetedms.query.TargetedMSTable;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.List;

/**
* User: jeckels
* Date: May 10, 2012
*/
public class TransitionPeptideSearchViewProvider implements QueryViewProvider<ProteinService.PeptideSearchForm>
{
    @Override
    public String getDataRegionName()
    {
        return "TargetedMSMatches";
    }

    @Nullable
    @Override
    public QueryView createView(ViewContext viewContext, final ProteinService.PeptideSearchForm form, BindException errors)
    {
        if (! viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
            return null;  // only enable this view if the TargetedMSModule is active

        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "Peptide");
        settings.addAggregates(new Aggregate(FieldKey.fromParts("PeptideGroupId", "RunId", "File"), Aggregate.BaseType.COUNT, null, true));
        settings.addAggregates(new Aggregate(FieldKey.fromParts("Sequence"), Aggregate.BaseType.COUNT, null, true));

        // Issue 17576: Peptide and Protein searches do not work for searching in subfolders
        if (form.isSubfolders())
            settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable result = (TargetedMSTable) getSchema().getTable(getSettings().getQueryName(), ContainerFilter.getContainerFilterByName(settings.getContainerFilterName(), getContainer(), getUser()), true, true);
                result.addCondition(new SimpleFilter(form.createFilter("Sequence")));

                List<FieldKey> visibleColumns = new ArrayList<>();
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "Label"));
                visibleColumns.add(FieldKey.fromParts("Sequence"));
                visibleColumns.add(FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
                visibleColumns.add(FieldKey.fromParts("CalcNeutralMass"));
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
                if (form.isSubfolders())
                {
                    visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
                }
                result.setDefaultVisibleColumns(visibleColumns);

                List<TableCustomizer> customizers = TargetedMSService.get().getPeptideSearchResultCustomizers();
                for(TableCustomizer customizer : customizers)
                {
                    customizer.customize(result);
                }

                return result;
            }
        };
        result.setTitle("Targeted MS Peptides");
        result.enableExpandCollapse("TargetedMSPeptides", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }
}
