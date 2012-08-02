package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 7/24/12
 * Time: 3:00 PM
 */
public class PrecursorAreaRatio extends AreaRatio
{
    private int _precursorChromInfoId;
    private int _precursorChromInfoStdId;

    public int getPrecursorChromInfoId()
    {
        return _precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(int precursorChromInfoId)
    {
        _precursorChromInfoId = precursorChromInfoId;
    }

    public int getPrecursorChromInfoStdId()
    {
        return _precursorChromInfoStdId;
    }

    public void setPrecursorChromInfoStdId(int precursorChromInfoStdId)
    {
        _precursorChromInfoStdId = precursorChromInfoStdId;
    }
}
