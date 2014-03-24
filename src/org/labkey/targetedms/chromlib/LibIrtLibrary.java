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

    public int getId()
    {
        return _id;
    }

    @Override
    public void setId(int id)
    {
        this._id = id;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        this._modifiedSequence = modifiedSequence;
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


