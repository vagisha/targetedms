/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.util.NetworkDrive;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 10:58 AM
 */
public class SkylineDocumentParser
{
    public static final float DEFAULT_TOLERANCE = 0.055f;

    private static final String SETTINGS_SUMMARY = "settings_summary";
    private static final String TRANSITION_SETTINGS = "transition_settings";
    private static final String TRANSITION_PREDICTION = "transition_prediction";
    private static final String PREDICT_COLLISION_ENERGY = "predict_collision_energy";
    private static final String REGRESSION_CE = "regression_ce";
    private static final String PREDICT_DECLUSTERING_POTENTIAL = "predict_declustering_potential";
    private static final String TRANSITION_INSTRUMENT = "transition_instrument";
    private static final String TRANSITION_FULL_SCAN = "transition_full_scan";
    private static final String ISOTOPE_ENRICHMENTS = "isotope_enrichments";
    private static final String ATOM_PERCENT_ENRICHMENT = "atom_percent_enrichment";
    private static final String PEPTIDE_SETTINGS = "peptide_settings";
    private static final String DATA_SETTINGS = "data_settings";
    private static final String MEASURED_RESULTS = "measured_results";
    private static final String REPLICATE = "replicate";
    private static final String SAMPLE_FILE = "sample_file";
    private static final String INSTRUMENT_INFO_LIST = "instrument_info_list";
    private static final String INSTRUMENT_INFO = "instrument_info";
    private static final String PEPTIDE_LIST = "peptide_list";
    private static final String PROTEIN = "protein";
    private static final String PEPTIDE = "peptide";
    private static final String NOTE = "note";
    private static final String PRECURSOR = "precursor";
    private static final String TRANSITION = "transition";
    private static final String PRECURSOR_MZ = "precursor_mz";
    private static final String ANNOTATION = "annotation";
    private static final String PRODUCT_MZ = "product_mz";
    private static final String COLLISION_ENERGY = "collision_energy";
    private static final String DECLUSTERING_POTENTIAL = "declustering_potential";
    private static final String LOSSES = "losses";
    private static final String NEUTRAL_LOSS = "neutral_loss";
    private static final String TRANSITION_PEAK = "transition_peak";
    private static final String PRECURSOR_PEAK = "precursor_peak";
    private static final String PEPTIDE_RESULT = "peptide_result";
    private static final String EXPLICIT_MODIFICATION = "explicit_modification";
    private static final String EXPLICIT_STATIC_MODIFICATIONS = "explicit_static_modifications";
    private static final String EXPLICIT_HEAVY_MODIFICATIONS = "explicit_heavy_modifications";
    private static final String IMPLICIT_MODIFICATION = "implicit_modification";
    private static final String IMPLICIT_HEAVY_MODIFICATIONS = "implicit_heavy_modifications";
    private static final String IMPLICIT_STATIC_MODIFICATIONS = "implicit_static_modifications";
    private static final String VARIABLE_MODIFICATION = "variable_modification";
    private static final String SEQUENCE = "sequence";
    private static final String BIBLIOSPEC_SPECTRUM_INFO = "bibliospec_spectrum_info";
    private static final String LIBRARY_NAME = "library_name";
    private static final String COUNT_MEASURED = "count_measured";

    private static final double MIN_SUPPORTED_VERSION = 1.2;

    private int _peptideGroupCount;
    private int _peptideCount;
    private int _precursorCount;
    private int _transitionCount;

    private List<SkylineBinaryParser> _caches = new ArrayList<SkylineBinaryParser>();

    private TransitionSettings _transitionSettings;
    private PeptideSettings _peptideSettings;
    private DataSettings _dataSettings;
    private List<Replicate> _replicateList;
    private Map<String, String> _sampleFileIdToFilePathMap;

    private double _matchTolerance = DEFAULT_TOLERANCE;

    private final XMLStreamReader _reader;
    private InputStream _inputStream;
    private final File _file;
    private Logger _log;

    public SkylineDocumentParser(File file, Logger log) throws XMLStreamException, IOException
    {
        _file = file;
        _inputStream = new FileInputStream(_file);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        _reader = inputFactory.createXMLStreamReader(_inputStream);
        _log = log;
        readDocumentVersion(_reader);
    }

    public void close()
    {
        if (_reader != null) try
        {
            _reader.close();
        }
        catch (XMLStreamException e)
        {
            _log.error(e);
        }
        if(_inputStream != null) try
        {
            _inputStream.close();
        }
        catch(IOException e)
        {
            _log.error(e);
        }
    }

    public void readSettings() throws XMLStreamException, IOException
    {
        _replicateList = new ArrayList<Replicate>();
        _sampleFileIdToFilePathMap = new HashMap<String, String>();

        readDocumentSettings(_reader);

        parseChromatograms();
    }

    private void parseChromatograms() throws IOException
    {
        // Just add a "d" based on the expected file extension
        File file = new File(_file.getPath() + "d");
        if (NetworkDrive.exists(file))
        {
            SkylineBinaryParser parser = new SkylineBinaryParser(file);
            parser.parse();

            _caches.add(parser);
        }
    }

    public List<Replicate> getReplicates()
    {
        return _replicateList;
    }

    public TransitionSettings getTransitionSettings()
    {
        return _transitionSettings;
    }

    public PeptideSettings getPeptideSettings()
    {
        return _peptideSettings;
    }

    private void readDocumentVersion(XMLStreamReader reader) throws XMLStreamException
    {
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isStartElement(reader, evtType, "srm_settings")) {

                Double version = XmlUtil.readRequiredDoubleAttribute(reader, "format_version", "srm_settings");
                if(version < MIN_SUPPORTED_VERSION)
                {
                    throw new IllegalStateException("The version of this Skyline document is "+version+
                                                    ". Version less than " + MIN_SUPPORTED_VERSION +
                                                    " is not supported.");
                }
                return;
            }
        }

        throw new IllegalStateException("Not a valid Skyline document. <srm_settings> element was not found.");
    }

    private void readDocumentSettings(XMLStreamReader reader) throws XMLStreamException
    {
        _dataSettings = new DataSettings();
         while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, SETTINGS_SUMMARY))
             {
                 break;
             }

             if(evtType == XMLStreamReader.START_ELEMENT)
             {
                 if(PEPTIDE_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     PeptideSettingsParser pepSettingsParser = new PeptideSettingsParser();
                     _peptideSettings = pepSettingsParser.parse(reader);
                 }
                 else if(TRANSITION_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readTransitionSettings(reader);
                 }
                 else if(DATA_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readDataSettings(reader);
                 }
                 else if(MEASURED_RESULTS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readMeasuredResults(reader);
                 }
             }
         }

         // Update the boolean type annotations for replicates. We do this after reading both the <data_settings> and
         // <measured_results> elements since older files have <data_settings> after <measured_results>
         updateReplicateAnnotations();
    }

    private void updateReplicateAnnotations()
    {
        if(_replicateList == null || _replicateList.size() == 0)
            return;
        for(Replicate replicate: _replicateList) {

            List<ReplicateAnnotation> annotations = replicate.getAnnotations();

            for(ReplicateAnnotation annot: annotations)
            {
                if(_dataSettings.isBooleanAnnotation(annot.getName()))
                {
                    // If we are reading an older file, <measured_results> were read before <data_settings>
                    // so we did not have annotation definitions while reading the replicate annotations.
                    // The value of boolean annotations is the same as the name of the annotation in .sky files.
                    // We need to change it to "true".
                    annot.setValue(Boolean.TRUE.toString());
                }
            }

            // Boolean type annotations are not listed in the .sky file if their value was false.
            // We would still like to store them in the database.
            List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations, DataSettings.AnnotationTarget.replicate);
            List<ReplicateAnnotation> missingReplAnnotations = new ArrayList<ReplicateAnnotation>(missingBooleanAnnotations.size());
            for(String missingAnotName: missingBooleanAnnotations)
            {
                addMissingBooleanAnnotation(missingReplAnnotations, missingAnotName, new ReplicateAnnotation());
            }

            if(missingReplAnnotations.size() > 0)
            {
                List<ReplicateAnnotation> combinedAnnotations = new ArrayList<ReplicateAnnotation>(annotations);
                combinedAnnotations.addAll(missingReplAnnotations);
                replicate.setAnnotations(combinedAnnotations);
            }
        }
    }

    private void readDataSettings(XMLStreamReader reader) throws XMLStreamException
    {
        while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, DATA_SETTINGS))
             {
                 break;
             }

             if(XmlUtil.isStartElement(reader, evtType, ANNOTATION))
             {
                 String name = reader.getAttributeValue(null, "name");
                 String targets = reader.getAttributeValue(null, "targets");
                 String type = reader.getAttributeValue(null, "type");
                 _dataSettings.addAnnotation(name, targets, type);
             }
         }
    }

    private void readTransitionSettings(XMLStreamReader reader) throws XMLStreamException
    {
        _transitionSettings = new TransitionSettings();
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_SETTINGS))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, TRANSITION_PREDICTION))
            {
                _transitionSettings.setPredictionSettings(readTransitionPrediction(reader));
            }
            if (XmlUtil.isStartElement(reader, evtType, TRANSITION_FULL_SCAN))
            {
                _transitionSettings.setFullScanSettings(readFullScanSettings(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_INSTRUMENT))
            {
                TransitionSettings.InstrumentSettings instrumentSettings = new TransitionSettings.InstrumentSettings();
                _transitionSettings.setInstrumentSettings(instrumentSettings);
                instrumentSettings.setDynamicMin(Boolean.parseBoolean(reader.getAttributeValue(null, "dynamic_min")));
                instrumentSettings.setMinMz(XmlUtil.readRequiredIntegerAttribute(reader, "min_mz", TRANSITION_INSTRUMENT));
                instrumentSettings.setMaxMz(XmlUtil.readRequiredIntegerAttribute(reader, "max_mz", TRANSITION_INSTRUMENT));
                _matchTolerance = XmlUtil.readRequiredDoubleAttribute(reader, "mz_match_tolerance", TRANSITION_INSTRUMENT);
                instrumentSettings.setMzMatchTolerance(_matchTolerance);
                instrumentSettings.setMinTime(XmlUtil.readIntegerAttribute(reader, "min_time"));
                instrumentSettings.setMaxTime(XmlUtil.readIntegerAttribute(reader, "max_time"));
                instrumentSettings.setMaxTransitions(XmlUtil.readIntegerAttribute(reader, "max_transitions"));
            }
        }
    }

    private TransitionSettings.FullScanSettings readFullScanSettings(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionSettings.FullScanSettings result = new TransitionSettings.FullScanSettings();
        result.setPrecursorIsotopes(reader.getAttributeValue(null, "precursor_isotopes"));
        result.setPrecursorIsotopeFilter(XmlUtil.readDoubleAttribute(reader, "precursor_isotope_filter"));
        result.setPrecursorRes(XmlUtil.readDoubleAttribute(reader, "precursor_res"));
        result.setPrecursorMassAnalyzer(reader.getAttributeValue(null, "precursor_mass_analyzer"));

        result.setPrecursorFilter(XmlUtil.readDoubleAttribute(reader, "precursor_filter")); // Guessed at attribute value name
        result.setPrecursorLeftFilter(XmlUtil.readDoubleAttribute(reader, "precursor_left_filter")); // Guessed at attribute value name
        result.setPrecursorRightFilter(XmlUtil.readDoubleAttribute(reader, "precursor_right_filter")); // Guessed at attribute value name
        result.setPrecursorResMz(XmlUtil.readDoubleAttribute(reader, "precursor_res_mz")); // Guessed at attribute value name
        result.setProductMassAnalyzer(reader.getAttributeValue(null, "product_mass_analyzer")); // Guessed at attribute value name

        List<TransitionSettings.IsotopeEnrichment> enrichments = new ArrayList<TransitionSettings.IsotopeEnrichment>();
        result.setIsotopeEnrichmentList(enrichments);

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_FULL_SCAN))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, ISOTOPE_ENRICHMENTS))
            {
                enrichments.addAll(readIsotopeEnrichments(reader));
            }
        }

        return result;
    }

    private List<TransitionSettings.IsotopeEnrichment> readIsotopeEnrichments(XMLStreamReader reader) throws XMLStreamException
    {
        List<TransitionSettings.IsotopeEnrichment> result = new ArrayList<TransitionSettings.IsotopeEnrichment>();
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, ISOTOPE_ENRICHMENTS))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, ATOM_PERCENT_ENRICHMENT))
            {
                result.add(readAtomPercentEnrichment(reader));
            }
        }
        return result;
    }

    private TransitionSettings.IsotopeEnrichment readAtomPercentEnrichment(XMLStreamReader reader) throws XMLStreamException
    {
        StringBuilder text = new StringBuilder();
        TransitionSettings.IsotopeEnrichment result = new TransitionSettings.IsotopeEnrichment();
        result.setSymbol(reader.getAttributeValue(null, "symbol"));
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, ATOM_PERCENT_ENRICHMENT))
            {
                break;
            }

            if (XmlUtil.isText(evtType))
            {
                text.append(reader.getText());
            }
        }

        if (text.length() == 0)
        {
            throw new XMLStreamException("No text content for <" + ATOM_PERCENT_ENRICHMENT + "> element, should contain a percent value");
        }
        result.setPercentEnrichment(Double.parseDouble(text.toString()));
        return result;
    }

    private TransitionSettings.PredictionSettings readTransitionPrediction(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionSettings.PredictionSettings settings = new TransitionSettings.PredictionSettings();
        settings.setPrecursorMassType(reader.getAttributeValue(null, "precursor_mass_type"));
        settings.setProductMassType(reader.getAttributeValue(null, "fragment_mass_type"));
        settings.setOptimizeBy(reader.getAttributeValue(null, "optimize_by"));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_PREDICTION))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, PREDICT_COLLISION_ENERGY))
            {
                settings.setCePredictor(readPredictor(reader, PREDICT_COLLISION_ENERGY));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PREDICT_DECLUSTERING_POTENTIAL))
            {
                settings.setDpPredictor(readPredictor(reader, PREDICT_DECLUSTERING_POTENTIAL));
            }
        }
        return settings;
    }

    private TransitionSettings.Predictor readPredictor(XMLStreamReader reader, String endElementName) throws XMLStreamException
    {
        TransitionSettings.Predictor predictor = new TransitionSettings.Predictor();
        predictor.setName(reader.getAttributeValue(null, "name"));
        String stepSize = reader.getAttributeValue(null, "step_size");
        if (stepSize != null)
        {
            predictor.setStepSize(Integer.parseInt(stepSize));
        }
        String stepCount = reader.getAttributeValue(null, "step_count");
        if (stepCount != null)
        {
            predictor.setStepCount(Integer.parseInt(stepCount));
        }
        List<TransitionSettings.PredictorSettings> allSettings = new ArrayList<TransitionSettings.PredictorSettings>();
        predictor.setSettings(allSettings);

        String slope = reader.getAttributeValue(null, "slope");
        String intercept = reader.getAttributeValue(null, "intercept");

        if (slope != null && intercept != null)
        {
            TransitionSettings.PredictorSettings predictorSettings = new TransitionSettings.PredictorSettings();
            predictorSettings.setSlope(Double.parseDouble(slope));
            predictorSettings.setIntercept(Double.parseDouble(intercept));
            allSettings.add(predictorSettings);
        }

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, endElementName))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, REGRESSION_CE))
            {
                TransitionSettings.PredictorSettings settings = new TransitionSettings.PredictorSettings();
                settings.setCharge(Integer.parseInt(reader.getAttributeValue(null, "charge")));
                slope = reader.getAttributeValue(null, "slope");
                if (slope != null)
                {
                    settings.setSlope(Double.parseDouble(slope));
                }
                intercept = reader.getAttributeValue(null, "intercept");
                if (intercept != null)
                {
                    settings.setIntercept(Double.parseDouble(intercept));
                }
                allSettings.add(settings);
            }
        }

        return predictor;
    }

    private void readMeasuredResults(XMLStreamReader reader) throws XMLStreamException
    {
         this._replicateList = new ArrayList<Replicate>();

         while(reader.hasNext()) {

             int evtType = _reader.next();
             if(XmlUtil.isEndElement(reader, evtType, MEASURED_RESULTS))
             {
                 break;
             }
             if(XmlUtil.isStartElement(reader, evtType, REPLICATE))
             {
                 _replicateList.add(readReplicate(reader));
             }

         }
    }

    private Replicate readReplicate(XMLStreamReader reader) throws XMLStreamException
    {
        Replicate replicate = new Replicate();
        replicate.setName(XmlUtil.readRequiredAttribute(reader, "name", REPLICATE));

        List<SampleFile> sampleFileList = new ArrayList<SampleFile>();
        replicate.setSampleFileList(sampleFileList);

        List<ReplicateAnnotation> annotations = new ArrayList<ReplicateAnnotation>();
        replicate.setAnnotations(annotations);

        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, REPLICATE))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, SAMPLE_FILE))
            {
                SampleFile sampleFile = readSampleFile(reader);
                if (sampleFile.getSkylineId() == null)
                {
                    sampleFile.setSkylineId(getDefaultSkylineSampleFileId(replicate.getName()));
                }
                _sampleFileIdToFilePathMap.put(sampleFile.getSkylineId(), sampleFile.getFilePath());
                sampleFileList.add(sampleFile);
            }
            else if(XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new ReplicateAnnotation()));
            }
        }

        return replicate;
    }

    private SampleFile readSampleFile(XMLStreamReader reader) throws XMLStreamException
    {
        SampleFile sampleFile = new SampleFile();
        sampleFile.setSkylineId(reader.getAttributeValue(null, "id"));
        sampleFile.setFilePath(XmlUtil.readRequiredAttribute(reader, "file_path", SAMPLE_FILE));
        sampleFile.setSampleName(XmlUtil.readAttribute(reader, "sample_name", SAMPLE_FILE));

        sampleFile.setAcquiredTime(XmlUtil.readDateAttribute(reader, "acquired_time"));
        sampleFile.setAcquiredTime(XmlUtil.readDateAttribute(reader, "modified_time"));

        List<Instrument> instrumentList = new ArrayList<Instrument>();
        sampleFile.setInstrumentList(instrumentList);

        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, SAMPLE_FILE))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, INSTRUMENT_INFO_LIST))
            {
                instrumentList.addAll(readInstrumentInfoList(reader));
            }
        }

        return sampleFile;
    }

    private List<Instrument> readInstrumentInfoList(XMLStreamReader reader) throws XMLStreamException
    {
        List<Instrument> result = new ArrayList<Instrument>();
        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, INSTRUMENT_INFO_LIST))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, INSTRUMENT_INFO))
            {
                result.add(readInstrumentInfo(reader));
            }
        }
        return result;
    }

    private Instrument readInstrumentInfo(XMLStreamReader reader) throws XMLStreamException
    {
        Instrument instrument = new Instrument();
        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, INSTRUMENT_INFO))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, "ionsource"))
            {
                instrument.setIonizationType(readTextElementValue(reader, "ionsource"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "analyzer"))
            {
                instrument.setAnalyzer(readTextElementValue(reader, "analyzer"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "detector"))
            {
                instrument.setDetector(readTextElementValue(reader, "detector"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "model"))
            {
                instrument.setModel(readTextElementValue(reader, "model"));
            }
        }
        return instrument;
    }

    private String readTextElementValue(XMLStreamReader reader, String elementName) throws XMLStreamException
    {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext())
        {
            int evtType = _reader.next();

            if (XmlUtil.isEndElement(reader, evtType, elementName))
            {
                break;
            }

            if (XmlUtil.isText(evtType))
            {
                result.append(reader.getText());
            }
        }
        return result.length() == 0 ? null : result.toString();
    }

    public boolean hasNextPeptideGroup() throws XMLStreamException
    {
       while (_reader.hasNext())
        {

            int evtType = _reader.next();
            if (evtType == XMLStreamReader.START_ELEMENT)
            {
                if (PEPTIDE_LIST.equalsIgnoreCase(_reader.getLocalName()) || PROTEIN.equalsIgnoreCase(_reader.getLocalName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public PeptideGroup nextPeptideGroup() throws XMLStreamException, DataFormatException, IOException
    {
        return readPeptideGroup(_reader);
    }

    private PeptideGroup readPeptideGroup(XMLStreamReader reader) throws XMLStreamException, IOException
    {
        PeptideGroup pepGroup = new PeptideGroup();
        List<Peptide> peptideList = new ArrayList<Peptide>();
        pepGroup.setPeptideList(peptideList);
        List<PeptideGroupAnnotation> annotations = new ArrayList<PeptideGroupAnnotation>();
        pepGroup.setAnnotations(annotations);

        boolean isProtein = PROTEIN.equalsIgnoreCase(_reader.getLocalName());

        if(isProtein)
        {
            // <protein> elements have the 'name'  and 'description' attribute
            pepGroup.setLabel(reader.getAttributeValue(null, "name"));
            pepGroup.setDescription(reader.getAttributeValue(null, "description"));
            pepGroup.setDecoy(Boolean.parseBoolean(reader.getAttributeValue(null, "decoy")));
            pepGroup.setProtein(true);
        }
        else
        {
            // <peptide_list> elements have the 'label_name' attribute
            pepGroup.setLabel(reader.getAttributeValue(null, "label_name"));
            pepGroup.setDescription(reader.getAttributeValue(null, "label_description"));
        }

        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_LIST) || XmlUtil.isEndElement(reader, evtType, PROTEIN))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, SEQUENCE))
            {
                pepGroup.setSequence(reader.getElementText().replaceAll("\\s+", ""));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PeptideGroupAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PEPTIDE))
            {
                peptideList.add(readPeptide(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                pepGroup.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.protein);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PeptideGroupAnnotation());
        }

        _peptideGroupCount++;
        return pepGroup;
    }

    private Peptide readPeptide(XMLStreamReader reader) throws XMLStreamException, IOException
    {
        Peptide peptide = new Peptide();
        List<Precursor> precursorList = new ArrayList<Precursor>();
        peptide.setPrecursorList(precursorList);
        List<PeptideAnnotation> annotations = new ArrayList<PeptideAnnotation>();
        peptide.setAnnotations(annotations);

        List<PeptideChromInfo> peptideChromInfoList = new ArrayList<PeptideChromInfo>();
        peptide.setPeptideChromInfoList(peptideChromInfoList);

        String start = reader.getAttributeValue(null, "start");
        if(null != start)
            peptide.setStart(Integer.parseInt(start));

        String end =  reader.getAttributeValue(null, "end");
        if(null != end)
            peptide.setEnd(Integer.parseInt(end));

        peptide.setSequence(reader.getAttributeValue(null, "sequence"));

        String prevAa = reader.getAttributeValue(null, "prev_aa");
        if(null != prevAa)
            peptide.setPrevAa(prevAa);

        String nextAa = reader.getAttributeValue(null, "next_aa");
        if(null != nextAa)
            peptide.setNextAa(nextAa);

        String decoy =  reader.getAttributeValue(null, "decoy");
        if(null != decoy)
            peptide.setDecoy(Boolean.parseBoolean(decoy));

        String calcNeutralPepMass = reader.getAttributeValue(null, "calc_neutral_pep_mass");
        if(null != calcNeutralPepMass)
            peptide.setCalcNeutralMass(Double.parseDouble(calcNeutralPepMass));

        String numMissedCleavages = reader.getAttributeValue(null, "num_missed_cleavages");
        if(null != numMissedCleavages)
            peptide.setNumMissedCleavages(Integer.parseInt(numMissedCleavages));

        String rank = reader.getAttributeValue(null, "rank");
        if(null != rank)
            peptide.setRank(Integer.parseInt(rank));

        String predictedRt = reader.getAttributeValue(null, "predicted_retention_time");
        if(null != predictedRt)
            peptide.setPredictedRetentionTime(Double.parseDouble(predictedRt));

        String avgMeasuredRt = reader.getAttributeValue(null, "avg_measured_retention_time");
        if(null != avgMeasuredRt)
            peptide.setAvgMeasuredRetentionTime(Double.parseDouble(avgMeasuredRt));

        List<Peptide.StructuralModification> structuralMods = new ArrayList<Peptide.StructuralModification>();
        List<Peptide.IsotopeModification> isotopeMods = new ArrayList<Peptide.IsotopeModification>();
        peptide.setStructuralMods(structuralMods);
        peptide.setIsotopeMods(isotopeMods);
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRECURSOR))
            {
                precursorList.add(readPrecursor(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                peptide.setNote(readNote(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PEPTIDE_RESULT))
            {
                peptideChromInfoList.add(readPeptideChromInfo(reader));
            }
            else if(XmlUtil.isStartElement(reader, evtType, VARIABLE_MODIFICATION))
            {
                structuralMods.add(readStructuralModification(reader));
            }
            else if(XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PeptideAnnotation()));
            }
            else if(XmlUtil.isStartElement(reader, evtType, EXPLICIT_STATIC_MODIFICATIONS))
            {
                structuralMods.addAll(readStructuralModifications(reader, EXPLICIT_STATIC_MODIFICATIONS, EXPLICIT_MODIFICATION));
            }
            else if(XmlUtil.isStartElement(reader, evtType, IMPLICIT_STATIC_MODIFICATIONS))
            {
                structuralMods.addAll(readStructuralModifications(reader, IMPLICIT_STATIC_MODIFICATIONS, IMPLICIT_MODIFICATION));
            }
            else if(XmlUtil.isStartElement(reader, evtType, EXPLICIT_HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, EXPLICIT_HEAVY_MODIFICATIONS, EXPLICIT_MODIFICATION));
            }
            else if(XmlUtil.isStartElement(reader, evtType, IMPLICIT_HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, IMPLICIT_HEAVY_MODIFICATIONS, IMPLICIT_MODIFICATION));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.peptide);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PeptideAnnotation());
        }

        _peptideCount++;
        return peptide;
    }

    private String readNote(XMLStreamReader reader) throws XMLStreamException
    {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext())
        {
            reader.next();
            int evtType = reader.getEventType();
            if (XmlUtil.isEndElement(reader, evtType, NOTE))
            {
                break;
            }
            if (XmlUtil.isText(evtType))
            {
                result.append(reader.getText());
            }
        }
        return result.length() == 0 ? null : result.toString();
    }

    private Peptide.IsotopeModification readIsotopeModification(XMLStreamReader reader, String isotopeLabel) throws XMLStreamException
    {
        Peptide.IsotopeModification mod = new Peptide.IsotopeModification();
        mod.setModificationName(XmlUtil.readRequiredAttribute(reader, "modification_name", VARIABLE_MODIFICATION));
        Double massDiff = XmlUtil.readDoubleAttribute(reader, "mass_diff");
        if (massDiff != null)
        {
            mod.setMassDiff(massDiff);
        }
        mod.setIndexAa(XmlUtil.readRequiredIntegerAttribute(reader, "index_aa", VARIABLE_MODIFICATION));

        mod.setIsotopeLabel(isotopeLabel);
        return mod;
    }

    private Peptide.StructuralModification readStructuralModification(XMLStreamReader reader) throws XMLStreamException
    {
        Peptide.StructuralModification mod = new Peptide.StructuralModification();
        mod.setModificationName(XmlUtil.readRequiredAttribute(reader, "modification_name", VARIABLE_MODIFICATION));
        mod.setMassDiff(XmlUtil.readDoubleAttribute(reader, "mass_diff"));
        mod.setIndexAa(XmlUtil.readRequiredIntegerAttribute(reader, "index_aa", VARIABLE_MODIFICATION));
        return mod;
    }

    private List<Peptide.StructuralModification> readStructuralModifications(XMLStreamReader reader, String parentElement, String childElement) throws XMLStreamException
    {
        List<Peptide.StructuralModification> modifications = new ArrayList<Peptide.StructuralModification>();
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, parentElement))
            {
                break;
            }

            else if (XmlUtil.isStartElement(reader, evtType, childElement))
            {
                modifications.add(readStructuralModification(reader));
            }
        }
        return modifications;
    }

    private List<Peptide.IsotopeModification> readIsotopeModifications(XMLStreamReader reader, String parentElement, String childElement) throws XMLStreamException
    {
        String isotopeLabel = reader.getAttributeValue(null, "isotope_label");
        if(isotopeLabel == null)
        {
            isotopeLabel = PeptideSettings.HEAVY_LABEL;
        }

        List<Peptide.IsotopeModification> modifications = new ArrayList<Peptide.IsotopeModification>();
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, parentElement))
            {
                break;
            }

            else if (XmlUtil.isStartElement(reader, evtType, childElement))
            {
                modifications.add(readIsotopeModification(reader, isotopeLabel));
            }
        }
        return modifications;
    }

    private PeptideChromInfo readPeptideChromInfo(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideChromInfo chromInfo = new PeptideChromInfo();
        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", PEPTIDE_RESULT));
        chromInfo.setSkylineSampleFileId(XmlUtil.readAttribute(reader, "file", getDefaultSkylineSampleFileId(chromInfo.getReplicateName())));
        chromInfo.setRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setPeakCountRatio(XmlUtil.readDoubleAttribute(reader, "peak_count_ratio"));
        // TODO: read predicted retention time and ratio to standard
        return chromInfo;
        // TODO: read note and annotations
    }

    private String getDefaultSkylineSampleFileId(String replicateName)
    {
        return replicateName+"_f0";
    }

    private Precursor readPrecursor(XMLStreamReader reader) throws XMLStreamException, IOException
    {
        Precursor precursor = new Precursor();
        List<Transition> transitionList = new ArrayList<Transition>();
        precursor.setTransitionList(transitionList);
        List<PrecursorAnnotation> annotations = new ArrayList<PrecursorAnnotation>();
        precursor.setAnnotations(annotations);

        List<PrecursorChromInfo> chromInfoList = new ArrayList<PrecursorChromInfo>();
        precursor.setChromInfoList(chromInfoList);

        String charge = reader.getAttributeValue(null, "charge");
        if(null != charge)
            precursor.setCharge(Integer.parseInt(charge));

        String calcNeutralMass = reader.getAttributeValue(null, "calc_neutral_mass");
        if(null != calcNeutralMass)
            precursor.setNeutralMass(Double.parseDouble(calcNeutralMass));

        String precursorMz = reader.getAttributeValue(null, "precursor_mz");
        if(null != precursorMz)
            precursor.setMz(Double.parseDouble(precursorMz));

        precursor.setModifiedSequence(reader.getAttributeValue(null, "modified_sequence"));

        precursor.setIsotopeLabel(XmlUtil.readAttribute(reader, "isotope_label", "light"));

        String collisionEnergy = reader.getAttributeValue(null, COLLISION_ENERGY);
        if(null != collisionEnergy)
            precursor.setCollisionEnergy(Double.parseDouble(collisionEnergy));

        String declustPotential = reader.getAttributeValue(null, DECLUSTERING_POTENTIAL);
        if(null != declustPotential)
            precursor.setDeclusteringPotential(Double.parseDouble(declustPotential));

        while(reader.hasNext()) {

            int evtType = reader.next();
            if(evtType == XMLStreamReader.END_ELEMENT &&
               PRECURSOR.equalsIgnoreCase(reader.getLocalName()))
            {
                break;
            }
            // TODO: handle other library types
            else if(XmlUtil.isStartElement(reader, evtType, BIBLIOSPEC_SPECTRUM_INFO))
            {
                precursor.setLibraryInfo(readLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION))
            {
                transitionList.add(readTransition(reader));
            }
            else if(XmlUtil.isStartElement(reader, evtType, PRECURSOR_PEAK))
            {
               chromInfoList.add(readPrecursorChromInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PrecursorAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                precursor.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.precursor);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PrecursorAnnotation());
        }

        List<Chromatogram> chromatograms = tryLoadChromatogram(transitionList, precursor.getMz(), _matchTolerance);
        Map<String, Chromatogram> filePathChromatogramMap = new HashMap<String, Chromatogram>();
        for(Chromatogram chromatogram: chromatograms)
        {
            filePathChromatogramMap.put(chromatogram.getFilePath(), chromatogram);
        }

        for(Iterator<PrecursorChromInfo> i = chromInfoList.iterator(); i.hasNext(); )
        {
            PrecursorChromInfo chromInfo = i.next();
            String filePath = _sampleFileIdToFilePathMap.get(chromInfo.getSkylineSampleFileId());
            Chromatogram chromatogram = filePathChromatogramMap.get(filePath);
            if (chromatogram == null)
            {
                _log.warn("Unable to find chromatograms for file path " + filePath);
                i.remove();
            }
            else
            {
                chromInfo.setChromatogram(chromatogram.getChromatogram());
                chromInfo.setNumPoints(chromatogram.getNumPoints());
                chromInfo.setNumTransitions(chromatogram.getNumTransitions());
            }
        }

        for(Transition transition: precursor.getTransitionList())
        {
            for(Iterator<TransitionChromInfo> iter = transition.getChromInfoList().iterator(); iter.hasNext(); )
            {
                TransitionChromInfo transChromInfo = iter.next();
                String filePath = _sampleFileIdToFilePathMap.get(transChromInfo.getSkylineSampleFileId());
                Chromatogram c = filePathChromatogramMap.get(filePath);
                if (c == null)
                {
                    _log.warn("Unable to find chromatograms for file path " + filePath);
                    iter.remove();
                }
                else
                {
                    int matchIndex = -1;
                    // Figure out which index into the list of transitions we're inserting.
                    // If there are multiple matches within the given mz match tolerance return the closest match.
                    double deltaNearestMz = Double.MAX_VALUE;
                    for (int i = 0; i < c.getTransitions().length; i++)
                    {
                        double deltaMz = Math.abs(transition.getMz() - c.getTransitions()[i]);

                        if (deltaMz < _transitionSettings.getInstrumentSettings().getMzMatchTolerance() &&
                            deltaMz < deltaNearestMz)
                        {
                            matchIndex = i;
                            deltaNearestMz = deltaMz;
                        }
                    }
                    if (matchIndex == -1)
                    {
                        throw new IllegalStateException("No transition match!");
                    }

                    transChromInfo.setChromatogramIndex(matchIndex);
                }
            }
        }

        _precursorCount++;
        return precursor;
    }

    private Precursor.LibraryInfo readLibraryInfo(XMLStreamReader reader) throws XMLStreamException
    {
        // <bibliospec_spectrum_info library_name="Yeast_mini" count_measured="895" />
        Precursor.LibraryInfo libInfo = new Precursor.LibraryInfo();
        libInfo.setLibraryName(XmlUtil.readRequiredAttribute(reader, LIBRARY_NAME, BIBLIOSPEC_SPECTRUM_INFO));
        libInfo.setScore1(XmlUtil.readRequiredDoubleAttribute(reader, COUNT_MEASURED, BIBLIOSPEC_SPECTRUM_INFO));
        return libInfo;
    }

    private PrecursorChromInfo readPrecursorChromInfo(XMLStreamReader reader) throws XMLStreamException
    {
        PrecursorChromInfo chromInfo = new PrecursorChromInfo();
        List<PrecursorChromInfoAnnotation> annotations = new ArrayList<PrecursorChromInfoAnnotation>();
        chromInfo.setAnnotations(annotations);

        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", PRECURSOR_PEAK));
        chromInfo.setSkylineSampleFileId(XmlUtil.readAttribute(reader, "file", getDefaultSkylineSampleFileId(chromInfo.getReplicateName())));
        chromInfo.setOptimizationStep(XmlUtil.readIntegerAttribute(reader, "step"));
        chromInfo.setBestRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setMinStartTime(XmlUtil.readDoubleAttribute(reader, "start_time"));
        chromInfo.setMaxEndTime(XmlUtil.readDoubleAttribute(reader, "end_time"));
        chromInfo.setTotalArea(XmlUtil.readDoubleAttribute(reader, "area"));
        chromInfo.setTotalBackground(XmlUtil.readDoubleAttribute(reader, "background"));
        Double fwhm =  XmlUtil.readDoubleAttribute(reader, "fwhm");
        // TODO: Found NaN value for fwhm in Study7.sky.  Should this happen?
        fwhm = (fwhm != null && fwhm.isNaN()) ? null : fwhm;
        chromInfo.setMaxFwhm(fwhm);
        chromInfo.setNumTruncated(XmlUtil.readIntegerAttribute(reader, "truncated"));
        chromInfo.setIdentified(XmlUtil.readBooleanAttribute(reader, "identified"));
        chromInfo.setLibraryDtop(XmlUtil.readDoubleAttribute(reader, "library_dtop"));
        chromInfo.setIsotopeDtop(XmlUtil.readDoubleAttribute(reader, "isotope_dtop"));
        chromInfo.setUserSet(XmlUtil.readBooleanAttribute(reader, "user_set"));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PRECURSOR_PEAK))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PrecursorChromInfoAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                chromInfo.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.precursor_result);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PrecursorChromInfoAnnotation());
        }

        return chromInfo;
    }

    private Transition readTransition(XMLStreamReader reader) throws XMLStreamException
    {
        Transition transition = new Transition();
        List<TransitionAnnotation> annotations = new ArrayList<TransitionAnnotation>();
        transition.setAnnotations(annotations);

        List<TransitionLoss> neutralLosses = new ArrayList<TransitionLoss>();
        transition.setNeutralLosses(neutralLosses);

        String fragment = reader.getAttributeValue(null, "fragment_type");
        transition.setFragmentType(fragment);

        String fragmentOrdinal = reader.getAttributeValue(null, "fragment_ordinal");
        if(fragmentOrdinal != null)
           transition.setFragmentOrdinal(Integer.parseInt(fragmentOrdinal));

        String charge =  reader.getAttributeValue(null, "product_charge");
        if(charge != null)
           transition.setCharge(Integer.parseInt(charge));

        String calcNeutralMass = reader.getAttributeValue(null, "calc_neutral_mass");
        if(calcNeutralMass != null)
            transition.setCalcNeutralMass(Double.parseDouble(calcNeutralMass));

        String neutralMassLoss = reader.getAttributeValue(null, "loss_neutral_mass");
        if(neutralMassLoss != null)
            transition.setNeutralLossMass(Double.parseDouble(neutralMassLoss));

        String cleavageAa = reader.getAttributeValue(null, "cleavage_aa");
        if(cleavageAa != null)
            transition.setCleavageAa(cleavageAa);

        String massIndex = reader.getAttributeValue(null, "mass_index");
        if(massIndex != null)
            transition.setMassIndex(Integer.parseInt(massIndex));

        String isotopeDistrRank = reader.getAttributeValue(null, "isotope_dist_rank");
        if(isotopeDistrRank != null)
            transition.setIsotopeDistRank(Integer.parseInt(isotopeDistrRank));

        String isotopeDistrProportion = reader.getAttributeValue(null, "isotope_dist_proportion");
        if(isotopeDistrProportion != null)
            transition.setIsotopeDistProportion(Double.parseDouble(isotopeDistrProportion));


        String decoyMassShift = reader.getAttributeValue(null, "decoy_mass_shift");
        if(decoyMassShift != null)
            transition.setDecoyMassShift(Double.parseDouble(decoyMassShift));


        if(transition.isPrecursorIon() && transition.getMassIndex() == null)
        {
            transition.setMassIndex(0);
        }

        List<TransitionChromInfo> chromInfoList = new ArrayList<TransitionChromInfo>();
        transition.setChromInfoList(chromInfoList);

        while(reader.hasNext()) {

            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, TRANSITION))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new TransitionAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRECURSOR_MZ))
            {
                Double precursorMz = XmlUtil.readDouble(reader, PRECURSOR_MZ);
                if (precursorMz != null)
                {
                    transition.setPrecursorMz(precursorMz);
                }
                // Should we blow up if precursorMz was null?
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRODUCT_MZ))
            {
                Double productMz = XmlUtil.readDouble(reader, PRODUCT_MZ);
                if (productMz != null)
                {
                    transition.setMz(productMz);
                }
                // Should we blow up if productMz was null?
            }
            else if (XmlUtil.isStartElement(reader, evtType, COLLISION_ENERGY))
            {
                transition.setCollisionEnergy(XmlUtil.readDouble(reader, COLLISION_ENERGY));
            }
            else if (XmlUtil.isStartElement(reader, evtType, DECLUSTERING_POTENTIAL))
            {
                transition.setDeclusteringPotential(Double.parseDouble(reader.getElementText()));
            }
            else if(XmlUtil.isStartElement(reader, evtType, TRANSITION_PEAK))
            {
                TransitionChromInfo chromInfo = readTransitionChromInfo(reader);
                chromInfoList.add(chromInfo);
            }
            else if (XmlUtil.isStartElement(reader, evtType, LOSSES))
            {
                neutralLosses.addAll(readLosses(reader));
            }
            // TODO: transition_lib_info
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.transition);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionAnnotation());
        }

        _transitionCount++;
        return transition;
    }

    private List<TransitionLoss> readLosses(XMLStreamReader reader) throws XMLStreamException
    {
        List<TransitionLoss> result = new ArrayList<TransitionLoss>();
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, LOSSES))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, NEUTRAL_LOSS))
            {
                TransitionLoss loss = new TransitionLoss();
                loss.setModificationName(XmlUtil.readAttribute(reader, "modification_name"));
                loss.setLossIndex(XmlUtil.readIntegerAttribute(reader, "loss_index", (loss.getModificationName() != null ? 0 : null)));
                loss.setFormula(XmlUtil.readAttribute(reader, "formula"));
                loss.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, "massdiff_monoisotopic"));
                loss.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, "massdiff_average"));
                result.add(loss);
            }
        }
        return result;
    }

    private <AnnotationTargetType extends AbstractAnnotation> AnnotationTargetType readAnnotation(XMLStreamReader reader, AnnotationTargetType annotation) throws XMLStreamException
    {
        annotation.setName(reader.getAttributeValue(null, "name"));
        StringBuilder value = new StringBuilder();
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, ANNOTATION))
            {
                break;
            }
            if (XmlUtil.isText(evtType))
            {
                value.append(reader.getText());
            }
        }
        if (_dataSettings.isBooleanAnnotation(annotation.getName()))
        {
            // Boolean types are omitted if they're false, so consider it to be "true"
            annotation.setValue(Boolean.TRUE.toString());
        }
        else if (value.length() > 0)
        {
            annotation.setValue(value.toString());
        }
        return annotation;
    }

    private <AnnotationTargetType extends AbstractAnnotation> void addMissingBooleanAnnotation(List<AnnotationTargetType> annotations,
                                                                                 String missingAnotName,
                                                                                 AnnotationTargetType annotation)
    {
        annotation.setName(missingAnotName);
        annotation.setValue(Boolean.FALSE.toString());
        annotations.add(annotation);
    }

    private TransitionChromInfo readTransitionChromInfo(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionChromInfo chromInfo = new TransitionChromInfo();
        List<TransitionChromInfoAnnotation> annotations = new ArrayList<TransitionChromInfoAnnotation>();
        chromInfo.setAnnotations(annotations);

        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", TRANSITION_PEAK));
        chromInfo.setSkylineSampleFileId(XmlUtil.readAttribute(reader, "file", getDefaultSkylineSampleFileId(chromInfo.getReplicateName())));
        chromInfo.setOptimizationStep(XmlUtil.readIntegerAttribute(reader, "step"));
        chromInfo.setRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setStartTime(XmlUtil.readDoubleAttribute(reader, "start_time"));
        chromInfo.setEndTime(XmlUtil.readDoubleAttribute(reader, "end_time"));
        chromInfo.setArea(XmlUtil.readDoubleAttribute(reader, "area"));
        chromInfo.setBackground(XmlUtil.readDoubleAttribute(reader, "background"));
        chromInfo.setHeight(XmlUtil.readDoubleAttribute(reader, "height"));
        Double fwhm =  XmlUtil.readDoubleAttribute(reader, "fwhm");
        // TODO: Found NaN value for fwhm in Study7.sky.  Should this happen?
        fwhm = (fwhm != null && fwhm.isNaN()) ? null : fwhm;
        chromInfo.setFwhm(fwhm);
        chromInfo.setFwhmDegenerate(XmlUtil.readBooleanAttribute(reader, "fwhm_degenerate"));
        chromInfo.setTruncated(XmlUtil.readBooleanAttribute(reader, "truncated"));
        chromInfo.setIdentified(XmlUtil.readBooleanAttribute(reader, "identified"));
        chromInfo.setPeakRank(XmlUtil.readIntegerAttribute(reader, "rank"));
        chromInfo.setUserSet(XmlUtil.readBooleanAttribute(reader, "user_set"));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, TRANSITION_PEAK))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new TransitionChromInfoAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                chromInfo.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.transition_result);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionChromInfoAnnotation());
        }

        return chromInfo;
    }

    private int findEntry(double precursorMz, double tolerance, Chromatogram[] chromatograms, int left, int right)
    {
        // Binary search for the right precursorMz
        if (left > right)
            return -1;
        int mid = (left + right) / 2;
        int compare = compareMz(precursorMz, chromatograms[mid].getPrecursorMz(), tolerance);
        if (compare < 0)
            return findEntry(precursorMz, tolerance, chromatograms, left, mid - 1);
        if (compare > 0)
            return findEntry(precursorMz, tolerance, chromatograms, mid + 1, right);

        // Scan backward until the first matching element is found.
        while (mid > 0 && matchMz(precursorMz, tolerance, chromatograms[mid - 1].getPrecursorMz()))
            mid--;

        return mid;
    }

    private static boolean matchMz(double mz1, double mz2, double tolerance)
    {
        return compareMz(mz1, mz2, tolerance) == 0;
    }

    private static int compareMz(double precursorMz1, double precursorMz2, double tolerance)
    {
        return Chromatogram.compareTolerant(precursorMz1, precursorMz2, tolerance);
    }


    public List<Chromatogram> tryLoadChromatogram(List<Transition> transitions, double precursorMz, double tolerance)
    {
        // Add precursor matches to a list, if they match at least 1 transition
        // in this group, and are potentially the maximal transition match.

        // Using only the maximum works well for the case where there are 2
        // precursors in the same document that match a single entry.
        // TODO: But it messes up when there are 2 sets of transitions for
        //       the same precursor covering different numbers of transitions.
        //       Skyline never creates this case, but it has been reported
        int maxTranMatch = 1;

        List<Chromatogram> listChrom = new ArrayList<Chromatogram>();
        for (SkylineBinaryParser cache : _caches) // TODO: do we ever have more than one SkylineBinaryParser?
        {
            // Filter the list of chromatograms based on our precursor mZ
            int i = findEntry(precursorMz, tolerance, cache.getChromatograms(), 0, cache.getChromatograms().length - 1);
            if (i == -1)
            {
                return Collections.emptyList();
            }

            // Add entries to a list until they no longer match
            List<Chromatogram> listChromatograms = new ArrayList<Chromatogram>();
            while (i < cache.getChromatograms().length &&
                    matchMz(precursorMz, cache.getChromatograms()[i].getPrecursorMz(), tolerance))
            {
                listChromatograms.add(cache.getChromatograms()[i++]);
            }

            for (Chromatogram chromInfo : listChromatograms)
            {
//                if (containsInfo(chromatogram, chromInfo))
//                    continue;

                // If the chromatogram set has an optimiztion function then the number
                // of matching chromatograms per transition is a reflection of better
                // matching.  Otherwise, we only expect one match per transition.
                // TODO - do we need this on the Java side?
                boolean multiMatch = false;//chromatogram.OptimizationFunction != null;
                int tranMatch = chromInfo.matchTransitions(transitions, tolerance, multiMatch);
                if (tranMatch >= maxTranMatch)
                {
                    // If new maximum, clear anything collected at the previous maximum
                    if (tranMatch > maxTranMatch)
                        listChrom.clear();

                    maxTranMatch = tranMatch;
                    listChrom.add(chromInfo);
                }
            }
        }

        // If more than one value was found, make a final pass to ensure that there
        // is only one precursor match per file.
        if (listChrom.size() > 1)
        {
            List<Chromatogram> listChromFinal = new ArrayList<Chromatogram>();
            for (Chromatogram chromInfo : listChrom)
            {
                String filePath = chromInfo.getFilePath();

                int fileIndex = findIndex(listChromFinal, filePath);
                if (fileIndex == -1)
                    listChromFinal.add(chromInfo);
                // Use the entry with the m/z closest to the target
                else if (Math.abs(precursorMz - chromInfo.getPrecursorMz()) <
                         Math.abs(precursorMz - listChromFinal.get(fileIndex).getPrecursorMz()))
                {
                    listChromFinal.set(fileIndex, chromInfo);
                }
            }
            return listChromFinal;
        }
        return listChrom;
    }

    private int findIndex(List<Chromatogram> listChromFinal, String filePath)
    {
        int i = 0;
        for (Chromatogram chromatogram : listChromFinal)
        {
            if (chromatogram.getFilePath().equals(filePath))
            {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int getPeptideGroupCount()
    {
        return _peptideGroupCount;
    }

    public int getPeptideCount()
    {
        return _peptideCount;
    }

    public int getPrecursorCount()
    {
        return _precursorCount;
    }

    public int getTransitionCount()
    {
        return _transitionCount;
    }
}
