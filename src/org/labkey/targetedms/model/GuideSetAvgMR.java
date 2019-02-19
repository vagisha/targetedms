package org.labkey.targetedms.model;

import java.util.Objects;

public class GuideSetAvgMR
{
    private int guideSetid;
    private String series;
    private String seriesLabel;
    private String seriesType;

    public int getGuideSetid()
    {
        return guideSetid;
    }

    public void setGuideSetid(int guideSetid)
    {
        this.guideSetid = guideSetid;
    }

    public String getSeries()
    {
        return series;
    }

    public void setSeries(String series)
    {
        this.series = series;
    }

    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    public String getSeriesType()
    {
        return seriesType;
    }

    public void setSeriesType(String seriesType)
    {
        this.seriesType = seriesType;
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuideSetAvgMR that = (GuideSetAvgMR) o;
        return guideSetid == that.guideSetid &&
                series.equals(that.series) &&
                seriesLabel.equals(that.seriesLabel) &&
                seriesType.equals(that.seriesType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(guideSetid, series, seriesLabel, seriesType);
    }
}
