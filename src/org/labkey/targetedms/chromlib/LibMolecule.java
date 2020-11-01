package org.labkey.targetedms.chromlib;

public class LibMolecule extends AbstractLibMolecule<LibMoleculePrecursor>
{
    private String _ionFormula;
    private String _customIonName;
    private double _massMonoisotopic;
    private double _massAverage;

    public String getIonFormula()
    {
        return _ionFormula;
    }

    public void setIonFormula(String ionFormula)
    {
        _ionFormula = ionFormula;
    }

    public String getCustomIonName()
    {
        return _customIonName;
    }

    public void setCustomIonName(String customIonName)
    {
        _customIonName = customIonName;
    }

    public double getMassMonoisotopic()
    {
        return _massMonoisotopic;
    }

    public void setMassMonoisotopic(double massMonoisotopic)
    {
        _massMonoisotopic = massMonoisotopic;
    }

    public double getMassAverage()
    {
        return _massAverage;
    }

    public void setMassAverage(double massAverage)
    {
        _massAverage = massAverage;
    }
}
