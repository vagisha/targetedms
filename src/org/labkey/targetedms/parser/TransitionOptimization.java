package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jul 13, 2012
 */
public class TransitionOptimization extends SkylineEntity
{
    private int _transitionId;
    private String _optimizationType;
    private double _optValue;

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

    public double getOptValue()
    {
        return _optValue;
    }

    public void setOptValue(double optValue)
    {
        _optValue = optValue;
    }
}
