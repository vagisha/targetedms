package org.labkey.test.components.targetedms;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cnathe on 4/29/15.
 */
public class GuideSetStats
{
    private String _queryName;
    private int _numRecords;
    private String _precursor;
    private Double _mean;
    private Double _stdDev;

    public GuideSetStats(String queryName, int numRecords)
    {
        _queryName = queryName;
        _numRecords = numRecords;
    }

    public GuideSetStats(String queryName, int numRecords, String precursor, Double mean, Double stdDev)
    {
        this(queryName, numRecords);
        _precursor = precursor;
        _mean = mean;
        _stdDev = stdDev;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    public int getNumRecords()
    {
        return _numRecords;
    }

    public String getPrecursor()
    {
        return _precursor;
    }

    public Double getMean()
    {
        return _mean;
    }

    public Double getStdDev()
    {
        return _stdDev;
    }
}
