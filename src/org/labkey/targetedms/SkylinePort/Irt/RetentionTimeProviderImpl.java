/*
 * Copyright (c) 2014-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
