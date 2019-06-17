/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;

public class MassModification
{
    /**
     * The maximum precision that we look at for matching.
     * Even if the library says that the mass of a modification is +57.0214635,
     * we still want to match that to our Carbamidomethyl (C) 52.021464.
     * Also, we want their Sodium: 22.989769 to match our Sodium: 22.989767
     */
    public static final int MAX_PRECISION = 5;
    private final double _mass;
    private final int _precision;
    public MassModification(double mass, int precision)
    {
        _mass = mass;
        _precision = precision;
    }

    public double getMass()
    {
        return _mass;
    }
    public int getPrecision()
    {
        return _precision;
    }

    public boolean matches(MassModification that)
    {
        int minPrecision = Math.min(Math.min(getPrecision(), that.getPrecision()), MAX_PRECISION);
        double thisRound = roundToPrecision(getMass(), minPrecision);
        double thatRound = roundToPrecision(that.getMass(), minPrecision);
        if (thisRound == thatRound)
        {
            return true;
        }
        double minDifference = Math.min(Math.abs(getMass() - thatRound), Math.abs(that.getMass() - thisRound))
                * pow10(minPrecision);
        if (minDifference < .500001)
        {
            return true;
        }
        return false;
    }

    private static double roundToPrecision(double value, int decimals)
    {
        double pow10 = pow10(decimals);
        return Math.round(value * pow10) / pow10;
    }

    private static double pow10(int power)
    {
        return Math.pow(10, power);
    }

    @Nullable
    public static MassModification parse(String strModification)
    {
        double mass;
        int ichDot;
        try
        {
            mass = Double.parseDouble(strModification);
        }
        catch (Exception e)
        {
            return null;
        }
        ichDot = strModification.lastIndexOf('.');
        if (ichDot < 0)
        {
            return new MassModification(mass, 0);
        }
        return new MassModification(mass, strModification.length() - ichDot - 1);
    }
}
