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
package org.labkey.targetedms;


import org.labkey.targetedms.parser.SkylineEntity;

/**
 * User: tgaluhn
 * Date: 3/21/2014
 *
 * Simple bean to represent the iRTPeptide table fields
 */
public class IrtPeptide extends SkylineEntity implements Comparable<IrtPeptide>
{
    private String modifiedSequence;
    private boolean iRTStandard;
    private double iRTValue;
    private int iRTScaleId;
    private int importCount;
    private int timeSource;

    public String getModifiedSequence()
    {
        return modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        this.modifiedSequence = modifiedSequence;
    }

    public boolean isiRTStandard()
    {
        return iRTStandard;
    }

    public void setiRTStandard(boolean iRTStandard)
    {
        this.iRTStandard = iRTStandard;
    }

    public double getiRTValue()
    {
        return iRTValue;
    }

    public void setiRTValue(double iRTValue)
    {
        this.iRTValue = iRTValue;
    }

    public int getiRTScaleId()
    {
        return iRTScaleId;
    }

    public void setiRTScaleId(int iRTScaledId)
    {
        this.iRTScaleId = iRTScaledId;
    }

    public int getImportCount()
    {
        return importCount;
    }

    public void setImportCount(int importCount)
    {
        this.importCount = importCount;
    }

    public int getTimeSource()
    {
        return timeSource;
    }

    public void setTimeSource(int timeSource)
    {
        this.timeSource = timeSource;
    }

    @Override
    public int compareTo(IrtPeptide pep)
    {
        return modifiedSequence.compareTo(pep.getModifiedSequence());
    }

    /**
     * Include a new import value in the weighted average iRT value for the peptide sequence.
     * @param newObservation
     */
    public void reweighValue(double newObservation)
    {
        double oldTotal = iRTValue * importCount;
        importCount++;
        iRTValue = (oldTotal + newObservation) / importCount;
    }
}
