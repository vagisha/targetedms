package org.labkey.targetedms.model.passport;

public class IPeptide
{
    private String sequence;
    private String peptideModifiedSequence;
    private int startIndex;
    private int endIndex;
    private long proteinId;
    private long panoramaPeptideId;
    private double beforeIntensity;
    private double beforeTotalArea;
    private double afterIntensity;
    private double afterTotalArea;
    private long precursorbeforeid;
    private long precursorafterid;
    private double beforeSumArea;
    private double afterSumArea;

    public double getBeforeSumArea()
    {
        return beforeSumArea;
    }

    public void setBeforeSumArea(double beforeSumArea)
    {
        this.beforeSumArea = beforeSumArea;
    }

    public double getAfterSumArea()
    {
        return afterSumArea;
    }

    public void setAfterSumArea(double afterSumArea)
    {
        this.afterSumArea = afterSumArea;
    }


    public long getPrecursorafterid()
    {
        return precursorafterid;
    }

    public void setPrecursorafterid(long precursorafterid)
    {
        this.precursorafterid = precursorafterid;
    }


    public double getBeforeTotalArea()
    {
        return beforeTotalArea;
    }

    public void setBeforeTotalArea(double beforeTotalArea)
    {
        this.beforeTotalArea = beforeTotalArea;
    }

    public double getAfterTotalArea()
    {
        return afterTotalArea;
    }

    public void setAfterTotalArea(double afterTotalArea)
    {
        this.afterTotalArea = afterTotalArea;
    }

    public String getPeptideModifiedSequence()
    {
        return peptideModifiedSequence;
    }

    public void setPeptideModifiedSequence(String peptideModifiedSequence)
    {
        this.peptideModifiedSequence = peptideModifiedSequence;
    }

    public long getPrecursorbeforeid()
    {
        return precursorbeforeid;
    }

    public void setPrecursorbeforeid(long precursorbeforeid)
    {
        this.precursorbeforeid = precursorbeforeid;
    }

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }

    public int getStartIndex()
    {
        return startIndex;
    }

    public void setStartIndex(int startIndex)
    {
        this.startIndex = startIndex;
    }

    public int getEndIndex()
    {
        return endIndex;
    }

    public void setEndIndex(int endIndex)
    {
        this.endIndex = endIndex;
    }

    public long getProteinId()
    {
        return proteinId;
    }

    public void setProteinId(long proteinId)
    {
        this.proteinId = proteinId;
    }

    public long getPanoramaPeptideId()
    {
        return panoramaPeptideId;
    }

    public void setPanoramaPeptideId(long panoramaPeptideId)
    {
        this.panoramaPeptideId = panoramaPeptideId;
    }

    public double getBeforeIntensity()
    {
        return beforeIntensity;
    }

    public void setBeforeIntensity(double beforeIntensity)
    {
        this.beforeIntensity = beforeIntensity;
    }

    public double getAfterIntensity()
    {
        return afterIntensity;
    }

    public void setAfterIntensity(double afterIntensity)
    {
        this.afterIntensity = afterIntensity;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!IPeptide.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        final IPeptide other = (IPeptide) o;
        return this.getPanoramaPeptideId() == other.getPanoramaPeptideId();
    }

    public int hashCode() {
        return (int) getPanoramaPeptideId();
    }
}
