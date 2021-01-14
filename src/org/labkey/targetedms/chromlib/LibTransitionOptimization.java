package org.labkey.targetedms.chromlib;

import java.util.Objects;

public class LibTransitionOptimization extends AbstractLibTransitionOptimization
{
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibTransitionOptimization that = (LibTransitionOptimization) o;
        return Objects.equals(_transitionId, that._transitionId) &&
                Objects.equals(_optimizationType, that._optimizationType) &&
                Objects.equals(_optimizationValue, that._optimizationValue);
    }
}
