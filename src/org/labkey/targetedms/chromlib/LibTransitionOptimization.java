package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.TransitionOptimization;

import java.util.Objects;

public class LibTransitionOptimization extends AbstractLibEntity
{
    public LibTransitionOptimization()
    {
    }

    public LibTransitionOptimization(TransitionOptimization optimization)
    {
       _optimizationType = optimization.getOptimizationType();
       _optimizationValue = optimization.getOptValue();
    }

    protected int _transitionId;
    protected String _optimizationType;
    protected Double _optimizationValue;

    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        _transitionId = transitionId;
    }

    public String getOptimizationType()
    {
        return _optimizationType;
    }

    public void setOptimizationType(String optimizationType)
    {
        _optimizationType = optimizationType;
    }

    public Double getOptimizationValue()
    {
        return _optimizationValue;
    }

    public void setOptimizationValue(Double optimizationValue)
    {
        _optimizationValue = optimizationValue;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibTransitionOptimization that = (LibTransitionOptimization) o;
        return Objects.equals(_transitionId, that._transitionId) &&
                Objects.equals(_optimizationType, that._optimizationType) &&
                Objects.equals(_optimizationValue, that._optimizationValue);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_transitionId, _optimizationType, _optimizationValue);
    }
}
