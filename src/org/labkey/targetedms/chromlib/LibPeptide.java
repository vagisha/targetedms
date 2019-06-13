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
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibPeptide implements ObjectWithId
{
    private int _id;
    private Integer _proteinId;
    private String _sequence;
    private Integer _startIndex;
    private Integer _endIndex;
    private Character _previousAa;
    private Character _nextAa;
    private Double _calcNeutralMass;
    private Integer _numMissedCleavages;

    private List<LibPeptideStructuralModification> _structuralModifications;

    private List<LibPrecursor> _precursors;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public Integer getProteinId()
    {
        return _proteinId;
    }

    public void setProteinId(Integer proteinId)
    {
        _proteinId = proteinId;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public Integer getStartIndex()
    {
        return _startIndex;
    }

    public void setStartIndex(Integer startIndex)
    {
        _startIndex = startIndex;
    }

    public Integer getEndIndex()
    {
        return _endIndex;
    }

    public void setEndIndex(Integer endIndex)
    {
        _endIndex = endIndex;
    }

    public Character getPreviousAa()
    {
        return _previousAa;
    }

    public void setPreviousAa(Character previousAa)
    {
        _previousAa = previousAa;
    }

    public Character getNextAa()
    {
        return _nextAa;
    }

    public void setNextAa(Character nextAa)
    {
        _nextAa = nextAa;
    }

    public Double getCalcNeutralMass()
    {
        return _calcNeutralMass;
    }

    public void setCalcNeutralMass(Double calcNeutralMass)
    {
        _calcNeutralMass = calcNeutralMass;
    }

    public Integer getNumMissedCleavages()
    {
        return _numMissedCleavages;
    }

    public void setNumMissedCleavages(Integer numMissedCleavages)
    {
        _numMissedCleavages = numMissedCleavages;
    }

    public void addStructuralModification(LibPeptideStructuralModification structuralModification)
    {
        if(_structuralModifications == null)
        {
            _structuralModifications = new ArrayList<>();
        }
        _structuralModifications.add(structuralModification);
    }

    List<LibPeptideStructuralModification> getStructuralModifications()
    {
        if(_structuralModifications == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_structuralModifications);
    }

    public void addPrecursor(LibPrecursor precursor)
    {
        if(_precursors == null)
        {
            _precursors = new ArrayList<>();
        }
        _precursors.add(precursor);
    }

    List<LibPrecursor> getPrecursors()
    {
        if(_precursors == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_precursors);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPeptide)) return false;

        LibPeptide peptide = (LibPeptide) o;

        if (!_calcNeutralMass.equals(peptide._calcNeutralMass)) return false;
        if (_endIndex != null ? !_endIndex.equals(peptide._endIndex) : peptide._endIndex != null) return false;
        if (_nextAa != null ? !_nextAa.equals(peptide._nextAa) : peptide._nextAa != null) return false;
        if (!_numMissedCleavages.equals(peptide._numMissedCleavages)) return false;
        if (_precursors != null ? !_precursors.equals(peptide._precursors) : peptide._precursors != null) return false;
        if (_previousAa != null ? !_previousAa.equals(peptide._previousAa) : peptide._previousAa != null) return false;
        if (_proteinId != null ? !_proteinId.equals(peptide._proteinId) : peptide._proteinId != null) return false;
        if (!_sequence.equals(peptide._sequence)) return false;
        if (_startIndex != null ? !_startIndex.equals(peptide._startIndex) : peptide._startIndex != null) return false;
        if (_structuralModifications != null ? !_structuralModifications.equals(peptide._structuralModifications) : peptide._structuralModifications != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _proteinId != null ? _proteinId.hashCode() : 0;
        result = 31 * result + _sequence.hashCode();
        result = 31 * result + (_startIndex != null ? _startIndex.hashCode() : 0);
        result = 31 * result + (_endIndex != null ? _endIndex.hashCode() : 0);
        result = 31 * result + (_previousAa != null ? _previousAa.hashCode() : 0);
        result = 31 * result + (_nextAa != null ? _nextAa.hashCode() : 0);
        result = 31 * result + _calcNeutralMass.hashCode();
        result = 31 * result + _numMissedCleavages.hashCode();
        result = 31 * result + (_structuralModifications != null ? _structuralModifications.hashCode() : 0);
        result = 31 * result + (_precursors != null ? _precursors.hashCode() : 0);
        return result;
    }
}
