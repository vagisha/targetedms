package org.labkey.targetedms.SkylinePort.Irt;

import org.jetbrains.annotations.NotNull;
import org.labkey.targetedms.IrtPeptide;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 6/2/2014
 *
 * Implementation of the Skyline IRetentionTimeProvider interface, impedance matched to the IrtPeptide class introduced
 * in earlier work to handle the import/export of the Irt db into LabKey Server.
 */
public class RetentionTimeProviderImpl implements IRetentionTimeProvider
{
    LinkedHashMap<String, IrtPeptide> _irtPeptides;

    public RetentionTimeProviderImpl(@NotNull List<IrtPeptide> irtPeptideList)
    {
        _irtPeptides = new LinkedHashMap<>(irtPeptideList.size());
        for (IrtPeptide pep : irtPeptideList)
        {
            _irtPeptides.put(pep.getModifiedSequence(), pep);
        }
    }

    @Override
    public String getName()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Double GetRetentionTime(String sequence)
    {
        if (_irtPeptides.containsKey(sequence))
            return _irtPeptides.get(sequence).getiRTValue();
        else return null;
    }

    @Override
    public Integer GetTimeSource(String sequence)
    {
        if (_irtPeptides.containsKey(sequence))
            return _irtPeptides.get(sequence).getTimeSource();
        else return null;
    }

    @Override
    public ArrayList<MeasuredRetentionTime> getPeptideRetentionTimes()
    {
        ArrayList<MeasuredRetentionTime> mrts = new ArrayList<>(_irtPeptides.size());
        for (IrtPeptide pep : _irtPeptides.values())
        {
            mrts.add(new MeasuredRetentionTime(pep.getModifiedSequence(), pep.getiRTValue(), true));
        }
        return mrts;
    }
}
