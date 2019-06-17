/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Date: 4/16/12
 * Time: 3:42 PM
 */
public class GeneralMoleculeChromInfo extends ChromInfo
{
    private int _generalMoleculeId;

    private double _peakCountRatio;
    private Double _retentionTime;
    private Double _calculatedConcentration;
    private boolean _excludeFromCalibration;

    public int getGeneralMoleculeId()
    {
        return _generalMoleculeId;
    }

    public void setGeneralMoleculeId(int gMId)
    {
        _generalMoleculeId = gMId;
    }

    public double getPeakCountRatio()
    {
        return _peakCountRatio;
    }

    public void setPeakCountRatio(double peakCountRatio)
    {
        _peakCountRatio = peakCountRatio;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getCalculatedConcentration()
    {
        return _calculatedConcentration;
    }

    public void setCalculatedConcentration(Double calculatedConcentration)
    {
        _calculatedConcentration = calculatedConcentration;
    }

    public boolean isExcludeFromCalibration()
    {
        return _excludeFromCalibration;
    }

    public void setExcludeFromCalibration(boolean excludeFromCalibration)
    {
        _excludeFromCalibration = excludeFromCalibration;
    }
}
