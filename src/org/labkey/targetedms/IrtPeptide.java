package org.labkey.targetedms;


/**
 * User: tgaluhn
 * Date: 3/21/2014
 *
 * Simple bean to represent the iRTPeptide table fields
 */
public class IrtPeptide
{
    private int id;
    private String modifiedSequence;
    private boolean iRTStandard;
    private double iRTValue;
    private int iRTScaleId;
    private int importCount;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

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
