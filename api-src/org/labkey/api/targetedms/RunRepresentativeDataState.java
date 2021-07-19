package org.labkey.api.targetedms;

public enum RunRepresentativeDataState
{
    /**
     * Don't change the ordering of these enum values without updating the values in targetedms.runs.representativedatastate
     */
    NotRepresentative(""),
    Representative_Protein("R - Protein"),
    Representative_Peptide("R - Peptide");

    private final String _label;

    RunRepresentativeDataState(String label)
    {
        _label = label;
    }

    public String getLabel()
    {
        return _label;
    }
}
