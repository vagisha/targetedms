package org.labkey.targetedms.clustergrammer;

import java.util.Map;

/**
 * Created by iansigmon on 4/8/16.
 */
public class ClustergrammerHeatMap implements HeatMap
{
    private Map<String, Map<String, Double>> _subjectTable;
    private String _title;

    public ClustergrammerHeatMap(Map<String, Map<String, Double>> rs, String title)
    {
        _subjectTable = rs;
        this._title = title;
    }

    @Override
    public Map getMatrix()
    {
        return _subjectTable;
    }

    @Override
    public String getTitle()
    {
        return this._title;
    }
}
