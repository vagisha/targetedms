/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.calculations;

import org.labkey.targetedms.calculations.quantification.CalibrationCurve;
import org.labkey.targetedms.calculations.quantification.CalibrationCurveDataSet;
import org.labkey.targetedms.calculations.quantification.GroupComparisonDataSet;
import org.labkey.targetedms.calculations.quantification.LinearFitResult;
import org.labkey.targetedms.calculations.quantification.NormalizationMethod;
import org.labkey.targetedms.calculations.quantification.PValues;
import org.labkey.targetedms.calculations.quantification.RegressionFit;
import org.labkey.targetedms.calculations.quantification.RegressionWeighting;
import org.labkey.targetedms.calculations.quantification.SampleType;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.Tuple3;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.CalibrationCurveEntity;
import org.labkey.targetedms.parser.FoldChange;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.GroupComparisonSettings;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nicksh on 4/13/2016.
 */
public class RunQuantifier
{
    ReplicateDataSet _replicateDataSet;
    NormalizationMethodAreas _normalizationMethodAreas;
    TargetedMSRun _targetedMSRun;
    List<PeptideSettings.IsotopeLabel> _labels;
    User _user;
    Container _container;

    public RunQuantifier(TargetedMSRun run, User user, Container container)
    {
        _targetedMSRun = run;
        _replicateDataSet = new ReplicateDataSet(run);
        _labels = IsotopeLabelManager.getIsotopeLabels(run.getRunId());
        _user = user;
        _container = container;
        _normalizationMethodAreas = new NormalizationMethodAreas(run, _user, _container, _replicateDataSet);
    }

    public List<FoldChange> calculateFoldChanges(GroupComparisonSettings groupComparisonSettings)
    {
        List<FoldChange> foldChanges = new ArrayList<>();
        for (PeptideGroup peptideGroup : listPeptideGroups())
        {
            foldChanges.addAll(calculateFoldChanges(groupComparisonSettings, peptideGroup));
        }
        double[] rawPValues = foldChanges.stream().mapToDouble(FoldChange::getAdjustedPValue).toArray();
        double[] adjustedPValues = PValues.adjustPValues(rawPValues);
        for (int i = 0; i < foldChanges.size(); i++)
        {
            FoldChange foldChange = foldChanges.get(i);
            foldChange.setAdjustedPValue(adjustedPValues[i]);
            foldChange.setGroupComparisonSettingsId(groupComparisonSettings.getId());
            foldChange.setRunId(_targetedMSRun.getRunId());
        }
        return foldChanges;
    }

    public List<CalibrationCurveEntity> calculateCalibrationCurves(QuantificationSettings quantificationSettings, List<GeneralMoleculeChromInfo> modifiedChromInfos)
    {
        RegressionFit regressionFit = RegressionFit.parse(quantificationSettings.getRegressionFit());
        if (RegressionFit.NONE == regressionFit)
        {
            return Collections.emptyList();
        }

        TargetedMSSchema schema = new TargetedMSSchema(_user, _container);
        List<CalibrationCurveEntity> calibrationCurves = new ArrayList<>();
        for (PeptideGroup peptideGroup : listPeptideGroups())
        {
            List<GeneralMolecule> generalMolecules = new ArrayList<>();
            generalMolecules.addAll(MoleculeManager.getMoleculesForGroup(peptideGroup.getId()));
            generalMolecules.addAll(PeptideManager.getPeptidesForGroup(peptideGroup.getId(), schema));
            for (GeneralMolecule molecule : generalMolecules)
            {
                NormalizationMethod normalizationMethod
                        = getNormalizationMethod(molecule, quantificationSettings.getNormalizationMethod());
                PeptideSettings.IsotopeLabel labelToQuantify
                        = getLabelToQuantify(normalizationMethod);
                if (labelToQuantify == null)
                {
                    return Collections.emptyList();
                }

                CalibrationCurveEntity calibrationCurve = calculateCalibrationCurveEntity(
                        quantificationSettings, labelToQuantify, molecule, modifiedChromInfos);
                if (calibrationCurve != null)
                {
                    calibrationCurves.add(calibrationCurve);
                }
            }
        }
        return calibrationCurves;
    }

    public CalibrationCurve calculateCalibrationCurve(QuantificationSettings quantificationSettings, GeneralMolecule generalMolecule, List<GeneralMoleculeChromInfo> modifiedChromInfos)
    {
        PeptideSettings.IsotopeLabel labelToQuantify
                = getLabelToQuantify(getNormalizationMethod(generalMolecule, quantificationSettings.getNormalizationMethod()));
        if (labelToQuantify == null)
        {
            return null;
        }
        return calculateCalibrationCurve(quantificationSettings, labelToQuantify, generalMolecule, modifiedChromInfos);
    }

    public CalibrationCurveEntity calculateCalibrationCurveEntity(QuantificationSettings quantificationSettings, PeptideSettings.IsotopeLabel isotopeLabel, GeneralMolecule generalMolecule, List<GeneralMoleculeChromInfo> modifiedChromInfos)
    {
        CalibrationCurve calibrationCurve = calculateCalibrationCurve(quantificationSettings, isotopeLabel, generalMolecule, modifiedChromInfos);
        CalibrationCurveEntity calibrationCurveRow
                = toCalibrationCurveEntity(calibrationCurve);
        calibrationCurveRow.setGeneralMoleculeId(generalMolecule.getId());
        calibrationCurveRow.setQuantificationSettingsId(quantificationSettings.getId());
        return calibrationCurveRow;
    }

    public CalibrationCurve calculateCalibrationCurve(QuantificationSettings quantificationSettings, PeptideSettings.IsotopeLabel isotopeLabel, GeneralMolecule generalMolecule, List<GeneralMoleculeChromInfo> modifiedChromInfos)
    {
        NormalizationMethod normalizationMethod;
        if (null != generalMolecule.getNormalizationMethod())
        {
            normalizationMethod = NormalizationMethod.fromName(generalMolecule.getNormalizationMethod());
        }
        else
        {
            normalizationMethod = NormalizationMethod.fromName(quantificationSettings.getNormalizationMethod());
        }
        if (normalizationMethod == null)
        {
            normalizationMethod = NormalizationMethod.NONE;
        }

        CalibrationCurveDataSet calibrationCurveDataSet = new CalibrationCurveDataSet();
        calibrationCurveDataSet.setNormalizationMethod(normalizationMethod);
        calibrationCurveDataSet.setRegressionFit(RegressionFit.parse(quantificationSettings.getRegressionFit()));
        calibrationCurveDataSet.setRegressionWeighting(
                RegressionWeighting.parse(quantificationSettings.getRegressionWeighting()));
        GeneralMoleculeResultDataSet generalMoleculeResultDataSet
                = new GeneralMoleculeResultDataSet(_user, _container, _replicateDataSet, generalMolecule);
        Collection<GeneralMoleculeChromInfo> moleculeChromInfos
                = generalMoleculeResultDataSet.getGeneralMoleculeChromInfos();
        Set<Integer> excludedReplicateIds = getExcludedReplicateIds(moleculeChromInfos);
        Map<Integer, CalibrationCurveDataSet.Replicate> calibrationCurveReplicates = new HashMap<>();
        for (Replicate replicate : _replicateDataSet.listReplicates())
        {
            Double moleculeConcentration = replicate.getAnalyteConcentration();
            if (null != moleculeConcentration && null != generalMolecule.getConcentrationMultiplier())
            {
                moleculeConcentration = moleculeConcentration * generalMolecule.getConcentrationMultiplier();
            }
            CalibrationCurveDataSet.Replicate replicateData = calibrationCurveDataSet.addReplicate(
                    SampleType.fromName(replicate.getSampleType()), moleculeConcentration,
                    excludedReplicateIds.contains(replicate.getId()));
            calibrationCurveReplicates.put(replicate.getId(), replicateData);
            generalMoleculeResultDataSet.addFeatureData(replicate, replicateData, quantificationSettings.getMsLevel(),
                    _normalizationMethodAreas.getNormalizationFactors(normalizationMethod), normalizationMethod.isAllowTruncatedTransitions());
        }

        CalibrationCurve calibrationCurve
                = calibrationCurveDataSet.getCalibrationCurve(isotopeLabel.getName());
        if (modifiedChromInfos != null)
        {
            for (GeneralMoleculeChromInfo generalMoleculeChromInfo : moleculeChromInfos)
            {
                Replicate replicate = _replicateDataSet.getReplicateFromSampleFileId(generalMoleculeChromInfo.getSampleFileId());
                if (replicate == null)
                {
                    continue;
                }
                CalibrationCurveDataSet.Replicate calibrationCurveReplicate = calibrationCurveReplicates.get(replicate.getId());
                Double calculatedConcentration = calibrationCurveDataSet.getCalculatedConcentration(
                        isotopeLabel.getName(), calibrationCurve, calibrationCurveReplicate);
                if (calculatedConcentration != null)
                {
                    generalMoleculeChromInfo.setCalculatedConcentration(calculatedConcentration);
                    modifiedChromInfos.add(generalMoleculeChromInfo);
                }
            }
        }
        return calibrationCurve;
    }

    public PeptideSettings.IsotopeLabel getLabelToQuantify(NormalizationMethod normalizationMethod)
    {
        NormalizationMethod.RatioToLabel ratioToLabel = null;
        if (normalizationMethod instanceof NormalizationMethod.RatioToLabel)
        {
            ratioToLabel = (NormalizationMethod.RatioToLabel) normalizationMethod;
        }
        for (PeptideSettings.IsotopeLabel isotopeLabel : _replicateDataSet.listIsotopeLabels())
        {
            if (ratioToLabel != null && Objects.equals(isotopeLabel.getName(), ratioToLabel.getIsotopeLabelTypeName()))
            {
                continue;
            }
            if (isotopeLabel.isStandard())
            {
                continue;
            }
            return isotopeLabel;
        }
        return null;
    }

    public CalibrationCurveEntity toCalibrationCurveEntity(CalibrationCurve calibrationCurve)
    {
        if (calibrationCurve == null)
        {
            return null;
        }
        CalibrationCurveEntity calibrationCurveRow = new CalibrationCurveEntity();
        calibrationCurveRow.setRunId(_targetedMSRun.getRunId());
        calibrationCurveRow.setIntercept(calibrationCurve.getIntercept());
        calibrationCurveRow.setSlope(calibrationCurve.getSlope());
        calibrationCurveRow.setPointCount(calibrationCurve.getPointCount());
        calibrationCurveRow.setErrorMessage(calibrationCurve.getErrorMessage());
        calibrationCurveRow.setQuadraticCoefficient(calibrationCurve.getQuadraticCoefficient());
        calibrationCurveRow.setRSquared(calibrationCurve.getRSquared());
        return calibrationCurveRow;
    }

    private List<FoldChange> calculateFoldChanges(GroupComparisonSettings settings, PeptideGroup peptideGroup)
    {
        List<FoldChange> foldChanges = new ArrayList<>();
        Collection<GeneralMolecule> generalMolecules = getGeneralMoleculesForGroup(peptideGroup);
        if (settings.isPerProtein())
        {
            Collection<GeneralMoleculeResultDataSet> resultDataSets = generalMolecules.stream()
                    .filter(peptide-> null == peptide.getStandardType())
                    .map(peptide -> new GeneralMoleculeResultDataSet(_user, _container, _replicateDataSet, peptide))
                    .collect(Collectors.toList());
            foldChanges.addAll(calculateFoldChanges(settings, resultDataSets));
        }
        else
        {
            for (GeneralMolecule generalMolecule : generalMolecules)
            {
                for (FoldChange foldChange : calculateFoldChanges(settings, Collections.singleton(new GeneralMoleculeResultDataSet(_user, _container, _replicateDataSet, generalMolecule))))
                {
                    foldChange.setGeneralMoleculeId(generalMolecule.getId());
                    foldChanges.add(foldChange);
                }
            }
        }
        for (FoldChange foldChange : foldChanges)
        {
            foldChange.setPeptideGroupId(peptideGroup.getId());
        }
        return foldChanges;
    }

    private List<GeneralMolecule> getGeneralMoleculesForGroup(PeptideGroup peptideGroup)
    {
        TargetedMSSchema schema = new TargetedMSSchema(_user, _container);
        List<GeneralMolecule> list = new ArrayList<>();
        list.addAll(PeptideManager.getPeptidesForGroup(peptideGroup.getId(), schema));
        list.addAll(MoleculeManager.getMoleculesForGroup(peptideGroup.getId()));
        return list;
    }

    private List<FoldChange> calculateFoldChanges(GroupComparisonSettings settings, Collection<GeneralMoleculeResultDataSet> peptideResults)
    {
        Set<String> caseValues;
        if (null == settings.getCaseValue())
        {
            caseValues = new HashSet<>(_replicateDataSet.listAnnotationValues(settings.getControlAnnotation()));
            caseValues.remove(settings.getControlValue());
        }
        else
        {
            caseValues = Collections.singleton(settings.getCaseValue());
        }
        List<FoldChange> foldChanges = new ArrayList<>();
        for (String caseValue : caseValues)
        {
            List<Tuple3<Boolean, String, Replicate>> replicateTuples = new ArrayList<>();
            for (Replicate replicate : _replicateDataSet.listReplicates())
            {
                String bioReplicate = null;
                if (null != settings.getIdentityAnnotation())
                {
                    bioReplicate = _replicateDataSet.getReplicateAnnotationValue(replicate, settings.getIdentityAnnotation());
                }
                String controlValue = _replicateDataSet.getReplicateAnnotationValue(replicate, settings.getControlAnnotation());
                if (Objects.equals(controlValue, settings.getControlValue()))
                {
                    replicateTuples.add(new Tuple3<>(true, bioReplicate, replicate));
                }
                else if (Objects.equals(controlValue, caseValue))
                {
                    replicateTuples.add(new Tuple3<>(false, bioReplicate, replicate));
                }
            }
            for (Integer msLevel : Arrays.asList(1, 2))
            {
                for (PeptideSettings.IsotopeLabel isotopeLabel : _labels)
                {
                    FoldChange foldChange = calculateFoldChange(settings, peptideResults, replicateTuples, isotopeLabel, msLevel);
                    if (foldChange == null)
                    {
                        continue;
                    }
                    foldChanges.add(foldChange);
                }
            }
        }
        return foldChanges;
    }

    @Nullable
    private FoldChange calculateFoldChange(GroupComparisonSettings settings, Collection<GeneralMoleculeResultDataSet> peptideResults,
                                           List<Tuple3<Boolean, String, Replicate>> replicates, PeptideSettings.IsotopeLabel isotopeLabel, Integer msLevel)
    {
        GroupComparisonDataSet foldChangeCalculator = new GroupComparisonDataSet();
        foldChangeCalculator.setNormalizationMethod(NormalizationMethod.fromName(settings.getNormalizationMethod()));
        if (foldChangeCalculator.getNormalizationMethod() instanceof NormalizationMethod.RatioToLabel
                && isotopeLabel.getName().equals(((NormalizationMethod.RatioToLabel) foldChangeCalculator.getNormalizationMethod()).getIsotopeLabelTypeName()))
        {
            return null;
        }
        for (Tuple3<Boolean, String, Replicate> tuple : replicates)
        {
            GroupComparisonDataSet.Replicate replicateData
                    = foldChangeCalculator.addReplicate(tuple.getKey(), tuple.getValue());
            for (GeneralMoleculeResultDataSet generalMoleculeResultDataSet : peptideResults)
            {
                NormalizationMethod normalizationMethod = getNormalizationMethod(
                        generalMoleculeResultDataSet.getGeneralMolecule(), settings.getNormalizationMethod());
                generalMoleculeResultDataSet.addFeatureData(tuple.third, replicateData, msLevel,
                        _normalizationMethodAreas.getNormalizationFactors(normalizationMethod),
                        normalizationMethod.isAllowTruncatedTransitions());
            }
        }
        LinearFitResult linearFitResult = foldChangeCalculator.calculateFoldChange(isotopeLabel.getName());
        if (linearFitResult == null)
        {
            return null;
        }
        FoldChange foldChange = new FoldChange();
        foldChange.setLog2FoldChange(linearFitResult.getEstimatedValue());
        foldChange.setStandardError(linearFitResult.getStandardError());
        foldChange.setDegreesOfFreedom(linearFitResult.getDegreesOfFreedom());
        foldChange.setIsotopeLabelId(isotopeLabel.getId());
        foldChange.setMsLevel(msLevel);
        // P-values get adjusted by caller
        foldChange.setAdjustedPValue(linearFitResult.getPValue());
        return foldChange;
    }

    private Collection<PeptideGroup> listPeptideGroups()
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), _targetedMSRun.getRunId());
        return new TableSelector(TargetedMSManager.getTableInfoPeptideGroup(), filter, null).getCollection(PeptideGroup.class);
    }

    public ReplicateDataSet getReplicateDataSet()
    {
        return _replicateDataSet;
    }

    public NormalizationMethod getNormalizationMethod(GeneralMolecule generalMolecule, String normalizationMethodName)
    {
        if (null != generalMolecule.getNormalizationMethod())
        {
            return NormalizationMethod.fromName(generalMolecule.getNormalizationMethod());
        }
        NormalizationMethod normalizationMethod = NormalizationMethod.fromName(normalizationMethodName);
        if (normalizationMethod == null)
        {
            return NormalizationMethod.NONE;
        }
        return normalizationMethod;
    }

    private Set<Integer> getExcludedReplicateIds(Collection<GeneralMoleculeChromInfo> generalMoleculeChromInfos)
    {
        Set<Integer> excludedReplicateIds = new HashSet<>();
        for (GeneralMoleculeChromInfo generalMoleculeChromInfo : generalMoleculeChromInfos)
        {
            if (generalMoleculeChromInfo.isExcludeFromCalibration())
            {
                Replicate replicate = _replicateDataSet.getReplicateFromSampleFileId(
                        generalMoleculeChromInfo.getSampleFileId());
                if (replicate != null)
                {
                    excludedReplicateIds.add(replicate.getId());
                }
            }
        }
        return excludedReplicateIds;
    }
}

