package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jul 11, 2012
 */
public class TransitionLoss extends SkylineEntity
{
    // The fields needed to match it up correctly
    private String _modificationName;
    private String _formula;
    private Double _massDiffMono;
    private Double _massDiffAvg;

    // The database fields
    private int _transitionId;
    private int _structuralModLossId;

    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        _transitionId = transitionId;
    }

    public int getStructuralModLossId()
    {
        return _structuralModLossId;
    }

    public void setStructuralModLossId(int structuralModLossId)
    {
        _structuralModLossId = structuralModLossId;
    }

    public void setModificationName(String modificationName)
    {
        _modificationName = modificationName;
    }

    public String getModificationName()
    {
        return _modificationName;
    }

    public void setFormula(String formula)
    {
        _formula = formula;
    }

    public void setMassDiffMono(Double massDiffMono)
    {
        _massDiffMono = massDiffMono;
    }

    public void setMassDiffAvg(Double massDiffAvg)
    {
        _massDiffAvg = massDiffAvg;
    }

    public boolean matches(PeptideSettings.PotentialLoss loss)
    {
        if (_formula != null ? !_formula.equals(loss.getFormula()) : loss.getFormula() != null) return false;
        if (_massDiffAvg != null ? !_massDiffAvg.equals(loss.getMassDiffAvg()) : loss.getMassDiffAvg() != null)
            return false;
        if (_massDiffMono != null ? !_massDiffMono.equals(loss.getMassDiffMono()) : loss.getMassDiffMono() != null)
            return false;

        return true;
    }

    @Override
    public String toString()
    {
        return "TransitionLoss{" +
                "_formula='" + _formula + '\'' +
                ", _massDiffMono=" + _massDiffMono +
                ", _massDiffAvg=" + _massDiffAvg +
                '}';
    }
}
