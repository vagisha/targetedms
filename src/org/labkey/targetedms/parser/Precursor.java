/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:28 AM
 */
public class Precursor extends GeneralPrecursor
{
    private String _modifiedSequence;
    private double _neutralMass;
    private String _isotopeLabel;
    private int _isotopeLabelId;
    private Double _decoyMassShift;
    private LibraryInfo _libraryInfo;
    private List<Transition> _transitionList;
    private List<PrecursorChromInfo> _chromInfoList;
    private List<PrecursorAnnotation> annotations;

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        this._isotopeLabelId = isotopeLabelId;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        this._modifiedSequence = modifiedSequence;
    }

    public double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(double neutralMass)
    {
        this._neutralMass = neutralMass;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        this._isotopeLabel = isotopeLabel;
    }

    public Double getDecoyMassShift()
    {
        return _decoyMassShift;
    }

    public void setDecoyMassShift(Double decoyMassShift)
    {
        this._decoyMassShift = decoyMassShift;
    }

    public List<Transition> getTransitionList()
    {
        return _transitionList;
    }

    public void setTransitionList(List<Transition> transitionList)
    {
        this._transitionList = transitionList;
    }

    public List<PrecursorChromInfo> getChromInfoList()
    {
        return _chromInfoList;
    }

    public void setChromInfoList(List<PrecursorChromInfo> chromInfoList)
    {
        _chromInfoList = chromInfoList;
    }

    public LibraryInfo getLibraryInfo()
    {
        return _libraryInfo;
    }

    public void setLibraryInfo(LibraryInfo libraryInfo)
    {
        _libraryInfo = libraryInfo;
    }

    public List<PrecursorAnnotation> getAnnotations()
    {
        return annotations;
    }

    public void setAnnotations(List<PrecursorAnnotation> annotations)
    {
        this.annotations = annotations;
    }

    public static Set<String> getColumns()
    {
        Set<String> colNames = new HashSet<>();
        colNames.add("Id");
        colNames.add("GeneralMoleculeId");
        colNames.add("Mz");
        colNames.add("Charge");
        colNames.add("CollisionEnergy");
        colNames.add("DeclusteringPotential");
        colNames.add("Decoy");
        colNames.add("Note");
        colNames.add("Modified");
        colNames.add("RepresentativeDataState");
        colNames.add("ExplicitCollisionEnergy");
        colNames.add("ExplicitDriftTimeMsec");
        colNames.add("ExplicitDriftTimeHighEnergyOffsetMsec");
        colNames.add("GeneralMoleculeId");
        colNames.add("IsotopeLabelId");
        colNames.add("NeutralMass");
        colNames.add("ModifiedSequence");
        colNames.add("DecoyMassShift");
        return colNames;
    }

    public static final class LibraryInfo extends SkylineEntity
    {
        private int _precursorId;
        private int _generalPrecursorId;
        private int _spectrumLibraryId;

        private String _libraryName;
        private double _score1;
        private double _score2;
        private double _score3;

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

        public double getScore1()
        {
            return _score1;
        }

        public void setScore1(double score1)
        {
            _score1 = score1;
        }

        public double getScore2()
        {
            return _score2;
        }

        public void setScore2(double score2)
        {
            _score2 = score2;
        }

        public double getScore3()
        {
            return _score3;
        }

        public void setScore3(double score3)
        {
            _score3 = score3;
        }
    }
}
