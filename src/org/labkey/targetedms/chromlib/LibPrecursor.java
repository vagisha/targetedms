package org.labkey.targetedms.chromlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibPrecursor implements ObjectWithId
{
    private int _id;
    private int _peptideId;
    private String _isotopeLabel;
    private Double _mz;
    private Integer _charge;
    private Double _neutralMass;
    private String _modifiedSequence;
    private Double _collisionEnergy;
    private Double _declusteringPotential;
    private Double _totalArea;
    private Boolean _representative;
    private byte[] _chromatogram;

    private List<LibTransition> _transitions;

    private List<LibPrecursorRetentionTime> _retentionTimes;

    private List<LibPrecursorIsotopeModification> _isotopeModifications;

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

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        this._isotopeLabel = isotopeLabel;
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        this._mz = mz;
    }

    public Integer getCharge()
    {
        return _charge;
    }

    public void setCharge(Integer charge)
    {
        this._charge = charge;
    }

    public Double getNeutralMass()
    {
        return _neutralMass;
    }

    public void setNeutralMass(Double neutralMass)
    {
        this._neutralMass = neutralMass;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        this._modifiedSequence = modifiedSequence;
    }

    public Double getCollisionEnergy()
    {
        return _collisionEnergy;
    }

    public void setCollisionEnergy(Double collisionEnergy)
    {
        this._collisionEnergy = collisionEnergy;
    }

    public Double getDeclusteringPotential()
    {
        return _declusteringPotential;
    }

    public void setDeclusteringPotential(Double declusteringPotential)
    {
        this._declusteringPotential = declusteringPotential;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        this._totalArea = totalArea;
    }

    public Boolean getRepresentative()
    {
        return _representative;
    }

    public void setRepresentative(Boolean representative)
    {
        this._representative = representative;
    }

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    public void setChromatogram(byte[] chromatogram)
    {
        this._chromatogram = chromatogram;
    }

    public void addTransition(LibTransition transition)
    {
        if(_transitions == null)
        {
            _transitions = new ArrayList<LibTransition>();
        }
        _transitions.add(transition);
    }

    public List<LibTransition> getTransitions()
    {
        if(_transitions == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_transitions);
    }

    public void addRetentionTime(LibPrecursorRetentionTime retentionTime)
    {
        if(_retentionTimes == null)
        {
            _retentionTimes = new ArrayList<LibPrecursorRetentionTime>();
        }
        _retentionTimes.add(retentionTime);
    }

    public List<LibPrecursorRetentionTime> getRetentionTimes()
    {
        if(_retentionTimes == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_retentionTimes);
    }

    public void addIsotopeModification(LibPrecursorIsotopeModification isotopeModification)
    {
        if(_isotopeModifications == null)
        {
            _isotopeModifications = new ArrayList<LibPrecursorIsotopeModification>();
        }
        _isotopeModifications.add(isotopeModification);
    }

    public List<LibPrecursorIsotopeModification> getIsotopeModifications()
    {
        if(_isotopeModifications == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_isotopeModifications);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibPrecursor)) return false;

        LibPrecursor precursor = (LibPrecursor) o;

        if (_peptideId != precursor._peptideId) return false;
        if (!_charge.equals(precursor._charge)) return false;
        if (!Arrays.equals(_chromatogram, precursor._chromatogram)) return false;
        if (_collisionEnergy != null ? !_collisionEnergy.equals(precursor._collisionEnergy) : precursor._collisionEnergy != null)
            return false;
        if (_declusteringPotential != null ? !_declusteringPotential.equals(precursor._declusteringPotential) : precursor._declusteringPotential != null)
            return false;
        if (!_isotopeLabel.equals(precursor._isotopeLabel)) return false;
        if (_isotopeModifications != null ? !_isotopeModifications.equals(precursor._isotopeModifications) : precursor._isotopeModifications != null)
            return false;
        if (!_modifiedSequence.equals(precursor._modifiedSequence)) return false;
        if (!_mz.equals(precursor._mz)) return false;
        if (!_neutralMass.equals(precursor._neutralMass)) return false;
        if (!_representative.equals(precursor._representative)) return false;
        if (_retentionTimes != null ? !_retentionTimes.equals(precursor._retentionTimes) : precursor._retentionTimes != null)
            return false;
        if (!_totalArea.equals(precursor._totalArea)) return false;
        if (_transitions != null ? !_transitions.equals(precursor._transitions) : precursor._transitions != null)
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _peptideId;
        result = 31 * result + _isotopeLabel.hashCode();
        result = 31 * result + _mz.hashCode();
        result = 31 * result + _charge.hashCode();
        result = 31 * result + _neutralMass.hashCode();
        result = 31 * result + _modifiedSequence.hashCode();
        result = 31 * result + (_collisionEnergy != null ? _collisionEnergy.hashCode() : 0);
        result = 31 * result + (_declusteringPotential != null ? _declusteringPotential.hashCode() : 0);
        result = 31 * result + _totalArea.hashCode();
        result = 31 * result + _representative.hashCode();
        result = 31 * result + (_chromatogram != null ? Arrays.hashCode(_chromatogram) : 0);
        result = 31 * result + (_transitions != null ? _transitions.hashCode() : 0);
        result = 31 * result + (_retentionTimes != null ? _retentionTimes.hashCode() : 0);
        result = 31 * result + (_isotopeModifications != null ? _isotopeModifications.hashCode() : 0);
        return result;
    }
}
