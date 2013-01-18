package org.labkey.targetedms.chromlib;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 1:38 PM
 */

public class LibPrecursorRetentionTime implements ObjectWithId
{
    private int _id;
    private int _precursorId;
    private int _sampleFileId;
    private Double _retentionTime;
    private Double _startTime;
    private Double _endTime;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        this._id = id;
    }

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        this._precursorId = precursorId;
    }

    public int getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(int sampleFileId)
    {
        this._sampleFileId = sampleFileId;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        this._retentionTime = retentionTime;
    }

    public Double getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Double startTime)
    {
        this._startTime = startTime;
    }

    public Double getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Double endTime)
    {
        this._endTime = endTime;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPrecursorRetentionTime)) return false;

        LibPrecursorRetentionTime that = (LibPrecursorRetentionTime) o;

        if (_precursorId != that._precursorId) return false;
        if (_sampleFileId != that._sampleFileId) return false;
        if (_endTime != null ? !_endTime.equals(that._endTime) : that._endTime != null) return false;
        if (_retentionTime != null ? !_retentionTime.equals(that._retentionTime) : that._retentionTime != null)
            return false;
        if (_startTime != null ? !_startTime.equals(that._startTime) : that._startTime != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _precursorId;
        result = 31 * result + _sampleFileId;
        result = 31 * result + (_retentionTime != null ? _retentionTime.hashCode() : 0);
        result = 31 * result + (_startTime != null ? _startTime.hashCode() : 0);
        result = 31 * result + (_endTime != null ? _endTime.hashCode() : 0);
        return result;
    }
}
