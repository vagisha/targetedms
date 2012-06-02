/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.ProteinService;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.TargetedMSTable;
import org.springframework.validation.BindException;

import java.util.ArrayList;
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

    @Override
    public QueryView createView(ViewContext viewContext, final ProteinService.ProteinSearchForm form, BindException errors)
    {
        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "Peptide");
        settings.addAggregates(new Aggregate("PeptideGroupId/RunId/File", Aggregate.Type.COUNT_DISTINCT));
        settings.setAllowChooseQuery(false);
        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable result = (TargetedMSTable) super.createTable();

                // Apply a filter to restrict to the set of matching proteins
                SQLFragment sql = new SQLFragment("PeptideGroupId IN (SELECT pg.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(" WHERE pg.SequenceId IN (");
                if (form.getSeqId().length > 0)
                {
                    String separator = "";
                    for (int seqId : form.getSeqId())
                    {
                        sql.append(separator);
                        sql.append(seqId);
                        separator = ",";
                    }
                }
                else
                {
                    sql.append("NULL");
                }
                sql.append("))");
                result.addCondition(sql);

                List<FieldKey> visibleColumns = new ArrayList<FieldKey>();
                visibleColumns.add(FieldKey.fromParts("Sequence"));
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
                result.setDefaultVisibleColumns(visibleColumns);
                return result;
            }
        };
        result.setTitle("TargetedMS Peptides");
        result.enableExpandCollapse("TargetedMSPeptides", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }
}
