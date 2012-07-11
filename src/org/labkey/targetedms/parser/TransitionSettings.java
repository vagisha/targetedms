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

import java.util.List;

/**
 * User: vsharma
 * Date: 4/18/12
 * Time: 10:33 PM
 */
public class TransitionSettings
{
    private FullScanSettings _fullScanSettings;
    private PredictionSettings _predictionSettings;
    private InstrumentSettings _instrumentSettings;

    public FullScanSettings getFullScanSettings()
    {
        return _fullScanSettings;
    }

    public void setFullScanSettings(FullScanSettings fullScanSettings)
    {
        _fullScanSettings = fullScanSettings;
    }

    public PredictionSettings getPredictionSettings()
    {
        return _predictionSettings;
    }

    public void setPredictionSettings(PredictionSettings predictionSettings)
    {
        _predictionSettings = predictionSettings;
    }

    public InstrumentSettings getInstrumentSettings()
    {
        return _instrumentSettings;
    }

    public void setInstrumentSettings(InstrumentSettings instrumentSettings)
    {
        _instrumentSettings = instrumentSettings;
    }

    public static final class FullScanSettings
    {
        private int _runId;
        private Double precursorFilter;
        private Double precursorLeftFilter;
        private Double precursorRightFilter;
        private String productMassAnalyzer;
        private Double _productRes;
        private Double _productResMz;
        private String _precursorIsotopes;
        private Double _precursorIsotopeFilter;
        private String _precursorMassAnalyzer;
        private Double _precursorRes;
        private Double _precursorResMz;
        private Boolean _scheduleFilter;

        private List<IsotopeEnrichment> _isotopeEnrichmentList;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public Double getPrecursorFilter()
        {
            return precursorFilter;
        }

        public void setPrecursorFilter(Double precursorFilter)
        {
            this.precursorFilter = precursorFilter;
        }

        public Double getPrecursorLeftFilter()
        {
            return precursorLeftFilter;
        }

        public void setPrecursorLeftFilter(Double precursorLeftFilter)
        {
            this.precursorLeftFilter = precursorLeftFilter;
        }

        public Double getPrecursorRightFilter()
        {
            return precursorRightFilter;
        }

        public void setPrecursorRightFilter(Double precursorRightFilter)
        {
            this.precursorRightFilter = precursorRightFilter;
        }

        public String getProductMassAnalyzer()
        {
            return productMassAnalyzer;
        }

        public void setProductMassAnalyzer(String productMassAnalyzer)
        {
            this.productMassAnalyzer = productMassAnalyzer;
        }

        public Double getProductRes()
        {
            return _productRes;
        }

        public void setProductRes(Double productRes)
        {
            _productRes = productRes;
        }

        public Double getProductResMz()
        {
            return _productResMz;
        }

        public void setProductResMz(Double productResMz)
        {
            _productResMz = productResMz;
        }

        public String getPrecursorIsotopes()
        {
            return _precursorIsotopes;
        }

        public void setPrecursorIsotopes(String precursorIsotopes)
        {
            _precursorIsotopes = precursorIsotopes;
        }

        public Double getPrecursorIsotopeFilter()
        {
            return _precursorIsotopeFilter;
        }

        public void setPrecursorIsotopeFilter(Double precursorIsotopeFilter)
        {
            _precursorIsotopeFilter = precursorIsotopeFilter;
        }

        public String getPrecursorMassAnalyzer()
        {
            return _precursorMassAnalyzer;
        }

        public void setPrecursorMassAnalyzer(String precursorMassAnalyzer)
        {
            _precursorMassAnalyzer = precursorMassAnalyzer;
        }

        public Double getPrecursorRes()
        {
            return _precursorRes;
        }

        public void setPrecursorRes(Double precursorRes)
        {
            _precursorRes = precursorRes;
        }

        public Double getPrecursorResMz()
        {
            return _precursorResMz;
        }

        public void setPrecursorResMz(Double precursorResMz)
        {
            _precursorResMz = precursorResMz;
        }

        public Boolean getScheduleFilter()
        {
            return _scheduleFilter;
        }

        public void setScheduleFilter(Boolean scheduleFilter)
        {
            _scheduleFilter = scheduleFilter;
        }

        public List<IsotopeEnrichment> getIsotopeEnrichmentList()
        {
            return _isotopeEnrichmentList;
        }

        public void setIsotopeEnrichmentList(List<IsotopeEnrichment> isotopeEnrichmentList)
        {
            _isotopeEnrichmentList = isotopeEnrichmentList;
        }
    }

    public static final class IsotopeEnrichment extends SkylineEntity
    {
        private int _runId;
        private String _symbol;
        private Double _percentEnrichment;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getSymbol()
        {
            return _symbol;
        }

        public void setSymbol(String symbol)
        {
            _symbol = symbol;
        }

        public Double getPercentEnrichment()
        {
            return _percentEnrichment;
        }

        public void setPercentEnrichment(Double percentEnrichment)
        {
            _percentEnrichment = percentEnrichment;
        }
    }

    public static final class InstrumentSettings
    {
        private int _runId;
        private Boolean _dynamicMin;
        private int _minMz;
        private int _maxMz;
        private double _mzMatchTolerance;
        private Integer _minTime;
        private Integer _maxTime;
        private Integer _maxTransitions;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public Boolean getDynamicMin()
        {
            return _dynamicMin;
        }

        public void setDynamicMin(Boolean dynamicMin)
        {
            _dynamicMin = dynamicMin;
        }

        public int getMinMz()
        {
            return _minMz;
        }

        public void setMinMz(int minMz)
        {
            _minMz = minMz;
        }

        public int getMaxMz()
        {
            return _maxMz;
        }

        public void setMaxMz(int maxMz)
        {
            _maxMz = maxMz;
        }

        public double getMzMatchTolerance()
        {
            return _mzMatchTolerance;
        }

        public void setMzMatchTolerance(double mzMatchTolerance)
        {
            _mzMatchTolerance = mzMatchTolerance;
        }

        public Integer getMinTime()
        {
            return _minTime;
        }

        public void setMinTime(Integer minTime)
        {
            _minTime = minTime;
        }

        public Integer getMaxTime()
        {
            return _maxTime;
        }

        public void setMaxTime(Integer maxTime)
        {
            _maxTime = maxTime;
        }

        public Integer getMaxTransitions()
        {
            return _maxTransitions;
        }

        public void setMaxTransitions(Integer maxTransitions)
        {
            _maxTransitions = maxTransitions;
        }
    }
    
    public static final class PredictionSettings
    {
        private int _runId;
        private String _precursorMassType;
        private String _productMassType;
        private String _optimizeBy;

        private int _cePredictorId;
        private int _dpPredictorId;

        private Predictor _cePredictor;
        private Predictor _dpPredictor;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getPrecursorMassType()
        {
            return _precursorMassType;
        }

        public void setPrecursorMassType(String precursorMassType)
        {
            _precursorMassType = precursorMassType;
        }

        public String getProductMassType()
        {
            return _productMassType;
        }

        public void setProductMassType(String productMassType)
        {
            _productMassType = productMassType;
        }

        public String getOptimizeBy()
        {
            return _optimizeBy;
        }

        public void setOptimizeBy(String optimizeBy)
        {
            _optimizeBy = optimizeBy;
        }

        public int getCePredictorId()
        {
            return _cePredictorId;
        }

        public void setCePredictorId(int cePredictorId)
        {
            _cePredictorId = cePredictorId;
        }

        public int getDpPredictorId()
        {
            return _dpPredictorId;
        }

        public void setDpPredictorId(int dpPredictorId)
        {
            _dpPredictorId = dpPredictorId;
        }

        public Predictor getCePredictor()
        {
            return _cePredictor;
        }

        public void setCePredictor(Predictor cePredictor)
        {
            _cePredictor = cePredictor;
        }

        public Predictor getDpPredictor()
        {
            return _dpPredictor;
        }

        public void setDpPredictor(Predictor dpPredictor)
        {
            _dpPredictor = dpPredictor;
        }
    }

    public static final class Predictor extends SkylineEntity
    {
        private String _name;
        private Integer _stepSize;
        private Integer _stepCount;

        private List<PredictorSettings> _settings;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public Integer getStepSize()
        {
            return _stepSize;
        }

        public void setStepSize(Integer stepSize)
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

        public List<PredictorSettings> getSettings()
        {
            return _settings;
        }

        public void setSettings(List<PredictorSettings> settings)
        {
            _settings = settings;
        }
    }

    public static class PredictorSettings extends SkylineEntity
    {
        private int _predictorId;
        private Integer _charge;
        private Double _slope;
        private Double _intercept;

        public Integer getCharge()
        {
            return _charge;
        }

        public void setCharge(Integer charge)
        {
            _charge = charge;
        }

        public Double getSlope()
        {
            return _slope;
        }

        public void setSlope(Double slope)
        {
            _slope = slope;
        }

        public Double getIntercept()
        {
            return _intercept;
        }

        public void setIntercept(Double intercept)
        {
            _intercept = intercept;
        }

        public int getPredictorId()
        {
            return _predictorId;
        }

        public void setPredictorId(int predictorId)
        {
            _predictorId = predictorId;
        }
    }
}
