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

import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.ISpectrumLibrary;

import java.util.List;

/**
 * User: vsharma
 * Date: 4/24/12
 * Time: 12:02 PM
 */
public class PeptideSettings
{
    private Enzyme _enzyme;
    private PeptideModifications _modifications;
    private SpectrumLibrarySettings _librarySettings;
    private EnzymeDigestionSettings _enzymeDigestionSettings;
    private PeptidePredictionSettings _peptidePredictionSettings;
    private QuantificationSettings _quantificationSettings;

    public static final String HEAVY_LABEL = "heavy";

    public Enzyme getEnzyme()
    {
        return _enzyme;
    }

    public void setEnzyme(Enzyme enzyme)
    {
        _enzyme = enzyme;
    }

    public PeptideModifications getModifications()
    {
        return _modifications;
    }

    public void setModifications(PeptideModifications modifications)
    {
        _modifications = modifications;
    }

    public SpectrumLibrarySettings getLibrarySettings()
    {
        return _librarySettings;
    }

    public void setLibrarySettings(SpectrumLibrarySettings librarySettings)
    {
        _librarySettings = librarySettings;
    }

    public void setDigestSettings(EnzymeDigestionSettings enzymeDigestionSettings)
    {
        _enzymeDigestionSettings = enzymeDigestionSettings;
    }

    public EnzymeDigestionSettings getEnzymeDigestionSettings()
    {
        return _enzymeDigestionSettings;
    }

    public PeptidePredictionSettings getPeptidePredictionSettings()
    {
        return _peptidePredictionSettings;
    }

    public void setPeptidePredictionSettings(PeptidePredictionSettings peptidePredictionSettings)
    {
        _peptidePredictionSettings = peptidePredictionSettings;
    }

    public QuantificationSettings getQuantificationSettings()
    {
        return _quantificationSettings;
    }

    public void setQuantificationSettings(QuantificationSettings quantificationSettings)
    {
        _quantificationSettings = quantificationSettings;
    }

    // ------------------------------------------------------------------------
    // Isotope labels
    // ------------------------------------------------------------------------
    public static final class IsotopeLabel extends SkylineEntity
    {
        private long _runId;
        private String _name;
        private boolean _standard;

        public static final String LIGHT = "light";

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public boolean isStandard()
        {
            return _standard;
        }

        public void setStandard(boolean standard)
        {
            _standard = standard;
        }
    }
    // ------------------------------------------------------------------------
    // Modification settings
    // ------------------------------------------------------------------------
    public static final class PeptideModifications
    {
        private ModificationSettings _modificationSettings;
        private List<IsotopeLabel> _isotopeLabels;
        private List<RunIsotopeModification> _isotopeModifications;
        private List<RunStructuralModification> _structuralModifications;

        public ModificationSettings getModificationSettings()
        {
            return _modificationSettings;
        }

        public void setModificationSettings(ModificationSettings modificationSettings)
        {
            _modificationSettings = modificationSettings;
        }

        public List<RunIsotopeModification> getIsotopeModifications()
        {
            return _isotopeModifications;
        }

        public void setIsotopeModifications(List<RunIsotopeModification> isotopeModifications)
        {
            _isotopeModifications = isotopeModifications;
        }

        public List<RunStructuralModification> getStructuralModifications()
        {
            return _structuralModifications;
        }

        public void setStructuralModifications(List<RunStructuralModification> structuralModifications)
        {
            _structuralModifications = structuralModifications;
        }

        public List<IsotopeLabel> getIsotopeLabels()
        {
            return _isotopeLabels;
        }

        public void setIsotopeLabels(List<IsotopeLabel> isotopeLabels)
        {
            _isotopeLabels = isotopeLabels;
        }
    }

    public static final class ModificationSettings
    {
        private long _runId;
        private int _maxVariableMods;
        private int _maxNeutralLosses;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public int getMaxVariableMods()
        {
            return _maxVariableMods;
        }

        public void setMaxVariableMods(int maxVariableMods)
        {
            _maxVariableMods = maxVariableMods;
        }

        public int getMaxNeutralLosses()
        {
            return _maxNeutralLosses;
        }

        public void setMaxNeutralLosses(int maxNeutralLosses)
        {
            _maxNeutralLosses = maxNeutralLosses;
        }
    }

    public static final class RunIsotopeModification extends IsotopeModification
    {
        private long _runId;
        private long _isotopeModId;
        private long _isotopeLabelId;

        private String _isotopeLabel;
        private Boolean _explicitMod;
        private String _relativeRt;  // One of "Matching", "Overlapping", "Preceding", "Unknown"

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public long getIsotopeModId()
        {
            return _isotopeModId;
        }

        public void setIsotopeModId(long isotopeModId)
        {
            _isotopeModId = isotopeModId;
        }

        public long getIsotopeLabelId()
        {
            return _isotopeLabelId;
        }

        public void setIsotopeLabelId(long isotopeLabelId)
        {
            _isotopeLabelId = isotopeLabelId;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(String isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public Boolean getExplicitMod()
        {
            return _explicitMod;
        }

        public void setExplicitMod(Boolean explicitMod)
        {
            _explicitMod = explicitMod;
        }

        public String getRelativeRt()
        {
            return _relativeRt;
        }

        public void setRelativeRt(String relativeRt)
        {
            _relativeRt = relativeRt;
        }
    }

    public static class IsotopeModification extends Modification implements IModification.IIsotopeModification
    {
        private Boolean _label13C;
        private Boolean _label15N;
        private Boolean _label18O;
        private Boolean _label2H;
        private Boolean _label37Cl;
        private Boolean _label81Br;

        @Override
        public Boolean getLabel13C()
        {
            return _label13C;
        }

        public void setLabel13C(Boolean label13C)
        {
            _label13C = label13C;
        }

        @Override
        public Boolean getLabel15N()
        {
            return _label15N;
        }

        public void setLabel15N(Boolean label15N)
        {
            _label15N = label15N;
        }

        @Override
        public Boolean getLabel18O()
        {
            return _label18O;
        }

        public void setLabel18O(Boolean label18O)
        {
            _label18O = label18O;
        }

        @Override
        public Boolean getLabel2H()
        {
            return _label2H;
        }

        public void setLabel2H(Boolean label2H)
        {
            _label2H = label2H;
        }

        public Boolean getLabel37Cl()
        {
            return _label37Cl;
        }

        public void setLabel37Cl(Boolean label37Cl)
        {
            _label37Cl = label37Cl;
        }

        public Boolean getLabel81Br()
        {
            return _label81Br;
        }

        public void setLabel81Br(Boolean label81Br)
        {
            _label81Br = label81Br;
        }
    }

    public static class PotentialLoss extends SkylineEntity
    {
        private long _structuralModId;
        private String _formula;
        private Double _massDiffMono;
        private Double _massDiffAvg;
        private String _inclusion;

        public long getStructuralModId()
        {
            return _structuralModId;
        }

        public void setStructuralModId(long structuralModId)
        {
            _structuralModId = structuralModId;
        }

        public String getFormula()
        {
            return _formula;
        }

        public void setFormula(String formula)
        {
            _formula = formula;
        }

        public Double getMassDiffMono()
        {
            return _massDiffMono;
        }

        public void setMassDiffMono(Double massDiffMono)
        {
            _massDiffMono = massDiffMono;
        }

        public Double getMassDiffAvg()
        {
            return _massDiffAvg;
        }

        public void setMassDiffAvg(Double massDiffAvg)
        {
            _massDiffAvg = massDiffAvg;
        }

        public String getInclusion()
        {
            return _inclusion;
        }

        public void setInclusion(String inclusion)
        {
            _inclusion = inclusion;
        }

        @Override
        /** NOT using the Id in the equality check - just the other fields */
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PotentialLoss that = (PotentialLoss) o;

            if (_formula != null ? !_formula.equals(that._formula) : that._formula != null) return false;
            if (_inclusion != null ? !_inclusion.equals(that._inclusion) : that._inclusion != null) return false;
            if (_massDiffAvg != null ? !_massDiffAvg.equals(that._massDiffAvg) : that._massDiffAvg != null)
                return false;
            if (_massDiffMono != null ? !_massDiffMono.equals(that._massDiffMono) : that._massDiffMono != null)
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _formula != null ? _formula.hashCode() : 0;
            result = 31 * result + (_massDiffMono != null ? _massDiffMono.hashCode() : 0);
            result = 31 * result + (_massDiffAvg != null ? _massDiffAvg.hashCode() : 0);
            result = 31 * result + (_inclusion != null ? _inclusion.hashCode() : 0);
            return result;
        }
    }

    public static final class RunStructuralModification extends StructuralModification
    {
        private long _runId;
        private long _structuralModId;

        private Boolean _explicitMod;
        private List<PotentialLoss> _potentialLosses;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public long getStructuralModId()
        {
            return _structuralModId;
        }

        public void setStructuralModId(long structuralModId)
        {
            _structuralModId = structuralModId;
        }

        public Boolean getExplicitMod()
        {
            return _explicitMod;
        }

        public boolean isModExplicit()
        {
            return _explicitMod == null ? false : _explicitMod;
        }

        public void setExplicitMod(Boolean explicitMod)
        {
            _explicitMod = explicitMod;
        }

        public void setPotentialLosses(List<PotentialLoss> potentialLosses)
        {
            _potentialLosses = potentialLosses;
        }

        public List<PotentialLoss> getPotentialLosses()
        {
            return _potentialLosses;
        }
    }

    public static class StructuralModification extends Modification implements IModification.IStructuralModification
    {
        private boolean _variable;

        @Override
        public boolean isVariable()
        {
            return _variable;
        }

        public void setVariable(boolean variable)
        {
            _variable = variable;
        }
    }

    public static class Modification extends SkylineEntity implements IModification
    {
        private String _name;
        private String _aminoAcid;
        private String _terminus;
        private String _formula;
        private Double _massDiffMono;
        private Double _massDiffAvg;
        private Integer _unimodId;

        @Override
        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        @Override
        public String getAminoAcid()
        {
            return _aminoAcid;
        }

        public void setAminoAcid(String aminoAcid)
        {
            _aminoAcid = aminoAcid;
        }

        @Override
        public String getTerminus()
        {
            return _terminus;
        }

        public void setTerminus(String terminus)
        {
            _terminus = terminus;
        }

        @Override
        public String getFormula()
        {
            return _formula;
        }

        public void setFormula(String formula)
        {
            _formula = formula;
        }

        @Override
        public Double getMassDiffMono()
        {
            return _massDiffMono;
        }

        public void setMassDiffMono(Double massDiffMono)
        {
            _massDiffMono = massDiffMono;
        }

        @Override
        public Double getMassDiffAvg()
        {
            return _massDiffAvg;
        }

        public void setMassDiffAvg(Double massDiffAvg)
        {
            _massDiffAvg = massDiffAvg;
        }

        @Override
        public Integer getUnimodId()
        {
            return _unimodId;
        }

        public void setUnimodId(Integer unimodId)
        {
            _unimodId = unimodId;
        }
    }

    // ------------------------------------------------------------------------
    // Enzyme settings
    // ------------------------------------------------------------------------
    public static final class EnzymeDigestionSettings
    {
        private long _enzymeId;
        private long _runId;
        private Integer _maxMissedCleavages;
        private Boolean _excludeRaggedEnds;

        public long getEnzymeId()
        {
            return _enzymeId;
        }

        public void setEnzymeId(long enzymeId)
        {
            _enzymeId = enzymeId;
        }

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public Integer getMaxMissedCleavages()
        {
            return _maxMissedCleavages;
        }

        public void setMaxMissedCleavages(Integer maxMissedCleavages)
        {
            _maxMissedCleavages = maxMissedCleavages;
        }

        public Boolean getExcludeRaggedEnds()
        {
            return _excludeRaggedEnds;
        }

        public void setExcludeRaggedEnds(Boolean excludeRaggedEnds)
        {
            _excludeRaggedEnds = excludeRaggedEnds;
        }
    }

    public static final class Enzyme extends SkylineEntity
    {
        private String _name;
        private String _cut; // amino acids at which this _enzyme cleaves the peptide
        private String _noCut;
        private String _sense; // 'N' or  'C'
        private String _cutC;
        private String _noCutC;
        private String _cutN;
        private String _noCutN;
        private Boolean _semi;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getCut()
        {
            return _cut;
        }

        public void setCut(String cut)
        {
            _cut = cut;
        }

        public String getNoCut()
        {
            return _noCut;
        }

        public void setNoCut(String noCut)
        {
            _noCut = noCut;
        }

        public String getSense()
        {
            return _sense;
        }

        public void setSense(String sense)
        {
            _sense = sense;
        }

        public String getCutC()
        {
            return _cutC;
        }

        public void setCutC(String cutC)
        {
            _cutC = cutC;
        }

        public String getNoCutC()
        {
            return _noCutC;
        }

        public void setNoCutC(String noCutC)
        {
            _noCutC = noCutC;
        }

        public String getCutN()
        {
            return _cutN;
        }

        public void setCutN(String cutN)
        {
            _cutN = cutN;
        }

        public String getNoCutN()
        {
            return _noCutN;
        }

        public void setNoCutN(String noCutN)
        {
            _noCutN = noCutN;
        }

        public Boolean getSemi()
        {
            return _semi;
        }

        public void setSemi(Boolean semi)
        {
            _semi = semi;
        }
    }

    // ------------------------------------------------------------------------
    // Spectrum Library Settings
    // ------------------------------------------------------------------------
    public static final class SpectrumLibrarySettings
    {
        private long _runId;
        private String _pick;  // One of 'library', 'filter', 'both', 'either'
        private String _rankType; // One of 'Picked intensity' or 'Spectrum count'
        private Integer _peptideCount;
        private Double _ionMatchTolerance;

        private List<SpectrumLibrary> libraries;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public String getPick()
        {
            return _pick;
        }

        public void setPick(String pick)
        {
            _pick = pick;
        }

        public String getRankType()
        {
            return _rankType;
        }

        public void setRankType(String rankType)
        {
            _rankType = rankType;
        }

        public Integer getPeptideCount()
        {
            return _peptideCount;
        }

        public void setPeptideCount(Integer peptideCount)
        {
            _peptideCount = peptideCount;
        }

        public Double getIonMatchTolerance()
        {
            return _ionMatchTolerance;
        }

        public void setIonMatchTolerance(Double ionMatchTolerance)
        {
            _ionMatchTolerance = ionMatchTolerance;
        }

        public List<SpectrumLibrary> getLibraries()
        {
            return libraries;
        }

        public void setLibraries(List<SpectrumLibrary> libraries)
        {
            this.libraries = libraries;
        }
    }

    public static final class SpectrumLibrary extends SkylineEntity implements ISpectrumLibrary
    {
        private long _runId;
        private String _name;
        private String _fileNameHint;
        private String _skylineLibraryId;  // lsid in <bibliospec_lite_library> element, id in others
        private String _revision;
        private String _libraryType;
        private Boolean _useExplicitPeakBounds;
        private String _panoramaServer;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getFileNameHint()
        {
            return _fileNameHint;
        }

        public void setFileNameHint(String fileNameHint)
        {
            _fileNameHint = fileNameHint;
        }

        public String getSkylineLibraryId()
        {
            return _skylineLibraryId;
        }

        public void setSkylineLibraryId(String skylineLibraryId)
        {
            _skylineLibraryId = skylineLibraryId;
        }

        public String getRevision()
        {
            return _revision;
        }

        public void setRevision(String revision)
        {
            _revision = revision;
        }

        public String getLibraryType()
        {
            return _libraryType;
        }

        public void setLibraryType(String libraryType)
        {
            _libraryType = libraryType;
        }

        public Boolean getUseExplicitPeakBounds()
        {
            return _useExplicitPeakBounds;
        }

        public void setUseExplicitPeakBounds(Boolean useExplicitPeakBounds)
        {
            _useExplicitPeakBounds = useExplicitPeakBounds;
        }

        public String getPanoramaServer()
        {
            return _panoramaServer;
        }

        public void setPanoramaServer(String panoramaServer)
        {
            _panoramaServer = panoramaServer;
        }
    }

    // ------------------------------------------------------------------------
    // Peptide Prediction Settings
    // ------------------------------------------------------------------------
    public static class PeptidePredictionSettings
    {
        private RetentionTimePredictionSettings _rtPredictionSettings;
        private DriftTimePredictionSettings _dtPredictionSettings;

        public RetentionTimePredictionSettings getRtPredictionSettings()
        {
            return _rtPredictionSettings;
        }

        public void setRtPredictionSettings(RetentionTimePredictionSettings rtPredictionSettings)
        {
            _rtPredictionSettings = rtPredictionSettings;
        }

        public DriftTimePredictionSettings getDtPredictionSettings()
        {
            return _dtPredictionSettings;
        }

        public void setDtPredictionSettings(DriftTimePredictionSettings dtPredictionSettings)
        {
            _dtPredictionSettings = dtPredictionSettings;
        }
    }

    public static final class RetentionTimePredictionSettings
    {
        private long _runId;
        private Boolean _useMeasuredRts;
        private Double _measuredRtWindow;
        private String _predictorName;
        private Double _timeWindow;
        private Double _regressionSlope;
        private Double _regressionIntercept;
        private Boolean _isIrt;
        private String _calculatorName;
        private String _irtDatabasePath;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public Boolean getUseMeasuredRts()
        {
            return _useMeasuredRts;
        }

        public void setUseMeasuredRts(Boolean useMeasuredRts)
        {
            _useMeasuredRts = useMeasuredRts;
        }

        public Double getMeasuredRtWindow()
        {
            return _measuredRtWindow;
        }

        public void setMeasuredRtWindow(Double measuredRtWindow)
        {
            _measuredRtWindow = measuredRtWindow;
        }

        public String getPredictorName()
        {
            return _predictorName;
        }

        public void setPredictorName(String predictorName)
        {
            _predictorName = predictorName;
        }

        public Double getTimeWindow()
        {
            return _timeWindow;
        }

        public void setTimeWindow(Double timeWindow)
        {
            _timeWindow = timeWindow;
        }

        public Double getRegressionSlope()
        {
            return _regressionSlope;
        }

        public void setRegressionSlope(Double regressionSlope)
        {
            _regressionSlope = regressionSlope;
        }

        public Double getRegressionIntercept()
        {
            return _regressionIntercept;
        }

        public void setRegressionIntercept(Double regressionIntercept)
        {
            _regressionIntercept = regressionIntercept;
        }

        public Boolean getIsIrt()
        {
            return _isIrt;
        }

        public void setIsIrt(Boolean irt)
        {
            _isIrt = irt;
        }

        public String getCalculatorName()
        {
            return _calculatorName;
        }

        public void setCalculatorName(String calculatorName)
        {
            _calculatorName = calculatorName;
        }

        public String getIrtDatabasePath()
        {
            return _irtDatabasePath;
        }

        public void setIrtDatabasePath(String irtDatabasePath)
        {
            _irtDatabasePath = irtDatabasePath;
        }
    }

    // ------------------------------------------------------------------------
    // Peptide Prediction Settings -- drift time prediction settings
    // ------------------------------------------------------------------------
    public static final class DriftTimePredictionSettings extends SkylineEntity
    {
        private long _runId;
        private Boolean _useSpectralLibraryDriftTimes;
        private Double _spectralLibraryDriftTimesResolvingPower;
        private String _predictorName;
        private Double _resolvingPower;
        private List<MeasuredDriftTime> _measuredDriftTimes;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public Boolean getUseSpectralLibraryDriftTimes()
        {
            return _useSpectralLibraryDriftTimes;
        }

        public void setUseSpectralLibraryDriftTimes(Boolean useSpectralLibraryDriftTimes)
        {
            _useSpectralLibraryDriftTimes = useSpectralLibraryDriftTimes;
        }

        public Double getSpectralLibraryDriftTimesResolvingPower()
        {
            return _spectralLibraryDriftTimesResolvingPower;
        }

        public void setSpectralLibraryDriftTimesResolvingPower(Double spectralLibraryDriftTimesResolvingPower)
        {
            _spectralLibraryDriftTimesResolvingPower = spectralLibraryDriftTimesResolvingPower;
        }

        public String getPredictorName()
        {
            return _predictorName;
        }

        public void setPredictorName(String predictorName)
        {
            _predictorName = predictorName;
        }

        public Double getResolvingPower()
        {
            return _resolvingPower;
        }

        public void setResolvingPower(Double resolvingPower)
        {
            _resolvingPower = resolvingPower;
        }

        public List<MeasuredDriftTime> getMeasuredDriftTimes()
        {
            return _measuredDriftTimes;
        }

        public void setMeasuredDriftTimes(List<MeasuredDriftTime> measuredDriftTimes)
        {
            _measuredDriftTimes = measuredDriftTimes;
        }
    }

    // ------------------------------------------------------------------------
    // Peptide Prediction Settings -- measured drift times
    // ------------------------------------------------------------------------
    public static final class MeasuredDriftTime extends SkylineEntity
    {
        private long _driftTimePredictionSettingsId;
        private String _modifiedSequence;
        private String _charge;
        private Double _driftTime;
        private Double _highEnergyDriftTimeOffset;
        private Double _ccs;
        private Double _ionMobility;
        private Double _highEnergyIonMobilityOffset;
        private String _ionMobilityUnits;

        public long getDriftTimePredictionSettingsId()
        {
            return _driftTimePredictionSettingsId;
        }

        public void setDriftTimePredictionSettingsId(long driftTimePredictionSettingsId)
        {
            _driftTimePredictionSettingsId = driftTimePredictionSettingsId;
        }

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public void setModifiedSequence(String modifiedSequence)
        {
            _modifiedSequence = modifiedSequence;
        }

        public String getCharge()
        {
            return _charge;
        }

        public void setCharge(String charge)
        {
            _charge = charge;
        }

        public Double getDriftTime()
        {
            return _driftTime;
        }

        public void setDriftTime(Double driftTime)
        {
            _driftTime = driftTime;
        }

        public Double getHighEnergyDriftTimeOffset()
        {
            return _highEnergyDriftTimeOffset;
        }

        public void setHighEnergyDriftTimeOffset(Double highEnergyDriftTimeOffset)
        {
            _highEnergyDriftTimeOffset = highEnergyDriftTimeOffset;
        }

        public Double getCcs()
        {
            return _ccs;
        }

        public void setCcs(Double ccs)
        {
            _ccs = ccs;
        }

        public Double getIonMobility()
        {
            return _ionMobility;
        }

        public void setIonMobility(Double ionMobility)
        {
            _ionMobility = ionMobility;
        }

        public Double getHighEnergyIonMobilityOffset()
        {
            return _highEnergyIonMobilityOffset;
        }

        public void setHighEnergyIonMobilityOffset(Double highEnergyIonMobilityOffset)
        {
            _highEnergyIonMobilityOffset = highEnergyIonMobilityOffset;
        }

        public String getIonMobilityUnits()
        {
            return _ionMobilityUnits;
        }

        public void setIonMobilityUnits(String ionMobilityUnits)
        {
            _ionMobilityUnits = ionMobilityUnits;
        }
    }
}
