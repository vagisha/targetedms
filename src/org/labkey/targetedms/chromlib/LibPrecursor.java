/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibPrecursor implements ObjectWithId
{
    private int _id;
    private int _peptideId;
    private String _isotopeLabel;
    private Double _mz;
    private Integer _charge;
    private Double _neutralMass;
    private String _modifiedSequence;
    private Double _collisionEnergy;
    private Double _declusteringPotential;
    private Double _totalArea;
    private byte[] _chromatogram;
    private int _uncompressedSize;
    private int _chromatogramFormat;
    private int _sampleFileId;

    private List<LibTransition> _transitions;

    private List<LibPrecursorRetentionTime> _retentionTimes;

    private List<LibPrecursorIsotopeModification> _isotopeModifications;
    private Integer _numTransitions;
    private Integer _numPoints;
    private Double _averageMassErrorPPM;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(int peptideId)
    {
        _peptideId = peptideId;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
    }

    public Integer getCharge()
    {
        return _charge;
    }

    public void setCharge(Integer charge)
    {
        _charge = charge;
    }

    public Double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(Double neutralMass)
    {
        _neutralMass = neutralMass;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        _collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        _declusteringPotential = declusteringPotential;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getUncompressedSize()
    {
        return _uncompressedSize;
    }

    public void setUncompressedSize(int uncompressedSize)
    {
        _uncompressedSize = uncompressedSize;
    }

    public int getChromatogramFormat()
    {
        return _chromatogramFormat;
    }

    public void setChromatogramFormat(int chromatogramFormat)
    {
        _chromatogramFormat = chromatogramFormat;
    }

    public void addTransition(LibTransition transition)
    {
        if(_transitions == null)
        {
            _transitions = new ArrayList<>();
        }
        _transitions.add(transition);
    }

    public List<LibTransition> getTransitions()
    {
        if(_transitions == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_transitions);
    }

    public void addRetentionTime(LibPrecursorRetentionTime retentionTime)
    {
        if(_retentionTimes == null)
        {
            _retentionTimes = new ArrayList<>();
        }
        _retentionTimes.add(retentionTime);
    }

    public List<LibPrecursorRetentionTime> getRetentionTimes()
    {
        if(_retentionTimes == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_retentionTimes);
    }

    public void addIsotopeModification(LibPrecursorIsotopeModification isotopeModification)
    {
        if(_isotopeModifications == null)
        {
            _isotopeModifications = new ArrayList<>();
        }
        _isotopeModifications.add(isotopeModification);
    }

    public List<LibPrecursorIsotopeModification> getIsotopeModifications()
    {
        if(_isotopeModifications == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_isotopeModifications);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPrecursor)) return false;

        LibPrecursor precursor = (LibPrecursor) o;

        if (_peptideId != precursor._peptideId) return false;
        if (!_charge.equals(precursor._charge)) return false;
        if (!Arrays.equals(_chromatogram, precursor._chromatogram)) return false;
        if (_collisionEnergy != null ? !_collisionEnergy.equals(precursor._collisionEnergy) : precursor._collisionEnergy != null)
            return false;
        if (_declusteringPotential != null ? !_declusteringPotential.equals(precursor._declusteringPotential) : precursor._declusteringPotential != null)
            return false;
        if (!_isotopeLabel.equals(precursor._isotopeLabel)) return false;
        if (_isotopeModifications != null ? !_isotopeModifications.equals(precursor._isotopeModifications) : precursor._isotopeModifications != null)
            return false;
        if (!_modifiedSequence.equals(precursor._modifiedSequence)) return false;
        if (!_mz.equals(precursor._mz)) return false;
        if (_sampleFileId != precursor._sampleFileId) return false;
        if (!_neutralMass.equals(precursor._neutralMass)) return false;
        if (_retentionTimes != null ? !_retentionTimes.equals(precursor._retentionTimes) : precursor._retentionTimes != null)
            return false;
        if (!_totalArea.equals(precursor._totalArea)) return false;
        if (_transitions != null ? !_transitions.equals(precursor._transitions) : precursor._transitions != null)
            return false;
        if (_numTransitions != null ? !_numTransitions.equals(precursor._numTransitions) : precursor._numTransitions != null)
            return false;
        if (_numPoints != null ? !_numPoints.equals(precursor._numPoints) : precursor._numPoints != null)
            return false;
        if (_averageMassErrorPPM != null ? !_averageMassErrorPPM.equals(precursor._averageMassErrorPPM) : precursor._averageMassErrorPPM != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _peptideId;
        result = 31 * result + _isotopeLabel.hashCode();
        result = 31 * result + _mz.hashCode();
        result = 31 * result + _charge.hashCode();
        result = 31 * result + _neutralMass.hashCode();
        result = 31 * result + _modifiedSequence.hashCode();
        result = 31 * result + (_collisionEnergy != null ? _collisionEnergy.hashCode() : 0);
        result = 31 * result + (_declusteringPotential != null ? _declusteringPotential.hashCode() : 0);
        result = 31 * result + _totalArea.hashCode();
        result = 31 * result + (_averageMassErrorPPM != null ? _averageMassErrorPPM.hashCode() : 0);
        result = 31 * result + (_numPoints != null ? _numPoints.hashCode() : 0);
        result = 31 * result + (_numTransitions != null ? _numTransitions.hashCode() : 0);
        result = 31 * result + _sampleFileId;
        result = 31 * result + (_chromatogram != null ? Arrays.hashCode(_chromatogram) : 0);
        result = 31 * result + (_transitions != null ? _transitions.hashCode() : 0);
        result = 31 * result + (_retentionTimes != null ? _retentionTimes.hashCode() : 0);
        result = 31 * result + (_isotopeModifications != null ? _isotopeModifications.hashCode() : 0);
        return result;
    }

    public void setNumTransitions(Integer numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public Integer getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumPoints(Integer numPoints)
    {
        _numPoints = numPoints;
    }

    public Integer getNumPoints()
    {
        return _numPoints;
    }

    public void setAverageMassErrorPPM(Double averageMassErrorPPM)
    {
        _averageMassErrorPPM = averageMassErrorPPM;
    }

    public Double getAverageMassErrorPPM()
    {
        return _averageMassErrorPPM;
    }

    public int getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(int sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }
}
