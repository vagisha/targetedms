package org.labkey.targetedms.clustergrammer;

import java.util.Map;

/**
 * Created by iansigmon on 4/7/16.
 */
public interface HeatMap<Column, Row, Value>
{
    Map<Column, Map<Row, Value>> getMatrix();
    String getTitle();
}
