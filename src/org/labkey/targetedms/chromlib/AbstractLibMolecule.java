package org.labkey.targetedms.chromlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractLibMolecule<PrecursorType extends AbstractLibPrecursor> extends AbstractLibEntity
{
    protected List<PrecursorType> _precursors;

    public void addPrecursor(PrecursorType precursor)
    {
        if(_precursors == null)
        {
            _precursors = new ArrayList<>();
        }
        _precursors.add(precursor);
    }

    List<PrecursorType> getPrecursors()
    {
        if(_precursors == null)
            return Collections.emptyList();
        else
            return Collections.unmodifiableList(_precursors);
    }

    @Override
    public int getCacheSize()
    {
        return super.getCacheSize() + getPrecursors().stream().mapToInt(AbstractLibEntity::getCacheSize).sum();
    }
}
