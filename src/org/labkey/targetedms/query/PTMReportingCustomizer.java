package org.labkey.targetedms.query;

import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query showing post-translational modification percentages
 * for different samples to hide all of the pivot values that aren't part of the run that's being filtered on.
 *
 * In the future, this will become more of a first-class type of report, but this approach allows for a minimal change
 * in 19.2 that should only impact installs where a .query.xml file refers to this class.
 */
public class PTMReportingCustomizer implements TableCustomizer
{
    @Override
    public void customize(TableInfo tableInfo)
    {
        if (HttpView.hasCurrentView())
        {
            ViewContext context = HttpView.currentContext();
            // Grab any filters that have been applied
            SimpleFilter filter = new SimpleFilter(context.getActionURL(), "query");
            for (SimpleFilter.FilterClause clause : filter.getClauses())
            {
                // Look for an equals filter on the RunId column that has a value specified
                if (clause.getFieldKeys().contains(FieldKey.fromParts("PeptideGroupId", "RunId")) &&
                        clause instanceof CompareType.CompareClause &&
                        ((CompareType.CompareClause)clause).getCompareType() == CompareType.EQUAL &&
                        clause.getParamVals().length == 1 &&
                        clause.getParamVals()[0] != null)
                {
                    try
                    {
                        // Get the sample names associated with that run
                        int runId = Integer.parseInt(clause.getParamVals()[0].toString());
                        Set<String> sampleNames = new CaseInsensitiveHashSet(new TableSelector(TargetedMSManager.getTableInfoSampleFile(), Collections.singleton("SampleName"), new SimpleFilter(FieldKey.fromParts("ReplicateId", "RunId"), runId), null).getArrayList(String.class));

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
