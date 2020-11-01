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

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibPrecursor extends AbstractLibPrecursor<LibTransition>
{
    private long _peptideId;
    private Double _neutralMass;
    private String _modifiedSequence;

    private List<LibPrecursorIsotopeModification> _isotopeModifications;

    public LibPrecursor() {}

    public LibPrecursor(Precursor precursor, Map<Long, String> isotopeLabelMap, PrecursorChromInfo bestChromInfo, TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        super(precursor, isotopeLabelMap, bestChromInfo, run, sampleFileIdMap);
        setNeutralMass(precursor.getNeutralMass());
        setModifiedSequence(precursor.getModifiedSequence());
    }

    public long getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(long peptideId)
    {
        _peptideId = peptideId;
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
        if (_charge != precursor._charge) return false;
        if (!Arrays.equals(_chromatogram, precursor._chromatogram)) return false;
        if (_collisionEnergy != null ? !_collisionEnergy.equals(precursor._collisionEnergy) : precursor._collisionEnergy != null)
            return false;
        if (_declusteringPotential != null ? !_declusteringPotential.equals(precursor._declusteringPotential) : precursor._declusteringPotential != null)
            return false;
        if (!_isotopeLabel.equals(precursor._isotopeLabel)) return false;
        if (_isotopeModifications != null ? !_isotopeModifications.equals(precursor._isotopeModifications) : precursor._isotopeModifications != null)
            return false;
        if (!_modifiedSequence.equals(precursor._modifiedSequence)) return false;
        if (_mz != precursor._mz) return false;
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
        int result = (int) _peptideId;
        result = 31 * result + getIsotopeLabel().hashCode();
        result = 31 * result + Double.hashCode(_mz);
        result = 31 * result + _charge;
        result = 31 * result + _neutralMass.hashCode();
        result = 31 * result + _modifiedSequence.hashCode();
        result = 31 * result + (_collisionEnergy != null ? _collisionEnergy.hashCode() : 0);
        result = 31 * result + (_declusteringPotential != null ? _declusteringPotential.hashCode() : 0);
        result = 31 * result + _totalArea.hashCode();
        result = 31 * result + (_averageMassErrorPPM != null ? _averageMassErrorPPM.hashCode() : 0);
        result = 31 * result + (_numPoints != null ? _numPoints.hashCode() : 0);
        result = 31 * result + (_numTransitions != null ? _numTransitions.hashCode() : 0);
        result = (int) (31 * result + _sampleFileId);
        result = 31 * result + (_chromatogram != null ? Arrays.hashCode(_chromatogram) : 0);
        result = 31 * result + (_transitions != null ? _transitions.hashCode() : 0);
        result = 31 * result + (_retentionTimes != null ? _retentionTimes.hashCode() : 0);
        result = 31 * result + (_isotopeModifications != null ? _isotopeModifications.hashCode() : 0);
        return result;
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() + getIsotopeModifications().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();
    }
}
