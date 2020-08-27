/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.parser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:04 PM
 */
public class TransitionChromInfo //extends ChromInfo<TransitionChromInfoAnnotation>
{
    private long _transitionId;
    private long _precursorChromInfoId;

    private Double _retentionTime;
    private Double _startTime;
    private Double _endTime;
    private Double _height;
    private Double _area;
    private Double _areaNormalized;
    private Double _background;
    private Double _massErrorPPM;
    private Double _fwhm;
    private Boolean _fwhmDegenerate;
    private Boolean _truncated;
    private Integer _peakRank;
    private String _identified;
    private Integer _optimizationStep;
    private String _userSet;
    private String _note;
    private int _chromatogramIndex;
    private Integer _pointsAcrossPeak;
    private Double _ccs;
    private Double _driftTime;
    private Double _driftTimeWindow;
    private Double _ionMobility;
    private Double _ionMobilityWindow;
    private String _ionMobilityType;
    private Integer Rank;
    private Integer RankByLevel;
    private Boolean ForcedIntegration;

    /** HACK TO UPDATE TO LONG FOR ID COLUMN - ISSUE 40831 */
    private String _replicateName;
    private String _skylineSampleFileId;
    private long _sampleFileId;
    private List<TransitionChromInfoAnnotation> _annotations = Collections.emptyList();
    private long _id;

    public String getSkylineSampleFileId()
    {
        return _skylineSampleFileId;
    }

    public void setSkylineSampleFileId(String skylineSampleFileId)
    {
        _skylineSampleFileId = skylineSampleFileId;
    }

    public String getReplicateName()
    {
        return _replicateName;
    }

    public void setReplicateName(String replicateName)
    {
        _replicateName = replicateName;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }


    public long getId()
    {
        return _id;
    }
    public void setId(long id)
    {
        _id = id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransitionChromInfo that = (TransitionChromInfo) o;
        return _id == that._id;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_id, getClass());
    }

    public List<TransitionChromInfoAnnotation> getAnnotations()
    {
        return _annotations;
    }

    public void setAnnotations(List<TransitionChromInfoAnnotation> annotations)
    {
        _annotations = Collections.unmodifiableList(annotations);
    }
    /** END HACK TO UPDATE TO LONG FOR ID COLUMN */



    public long getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(long transitionId)
    {
        _transitionId = transitionId;
    }

    public long getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(long precursorChromInfoId)
    {
        _precursorChromInfoId = precursorChromInfoId;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Double startTime)
    {
        _startTime = startTime;
    }

    public Double getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Double endTime)
    {
        _endTime = endTime;
    }

    public Double getHeight()
    {
        return _height;
    }

    public void setHeight(Double height)
    {
        _height = height;
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        _area = area;
    }

    public Double getAreaNormalized()
    {
        return _areaNormalized;
    }

    public void setAreaNormalized(Double areaNormalized)
    {
        _areaNormalized = areaNormalized;
    }

    public Double getBackground()
    {
        return _background;
    }

    public void setBackground(Double background)
    {
        _background = background;
    }

    public Double getMassErrorPPM()
    {
        return _massErrorPPM;
    }

    public void setMassErrorPPM(Double massErrorPPM)
    {
        _massErrorPPM = massErrorPPM;
    }

    public Double getFwhm()
    {
        return _fwhm;
    }

    public void setFwhm(Double fwhm)
    {
        _fwhm = fwhm;
    }

    public Boolean getFwhmDegenerate()
    {
        return _fwhmDegenerate;
    }

    public void setFwhmDegenerate(Boolean fwhmDegenerate)
    {
        _fwhmDegenerate = fwhmDegenerate;
    }

    public Boolean getTruncated()
    {
        return _truncated;
    }

    public void setTruncated(Boolean truncated)
    {
        _truncated = truncated;
    }

    public Integer getPeakRank()
    {
        return _peakRank;
    }

    public void setPeakRank(Integer peakRank)
    {
        _peakRank = peakRank;
    }

    public String getIdentified()
    {
        return _identified;
    }

    public void setIdentified(String identified)
    {
        _identified = identified;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        _optimizationStep = optimizationStep;
    }

    public boolean isOptimizationPeak()
    {
        return _optimizationStep != null;
    }

    public String getUserSet()
    {
        return _userSet;
    }

    public void setUserSet(String userSet)
    {
        _userSet = userSet;
    }

    public String getNote()
    {
        return _note;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public void setChromatogramIndex(int chromatogramIndex)
    {
        _chromatogramIndex = chromatogramIndex;
    }

    public int getChromatogramIndex()
    {
        return _chromatogramIndex;
    }

    public Integer getPointsAcrossPeak()
    {
        return _pointsAcrossPeak;
    }

    public void setPointsAcrossPeak(Integer pointsAcrossPeak)
    {
        _pointsAcrossPeak = pointsAcrossPeak;
    }

    public Double getCcs()
    {
        return _ccs;
    }

    public void setCcs(Double ccs)
    {
        _ccs = ccs;
    }

    public Double getDriftTime()
    {
        return _driftTime;
    }

    public void setDriftTime(Double driftTime)
    {
        _driftTime = driftTime;
    }

    public Double getDriftTimeWindow()
    {
        return _driftTimeWindow;
    }

    public void setDriftTimeWindow(Double driftTimeWindow)
    {
        _driftTimeWindow = driftTimeWindow;
    }

    public Double getIonMobility()
    {
        return _ionMobility;
    }

    public void setIonMobility(Double ionMobility)
    {
        _ionMobility = ionMobility;
    }

    public Double getIonMobilityWindow()
    {
        return _ionMobilityWindow;
    }

    public void setIonMobilityWindow(Double ionMobilityWindow)
    {
        _ionMobilityWindow = ionMobilityWindow;
    }

    public String getIonMobilityType()
    {
        return _ionMobilityType;
    }

    public void setIonMobilityType(String ionMobilityType)
    {
        _ionMobilityType = ionMobilityType;
    }

    public Integer getRank()
    {
        return Rank;
    }

    public void setRank(Integer rank)
    {
        Rank = rank;
    }

    public Integer getRankByLevel()
    {
        return RankByLevel;
    }

    public void setRankByLevel(Integer rankByLevel)
    {
        RankByLevel = rankByLevel;
    }

    public Boolean getForcedIntegration()
    {
        return ForcedIntegration;
    }

    public void setForcedIntegration(Boolean forcedIntegration)
    {
        ForcedIntegration = forcedIntegration;
    }
}
