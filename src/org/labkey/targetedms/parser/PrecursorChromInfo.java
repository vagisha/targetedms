/*
 * Copyright (c) 2012 LabKey Corporation
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
 * Time: 3:39 PM
 */
public class PrecursorChromInfo extends ChromInfo<PrecursorChromInfoAnnotation>
{
    private int _precursorId;
    private int _peptideChromInfoId;

    private Double _bestRetentionTime;
    private Double _minStartTime;
    private Double _maxEndTime;
    private Double _totalArea;
    private Double _totalAreaNormalized;
    private Double _totalBackground;
    private Double _maxFwhm;
    private Double _maxHeight;
    private Double _averageMassErrorPPM;
    private Double _peakCountRatio;
    private Integer _numTruncated;
    private Boolean _identified;
    private Double _libraryDotP;
    private Double _isotopeDotP;
    private Integer _optimizationStep;
    private Boolean _userSet;
    private String _note;

    private byte[] _chromatogram;
    private int _numPoints;
    private int _numTransitions;


    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        this._precursorId = precursorId;
    }

    public int getPeptideChromInfoId()
    {
        return _peptideChromInfoId;
    }

    public void setPeptideChromInfoId(int peptideChromInfoId)
    {
        this._peptideChromInfoId = peptideChromInfoId;
    }

    public Double getBestRetentionTime()
    {
        return _bestRetentionTime;
    }

    public void setBestRetentionTime(Double bestRetentionTime)
    {
        this._bestRetentionTime = bestRetentionTime;
    }

    public Double getMinStartTime()
    {
        return _minStartTime;
    }

    public void setMinStartTime(Double minStartTime)
    {
        this._minStartTime = minStartTime;
    }

    public Double getMaxEndTime()
    {
        return _maxEndTime;
    }

    public void setMaxEndTime(Double maxEndTime)
    {
        this._maxEndTime = maxEndTime;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        this._totalArea = totalArea;
    }

    public Double getTotalAreaNormalized()
    {
        return _totalAreaNormalized;
    }

    public void setTotalAreaNormalized(Double totalAreaNormalized)
    {
        this._totalAreaNormalized = totalAreaNormalized;
    }

    public Double getTotalBackground()
    {
        return _totalBackground;
    }

    public void setTotalBackground(Double totalBackground)
    {
        this._totalBackground = totalBackground;
    }

    public Double getMaxFwhm()
    {
        return _maxFwhm;
    }

    public void setMaxFwhm(Double maxFwhm)
    {
        this._maxFwhm = maxFwhm;
    }

    public Double getMaxHeight()
    {
        return _maxHeight;
    }

    public void setMaxHeight(Double maxHeight)
    {
        _maxHeight = maxHeight;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getPeakCountRatio()
    {
        return _peakCountRatio;
    }

    public void setPeakCountRatio(Double peakCountRatio)
    {
        this._peakCountRatio = peakCountRatio;
    }

    public Integer getNumTruncated()
    {
        return _numTruncated;
    }

    public void setNumTruncated(Integer numTruncated)
    {
        this._numTruncated = numTruncated;
    }

    public Boolean getIdentified()
    {
        return _identified;
    }

    public void setIdentified(Boolean identified)
    {
        this._identified = identified;
    }

    public Double getLibraryDotP()
    {
        return _libraryDotP;
    }

    public void setLibraryDotP(Double libraryDotP)
    {
        this._libraryDotP = libraryDotP;
    }

    public Double getIsotopeDotP()
    {
        return _isotopeDotP;
    }

    public void setIsotopeDotP(Double isotopeDotP)
    {
        this._isotopeDotP = isotopeDotP;
    }

    public Integer getOptimizationStep()
    {
        return _optimizationStep;
    }

    public void setOptimizationStep(Integer optimizationStep)
    {
        this._optimizationStep = optimizationStep;
    }

    public Boolean getUserSet()
    {
        return _userSet;
    }

    public void setUserSet(Boolean userSet)
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

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getNumPoints()
    {
        return _numPoints;
    }

    public void setNumPoints(int numPoints)
    {
        _numPoints = numPoints;
    }

    public int getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumTransitions(int numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public Chromatogram createChromatogram()
    {
        Chromatogram result = new Chromatogram();
        result.setChromatogram(_chromatogram);
        result.setNumPoints(_numPoints);
        result.setNumTransitions(_numTransitions);
        return result;
    }
}
