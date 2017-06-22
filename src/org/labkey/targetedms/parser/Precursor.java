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
    private LibraryInfo _libraryInfo;

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

    public Double getDecoyMassShift()
    {
        return _decoyMassShift;
    }

    public void setDecoyMassShift(Double decoyMassShift)
    {
        this._decoyMassShift = decoyMassShift;
    }

    public LibraryInfo getLibraryInfo()
    {
        return _libraryInfo;
    }

    public void setLibraryInfo(LibraryInfo libraryInfo)
    {
        _libraryInfo = libraryInfo;
    }

    @Override
    public String toString()
    {
        return getModifiedSequence();
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
