package org.labkey.targetedms.parser;

/**
 * Created by vsharma on 9/23/2016.
 */
public class CustomIon
{
    private final static String massFormat = "{0} [{1:F06}/{2:F06}]";

    private CustomIon(){}

    public static String getName(Molecule molecule)
    {
        return getName(molecule.getCustomIonName(), molecule.getIonFormula(), molecule.getMassMonoisotopic(), molecule.getMassAverage());
    }

    public static String getName(MoleculeTransition transition)
    {
        return getName(transition.getCustomIonName(), transition.getIonFormula(), transition.getMassMonoisotopic(), transition.getMassAverage());
    }

    private static String getName(String customIonName, String ionFormula, Double massMonoisotopic, double massAverage)
    {
        if(customIonName != null)
        {
            return customIonName;
        }
        if(ionFormula != null)
        {
            return ionFormula;
        }
        return String.format(massFormat, "Ion", massMonoisotopic, massAverage);
    }
}
