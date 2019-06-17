/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

/**
 * User: tgaluhn
 * Date: 3/19/14
 *
 * Helper class for exporting the chromatogram library.
 * One instance of this class represents one peptide's data in the iRTScale.
 *
 */
public class LibIrtLibrary implements ObjectWithId
{
    private int _id;


    private String _modifiedSequence;
    private Double _irtValue;
    private Boolean _irtStandard;
    private Integer _timeSource;

    public int getId()
    {
        return _id;
    }

    @Override
    public void setId(int id)
    {
        _id = id;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public Double getIrtValue()
    {
        return _irtValue;
    }

    public void setIrtValue(Double irtValue)
    {
        _irtValue = irtValue;
    }

    public Boolean getIrtStandard()
    {
        return _irtStandard;
    }

    public void setIrtStandard(Boolean irtStandard)
    {
        _irtStandard = irtStandard;
    }

    public Integer getTimeSource()
    {
        return _timeSource;
    }

    public void setTimeSource(Integer timeSource)
    {
        _timeSource = timeSource;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibIrtLibrary)) return false;

        LibIrtLibrary that = (LibIrtLibrary) o;

        if (!_modifiedSequence.equals(that._modifiedSequence)) return false;
        if (!_irtValue.equals(that._irtValue)) return false;
        if (!_irtStandard.equals(that._irtStandard)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _modifiedSequence.hashCode();
        result = 31 * result + _irtValue.hashCode();
        result = 31 * result + _irtStandard.hashCode();

        return result;
    }
}


