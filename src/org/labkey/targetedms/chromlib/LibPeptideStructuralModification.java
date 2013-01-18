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
        this._id = id;
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
