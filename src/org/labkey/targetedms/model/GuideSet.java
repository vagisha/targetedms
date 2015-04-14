package org.labkey.targetedms.model;

import org.labkey.api.data.Entity;

import java.util.Date;

/**
 * Created by cnathe on 4/9/2015.
 */
public class GuideSet extends Entity
{
    private int _rowId;

    private String _comment;
    private Date _trainingStart;
    private Date _trainingEnd;

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

    public Date getTrainingStart()
    {
        return _trainingStart;
    }

    public void setTrainingStart(Date trainingStart)
    {
        _trainingStart = trainingStart;
    }

    public Date getTrainingEnd()
    {
        return _trainingEnd;
    }

    public void setTrainingEnd(Date trainingEnd)
    {
        _trainingEnd = trainingEnd;
    }
}
