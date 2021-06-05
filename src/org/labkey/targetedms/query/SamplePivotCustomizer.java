package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all of the pivot values
 * that aren't part of the run that's being filtered on. This lets you view a single document's worth of data
 * without seeing empty columns for all of the other samples in the same container.
 */
public class SamplePivotCustomizer implements TableCustomizer
{
    private final FieldKey _runIdField;

    public SamplePivotCustomizer(MultiValuedMap<String, String> props)
    {
        Collection<String> runIdFilter = props.get("runIdField");
        if (runIdFilter == null || runIdFilter.size() != 1)
        {
            throw new IllegalArgumentException("Must have exactly one property named 'runIdField'");
        }
        _runIdField = FieldKey.fromString(runIdFilter.iterator().next());
    }

    @Override
    public void customize(TableInfo tableInfo)
    {
        if (HttpView.hasCurrentView())
        {
            ViewContext context = HttpView.currentContext();

            ActionURL filterURL;

            if (context.getActionURL().getAction().equalsIgnoreCase("getQueryDetails"))
            {
                // When getting the query metadata to customize the view, the Run ID parameter doesn't get propagated to
                // the current URL. Look for it from the referrer URL
                String referer = context.getRequest().getHeader("Referer");
                filterURL = referer == null ? context.getActionURL() : new ActionURL(referer);
            }
            else
            {
                // Grab any filters that have been applied. Use QuerySettings, which will find the right values whether it's
                // a GET or POST
                QuerySettings settings = new QuerySettings(context, "query");
                filterURL = settings.getSortFilterURL();
            }

            SimpleFilter filter = new SimpleFilter(filterURL, "query");
            for (SimpleFilter.FilterClause clause : filter.getClauses())
            {
                // Look for an equals filter on the RunId column that has a value specified
                if (clause.getFieldKeys().contains(_runIdField) &&
                        clause instanceof CompareType.CompareClause &&
                        ((CompareType.CompareClause)clause).getCompareType() == CompareType.EQUAL &&
                        clause.getParamVals().length == 1 &&
                        clause.getParamVals()[0] != null)
                {
                    try
                    {
                        // Get the sample names associated with that run
                        long runId = Integer.parseInt(clause.getParamVals()[0].toString());
                        Set<String> sampleNames = new CaseInsensitiveHashSet();
                        TargetedMSSchema schema = new TargetedMSSchema(context.getUser(), context.getContainer());
                        TableInfo sampleFileTable = schema.getTable(TargetedMSSchema.TABLE_SAMPLE_FILE);
                        for (Map<String, Object> sampleInfo : new TableSelector(sampleFileTable,
                                QueryService.get().getColumns(sampleFileTable, Set.of(FieldKey.fromParts("SampleName"), FieldKey.fromParts("ReplicateId", "Name"))).values(),
                                new SimpleFilter(FieldKey.fromParts("ReplicateId", "RunId"), runId), null).getMapCollection())
                        {
                            for (Object sampleName : sampleInfo.values())
                            {
                                if (sampleName != null)
                                {
                                    sampleNames.add(sampleName.toString());
                                }
                            }
                        }

                        // Match the samples from the desired run against all of the pivoted columns
                        List<FieldKey> defaultColumns = new ArrayList<>(tableInfo.getDefaultVisibleColumns());
                        Iterator<FieldKey> iter = defaultColumns.iterator();
                        while (iter.hasNext())
                        {
                            FieldKey column = iter.next();
                            if (column.getName().contains("::"))
                            {
                                // Pivot columns get names like MySample::PercentModified
                                String columnSampleName = column.getName().split("::")[0];

                                // Kick out columns that don't match our run's set of samples
                                if (!sampleNames.contains(columnSampleName))
                                {
                                    iter.remove();
                                }
                            }
                        }
                        tableInfo.setDefaultVisibleColumns(defaultColumns);
                    }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
