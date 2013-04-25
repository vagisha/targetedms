/*
 * Copyright (c) 2013 LabKey Corporation
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

/**
 * User: brendanx
 * Date: Apr 20, 2013
 */
public class ChromatogramTran
{
    // Size is one four-byte float
    // Divide by 8 to convert from bits to bytes
    public static final int SIZE4 = Float.SIZE / 8;
    // Grown slightly in format version 5
    public static final int SIZE5 = (Double.SIZE + Float.SIZE + Short.SIZE*2) / 8;

    public static int getSize(int formatVersion)
    {
        return (formatVersion > SkylineBinaryParser.FORMAT_VERSION_CACHE_4 ? SIZE5 : SIZE4);
    }

    public enum Source
    {
        fragment, sim, ms1, unknown;

        public static Source fromBits(short bits)
        {
//            source1 =       0x01,   // unknown = 00, fragment = 01
//            source2 =       0x02,   // ms1     = 10, sim      = 11

            boolean source1 = (bits & 0x01) != 0;
            boolean source2 = (bits & 0x02) != 0;
            if (!source1 && !source2)
                return unknown;
            if (!source2)
                return fragment;
            if (!source1)
                return ms1;
            return sim;
        }
    }

    private double _product;
    private float _extractionWidth;
    private Source _source;

    public ChromatogramTran(double product)
    {
        this(product, 0, Source.unknown);
    }

    public ChromatogramTran(double product, float extractionWidth, Source source)
    {
        _product = product;
        _extractionWidth = extractionWidth;
        _source = source;
    }

    public double getProduct()
    {
        return _product;
    }

    public float getExtractionWidth()
    {
        return _extractionWidth;
    }

    public Source getSource()
    {
        return _source;
    }
}
