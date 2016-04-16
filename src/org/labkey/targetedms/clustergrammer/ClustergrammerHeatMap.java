package org.labkey.targetedms.clustergrammer;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by iansigmon on 4/8/16.
 */
public class ClustergrammerHeatMap implements HeatMap
{
    Map<String, Map<String, Double>> _subjectTable;
    String title;

    public ClustergrammerHeatMap(Map rs, String title) throws SQLException
    {
        _subjectTable = rs;
        this.title = title;
    }

    @Override
    public Map getMatrix()
    {
        return _subjectTable;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }
}
