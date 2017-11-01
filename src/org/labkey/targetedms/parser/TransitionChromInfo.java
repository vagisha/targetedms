/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:04 PM
 */
public class TransitionChromInfo extends ChromInfo<TransitionChromInfoAnnotation>
{
    private int _transitionId;
    private int _precursorChromInfoId;

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


    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        this._transitionId = transitionId;
    }

    public int getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(int precursorChromInfoId)
    {
        this._precursorChromInfoId = precursorChromInfoId;
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

    public Double getHeight()
    {
        return _height;
    }

    public void setHeight(Double height)
    {
        this._height = height;
    }

    public Double getArea()
    {
        return _area;
    }

    public void setArea(Double area)
    {
        this._area = area;
    }

    public Double getAreaNormalized()
    {
        return _areaNormalized;
    }

    public void setAreaNormalized(Double areaNormalized)
    {
        this._areaNormalized = areaNormalized;
    }

    public Double getBackground()
    {
        return _background;
    }

    public void setBackground(Double background)
    {
        this._background = background;
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
        this._fwhm = fwhm;
    }

    public Boolean getFwhmDegenerate()
    {
        return _fwhmDegenerate;
    }

    public void setFwhmDegenerate(Boolean fwhmDegenerate)
    {
        this._fwhmDegenerate = fwhmDegenerate;
    }

    public Boolean getTruncated()
    {
        return _truncated;
    }

    public void setTruncated(Boolean truncated)
    {
        this._truncated = truncated;
    }

    public Integer getPeakRank()
    {
        return _peakRank;
    }

    public void setPeakRank(Integer peakRank)
    {
        this._peakRank = peakRank;
    }

    public String getIdentified()
    {
        return _identified;
    }

    public void setIdentified(String identified)
    {
        this._identified = identified;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        this._optimizationStep = optimizationStep;
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
        this._userSet = userSet;
    }

    public String getNote()
    {
        return _note;
    }

    public void setNote(String note)
    {
        this._note = note;
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
}
