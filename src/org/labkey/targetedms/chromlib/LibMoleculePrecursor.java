package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.util.Map;

public class LibMoleculePrecursor extends AbstractLibPrecursor<LibMoleculeTransition>
{
    private long _moleculeId;

    private Double _massMonoisotopic;
    private Double _massAverage;
    private String _molecule;

    public LibMoleculePrecursor() {}

    public LibMoleculePrecursor(MoleculePrecursor p, Map<Long, String> isotopeLabelMap, PrecursorChromInfo chromInfo,
                                TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        super(p, isotopeLabelMap, chromInfo, run, sampleFileIdMap);
    }

    public long getMoleculeId()
    {
        return _moleculeId;
    }

    public void setMoleculeId(long moleculeId)
    {
        _moleculeId = moleculeId;
    }

    public Double getMassMonoisotopic()
    {
        return _massMonoisotopic;
    }

    public void setMassMonoisotopic(Double massMonoisotopic)
    {
        _massMonoisotopic = massMonoisotopic;
    }

    public Double getMassAverage()
    {
        return _massAverage;
    }

    public void setMassAverage(Double massAverage)
    {
        _massAverage = massAverage;
    }

    /** “id” from the <molecule> elements in Skyline XML files, but Id is our RowId value in all of these tables */
    public String getMolecule()
    {
        return _molecule;
    }

    public void setMolecule(String molecule)
    {
        _molecule = molecule;
    }
}
