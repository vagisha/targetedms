package org.labkey.api.targetedms;

public interface IModification
{
    long getId();

    String getName();

    String getAminoAcid();

    String getTerminus();

    String getFormula();

    Double getMassDiffMono();

    Double getMassDiffAvg();

    Integer getUnimodId();

    interface IStructuralModification extends IModification
    {
        boolean isVariable();
    }

    interface IIsotopeModification extends IModification
    {
        Boolean getLabel13C();

        Boolean getLabel15N();

        Boolean getLabel18O();

        Boolean getLabel2H();
    }
}

