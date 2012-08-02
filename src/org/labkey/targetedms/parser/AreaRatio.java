package org.labkey.targetedms.parser;

/**
 * User: vsharma
 * Date: 7/24/12
 * Time: 11:06 AM
 */
public class AreaRatio extends SkylineEntity
{
    private int _isotopeLabelId;
    private int _isotopeLabelStdId;
    private double _areaRatio;

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }

    public int getIsotopeLabelStdId()
    {
        return _isotopeLabelStdId;
    }

    public void setIsotopeLabelStdId(int isotopeLabelStdId)
    {
        _isotopeLabelStdId = isotopeLabelStdId;
    }

    public double getAreaRatio()
    {
        return _areaRatio;
    }

    public void setAreaRatio(double areaRatio)
    {
        _areaRatio = areaRatio;
    }
}
