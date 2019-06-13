/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Created by vsharma on 8/16/2016.
 */
public class SignedMz implements Comparable<SignedMz>
{
    private final Double _mz;
    private final boolean _isNegative;

    public SignedMz(Double mz, boolean isNegative)
    {
        _mz = mz;
        _isNegative = isNegative;
    }

    public Double getMz()
    {
        return _mz;
    }

    public boolean isNegative()
    {
        return _isNegative;
    }

    public boolean hasValue()
    {
        return _mz != null;
    }

    @Override
    public int compareTo(@NotNull SignedMz other)
    {
        if (hasValue() != other.hasValue())
        {
            return hasValue() ? 1 : -1;
        }
        if (isNegative() != other.isNegative())
        {
            return isNegative() ? -1 : 1;
        }
        // Same sign
        if (hasValue())
            return _mz.compareTo(other.getMz());

        return 0; // Both empty
    }

    public int compareTolerant(SignedMz other, double tolerance)
    {
        if (hasValue() != other.hasValue())
        {
            return hasValue() ? 1 : -1;
        }
        if (isNegative() != other.isNegative())
        {
            return isNegative() ? -1 : 1; // Not interested in tolerance when signs disagree
        }
        // Same sign
        if (Math.abs(_mz - other.getMz()) <= tolerance)
            return 0;
        return _mz.compareTo(other.getMz());
    }
}
