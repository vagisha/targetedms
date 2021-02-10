package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.labkey.targetedms.chromlib.BaseDaoImpl.readDouble;

public class LibMoleculePrecursor extends AbstractLibPrecursor<LibMoleculeTransition>
{
    private long _moleculeId;

    private Double _massMonoisotopic;
    private Double _massAverage;
    private String _adduct;

    public LibMoleculePrecursor() {}

    public LibMoleculePrecursor(MoleculePrecursor p, Map<Long, String> isotopeLabelMap, PrecursorChromInfo chromInfo,
                                TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        super(p, isotopeLabelMap, chromInfo, run, sampleFileIdMap);
        setAdduct(p.getAdduct());
        setMassMonoisotopic(p.getMassMonoisotopic());
        setMassAverage(p.getMassAverage());
    }

    public LibMoleculePrecursor(ResultSet rs) throws SQLException
    {
        super(rs);
        setMoleculeId(rs.getInt(Constants.MoleculePrecursorColumn.MoleculeId.baseColumn().name()));
        setMassMonoisotopic(readDouble(rs, Constants.MoleculePrecursorColumn.MassMonoisotopic.baseColumn().name()));
        setMassAverage(readDouble(rs, Constants.MoleculePrecursorColumn.MassAverage.baseColumn().name()));
        setAdduct(rs.getString(Constants.MoleculePrecursorColumn.Adduct.baseColumn().name()));
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

    public String getAdduct()
    {
        return _adduct;
    }

    public void setAdduct(String adduct)
    {
        _adduct = adduct;
    }
}
