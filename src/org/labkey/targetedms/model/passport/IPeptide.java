package org.labkey.targetedms.model.passport;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IPeptide
{
    private String sequence;
    private int startIndex;
    private int endIndex;
    private long proteinId;
    private long panoramaPeptideId;

    private List<ReplicateInfo> replicateInfo = new ArrayList<>();

    private double beforeIntensity;
    private double beforeTotalArea;
    private long precursorbeforeid;
    private double beforeSumArea;

    private double afterIntensity;
    private double afterTotalArea;
    private long precursorafterid;
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

    public void addReplicateInfo(String replicate, String timepoint, String index, long intensity, long precursorId, Date acquiredTime)
    {
        replicateInfo.add(new ReplicateInfo(replicate, timepoint, index, intensity, precursorId, acquiredTime));
    }

    public List<ReplicateInfo> getReplicateInfo()
    {
        return replicateInfo;
    }

    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.put("beforeintensity", getBeforeIntensity());
        result.put("normalizedafterintensity", getAfterIntensity());
        result.put("startindex", getStartIndex());
        result.put("endindex", getEndIndex());
        result.put("sequence", getSequence());
        result.put("panoramapeptideid", getPanoramaPeptideId());
        result.put("panoramaprecursorbeforeid", getPrecursorbeforeid());
        result.put("panoramaprecursorafterid", getPrecursorafterid());

        JSONArray replicateInfo = new JSONArray();
        for (IPeptide.ReplicateInfo info : getReplicateInfo())
        {
            JSONObject i = new JSONObject();
            i.put("grouping", info.getGrouping());
            i.put("timepoint", info.getTimepoint());
            i.put("replicate", info.getReplicate());
            i.put("acquiredTime", info.getAcquiredTime());
            i.put("intensity", info.getIntensity());
            i.put("precursorChromInfoId", info.getPrecursorChromInfoId());
            replicateInfo.put(i);
        }
        result.put("replicateInfo", replicateInfo);
        return result;
    }

    public static class ReplicateInfo
    {
        private final String _replicate;
        private final String _timepoint;
        private final String _grouping;
        private final double _intensity;
        private final long _precursorChromInfoId;
        private final Date _acquiredTime;

        public ReplicateInfo(String replicate, String timepoint, String grouping, double intensity, long precursorChromInfoId, Date acquiredTime)
        {
            _replicate = replicate;
            _timepoint = timepoint;
            _grouping = grouping;
            _intensity = intensity;
            _precursorChromInfoId = precursorChromInfoId;
            _acquiredTime = acquiredTime;
        }

        public String getReplicate()
        {
            return _replicate;
        }

        public String getTimepoint()
        {
            return _timepoint;
        }

        public String getGrouping()
        {
            return _grouping;
        }

        public double getIntensity()
        {
            return _intensity;
        }

        public long getPrecursorChromInfoId()
        {
            return _precursorChromInfoId;
        }

        public Date getAcquiredTime()
        {
            return _acquiredTime;
        }
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
