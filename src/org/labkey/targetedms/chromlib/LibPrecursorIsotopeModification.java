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
public class LibPrecursorIsotopeModification implements ObjectWithId
{
    private int _id;
    private int _precursorId;
    private int _isotopeModificationId;
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

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

    public int getIsotopeModificationId()
    {
        return _isotopeModificationId;
    }

    public void setIsotopeModificationId(int isotopeModificationId)
    {
        _isotopeModificationId = isotopeModificationId;
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
        if (!(o instanceof LibPrecursorIsotopeModification)) return false;

        LibPrecursorIsotopeModification that = (LibPrecursorIsotopeModification) o;

        if (_isotopeModificationId != that._isotopeModificationId) return false;
        if (_precursorId != that._precursorId) return false;
        if (!_indexAa.equals(that._indexAa)) return false;
        if (!_massDiff.equals(that._massDiff)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _precursorId;
        result = 31 * result + _isotopeModificationId;
        result = 31 * result + _indexAa.hashCode();
        result = 31 * result + _massDiff.hashCode();
        return result;
    }
}
