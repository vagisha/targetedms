package org.labkey.targetedms.parser;

public class GeneralMolecule extends AnnotatedEntity<GeneralMoleculeAnnotation>
{
    protected int _peptideGroupId;
    protected Double _rtCalculatorScore;
    protected Double _predictedRetentionTime;
    protected Double _avgMeasuredRetentionTime;  // average measured retention time over all replicates
    protected String _note;
    protected Double _explicitRetentionTime;

    public int getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(int peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }
    public Double getPredictedRetentionTime()
    {
        return _predictedRetentionTime;
    }

    public void setPredictedRetentionTime(Double predictedRetentionTime)
    {
        _predictedRetentionTime = predictedRetentionTime;
    }

    public Double getAvgMeasuredRetentionTime()
    {
        return _avgMeasuredRetentionTime;
    }

    public void setAvgMeasuredRetentionTime(Double avgMeasuredRetentionTime)
    {
        _avgMeasuredRetentionTime = avgMeasuredRetentionTime;
    }

    public Double getRtCalculatorScore()
    {
        return _rtCalculatorScore;
    }

    public void setRtCalculatorScore(Double rtCalculatorScore)
    {
        _rtCalculatorScore = rtCalculatorScore;
    }
    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public Double getExplicitRetentionTime()
    {
        return _explicitRetentionTime;
    }

    public void setExplicitRetentionTime(Double explicitRetentionTime)
    {
        _explicitRetentionTime = explicitRetentionTime;
    }

}
