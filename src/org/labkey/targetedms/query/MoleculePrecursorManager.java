package org.labkey.targetedms.query;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.MoleculePrecursor;

import java.util.List;

public class MoleculePrecursorManager
{
    private MoleculePrecursorManager() {}

    public static List<MoleculePrecursor> getPrecursorsForMolecule(int moleculeId, TargetedMSSchema targetedMSSchema)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("MoleculeId"), moleculeId);

        Sort sort = new Sort("Charge");

        List<MoleculePrecursor> precursors = new TableSelector(new MoleculePrecursorTableInfo(targetedMSSchema), MoleculePrecursor.getColumns(), filter,  sort).getArrayList(MoleculePrecursor.class);

        if (precursors.isEmpty())
        {
            throw new NotFoundException(String.format("No precursors found for moleculeId %d", moleculeId));
        }

        return precursors;
    }
}
