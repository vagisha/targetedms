package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class PrecursorAnnotation extends AbstractAnnotation
{
    private int _precursorId;

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }
}
