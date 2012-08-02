package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 7/24/12
 * Time: 3:00 PM
 */
public class PeptideAreaRatio extends AreaRatio
{
    private int _peptideChromInfoId;
    private int _peptideChromInfoStdId;

    public int getPeptideChromInfoId()
    {
        return _peptideChromInfoId;
    }

    public void setPeptideChromInfoId(int peptideChromInfoId)
    {
        _peptideChromInfoId = peptideChromInfoId;
    }

    public int getPeptideChromInfoStdId()
    {
        return _peptideChromInfoStdId;
    }

    public void setPeptideChromInfoStdId(int peptideChromInfoStdId)
    {
        _peptideChromInfoStdId = peptideChromInfoStdId;
    }
}
