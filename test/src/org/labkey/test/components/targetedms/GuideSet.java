package org.labkey.test.components.targetedms;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cnathe on 4/28/15.
 */
public class GuideSet
{
    private int _rowId;
    private String _startDate;
    private String _endDate;
    private String _comment;
    private List<GuideSetStats> _stats;

    public GuideSet(String startDate, String endDate, String comment)
    {
        _startDate = startDate;
        _endDate = endDate;
        _comment = comment;
        _stats = new ArrayList<>();
    }

    public String getStartDate()
    {
        return _startDate;
    }

    public String getEndDate()
    {
        return _endDate;
    }

    public String getComment()
    {
        return _comment;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public void addStats(GuideSetStats stats)
    {
        _stats.add(stats);
    }

    public List<GuideSetStats> getStats()
    {
        return _stats;
    }
}
