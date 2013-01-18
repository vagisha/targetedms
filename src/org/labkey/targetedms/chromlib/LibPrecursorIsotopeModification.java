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
        this._id = id;
    }

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        this._precursorId = precursorId;
    }

    public int getIsotopeModificationId()
    {
        return _isotopeModificationId;
    }

    public void setIsotopeModificationId(int isotopeModificationId)
    {
        this._isotopeModificationId = isotopeModificationId;
    }

    public Integer getIndexAa()
    {
        return _indexAa;
    }

    public void setIndexAa(Integer indexAa)
    {
        this._indexAa = indexAa;
    }

    public Double getMassDiff()
    {
        return _massDiff;
    }

    public void setMassDiff(Double massDiff)
    {
        this._massDiff = massDiff;
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
