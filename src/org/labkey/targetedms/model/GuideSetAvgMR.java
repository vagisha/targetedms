/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.model;

import java.util.Objects;

public class GuideSetAvgMR
{
    private final int guideSetid;
    private final String seriesLabel;
    private final String seriesType;

    private double _average;
    private double _standardDev;

    public GuideSetAvgMR(int guideSetid, String seriesLabel, String seriesType)
    {
        this.guideSetid = guideSetid;
        this.seriesLabel = seriesLabel;
        this.seriesType = seriesType;
    }

    public int getGuideSetid()
    {
        return guideSetid;
    }

    public String getSeriesLabel()
    {
        return seriesLabel;
    }

    public double getAverage()
    {
        return _average;
    }

    public void setAverage(double average)
    {
        _average = average;
    }

    public double getStandardDev()
    {
        return _standardDev;
    }

    public void setStandardDev(double standardDev)
    {
        _standardDev = standardDev;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuideSetAvgMR that = (GuideSetAvgMR) o;
        return guideSetid == that.guideSetid &&
                seriesLabel.equals(that.seriesLabel) &&
                seriesType.equals(that.seriesType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(guideSetid, seriesLabel, seriesType);
    }
}
