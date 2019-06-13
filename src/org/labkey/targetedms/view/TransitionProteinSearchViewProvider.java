/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.ExperimentTitleDisplayColumn;
import org.labkey.targetedms.query.JournalManager;
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

        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "PeptideGroup");
        settings.addAggregates(new Aggregate(FieldKey.fromParts("RunId", "File"), Aggregate.BaseType.COUNT, null, true));

        // Issue 17576: Peptide and Protein searches do not work for searching in subfolders
        if (form.isIncludeSubfolders())
            settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable inner = (TargetedMSTable) super.createTable();
                FilteredTable result = new FilteredTable(inner, getSchema());
                result.wrapAllColumns(true);

                // Apply a filter to restrict to the set of matching proteins
                SQLFragment sql = new SQLFragment("Id IN (SELECT pg.Id FROM targetedms.PeptideGroup AS pg ");

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

                boolean isJournalFolder = JournalManager.isJournalProject(viewContext.getContainer());
                if (isJournalFolder)
                {
                    addExperimentTitleColumn(result, getContainer());
                }

                List<FieldKey> visibleColumns = new ArrayList<>();
                if(isJournalFolder)
                {
                    visibleColumns.add(FieldKey.fromParts("Experiment"));
                }
                visibleColumns.add(FieldKey.fromParts("Label"));
                visibleColumns.add(FieldKey.fromParts("Description"));
                visibleColumns.add(FieldKey.fromParts("Accession"));
                visibleColumns.add(FieldKey.fromParts("PreferredName"));
                visibleColumns.add(FieldKey.fromParts("Gene"));
                visibleColumns.add(FieldKey.fromParts("Species"));
                visibleColumns.add(FieldKey.fromParts("RunId", "File"));
                if(form.isIncludeSubfolders())
                {
                    visibleColumns.add(FieldKey.fromParts("RunId", "Folder", "Path"));
                }

                result.setDefaultVisibleColumns(visibleColumns);
                return result;
            }
        };
        result.setTitle("Targeted MS Proteins");
        result.enableExpandCollapse("TargetedMSProteins", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }

    @NotNull
    private void addExperimentTitleColumn(FilteredTable result, Container container)
    {
        SQLFragment whereSql = new SQLFragment(" WHERE runs.Id = ").append(ExprColumn.STR_TABLE_ALIAS).append(".runId");
        ExperimentTitleDisplayColumn col = new ExperimentTitleDisplayColumn(result, container, whereSql, "runs");
        result.addColumn(col);
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
