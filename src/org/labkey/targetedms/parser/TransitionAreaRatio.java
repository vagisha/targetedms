package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 7/24/12
 * Time: 3:00 PM
 */
public class TransitionAreaRatio extends AreaRatio
{
    private int _transitionChromInfoId;
    private int _transitionChromInfoStdId;

    public int getTransitionChromInfoId()
    {
        return _transitionChromInfoId;
    }

    public void setTransitionChromInfoId(int transitionChromInfoId)
    {
        _transitionChromInfoId = transitionChromInfoId;
    }

    public int getTransitionChromInfoStdId()
    {
        return _transitionChromInfoStdId;
    }

    public void setTransitionChromInfoStdId(int transitionChromInfoStdId)
    {
        _transitionChromInfoStdId = transitionChromInfoStdId;
    }
}
