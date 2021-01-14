package org.labkey.targetedms.chromlib;

import java.util.Objects;

public abstract class AbstractLibTransitionOptimization extends AbstractLibEntity
{
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
    public int hashCode()
    {
        return Objects.hash(_transitionId, _optimizationType, _optimizationValue);
    }
}
