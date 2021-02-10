package org.labkey.targetedms.chromlib;

public class LibMolecule extends AbstractLibMolecule<LibMoleculePrecursor>
{
    private Integer _moleculeListId;
    private String _chemicalFormula;
    private String _moleculeName;
    private double _massMonoisotopic;
    private double _massAverage;
    private String _moleculeAccession;

    public String getChemicalFormula()
    {
        return _chemicalFormula;
    }

    public void setChemicalFormula(String chemicalFormula)
    {
        _chemicalFormula = chemicalFormula;
    }

    public String getMoleculeName()
    {
        return _moleculeName;
    }

    public void setMoleculeName(String moleculeName)
    {
        _moleculeName = moleculeName;
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

    public Integer getMoleculeListId()
    {
        return _moleculeListId;
    }

    /** “id” from the <molecule> elements in Skyline XML files, but Id is our RowId value in all of these tables */
    public void setMoleculeListId(Integer moleculeListId)
    {
        _moleculeListId = moleculeListId;
    }

    public String getMoleculeAccession()
    {
        return _moleculeAccession;
    }

    public void setMoleculeAccession(String moleculeAccession)
    {
        _moleculeAccession = moleculeAccession;
    }
}
