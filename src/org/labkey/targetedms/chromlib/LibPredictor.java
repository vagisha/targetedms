package org.labkey.targetedms.chromlib;

import java.util.Objects;

public class LibPredictor extends AbstractLibEntity
{
    private String _name;
    private Double _stepSize;
    private Integer _stepCount;

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public Double getStepSize()
    {
        return _stepSize;
    }

    public void setStepSize(Double stepSize)
    {
        _stepSize = stepSize;
    }

    public Integer getStepCount()
    {
        return _stepCount;
    }

    public void setStepCount(Integer stepCount)
    {
        _stepCount = stepCount;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibPredictor that = (LibPredictor) o;
        return Objects.equals(_name, that._name) && Objects.equals(_stepSize, that._stepSize) && Objects.equals(_stepCount, that._stepCount);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_name, _stepSize, _stepCount);
    }
}
