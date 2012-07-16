/*
 * Copyright (c) 2012 LabKey Corporation
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

import java.util.List;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Peptide extends AnnotatedEntity<PeptideAnnotation>
{
    private int _peptideGroupId;

    private String _sequence;
    private String _prevAa;
    private String _nextAa;
    private Integer _start;
    private Integer _end;
    private double _calcNeutralMass;
    private int _numMissedCleavages;
    private Double _predictedRetentionTime;
    private Double _avgMeasuredRetentionTime;  // average measured retention time over all replicates


    private Integer _rank;  // peptide rank either by spectrum count or by the total intensity
                           // of the picked transition peaks.
                           // If there are multiple precursorList, the rank of the best precursor is used.

    private Boolean _decoy;

    private List<Precursor> _precursorList;
    private List<PeptideChromInfo> _peptideChromInfoList;

    private List<StructuralModification> _structuralMods;
    private List<IsotopeModification> _isotopeMods;
    private String _note;


    public int getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(int peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public String getPrevAa()
    {
        return _prevAa;
    }

    public void setPrevAa(String prevAa)
    {
        _prevAa = prevAa;
    }

    public String getNextAa()
    {
        return _nextAa;
    }

    public void setNextAa(String nextAa)
    {
        _nextAa = nextAa;
    }

    public Integer getStart()
    {
        return _start;
    }

    public void setStart(Integer start)
    {
        _start = start;
    }

    public Integer getEnd()
    {
        return _end;
    }

    public void setEnd(Integer end)
    {
        _end = end;
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

    public Double getPredictedRetentionTime()
    {
        return _predictedRetentionTime;
    }

    public void setPredictedRetentionTime(Double predictedRetentionTime)
    {
        _predictedRetentionTime = predictedRetentionTime;
    }

    public Double getAvgMeasuredRetentionTime()
    {
        return _avgMeasuredRetentionTime;
    }

    public void setAvgMeasuredRetentionTime(Double avgMeasuredRetentionTime)
    {
        _avgMeasuredRetentionTime = avgMeasuredRetentionTime;
    }

    public Integer getRank()
    {
        return _rank;
    }

    public void setRank(Integer rank)
    {
        _rank = rank;
    }

    public Boolean getDecoy()
    {
        return _decoy;
    }

    public void setDecoy(Boolean decoy)
    {
        _decoy = decoy;
    }

    public List<Precursor> getPrecursorList()
    {
        return _precursorList;
    }

    public void setPrecursorList(List<Precursor> precursorList)
    {
        _precursorList = precursorList;
    }

    public List<PeptideChromInfo> getPeptideChromInfoList()
    {
        return _peptideChromInfoList;
    }

    public void setPeptideChromInfoList(List<PeptideChromInfo> peptideChromInfoList)
    {
        _peptideChromInfoList = peptideChromInfoList;
    }

    public List<StructuralModification> getStructuralMods()
    {
        return _structuralMods;
    }

    public void setStructuralMods(List<StructuralModification> structuralMods)
    {
        _structuralMods = structuralMods;
    }

    public List<IsotopeModification> getIsotopeMods()
    {
        return _isotopeMods;
    }

    public void setIsotopeMods(List<IsotopeModification> isotopeMods)
    {
        _isotopeMods = isotopeMods;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    private static class Modification extends SkylineEntity
    {
        private int _peptideId;

        private String _modificationName;
        private Double _massDiff;
        private int _indexAa;

        public int getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(int peptideId)
        {
            _peptideId = peptideId;
        }

        public String getModificationName()
        {
            return _modificationName;
        }

        public void setModificationName(String modificationName)
        {
            _modificationName = modificationName;
        }

        public Double getMassDiff()
        {
            return _massDiff;
        }

        public void setMassDiff(Double massDiff)
        {
            _massDiff = massDiff;
        }

        public int getIndexAa()
        {
            return _indexAa;
        }

        public void setIndexAa(int indexAa)
        {
            _indexAa = indexAa;
        }
    }

    public static final class StructuralModification extends Modification
    {
        private int _structuralModId;

        public int getStructuralModId()
        {
            return _structuralModId;
        }

        public void setStructuralModId(int structuralModId)
        {
            _structuralModId = structuralModId;
        }
    }

    public static final class IsotopeModification extends Modification
    {
        private int _isotopeLabelId;
        private int _isotopeModId;

        private String _isotopeLabel;

        public int getIsotopeLabelId()
        {
            return _isotopeLabelId;
        }

        public void setIsotopeLabelId(int isotopeLabelId)
        {
            _isotopeLabelId = isotopeLabelId;
        }

        public int getIsotopeModId()
        {
            return _isotopeModId;
        }

        public void setIsotopeModId(int isotopeModId)
        {
            _isotopeModId = isotopeModId;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(String isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }
    }
}
