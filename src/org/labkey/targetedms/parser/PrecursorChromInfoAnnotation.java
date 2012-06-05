package org.labkey.targetedms.parser;

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class PrecursorChromInfoAnnotation extends AbstractAnnotation
{
    private int _precursorChromInfoId;

    public int getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(int precursorChromInfoId)
    {
        _precursorChromInfoId = precursorChromInfoId;
    }
}
