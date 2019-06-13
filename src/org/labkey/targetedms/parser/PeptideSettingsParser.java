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

import org.apache.commons.lang3.StringUtils;
import org.labkey.targetedms.SkylineFileUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: vsharma
 * Date: 4/24/12
 * Time: 1:09 PM
 */
class PeptideSettingsParser
{
    private static final String PEPTIDE_SETTINGS = "peptide_settings";
    private static final String ENZYME = "enzyme";
    private static final String DIGEST_SETTINGS = "digest_settings";
    private static final String PEPTIDE_PREDICTION = "peptide_prediction";
    private static final String PEPTIDE_LIBRARIES = "peptide_libraries";
    private static final String PEPTIDE_MODIFICATIONS = "peptide_modifications";
    private static final String INTERNAL_STANDARD = "internal_standard";
    private static final String STATIC_MODIFICATIONS = "static_modifications";
    private static final String STATIC_MODIFICATION = "static_modification";
    private static final String POTENTIAL_LOSS = "potential_loss";
    private static final String HEAVY_MODIFICATIONS = "heavy_modifications";
    private static final String AMINOACID = "aminoacid";
    private static final String TERMINUS = "terminus";
    private static final String FORMULA = "formula";
    private static final String MASSDIFF_MONOISOTOPIC = "massdiff_monoisotopic";
    private static final String MASSDIFF_AVERAGE = "massdiff_average";
    private static final String EXPLICIT_DECL = "explicit_decl";
    private static final String UNIMOD_ID = "unimod_id";
    private static final String NAME = "name";
    private static final String PICK = "pick";
    private static final String RANK_TYPE = "rank_type";
    private static final String PEPTIDE_COUNT = "peptide_count";
    private static final String BIBLIOSPEC_LITE_LIB = "bibliospec_lite_library";
    private static final String BIBLIOSPEC_LIB = "bibliospec_library";
    private static final String HUNTER_LIB = "hunter_library";
    private static final String NIST_LIB = "nist_library";
    private static final String SPECTRAST_LIB = "spectrast_library";
    private static final String FILE_NAME_HINT = "file_name_hint";
    private static final String LSID = "lsid";
    private static final String ID = "id";
    private static final String REVISION = "revision";
    private static final String DOCUMENT_LIBRARY = "document_library";
    private static final String USE_MEASURED_RTS = "use_measured_rts";
    private static final String MEASURED_RT_WINDOW = "measured_rt_window";
    private static final String PREDICT_RETENTION_TIME = "predict_retention_time";
    private static final String TIME_WINDOW = "time_window";
    private static final String CALCULATOR = "calculator";
    private static final String REGRESSION_RT = "regression_rt";
    private static final String SLOPE = "slope";
    private static final String INTERCEPT = "intercept";
    private static final String IRT_CALCULATOR = "irt_calculator";
    private static final String IRT_DATABASE_PATH = "database_path";
    private static final String PREDICT_DRIFT_TIME = "predict_drift_time";
    private static final String USE_SPECTRAL_LIBRARY_DRIFT_TIMES = "use_spectral_library_drift_times";
    private static final String SPECTRAL_LIBRARY_DRIFT_TIMES_RESOLVING_POWER = "spectral_library_drift_times_resolving_power";
    private static final String RESOLVING_POWER = "resolving_power";
    private static final String MEASURED_DT = "measured_dt";
    private static final String QUANTIFICATION = "quantification";
    private static final String WEIGHTING = "weighting";
    private static final String FIT = "fit";
    private static final String NORMALIZATION = "normalization";
    private static final String MS_LEVEL = "ms_level";
    private static final String UNITS = "units";
    private static final String MAX_LOQ_BIAS = "max_loq_bias";
    private static final String MAX_LOQ_CV = "max_loq_cv";
    private static final String LOD_CALCULATION = "lod_calculation";

    private String _documentName;

    public PeptideSettings parse(XMLStreamReader reader, String documentName) throws XMLStreamException
    {
        _documentName = documentName;

        PeptideSettings settings = new PeptideSettings();

        while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_SETTINGS))
             {
                 break;
             }

             if(XmlUtil.isStartElement(reader, evtType, ENZYME))
             {
                 settings.setEnzyme(readEnzyme(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, DIGEST_SETTINGS))
             {
                 settings.setDigestSettings(readDigestSettings(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_PREDICTION))
             {
                 settings.setPeptidePredictionSettings(getPeptidePredictionSettings(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_LIBRARIES))
             {
                 settings.setLibrarySettings(readLibrarySettings(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_MODIFICATIONS))
             {
                 settings.setModifications(readModifications(reader));
             }
             else if (XmlUtil.isStartElement(reader, evtType, QUANTIFICATION)) {
                 settings.setQuantificationSettings(readQuantificationSettings(reader));
             }
         }

        return settings;
    }

    private PeptideSettings.EnzymeDigestionSettings readDigestSettings(XMLStreamReader reader)
    {
        PeptideSettings.EnzymeDigestionSettings result = new PeptideSettings.EnzymeDigestionSettings();
        result.setMaxMissedCleavages(XmlUtil.readIntegerAttribute(reader, "max_missed_cleavages"));
        result.setExcludeRaggedEnds(XmlUtil.readBooleanAttribute(reader, "exclude_ragged_ends"));
        return result;
    }

    private PeptideSettings.Enzyme readEnzyme(XMLStreamReader reader)
    {
        PeptideSettings.Enzyme enzyme = new PeptideSettings.Enzyme();
        enzyme.setName(XmlUtil.readRequiredAttribute(reader, NAME, ENZYME));
        enzyme.setCut(StringUtils.trimToNull(reader.getAttributeValue(null, "cut")));
        enzyme.setNoCut(StringUtils.trimToNull(reader.getAttributeValue(null, "no_cut")));
        enzyme.setSense(StringUtils.trimToNull(reader.getAttributeValue(null, "sense")));
        enzyme.setCutC(StringUtils.trimToNull(reader.getAttributeValue(null, "cut_c")));
        enzyme.setNoCutC(StringUtils.trimToNull(reader.getAttributeValue(null, "no_cut_c")));
        enzyme.setCutN(StringUtils.trimToNull(reader.getAttributeValue(null, "cut_n")));
        enzyme.setNoCutN(StringUtils.trimToNull(reader.getAttributeValue(null, "no_cut_n")));
        return enzyme;
    }

    private PeptideSettings.PeptideModifications readModifications(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.PeptideModifications modifications = new PeptideSettings.PeptideModifications();

        PeptideSettings.ModificationSettings settings = new  PeptideSettings.ModificationSettings();
        Integer maxVariableMods = XmlUtil.readIntegerAttribute(reader, "max_variable_mods");
        if (maxVariableMods != null)
        {
            settings.setMaxVariableMods(maxVariableMods.intValue());
        }
        Integer maxNeutralLosses = XmlUtil.readIntegerAttribute(reader, "max_neutral_losses");
        if (maxNeutralLosses != null)
        {
            settings.setMaxNeutralLosses(maxNeutralLosses.intValue());
        }
        modifications.setModificationSettings(settings);

        // If there is a single internal standard it is written out as an attribute.
        // Otherwise, there is one <internal_standard> element for each standard
        String inernalStandard = reader.getAttributeValue(null, INTERNAL_STANDARD);
        Set<String> internalStandards = new HashSet<>();

        List<PeptideSettings.RunStructuralModification> staticMods = new ArrayList<>();
        List<PeptideSettings.RunIsotopeModification> isotopeMods = new ArrayList<>();
        modifications.setStructuralModifications(staticMods);
        modifications.setIsotopeModifications(isotopeMods);

        if(null != inernalStandard)
        {
            internalStandards.add(inernalStandard);
        }

        List<String> isotopeLabelNames  = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, INTERNAL_STANDARD))
            {
                internalStandards.add(XmlUtil.readRequiredAttribute(reader, NAME, INTERNAL_STANDARD));
            }
            else if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATIONS))
            {
                staticMods.addAll(readStaticModifications(reader));
            }
            else if(XmlUtil.isStartElement(reader, evtType, HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, isotopeLabelNames));
            }
        }

        // Mark the label types that were used as an internal standard
        isotopeLabelNames.add(0, PeptideSettings.IsotopeLabel.LIGHT);

        // If we did not find either the "internal_standard" attribute or elements, check if we have
        // a "heavy" isotope label.  If we do, set "heavy" as the internal standard
        if(internalStandards.size() == 0 && isotopeLabelNames.contains(PeptideSettings.HEAVY_LABEL))
        {
            internalStandards.add(PeptideSettings.HEAVY_LABEL);
        }

        List<PeptideSettings.IsotopeLabel> labels = new ArrayList<>(isotopeLabelNames.size());
        for(String name: isotopeLabelNames)
        {
            PeptideSettings.IsotopeLabel isotopeLabel = new PeptideSettings.IsotopeLabel();
            isotopeLabel.setName(name);
            if(internalStandards.contains(name))
            {
                isotopeLabel.setStandard(true);
            }
            labels.add(isotopeLabel);
        }
        modifications.setIsotopeLabels(labels);
        return modifications;
    }

    private List<PeptideSettings.RunStructuralModification> readStaticModifications(XMLStreamReader reader) throws XMLStreamException
    {
        List<PeptideSettings.RunStructuralModification> modList = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, STATIC_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATION))
            {
                modList.add(readStaticModification(reader));
            }
        }
        return modList;
    }
    
    private PeptideSettings.RunStructuralModification readStaticModification(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.RunStructuralModification mod = new PeptideSettings.RunStructuralModification();
        mod.setName(XmlUtil.readRequiredAttribute(reader, NAME, STATIC_MODIFICATION));
        mod.setAminoAcid(reader.getAttributeValue(null, AMINOACID));
        mod.setTerminus(reader.getAttributeValue(null, TERMINUS));
        mod.setVariable(XmlUtil.readBooleanAttribute(reader, "variable", false));
        mod.setFormula(reader.getAttributeValue(null, FORMULA));
        mod.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, MASSDIFF_MONOISOTOPIC));
        mod.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, MASSDIFF_AVERAGE));
        mod.setExplicitMod(XmlUtil.readBooleanAttribute(reader, EXPLICIT_DECL));
        mod.setUnimodId(XmlUtil.readIntegerAttribute(reader, UNIMOD_ID));

        List<PeptideSettings.PotentialLoss> potentialLosses = new ArrayList<>();
        mod.setPotentialLosses(potentialLosses);

        while (reader.hasNext())
        {
            int evtType = reader.next();

            if (XmlUtil.isEndElement(reader, evtType, STATIC_MODIFICATION))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, POTENTIAL_LOSS))
            {
                PeptideSettings.PotentialLoss potentialLoss = new PeptideSettings.PotentialLoss();
                potentialLoss.setFormula(reader.getAttributeValue(null, "formula"));
                potentialLoss.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, "massdiff_average"));
                potentialLoss.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, "massdiff_monoisotopic"));
                potentialLoss.setInclusion(XmlUtil.readAttribute(reader, "inclusion"));
                potentialLosses.add(potentialLoss);
            }
        }
        
        return mod;
    }

    private List<PeptideSettings.RunIsotopeModification> readIsotopeModifications(XMLStreamReader reader, List<String> isotopeLabelNames) throws XMLStreamException
    {
        String isotopeLabel = reader.getAttributeValue(null, "isotope_label");
        if(isotopeLabel == null)
        {
            isotopeLabel = PeptideSettings.HEAVY_LABEL;
        }

        List<PeptideSettings.RunIsotopeModification> modList = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, HEAVY_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATION))
            {
                // These are the modifications associated with one isotope label
                // Example:
                // <heavy_modifications isotope_label="all 15N">
                // <static_modification name="all 15N" label_15N="true" />
                // </heavy_modifications>
                PeptideSettings.RunIsotopeModification mod = readIsotopeModification(reader);
                mod.setIsotopeLabel(isotopeLabel);
                modList.add(mod);
            }
        }

        isotopeLabelNames.add(isotopeLabel);
        return modList;
    }

     private PeptideSettings.RunIsotopeModification readIsotopeModification(XMLStreamReader reader)
     {
        PeptideSettings.RunIsotopeModification mod = new PeptideSettings.RunIsotopeModification();
        mod.setName(XmlUtil.readRequiredAttribute(reader, NAME, STATIC_MODIFICATION));
        mod.setAminoAcid(reader.getAttributeValue(null, AMINOACID));
        mod.setTerminus(reader.getAttributeValue(null, TERMINUS));
        mod.setFormula(reader.getAttributeValue(null, FORMULA));
        mod.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, MASSDIFF_MONOISOTOPIC));
        mod.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, MASSDIFF_AVERAGE));
        mod.setExplicitMod(XmlUtil.readBooleanAttribute(reader, EXPLICIT_DECL));
        mod.setUnimodId(XmlUtil.readIntegerAttribute(reader, UNIMOD_ID));
        mod.setLabel13C(XmlUtil.readBooleanAttribute(reader, "label_13C"));
        mod.setLabel15N(XmlUtil.readBooleanAttribute(reader, "label_15N"));
        mod.setLabel18O(XmlUtil.readBooleanAttribute(reader, "label_18O"));
        mod.setLabel2H(XmlUtil.readBooleanAttribute(reader, "label_2H"));
        mod.setRelativeRt(reader.getAttributeValue(null, "relative_rt"));

        return mod;
    }

    private PeptideSettings.SpectrumLibrarySettings readLibrarySettings(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.SpectrumLibrarySettings settings = new PeptideSettings.SpectrumLibrarySettings();
        settings.setPick(XmlUtil.readRequiredAttribute(reader, PICK, PEPTIDE_LIBRARIES));
        settings.setRankType(XmlUtil.readAttribute(reader, RANK_TYPE, null));
        settings.setPeptideCount(XmlUtil.readIntegerAttribute(reader, PEPTIDE_COUNT));

        List<PeptideSettings.SpectrumLibrary> libraryList = new ArrayList<>();
        settings.setLibraries(libraryList);

        boolean documentLibrary = XmlUtil.readBooleanAttribute(reader, DOCUMENT_LIBRARY, false);
        if(documentLibrary && _documentName != null)
        {
            // If there is a "document library" we will not have a separate library element
            // with the library name.  Document libraries have the same name as the .sky file.
            libraryList.add(getDocumentLibrary());
        }

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_LIBRARIES))
            {
                break;
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, BIBLIOSPEC_LIB) ||
                   XmlUtil.isElement(reader, BIBLIOSPEC_LITE_LIB) ||
                   XmlUtil.isElement(reader, HUNTER_LIB) ||
                   XmlUtil.isElement(reader, NIST_LIB) ||
                   XmlUtil.isElement(reader, SPECTRAST_LIB))
                {
                    libraryList.add(readLibrary(reader, reader.getLocalName()));
                }
            }
        }
        return  settings;
    }

    private PeptideSettings.SpectrumLibrary getDocumentLibrary()
    {
        PeptideSettings.SpectrumLibrary library = new PeptideSettings.SpectrumLibrary();
        library.setName(_documentName);
        library.setLibraryType(BIBLIOSPEC_LITE_LIB.substring(0, BIBLIOSPEC_LITE_LIB.indexOf("_library")));
        // "document" libraries are built using "import peptide search results" in Skyline.  Skyline xml does not
        // include a "file_name_hint" attribute for these libraries.  They have the same name as the Skyline document.
        library.setFileNameHint(_documentName + SkylineFileUtils.EXT_BLIB_W_DOT);
        return library;
    }

    private PeptideSettings.SpectrumLibrary readLibrary(XMLStreamReader reader, String elementName)
    {
        PeptideSettings.SpectrumLibrary library = new PeptideSettings.SpectrumLibrary();
        library.setName(XmlUtil.readRequiredAttribute(reader, NAME, elementName));
        library.setFileNameHint(XmlUtil.readAttribute(reader, FILE_NAME_HINT, null));
        library.setRevision(XmlUtil.readAttribute(reader, REVISION, null));
        library.setLibraryType(elementName.substring(0, elementName.indexOf("_library")));

        String skylineLibraryId;
        if(BIBLIOSPEC_LITE_LIB.equalsIgnoreCase(elementName))
        {
            skylineLibraryId = XmlUtil.readRequiredAttribute(reader, LSID, BIBLIOSPEC_LITE_LIB);
        }
        else
        {
            skylineLibraryId = XmlUtil.readAttribute(reader, ID, null);
        }
        // SpectrumLibrary.SkylineLibraryId is limited to 200 characters. Truncate longer ids for now since we don't use them anywhere.
        // TODO: Increase limit on this column.
        if(skylineLibraryId != null)
        {
            library.setSkylineLibraryId(skylineLibraryId.length() > 200 ? skylineLibraryId.substring(0, 200) : skylineLibraryId);
        }
        return library;
    }

    private PeptideSettings.PeptidePredictionSettings getPeptidePredictionSettings(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.PeptidePredictionSettings settings = new PeptideSettings.PeptidePredictionSettings();
        Boolean useMeasuredRt = XmlUtil.readBooleanAttribute(reader, USE_MEASURED_RTS);
        Double measuredRtWindow = XmlUtil.readDoubleAttribute(reader, MEASURED_RT_WINDOW);
        Boolean useSpectralLibraryDriftTimes = XmlUtil.readBooleanAttribute(reader, USE_SPECTRAL_LIBRARY_DRIFT_TIMES);
        Double resolvingPower = XmlUtil.readDoubleAttribute(reader, SPECTRAL_LIBRARY_DRIFT_TIMES_RESOLVING_POWER);


        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_PREDICTION))
            {
                break;
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, PREDICT_RETENTION_TIME))
                {
                    PeptideSettings.RetentionTimePredictionSettings rtPredictionSettings = new PeptideSettings.RetentionTimePredictionSettings();
                    rtPredictionSettings.setUseMeasuredRts(useMeasuredRt);
                    rtPredictionSettings.setMeasuredRtWindow(measuredRtWindow);
                    settings.setRtPredictionSettings(rtPredictionSettings);
                    readRetentionTimePredictorSettings(reader, rtPredictionSettings);
                }
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, PREDICT_DRIFT_TIME))
                {
                    PeptideSettings.DriftTimePredictionSettings dtPredictionSettings = new PeptideSettings.DriftTimePredictionSettings();
                    dtPredictionSettings.setUseSpectralLibraryDriftTimes(useSpectralLibraryDriftTimes);
                    dtPredictionSettings.setSpectralLibraryDriftTimesResolvingPower(resolvingPower);
                    settings.setDtPredictionSettings(dtPredictionSettings);
                    readDriftTimePredictorSettings(reader, dtPredictionSettings);
                }
            }
        }

        return settings;
    }

    private void readDriftTimePredictorSettings(XMLStreamReader reader, PeptideSettings.DriftTimePredictionSettings settings) throws XMLStreamException
    {
        settings.setPredictorName(XmlUtil.readAttribute(reader, NAME));
        settings.setResolvingPower(XmlUtil.readDoubleAttribute(reader, RESOLVING_POWER));
        List<PeptideSettings.MeasuredDriftTime> measuredDriftTimes = new ArrayList<>();
        settings.setMeasuredDriftTimes(measuredDriftTimes);

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PREDICT_DRIFT_TIME))
            {
                break;
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, MEASURED_DT))
                {
                    PeptideSettings.MeasuredDriftTime measuredDt = new PeptideSettings.MeasuredDriftTime();
                    measuredDt.setModifiedSequence(XmlUtil.readRequiredAttribute(reader, "modified_sequence", MEASURED_DT));
                    measuredDt.setCharge(XmlUtil.readRequiredIntegerAttribute(reader, "charge", MEASURED_DT));
                    measuredDt.setDriftTime(XmlUtil.readRequiredDoubleAttribute(reader, "drift_time", MEASURED_DT));
                    measuredDt.setHighEnergyDriftTimeOffset(XmlUtil.readDoubleAttribute(reader, "high_energy_drift_time_offset"));
                    measuredDriftTimes.add(measuredDt);
                }
            }
        }
    }

    private void readRetentionTimePredictorSettings(XMLStreamReader reader, PeptideSettings.RetentionTimePredictionSettings settings) throws XMLStreamException
    {
        settings.setPredictorName(XmlUtil.readAttribute(reader, NAME));
        settings.setTimeWindow(XmlUtil.readDoubleAttribute(reader, TIME_WINDOW));
        settings.setCalculatorName(XmlUtil.readAttribute(reader, CALCULATOR));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PREDICT_RETENTION_TIME))
            {
                break;
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, IRT_CALCULATOR))
                {
                    settings.setIsIrt(Boolean.TRUE);
                    settings.setCalculatorName(XmlUtil.readAttribute(reader, NAME));
                    settings.setIrtDatabasePath(XmlUtil.readAttribute(reader, IRT_DATABASE_PATH));
                }
                else if(XmlUtil.isElement(reader, REGRESSION_RT))
                {
                    settings.setRegressionSlope(XmlUtil.readDoubleAttribute(reader, SLOPE));
                    settings.setRegressionIntercept(XmlUtil.readDoubleAttribute(reader, INTERCEPT));
                }
            }
        }
    }

    private QuantificationSettings readQuantificationSettings(XMLStreamReader reader)
    {
        QuantificationSettings quantificationSettings = new QuantificationSettings();
        quantificationSettings.setRegressionWeighting(XmlUtil.readAttribute(reader, WEIGHTING));
        quantificationSettings.setRegressionFit(XmlUtil.readAttribute(reader, FIT));
        quantificationSettings.setNormalizationMethod(XmlUtil.readAttribute(reader, NORMALIZATION));
        quantificationSettings.setMsLevel(XmlUtil.readIntegerAttribute(reader, MS_LEVEL));
        quantificationSettings.setUnits(XmlUtil.readAttribute(reader, UNITS));
        quantificationSettings.setMaxLOQBias(XmlUtil.readDoubleAttribute(reader, MAX_LOQ_BIAS));
        quantificationSettings.setMaxLOQCV(XmlUtil.readDoubleAttribute(reader, MAX_LOQ_CV));
        quantificationSettings.setLODCalculation(XmlUtil.readAttribute(reader, LOD_CALCULATION));
        return quantificationSettings;
    }
}
