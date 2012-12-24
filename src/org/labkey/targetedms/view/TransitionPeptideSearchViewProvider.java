/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.ProteinService;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.TargetedMSTable;
import org.springframework.validation.BindException;

/**
* User: jeckels
* Date: May 10, 2012
*/
public class TransitionPeptideSearchViewProvider implements ProteinService.QueryViewProvider<ProteinService.PeptideSearchForm>
{
    @Override
    public String getDataRegionName()
    {
        return "TargetedMSMatches";
    }

    @Override
    public QueryView createView(ViewContext viewContext, final ProteinService.PeptideSearchForm form, BindException errors)
    {
        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "Peptide");
        settings.addAggregates(new Aggregate("PeptideGroupId/RunId/File", Aggregate.Type.COUNT_DISTINCT));
        settings.addAggregates(new Aggregate("Sequence", Aggregate.Type.COUNT_DISTINCT));
        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable result = (TargetedMSTable) super.createTable();
                result.addCondition(new SimpleFilter(form.createFilter("Sequence")));
                return result;
            }
        };
        result.setTitle("TargetedMS Peptides");
        result.enableExpandCollapse("TargetedMSPeptides", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }
}
