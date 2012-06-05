package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class PeptideGroupAnnotation extends AbstractAnnotation
{
    private int _peptideGroupId;

    public int getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(int peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }
}
