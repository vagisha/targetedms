package org.labkey.targetedms.chart;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.query.MoleculePrecursorManager;

import java.util.List;

public class MoleculePrecursorColorIndexer
{
    private int _minCharge = Integer.MAX_VALUE;

    public MoleculePrecursorColorIndexer(int moleculeId, User user, Container container)
    {
        if (moleculeId > 0)
        {
            List<MoleculePrecursor> precursors = MoleculePrecursorManager.getPrecursorsForMolecule(moleculeId, new TargetedMSSchema(user, container));
            for (MoleculePrecursor precursor : precursors)
            {
                _minCharge = Math.min(_minCharge, precursor.getCharge());
            }
        }
    }

    public void setMinCharge(int charge)
    {
        _minCharge = charge;
    }

    public int getColorIndex(int precursorId, User user, Container container)
    {
        // CONSIDER caching the colors, as they will be the same for all the replicates
        MoleculePrecursor precursor = MoleculePrecursorManager.getPrecursor(container, precursorId, user);
        return getColorIndex(precursor.getCharge());
    }

    public int getColorIndex(int charge)
    {
        return (charge - _minCharge);

    }
}
