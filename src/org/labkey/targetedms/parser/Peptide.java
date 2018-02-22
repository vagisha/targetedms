/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Peptide extends GeneralMolecule
{
    private String _sequence;
    private String _peptideModifiedSequence;
    private String _previousAa;
    private String _nextAa;
    private Integer _startIndex;
    private Integer _endIndex;
    private double _calcNeutralMass;
    private int _numMissedCleavages;

    private Integer _rank;  // peptide rank either by spectrum count or by the total intensity
                           // of the picked transition peaks.
                           // If there are multiple precursorList, the rank of the best precursor is used.

    private Boolean _decoy;

    private List<Precursor> _precursorList;
    private List<StructuralModification> _structuralMods;
    private List<IsotopeModification> _isotopeMods;

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public String getPeptideModifiedSequence()
    {
        return _peptideModifiedSequence;
    }

    public void setPeptideModifiedSequence(String modifiedSeqence)
    {
        _peptideModifiedSequence = modifiedSeqence;
    }

    public String getPreviousAa()
    {
        return _previousAa;
    }

    public void setPreviousAa(String previousAa)
    {
        _previousAa = previousAa;
    }

    public String getNextAa()
    {
        return _nextAa;
    }

    public void setNextAa(String nextAa)
    {
        _nextAa = nextAa;
    }

    public Integer getStartIndex()
    {
        return _startIndex;
    }

    public void setStartIndex(Integer start)
    {
        _startIndex = start;
    }

    public Integer getEndIndex()
    {
        return _endIndex;
    }

    public void setEndIndex(Integer end)
    {
        _endIndex = end;
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

    public boolean isDecoyPeptide()
    {
        return _decoy != null && _decoy.equals(Boolean.TRUE);
    }

    public List<Precursor> getPrecursorList()
    {
        return _precursorList;
    }

    public void setPrecursorList(List<Precursor> precursorList)
    {
        _precursorList = precursorList;
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

    public boolean isStandardTypePeptide()
    {
        return getStandardType() != null;
    }

    @Override
    public String getPrecursorKey(GeneralMolecule gm, GeneralPrecursor gp)
    {
        StringBuilder key = new StringBuilder();
        key.append(((Peptide) gm).getPeptideModifiedSequence());
        key.append("_").append(gp.getCharge());
        return key.toString();
    }

    @Override
    public String getTextId()
    {
        return getPeptideModifiedSequence();
    }

    @Override
    public boolean textIdMatches(String textId)
    {
        if (textId.startsWith("#"))
        {
            // Small molecule textId.
            return false;
        }
        return modifiedSequencesMatch(getPeptideModifiedSequence(), textId);
    }

    public static class Modification extends SkylineEntity
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

    public static boolean modifiedSequencesMatch(String modSeq1, String modSeq2)
    {
        if (modSeq1.equals(modSeq2))
        {
            return true;
        }
        if (modSeq1.startsWith("#") || modSeq2.startsWith("#"))
        {
            return false;
        }
        List<Pair<Integer, String>> mods1 = new ArrayList<>();
        String unmodSeq1 = stripModifications(modSeq1, mods1);
        List<Pair<Integer, String>> mods2 = new ArrayList<>();
        String unmodSeq2 = stripModifications(modSeq2, mods2);
        if (!unmodSeq1.equals(unmodSeq2))
        {
            return false;
        }
        if (mods1.size() != mods2.size())
        {
            return false;
        }
        for (int i = 0; i < mods1.size(); i++)
        {
            Pair<Integer, String> mod1 = mods1.get(i);
            Pair<Integer, String> mod2 = mods2.get(i);
            if (!mod1.first.equals(mod2.first))
            {
                return false;
            }
            if (mod1.second.equals(mod2.second))
            {
                continue;
            }
            MassModification massMod1 = MassModification.parse(mod1.second);
            MassModification massMod2 = MassModification.parse(mod2.second);
            if (massMod1 == null || massMod2 == null)
            {
                return false;
            }
            if (!massMod1.matches(massMod2))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds to a list all of the modifications in a modifiedSequence, and returns the unmodified sequence.
     */
    public static String stripModifications(String modifiedSequence, List<Pair<Integer, String>> modifications)
    {
        StringBuilder unmodifiedSequence = null;
        Integer modificationStart = null;
        for (int i = 0; i < modifiedSequence.length(); i++)
        {
            char ch = modifiedSequence.charAt(i);
            if (modificationStart != null) {
                if (ch == ']')
                {
                    if (modifications != null)
                    {
                        String strModification = modifiedSequence.substring(modificationStart, i);
                        modifications.add(new Pair<Integer, String>(unmodifiedSequence.length() - 1, strModification));
                    }
                    modificationStart = null;
                }
            }
            else
            {
                if (ch == '[')
                {
                    if (unmodifiedSequence == null)
                    {
                        unmodifiedSequence = new StringBuilder(modifiedSequence.substring(0, i));
                    }
                    modificationStart = i + 1;
                }
                else
                {
                    if (unmodifiedSequence != null)
                    {
                        unmodifiedSequence.append(ch);
                    }
                }
            }
        }
        if (unmodifiedSequence != null)
        {
            return unmodifiedSequence.toString();
        }
        return modifiedSequence;
    }
}
