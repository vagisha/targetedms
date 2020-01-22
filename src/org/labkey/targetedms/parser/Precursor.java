/*
 * Copyright (c) 2012-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Precursor extends GeneralPrecursor<Transition>
{
    private String _modifiedSequence;
    private double _neutralMass;
    private Double _decoyMassShift;
    private BibliospecLibraryInfo _bibliospecLibraryInfo;
    private HunterLibraryInfo _hunterLibraryInfo;
    private NistLibraryInfo _nistLibraryInfo;
    private SpectrastLibraryInfo _spectrastLibraryInfo;
    private ChromatogramLibraryInfo _chromatogramLibraryInfo;

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(double neutralMass)
    {
        _neutralMass = neutralMass;
    }

    public Double getDecoyMassShift()
    {
        return _decoyMassShift;
    }

    public void setDecoyMassShift(Double decoyMassShift)
    {
        _decoyMassShift = decoyMassShift;
    }

    public BibliospecLibraryInfo getBibliospecLibraryInfo()
    {
        return _bibliospecLibraryInfo;
    }

    public void setBibliospecLibraryInfo(BibliospecLibraryInfo bibliospecLibraryInfo)
    {
        _bibliospecLibraryInfo = bibliospecLibraryInfo;
    }

    public HunterLibraryInfo getHunterLibraryInfo()
    {
        return _hunterLibraryInfo;
    }

    public void setHunterLibraryInfo(HunterLibraryInfo hunterLibraryInfo)
    {
        _hunterLibraryInfo = hunterLibraryInfo;
    }

    public NistLibraryInfo getNistLibraryInfo()
    {
        return _nistLibraryInfo;
    }

    public void setNistLibraryInfo(NistLibraryInfo nistLibraryInfo)
    {
        _nistLibraryInfo = nistLibraryInfo;
    }

    public SpectrastLibraryInfo getSpectrastLibraryInfo()
    {
        return _spectrastLibraryInfo;
    }

    public void setSpectrastLibraryInfo(SpectrastLibraryInfo spectrastLibraryInfo)
    {
        _spectrastLibraryInfo = spectrastLibraryInfo;
    }

    public ChromatogramLibraryInfo getChromatogramLibraryInfo()
    {
        return _chromatogramLibraryInfo;
    }

    public void setChromatogramLibraryInfo(ChromatogramLibraryInfo chromatogramLibraryInfo)
    {
        _chromatogramLibraryInfo = chromatogramLibraryInfo;
    }

    @Override
    public String toString()
    {
        return getModifiedSequence();
    }

    public static class LibraryInfo extends SkylineEntity
    {
        private int _precursorId;
        private int _generalPrecursorId;
        private int _spectrumLibraryId;

        private String _libraryName;

        public int getPrecursorId()
        {
            return _precursorId;
        }

        public void setPrecursorId(int precursorId)
        {
            _precursorId = precursorId;
        }

        public int getGeneralPrecursorId()
        {
            return _generalPrecursorId;
        }

        public void setGeneralPrecursorId(int generalPrecursorId)
        {
            _generalPrecursorId = generalPrecursorId;
        }

        public int getSpectrumLibraryId()
        {
            return _spectrumLibraryId;
        }

        public void setSpectrumLibraryId(int spectrumLibraryId)
        {
            _spectrumLibraryId = spectrumLibraryId;
        }

        public String getLibraryName()
        {
            return _libraryName;
        }

        public void setLibraryName(String libraryName)
        {
            _libraryName = libraryName;
        }
    }

    public static final class BibliospecLibraryInfo extends LibraryInfo
    {
        private Double _countMeasured;
        private Double _score;
        private String _scoreType;

        public Double getCountMeasured()
        {
            return _countMeasured;
        }

        public void setCountMeasured(Double countMeasured)
        {
            _countMeasured = countMeasured;
        }

        public Double getScore()
        {
            return _score;
        }

        public void setScore(Double score)
        {
            _score = score;
        }

        public String getScoreType()
        {
            return _scoreType;
        }

        public void setScoreType(String scoreType)
        {
            _scoreType = scoreType;
        }
    }

    public static final class HunterLibraryInfo extends LibraryInfo
    {
        private Double _expect;
        private Double _processedIntensity;

        public Double getExpect()
        {
            return _expect;
        }

        public void setExpect(Double expect)
        {
            _expect = expect;
        }

        public Double getProcessedIntensity()
        {
            return _processedIntensity;
        }

        public void setProcessedIntensity(Double processedIntensity)
        {
            _processedIntensity = processedIntensity;
        }
    }

    public static final class NistLibraryInfo extends LibraryInfo
    {
        private Double _countMeasured;
        private Double _totalIntensity;
        private Double _tfRatio;

        public Double getCountMeasured()
        {
            return _countMeasured;
        }

        public void setCountMeasured(Double countMeasured)
        {
            _countMeasured = countMeasured;
        }

        public Double getTotalIntensity()
        {
            return _totalIntensity;
        }

        public void setTotalIntensity(Double totalIntensity)
        {
            _totalIntensity = totalIntensity;
        }

        public Double getTfRatio()
        {
            return _tfRatio;
        }

        public void setTfRatio(Double tfRatio)
        {
            _tfRatio = tfRatio;
        }
    }

    public static final class SpectrastLibraryInfo extends LibraryInfo
    {
        private Double _countMeasured;
        private Double _totalIntensity;
        private Double _tfRatio;

        public Double getCountMeasured()
        {
            return _countMeasured;
        }

        public void setCountMeasured(Double countMeasured)
        {
            _countMeasured = countMeasured;
        }

        public Double getTotalIntensity()
        {
            return _totalIntensity;
        }

        public void setTotalIntensity(Double totalIntensity)
        {
            _totalIntensity = totalIntensity;
        }

        public Double getTfRatio()
        {
            return _tfRatio;
        }

        public void setTfRatio(Double tfRatio)
        {
            _tfRatio = tfRatio;
        }
    }

    public static final class ChromatogramLibraryInfo extends LibraryInfo
    {
        private Double _peakArea;

        public Double getPeakArea()
        {
            return _peakArea;
        }

        public void setPeakArea(Double peakArea)
        {
            _peakArea = peakArea;
        }
    }
}
