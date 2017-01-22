package org.labkey.targetedms.parser;

import org.jetbrains.annotations.Nullable;

/**
 * Created by nicksh on 3/29/2016.
 */
public class QuantificationSettings extends SkylineEntity
{
    private int _runId;
    private String _regressionWeighting;
    private String _regressionFit;
    private String _normalizationMethod;
    private Integer _msLevel;
    private String _units;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    @Nullable
    public String getRegressionWeighting()
    {
        return _regressionWeighting;
    }

    public void setRegressionWeighting(@Nullable String regressionWeighting)
    {
        _regressionWeighting = regressionWeighting;
    }

    @Nullable
    public String getRegressionFit()
    {
        return _regressionFit;
    }

    public void setRegressionFit(@Nullable String regressionFit)
    {
        _regressionFit = regressionFit;
    }

    @Nullable
    public String getNormalizationMethod()
    {
        return _normalizationMethod;
    }

    public void setNormalizationMethod(@Nullable String normalizationMethod)
    {
        _normalizationMethod = normalizationMethod;
    }

    @Nullable
    public Integer getMsLevel()
    {
        return _msLevel;
    }

    public void setMsLevel(@Nullable Integer msLevel)
    {
        _msLevel = msLevel;
    }


    @Nullable
    public String getUnits()
    {
        return _units;
    }

    public void setUnits(@Nullable String units)
    {
        _units = units;
    }
}
