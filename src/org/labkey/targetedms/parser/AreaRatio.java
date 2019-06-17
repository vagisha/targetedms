/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
 * User: vsharma
 * Date: 7/24/12
 * Time: 11:06 AM
 */
public class AreaRatio extends SkylineEntity
{
    private int _isotopeLabelId;
    private int _isotopeLabelStdId;
    private double _areaRatio;

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }

    public int getIsotopeLabelStdId()
    {
        return _isotopeLabelStdId;
    }

    public void setIsotopeLabelStdId(int isotopeLabelStdId)
    {
        _isotopeLabelStdId = isotopeLabelStdId;
    }

    public double getAreaRatio()
    {
        return _areaRatio;
    }

    public void setAreaRatio(double areaRatio)
    {
        _areaRatio = areaRatio;
    }
}
