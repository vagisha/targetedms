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

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 1:46 PM
 */
public class LibPeptideStructuralModification implements ObjectWithId
{
    private int _id;
    private int _peptideId;
    private int _structuralModificationId;
    private Integer _indexAa;
    private Double _massDiff;

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

    public int getStructuralModificationId()
    {
        return _structuralModificationId;
    }

    public void setStructuralModificationId(int structuralModificationId)
    {
        _structuralModificationId = structuralModificationId;
    }

    public Integer getIndexAa()
    {
        return _indexAa;
    }

    public void setIndexAa(Integer indexAa)
    {
        _indexAa = indexAa;
    }

    public Double getMassDiff()
    {
        return _massDiff;
    }

    public void setMassDiff(Double massDiff)
    {
        _massDiff = massDiff;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPeptideStructuralModification)) return false;

        LibPeptideStructuralModification that = (LibPeptideStructuralModification) o;

        if (_peptideId != that._peptideId) return false;
        if (_structuralModificationId != that._structuralModificationId) return false;
        if (!_indexAa.equals(that._indexAa)) return false;
        if (!_massDiff.equals(that._massDiff)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _peptideId;
        result = 31 * result + _structuralModificationId;
        result = 31 * result + _indexAa.hashCode();
        result = 31 * result + _massDiff.hashCode();
        return result;
    }
}
