/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.targetedms.SkylineDocImporter;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.ReplicateManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 1/31/14
 * Time: 10:33 AM
 */
public class PeakAreaRatioCalculator
{
    private final Peptide _peptide;
    private Map<Integer, PeptideAreaRatioCalculator> _peptideAreaRatioCalculatorMap;

    private static Logger _log = Logger.getLogger(SkylineDocImporter.class);

    // All the precursors and transitions and chrom infos for this peptide must already have database IDs.
    public PeakAreaRatioCalculator(Peptide peptide)
    {
        _peptide = peptide;

        _peptideAreaRatioCalculatorMap = new HashMap<>();
    }

    public void init(Map<SkylineDocImporter.SampleFileKey, SampleFile> skylineIdSampleFileMap)
    {
        for(GeneralMoleculeChromInfo generalMoleculeChromInfo : _peptide.getGeneralMoleculeChromInfoList())
        {
            int sampleFileId = generalMoleculeChromInfo.getSampleFileId();
            _peptideAreaRatioCalculatorMap.put(sampleFileId, new PeptideAreaRatioCalculator(generalMoleculeChromInfo));
        }

        for(Precursor precursor: _peptide.getPrecursorList())
        {
            for(PrecursorChromInfo precursorChromInfo : precursor.getChromInfoList())
            {
                if(precursorChromInfo.isOptimizationPeak())
                {
                    continue; // Do not calculate area ratios for precursor peaks where "step" (optimization step) attribute is present
                }

                int sampleFileId = precursorChromInfo.getSampleFileId();
                PeptideAreaRatioCalculator calculator = getPeptideAreaRatioCalculator(sampleFileId);
                if(calculator != null)
                {
                    PrecursorAreaRatioCalculator precursorCalculator = calculator.getPrecursorAreaRatioCalculator(_peptide, precursor);
                    precursorCalculator.addChromInfo(precursor.getIsotopeLabelId(), precursorChromInfo);
                }
            }

            for(Transition transition: precursor.getTransitionsList())
            {
                for(TransitionChromInfo transitionChromInfo: transition.getChromInfoList())
                {
                    if(transitionChromInfo.isOptimizationPeak())
                    {
                        continue; // Do not calculate area ratios for transition peaks where "step" (optimization step) attribute is present
                    }

                    int sampleFileId = transitionChromInfo.getSampleFileId();
                    PeptideAreaRatioCalculator calculator = getPeptideAreaRatioCalculator(sampleFileId);
                    if(calculator != null)
                    {
                        TransitionAreaRatioCalculator transitionCalculator = calculator.getTransitionAreaRatioCalculator(_peptide, precursor, transition);
                        transitionCalculator.addChromInfo(precursor.getIsotopeLabelId(), transitionChromInfo);
                    }
                }
            }
        }
    }


    private PeptideAreaRatioCalculator getPeptideAreaRatioCalculator(int sampleFileId)
    {
        return  _peptideAreaRatioCalculatorMap.get(sampleFileId);
    }

    public PeptideAreaRatio getPeptideAreaRatio(Integer sampleFileId, Integer numLabelId, Integer denomLabelId)
    {
        PeptideAreaRatioCalculator pCalc = getPeptideAreaRatioCalculator(sampleFileId);
        return pCalc != null ? pCalc.getRatio(numLabelId, denomLabelId) : null;
    }

    public PrecursorAreaRatio getPrecursorAreaRatio(Integer sampleFileId, Precursor precursor, Integer numLabelId, Integer denomLabelId)
    {
        if(precursor.getIsotopeLabelId() != numLabelId)
            return null;

        PeptideAreaRatioCalculator pCalc = getPeptideAreaRatioCalculator(sampleFileId);
        if(pCalc == null)
            return null;

        PrecursorAreaRatioCalculator calculator = pCalc.getPrecursorAreaRatioCalculator(_peptide, precursor);
        return calculator.getRatio(numLabelId, denomLabelId);
    }

    public TransitionAreaRatio getTransitionAreaRatio(Integer sampleFileId, Precursor precursor, Transition transition, Integer numLabelId, Integer denomLabelId)
    {
        if(precursor.getIsotopeLabelId() != numLabelId)
            return null;

        PeptideAreaRatioCalculator pCalc = getPeptideAreaRatioCalculator(sampleFileId);
        if(pCalc == null)
            return null;

        TransitionAreaRatioCalculator calculator = pCalc.getTransitionAreaRatioCalculator(_peptide,precursor, transition);
        return calculator.getRatio(numLabelId, denomLabelId);
    }

    private class PeptideAreaRatioCalculator
    {
        private Map<String, PrecursorAreaRatioCalculator> _calculatorMap;
        private GeneralMoleculeChromInfo _generalMoleculeChromInfo;

        public PeptideAreaRatioCalculator(GeneralMoleculeChromInfo generalMoleculeChromInfo)
        {
            _generalMoleculeChromInfo = generalMoleculeChromInfo;
            _calculatorMap = new HashMap<>();
        }

        PeptideAreaRatio getRatio(int numIsotopeLabelId, int denomIsotopeLabelId)
        {
            LabelRatioAreas areas = new LabelRatioAreas();

            for(String precursorKey: _calculatorMap.keySet())
            {
                PrecursorAreaRatioCalculator pCalc = _calculatorMap.get(precursorKey);

                LabelRatioAreas precursorAreas = pCalc.getLabelRatioAreas(numIsotopeLabelId, denomIsotopeLabelId);
                areas.addNumeratorArea(precursorAreas.getNumeratorArea());
                areas.addDenominatorArea(precursorAreas.getDenominatorArea());
            }

            if(areas.getNumeratorArea() != null && areas.getDenominatorArea() != null)
            {
                Double ratio = PeakAreaRatioCalculator.calculateRatio(areas.getNumeratorArea(), areas.getDenominatorArea());
                if(ratio == null)
                    return null;
                PeptideAreaRatio pRatio = new PeptideAreaRatio();
                pRatio.setIsotopeLabelId(numIsotopeLabelId);
                pRatio.setIsotopeLabelStdId(denomIsotopeLabelId);
                pRatio.setPeptideChromInfoId(_generalMoleculeChromInfo.getId());
                pRatio.setPeptideChromInfoStdId(_generalMoleculeChromInfo.getId()); // TODO: drop this column from the table?
                pRatio.setAreaRatio(ratio);
                return pRatio;
            }

            return null;
        }

        private PrecursorAreaRatioCalculator getPrecursorAreaRatioCalculator(Peptide peptide, Precursor precursor)
        {
            String precursorKey = peptide.getPrecursorKey(peptide, precursor);
            PrecursorAreaRatioCalculator calculator = _calculatorMap.get(precursorKey);
            if(calculator == null)
            {
                calculator = new PrecursorAreaRatioCalculator(precursorKey, _generalMoleculeChromInfo.getSampleFileId());
                _calculatorMap.put(precursorKey, calculator);
            }
            return calculator;
        }

        private TransitionAreaRatioCalculator getTransitionAreaRatioCalculator(Peptide peptide, Precursor precursor, Transition transition)
        {
            PrecursorAreaRatioCalculator precursorCalculator = getPrecursorAreaRatioCalculator(peptide, precursor);
            return precursorCalculator.getTransitionAreaRatioCalculator(transition, precursor);
        }
    }

    // Calculator for precursor area ratios in one sample file
    private class PrecursorAreaRatioCalculator extends AreaRatioCalculator<PrecursorChromInfo, PrecursorAreaRatio>
    {
        private Map<String, TransitionAreaRatioCalculator> _calculatorMap;
        private String _key;
        private int _sampleFileId;

        PrecursorAreaRatioCalculator(String precursorKey, int sampleFileId)
        {
            super();
            _calculatorMap = new HashMap<>();
            _key = precursorKey;
            _sampleFileId = sampleFileId;
        }

        @Override
        PrecursorAreaRatio getRatio(int numIsotopeLabelId, int denomIsotopeLabelId)
        {
            LabelRatioAreas areas = getLabelRatioAreas(numIsotopeLabelId, denomIsotopeLabelId);

            if(areas.getNumeratorArea() != null && areas.getDenominatorArea() != null)
            {
                Double ratio = PeakAreaRatioCalculator.calculateRatio(areas.getNumeratorArea(), areas.getDenominatorArea());
                if(ratio ==  null)
                    return null;
                PrecursorAreaRatio pRatio = new PrecursorAreaRatio();
                pRatio.setIsotopeLabelId(numIsotopeLabelId);
                pRatio.setIsotopeLabelStdId(denomIsotopeLabelId);
                pRatio.setPrecursorChromInfoId(getChromInfo(numIsotopeLabelId).getId());
                pRatio.setPrecursorChromInfoStdId(getChromInfo(denomIsotopeLabelId).getId());
                pRatio.setAreaRatio(ratio);
                return pRatio;
            }

            return null;
        }

        LabelRatioAreas getLabelRatioAreas(int numIsotopeLabelId, int denomIsotopeLabelId)
        {
            LabelRatioAreas areas = new LabelRatioAreas();

            for(String transitionKey: _calculatorMap.keySet())
            {
                TransitionAreaRatioCalculator calculator = _calculatorMap.get(transitionKey);

                TransitionChromInfo numChromInfo = calculator.getChromInfo(numIsotopeLabelId);
                TransitionChromInfo denomChromInfo = calculator.getChromInfo(denomIsotopeLabelId);

                Double na = numChromInfo == null ? null : numChromInfo.getArea();
                Double da = denomChromInfo == null ? null : denomChromInfo.getArea();

                // For transition group (precursor) area ratio calculation in Skyline look at
                // PeptideChromInfoCalculator.CalcTransitionGroupRatio. This function calculates
                    // peptide area ratio as well when precursorCharge == -1.
                if(na == null || na == 0.0)
                    continue;

                if(da == null || da == 0.0)
                    continue;

                areas.addNumeratorArea(na);
                areas.addDenominatorArea(da);
            }
            return areas;
        }

        @Override
        PrecursorChromInfo getChromInfo(int isotopeLabelId)
        {
            PrecursorChromInfo chromInfo = super.getChromInfo(isotopeLabelId);
            if(chromInfo == null)
            {
                throw new IllegalStateException("Could not find precursor chrom info for isotope label "
                                                + getIsotopeLabelName(isotopeLabelId)
                                                + " precursor key " + _key
                                                + " in sample file " + getSampleFileName(_sampleFileId));
            }
            return chromInfo;
        }

        TransitionAreaRatioCalculator getTransitionAreaRatioCalculator(Transition transition, Precursor precursor)
        {
            String transitionKey = getTransitionKey(transition, precursor);
            TransitionAreaRatioCalculator calculator = _calculatorMap.get(transitionKey);
            if(calculator == null)
            {
                calculator = new TransitionAreaRatioCalculator();
                _calculatorMap.put(transitionKey, calculator);
            }
            return calculator;
        }
    }

    // Calculator for transition area ratios for a single transtion from one precursor
    // in one sample file.
    private class TransitionAreaRatioCalculator extends AreaRatioCalculator<TransitionChromInfo, TransitionAreaRatio>
    {
        @Override
        TransitionAreaRatio getRatio(int numIsotopeLabelId, int denomIsotopeLabelId)
        {
            TransitionChromInfo numChromInfo = getChromInfo(numIsotopeLabelId);
            TransitionChromInfo denomChromInfo = getChromInfo(denomIsotopeLabelId);

            if(numChromInfo != null && denomChromInfo != null)
            {
                Double ratio = PeakAreaRatioCalculator.calculateRatio(numChromInfo.getArea(), denomChromInfo.getArea());
                if(ratio ==  null)
                    return null;
                TransitionAreaRatio taRatio = new TransitionAreaRatio();
                taRatio.setTransitionChromInfoId(numChromInfo.getId());
                taRatio.setTransitionChromInfoStdId(denomChromInfo.getId());
                taRatio.setAreaRatio(ratio);
                taRatio.setIsotopeLabelId(numIsotopeLabelId);
                taRatio.setIsotopeLabelStdId(denomIsotopeLabelId);
                return taRatio;
            }
            return null;
        }
    }

    private abstract class AreaRatioCalculator <T extends ChromInfo, R extends AreaRatio>
    {
        private Map<Integer, T> _labelIdChromInfoMap;

        public AreaRatioCalculator()
        {
            _labelIdChromInfoMap = new HashMap<>();
        }

        public void addChromInfo(int isotopeLabelId, T chromInfo)
        {
            if(_labelIdChromInfoMap.containsKey(isotopeLabelId))
            {
                throw new IllegalStateException("Area for isotope label " + getIsotopeLabelName(isotopeLabelId)
                                                + " already exists.");
            }

            _labelIdChromInfoMap.put(isotopeLabelId, chromInfo);
        }

        abstract R getRatio(int numIsotopeLabelId, int denomIsotopeLabelId);


        T getChromInfo(int isotopeLabelId)
        {
            return _labelIdChromInfoMap.get(isotopeLabelId);
        }
    }

    private class LabelRatioAreas
    {
        private Double _numeratorArea;
        private Double _denominatorArea;

        public Double getNumeratorArea()
        {
            return _numeratorArea;
        }

        public void addNumeratorArea(Double area)
        {
            if(_numeratorArea == null)
            {
                _numeratorArea = area;
            }
            else if(area != null)
            {
                _numeratorArea += area;
            }
        }

        public Double getDenominatorArea()
        {
            return _denominatorArea;
        }

        public void addDenominatorArea(Double area)
        {
            if(_denominatorArea == null)
            {
                _denominatorArea = area;
            }
            else if(area != null)
            {
                _denominatorArea += area;
            }
        }
    }

    private String getIsotopeLabelName(int isotopeLabelId)
    {
        PeptideSettings.IsotopeLabel label = IsotopeLabelManager.getIsotopeLabel(isotopeLabelId);
        if(label != null)
            return label.getName();
        return "(Error getting isotope label for ID " + isotopeLabelId + ")";
    }

    private String getSampleFileName(int sampleFileId)
    {
        SampleFile sampleFile = ReplicateManager.getSampleFile(sampleFileId);
        if(sampleFile != null)
            return sampleFile.getSampleName();
        return "(Error getting sample file for ID " + sampleFileId + ")";
    }

    private static Double calculateRatio(Double numerator, Double denominator)
    {
        if(denominator == null || denominator == 0.0)
            return null;
        if(numerator == null)
            return null;
        return numerator / denominator;
    }

    public static String getTransitionKey(GeneralTransition generalTransition, GeneralPrecursor generalPrecursor)
    {
        if (generalTransition instanceof Transition) {
            return getPeptideTransitionKey((Transition) generalTransition, (Precursor) generalPrecursor);
        }
        return getMoleculeTransitionKey((MoleculeTransition) generalTransition, (MoleculePrecursor) generalPrecursor);
    }

    private static String getMoleculeTransitionKey(MoleculeTransition transition, MoleculePrecursor precursor) {
        // TODO(nicksh): If getCustomIonName is null, then fall back to unlabeled formula.
        String fragment = transition.getFragmentType()
                + (transition.isPrecursorIon() ? transition.getMassIndex() : transition.getCustomIonName());

        int fragmentCharge = transition.isPrecursorIon() ? precursor.getCharge() : transition.getCharge();
        StringBuilder key = new StringBuilder();
        key.append(fragment)
                .append("_")
                .append(fragmentCharge);
        return key.toString();
    }

    private static String getPeptideTransitionKey(Transition transition , Precursor precursor) {
        String fragment = transition.getFragmentType()
                + (transition.isPrecursorIon() ? transition.getMassIndex() : (transition.isCustomIon() ? transition.getMeasuredIonName() : transition.getFragmentOrdinal()));

        int fragmentCharge = transition.isPrecursorIon() ? precursor.getCharge() : transition.getCharge();
        StringBuilder key = new StringBuilder();
        key.append(fragment)
                .append("_")
                .append(fragmentCharge);
        List<TransitionLoss> transitionLosses = transition.getNeutralLosses();
        if(transitionLosses != null && transitionLosses.size() > 0)
        {
            for(TransitionLoss loss: transitionLosses)
            {
                key.append("_").append(loss.toString());
            }
        }
        return key.toString();
    }
}
