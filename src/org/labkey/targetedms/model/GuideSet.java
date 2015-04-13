package org.labkey.targetedms.model;

import org.labkey.api.data.Entity;

import java.sql.Timestamp;

/**
 * Created by Cory on 4/9/2015.
 */
public class GuideSet extends Entity
{
    private int _rowId;

    private String _comment;
    private Timestamp _trainingStart;
    private Timestamp _trainingEnd;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(String comment)
    {
        _comment = comment;
    }

    public Timestamp getTrainingStart()
    {
        return _trainingStart;
    }

    public void setTrainingStart(Timestamp trainingStart)
    {
        _trainingStart = trainingStart;
    }

    public Timestamp getTrainingEnd()
    {
        return _trainingEnd;
    }

    public void setTrainingEnd(Timestamp trainingEnd)
    {
        _trainingEnd = trainingEnd;
    }
}
