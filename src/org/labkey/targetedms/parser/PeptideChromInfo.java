/*
 * Copyright (c) 2012 LabKey Corporation
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
public class PeptideChromInfo extends ChromInfo
{
    private int _peptideId;

    private double _peakCountRatio;
    private Double _retentionTime;
    private Double _predictedRetentionTime;
    private double _ratioToStandard;

    public int getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(int peptideId)
    {
        this._peptideId = peptideId;
    }

    public double getPeakCountRatio()
    {
        return _peakCountRatio;
    }

    public void setPeakCountRatio(double peakCountRatio)
    {
        this._peakCountRatio = peakCountRatio;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        this._retentionTime = retentionTime;
    }

    public Double getPredictedRetentionTime()
    {
        return _predictedRetentionTime;
    }

    public void setPredictedRetentionTime(Double predictedRetentionTime)
    {
        this._predictedRetentionTime = predictedRetentionTime;
    }

    public double getRatioToStandard()
    {
        return _ratioToStandard;
    }

    public void setRatioToStandard(double ratioToStandard)
    {
        this._ratioToStandard = ratioToStandard;
    }
}
