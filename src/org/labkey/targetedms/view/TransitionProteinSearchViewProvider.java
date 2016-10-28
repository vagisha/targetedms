/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.TargetedMSTable;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
* User: jeckels
* Date: May 10, 2012
*/
public class TransitionProteinSearchViewProvider implements ProteinService.QueryViewProvider<ProteinService.ProteinSearchForm>
{
    @Override
    public String getDataRegionName()
    {
        return "TargetedMSMatches";
    }

    @Nullable
    @Override
    public QueryView createView(ViewContext viewContext, final ProteinService.ProteinSearchForm form, BindException errors)
    {
        if (! viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
            return null;  // only enable this view if the TargetedMSModule is active

        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "Peptide");
        settings.addAggregates(new Aggregate(FieldKey.fromParts("PeptideGroupId", "RunId", "File"), Aggregate.BaseType.COUNT, null, true));

        // Issue 17576: Peptide and Protein searches do not work for searching in subfolders
        if (form.isIncludeSubfolders())
            settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable result = (TargetedMSTable) super.createTable();

                // Apply a filter to restrict to the set of matching proteins
                SQLFragment sql = new SQLFragment("PeptideGroupId IN (SELECT pg.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");

                sql.append(" WHERE ( ");
                if (form.getSeqId().length > 0)
                {
                    sql.append("pg.SequenceId IN (");
                    String separator = "";
                    for (int seqId : form.getSeqId())
                    {
                        sql.append(separator);
                        sql.append(seqId);
                        separator = ",";
                    }
                    sql.append(")");
                    sql.append(" OR ");
                }

                sql.append(getProteinLabelCondition("pg.Label", getProteinLabels(form.getIdentifier()), form.isExactMatch()));

                sql.append("))");
                result.addCondition(sql);

                List<FieldKey> visibleColumns = new ArrayList<>();
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "Label"));
                visibleColumns.add(FieldKey.fromParts("Sequence"));
                if (form.isIncludeSubfolders())
                {
                    visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
                }
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
                result.setDefaultVisibleColumns(visibleColumns);
                return result;
            }
        };
        result.setTitle("Targeted MS Peptides");
        result.enableExpandCollapse("TargetedMSPeptides", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }

    private List<String> getProteinLabels(String labels)
    {
        if(StringUtils.isBlank(labels))
            return Collections.emptyList();

        return Arrays.asList(StringUtils.split(labels, " \t\n\r,"));
    }

    private SQLFragment getProteinLabelCondition (String columnName, List<String> labels, boolean exactMatch)
    {
        SQLFragment sqlFragment = new SQLFragment();
        String separator = "";
        sqlFragment.append("(");
        if (labels.isEmpty())
        {
            sqlFragment.append("1 = 2");
        }
        for (String param : labels)
        {
            sqlFragment.append(separator);
            sqlFragment.append("LOWER (" + columnName + ")");
            if (exactMatch)
            {
                sqlFragment.append(" = LOWER(?)");
                sqlFragment.add(param);
            }
            else
            {
                sqlFragment.append(" LIKE LOWER(?)");
                sqlFragment.add("%" + param + "%");
            }
            separator = " OR ";
        }
        sqlFragment.append(")");
        return sqlFragment;
    }
}
