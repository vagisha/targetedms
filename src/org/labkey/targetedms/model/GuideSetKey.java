package org.labkey.targetedms.model;

import java.util.Objects;

public class GuideSetKey
{
    private final int _metricId;
    private final int _metricSeriesIndex;

    private final int _guideSetId;
    private final String _seriesLabel;
    private final int _hashCode;

    public GuideSetKey(int metricId, int metricSeriesIndex, int guideSetId, String seriesLabel)
    {
        _metricId = metricId;
        _metricSeriesIndex = metricSeriesIndex;
        _guideSetId = guideSetId;
        _seriesLabel = seriesLabel;
        _hashCode = Objects.hash(_metricId, _metricSeriesIndex, _guideSetId, _seriesLabel);
    }

    public int getMetricId()
    {
        return _metricId;
    }

    public int getMetricSeriesIndex()
    {
        return _metricSeriesIndex;
    }

    public int getGuideSetId()
    {
        return _guideSetId;
    }

    public String getSeriesLabel()
    {
        return _seriesLabel;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuideSetKey that = (GuideSetKey) o;
        return _metricId == that._metricId &&
                _metricSeriesIndex == that._metricSeriesIndex &&
                _guideSetId == that._guideSetId &&
                Objects.equals(_seriesLabel, that._seriesLabel);
    }

    @Override
    public int hashCode()
    {
        return _hashCode;
    }

    @Override
    public String toString()
    {
        return "GuideSet: " + getGuideSetId() + ", Metric: " + _metricId + "." + _metricSeriesIndex + ", Series: " + _seriesLabel;
    }
}
