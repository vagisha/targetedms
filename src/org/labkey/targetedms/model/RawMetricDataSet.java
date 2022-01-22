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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.targetedms.model.OutlierCounts;
import org.labkey.api.visualization.Stats;
import org.labkey.targetedms.chart.LabelFactory;

import java.text.DecimalFormat;

public class RawMetricDataSet
{
    private final SampleFileQCMetadata _sampleFile;

    String seriesLabel;
    Double metricValue;
    int metricId;
    int metricSeriesIndex;

    Long precursorChromInfoId;

    Double mR;
    Double cusumMP;
    Double cusumMN;
    Double cusumVP;
    Double cusumVN;

    PrecursorInfo _precursor;

    private GuideSetKey _guideSetKey;

    public RawMetricDataSet(SampleFileQCMetadata metadata, PrecursorInfo precursor)
    {
        if (metadata == null)
        {
            throw new IllegalArgumentException();
        }
        _sampleFile = metadata;
        _precursor = precursor;
    }

    public SampleFileQCMetadata getSampleFile()
    {
        return _sampleFile;
    }

    public static class PrecursorInfo
    {
        /** Not thread safe but expensive to create so carefully shared */
        DecimalFormat format;

        long precursorId;
        double mz;
        String modifiedSequence;
        String customIonName;
        String ionFormula;
        Double massMonoisotopic;
        Double massAverage;
        int precursorCharge;

        String seriesLabel;

        public PrecursorInfo(DecimalFormat format)
        {
            this.format = format;
        }

        public void setPrecursorId(long precursorId)
        {
            this.precursorId = precursorId;
        }

        public long getPrecursorId()
        {
            return precursorId;
        }

        public void setMz(double mz)
        {
            this.mz = mz;
        }

        public void setPrecursorCharge(int precursorCharge)
        {
            this.precursorCharge = precursorCharge;
        }

        public void setModifiedSequence(String modifiedSequence)
        {
            this.modifiedSequence = modifiedSequence;
        }

        public void setCustomIonName(String customIonName)
        {
            this.customIonName = customIonName;
        }

        public void setIonFormula(String ionFormula)
        {
            this.ionFormula = ionFormula;
        }

        public void setMassMonoisotopic(Double massMonoisotopic)
        {
            this.massMonoisotopic = massMonoisotopic;
        }

        public void setMassAverage(Double massAverage)
        {
            this.massAverage = massAverage;
        }

        public String getSeriesLabel()
        {
            if (seriesLabel == null)
            {
                StringBuilder modifiedSL = new StringBuilder();

                if (null != modifiedSequence)
                {
                    modifiedSL.append(modifiedSequence);
                }
                else
                {
                    if (null != customIonName)
                    {
                        modifiedSL.append(customIonName);
                        modifiedSL.append(", ");
                    }

                    if (null != ionFormula)
                    {
                        modifiedSL.append(ionFormula);
                        modifiedSL.append(",");
                    }

                    if (null != massMonoisotopic && null != massAverage)
                    {
                        modifiedSL.append(" [");
                        modifiedSL.append(format.format(massMonoisotopic));
                        modifiedSL.append("/");
                        modifiedSL.append(format.format(massAverage));
                        modifiedSL.append("] ");
                    }
                }

                modifiedSL.append(" ");
                modifiedSL.append(LabelFactory.getChargeLabel(precursorCharge, false));

                modifiedSL.append(", ");
                modifiedSL.append(format.format(mz));

                seriesLabel = modifiedSL.toString();
            }
            return seriesLabel;
        }

        public String getDataType()
        {
            return modifiedSequence != null ? "Peptide" : "Fragment";
        }
    }

    @Nullable
    public String getSeriesLabel()
    {
        if (null != seriesLabel)
        {
            return seriesLabel;
        }
        else if (_precursor != null)
        {
            return _precursor.getSeriesLabel();
        }
        return "";
    }

    public void setSeriesLabel(String seriesLabel)
    {
        this.seriesLabel = seriesLabel;
    }

    @Nullable
    public Double getMetricValue()
    {
        return metricValue;
    }

    public void setMetricValue(Double metricValue)
    {
        this.metricValue = metricValue;
    }

    public int getMetricId()
    {
        return metricId;
    }

    public void setMetricId(int metricId)
    {
        this.metricId = metricId;
    }

    public int getMetricSeriesIndex()
    {
        return metricSeriesIndex;
    }

    public void setMetricSeriesIndex(int metricSeriesIndex)
    {
        this.metricSeriesIndex = metricSeriesIndex;
    }

    public GuideSetKey getGuideSetKey()
    {
        if (_guideSetKey == null)
        {
            _guideSetKey = new GuideSetKey(getMetricId(), getMetricSeriesIndex(), _sampleFile.getGuideSetId(), getSeriesLabel());
        }
        return _guideSetKey;
    }

    @Nullable
    public Long getPrecursorId()
    {
        return _precursor == null ? null : _precursor.precursorId;
    }

    @Nullable
    public Long getPrecursorChromInfoId()
    {
        return precursorChromInfoId;
    }

    public void setPrecursorChromInfoId(Long precursorChromInfoId)
    {
        this.precursorChromInfoId = precursorChromInfoId;
    }

    public String getDataType()
    {
        return _precursor == null ? "Other" : _precursor.getDataType();
    }

    @Nullable
    public Double getMz()
    {
        return _precursor == null ? null : _precursor.mz;
    }

    @Nullable
    public Double getmR()
    {
        return mR;
    }

    public void setmR(Double mR)
    {
        this.mR = mR;
    }

    @Nullable
    public Double getCUSUMmP()
    {
        return cusumMP;
    }

    public void setCUSUMmP(Double d)
    {
        this.cusumMP = d;
    }

    @Nullable
    public Double getCUSUMmN()
    {
        return cusumMN;
    }

    public void setCUSUMmN(Double d)
    {
        this.cusumMN = d;
    }

    @Nullable
    public Double getCUSUMvP()
    {
        return cusumVP;
    }

    public void setCUSUMvP(Double d)
    {
        this.cusumVP = d;
    }

    @Nullable
    public Double getCUSUMvN()
    {
        return cusumVN;
    }

    public void setCUSUMvN(Double d)
    {
        this.cusumVN = d;
    }

    public boolean isLeveyJenningsOutlier(GuideSetStats stat)
    {
        if (stat == null || _sampleFile.isIgnoreInQC(stat.getKey().getMetricId()) || metricValue == null)
        {
            return false;
        }

        double upperLimit = stat.getAverage() + stat.getStandardDeviation() * 3;
        double lowerLimit = stat.getAverage() - stat.getStandardDeviation() * 3;

        return metricValue > upperLimit || metricValue < lowerLimit || (Double.isNaN(stat.getStandardDeviation()) && metricValue != stat.getAverage());
    }

    public boolean isMovingRangeOutlier(GuideSetStats stat)
    {
        return  stat != null && !_sampleFile.isIgnoreInQC(getMetricId()) && mR != null && mR > Stats.MOVING_RANGE_UPPER_LIMIT_WEIGHT * stat.getMovingRangeAverage();
    }

    private boolean isCUSUMOutlier(Double value)
    {
        return !_sampleFile.isIgnoreInQC(getMetricId()) && value != null && value > Stats.CUSUM_CONTROL_LIMIT;
    }

    public boolean isCUSUMvPOutlier()
    {
        return isCUSUMOutlier(cusumVP);
    }

    public boolean isCUSUMvNOutlier()
    {
        return isCUSUMOutlier(cusumVN);
    }

    public boolean isCUSUMmPOutlier()
    {
        return isCUSUMOutlier(cusumMP);
    }

    public boolean isCUSUMmNOutlier()
    {
        return isCUSUMOutlier(cusumMN);
    }

    public void increment(@NotNull OutlierCounts counts, @NotNull GuideSetStats stats)
    {
        counts.setTotalCount(counts.getTotalCount() + 1);
        if (isLeveyJenningsOutlier(stats))
        {
            counts.setLeveyJennings(counts.getLeveyJennings() + 1);
        }
        if (isMovingRangeOutlier(stats))
        {
            counts.setmR(counts.getmR() + 1);
        }
        if (isCUSUMmPOutlier())
        {
            counts.setCUSUMmP(counts.getCUSUMmP() + 1);
        }
        if (isCUSUMmNOutlier())
        {
            counts.setCUSUMmN(counts.getCUSUMmN() + 1);
        }
        if (isCUSUMvPOutlier())
        {
            counts.setCUSUMvP(counts.getCUSUMvP() + 1);
        }
        if (isCUSUMvNOutlier())
        {
            counts.setCUSUMvN(counts.getCUSUMvN() + 1);
        }
    }
}
