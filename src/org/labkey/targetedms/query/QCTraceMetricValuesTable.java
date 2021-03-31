package org.labkey.targetedms.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.targetedms.TargetedMSSchema;

public class QCTraceMetricValuesTable extends TargetedMSTable
{
    public QCTraceMetricValuesTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSSchema.getSchema().getTable(TargetedMSSchema.TABLE_QC_TRACE_METRIC_VALUES), schema, cf, TargetedMSSchema.ContainerJoinType.SampleFileFK);
        TargetedMSTable.fixupLookups(this);
    }
}
