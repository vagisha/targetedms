package org.labkey.targetedms.view;

import org.labkey.api.ProteinService;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.TargetedMSTable;
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
        settings.setAllowChooseQuery(false);
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
