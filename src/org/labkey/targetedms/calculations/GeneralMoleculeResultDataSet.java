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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.calculations.quantification.ReplicateData;
import org.labkey.targetedms.calculations.quantification.TransitionAreas;
import org.labkey.targetedms.parser.ChromInfo;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.PeakAreaRatioCalculator;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.MoleculeTransitionManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.TransitionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nicksh on 4/13/2016.
 */
public class GeneralMoleculeResultDataSet
{
    private final GeneralMolecule _generalMolecule;
    private ReplicateDataSet _replicateSet;
    private Map<Long, List<ChromInfoRecord>> _chromInfoRecordsByReplicateId = new HashMap<>();
    private Set<Integer> _msLevels = new HashSet<>();
    private Set<String> _labels = new HashSet<>();

    public GeneralMoleculeResultDataSet(User user, Container container, ReplicateDataSet replicateSet, GeneralMolecule peptide)
    {
        _generalMolecule = peptide;
        _replicateSet = replicateSet;
        if (replicateSet.getUseTransitionChromInfos())
        {
            addPeptideResults(user, container, peptide);
        }
        else
        {
            addPrecursorLevelResults(user, container, peptide);
        }
    }

    public GeneralMolecule getGeneralMolecule()
    {
        return _generalMolecule;
    }

    public ReplicateDataSet getReplicateSet()
    {
        return _replicateSet;
    }

    public double getTotalArea(long sampleFileId, PeptideSettings.IsotopeLabel isotopeLabel)
    {
        Replicate replicate = _replicateSet.getReplicateFromSampleFileId(sampleFileId);
        if (replicate == null)
        {
            return 0;
        }
        double total = 0;
        List<ChromInfoRecord> records = _chromInfoRecordsByReplicateId.get(replicate.getId());
        if (records != null)
        {
            for (ChromInfoRecord chromInfoRecord : records)
            {
                if (chromInfoRecord.getSampleFileId() != sampleFileId)
                {
                    continue;
                }
                if (isotopeLabel != null && !isotopeLabel.getName().equals(chromInfoRecord.getLabel()))
                {
                    continue;
                }
                if (chromInfoRecord.getArea() != null)
                {
                    total += chromInfoRecord.getArea();
                }
            }
        }
        return total;
    }

    private void addPeptideResults(User user, Container container, GeneralMolecule generalMolecule)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        for (GeneralPrecursor precursor : getPrecursors(generalMolecule, schema))
        {
            String precursorKey = generalMolecule.getPrecursorKey(generalMolecule, precursor);
            for (GeneralTransition transition : getTransitions(precursor, schema))
            {
                String transitionKey = PeakAreaRatioCalculator.getTransitionKey(transition, precursor);
                String featureName = generalMolecule.getId() + precursorKey + transitionKey;
                for (TransitionChromInfo transitionChromInfo : TransitionManager.getTransitionChromInfoListForTransition(transition.getId()))
                {
                    Replicate replicate = _replicateSet.getReplicateFromSampleFileId(transitionChromInfo.getSampleFileId());
                    if (replicate == null)
                    {
                        continue;
                    }
                    ChromInfoRecord chromInfoRecord = new ChromInfoRecord(featureName, replicate,
                            _replicateSet.getIsotopeLabelName(precursor.getIsotopeLabelId()), transition, transitionChromInfo);
                    addChromInfoRecord(chromInfoRecord);
                }
            }
        }
    }

    private void addPrecursorLevelResults(User user, Container container, GeneralMolecule generalMolecule)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        Map<Long, List<PrecursorChromInfo>> chromInfosByPrecursorId = PrecursorManager
                .getPrecursorChromInfosForPeptide(generalMolecule.getId()).stream()
                .collect(Collectors.groupingBy(precursorChromInfo -> precursorChromInfo.getPrecursorId()));

        for (GeneralPrecursor precursor : getPrecursors(generalMolecule, schema))
        {
            List<PrecursorChromInfo> chromInfos = chromInfosByPrecursorId.get(precursor.getId());
            if (chromInfos == null)
            {
                continue;
            }
            String isotopeLabel = _replicateSet.getIsotopeLabelName(precursor.getIsotopeLabelId());
            String precursorKey = generalMolecule.getPrecursorKey(generalMolecule, precursor);
            for (PrecursorChromInfo precursorChromInfo : chromInfos)
            {
                Replicate replicate = _replicateSet.getReplicateFromSampleFileId(precursorChromInfo.getSampleFileId());
                if (replicate == null)
                {
                    continue;
                }

                addChromInfoRecord(new ChromInfoRecord(precursorKey, replicate, isotopeLabel, 1,
                    precursorChromInfo.getTotalAreaMs1(), precursorChromInfo.getSampleFileId()));
                addChromInfoRecord(new ChromInfoRecord(precursorKey, replicate, isotopeLabel, 2,
                    precursorChromInfo.getTotalAreaFragment(), precursorChromInfo.getSampleFileId()));
            }
        }
    }

    private List<? extends GeneralPrecursor> getPrecursors(GeneralMolecule generalMolecule, TargetedMSSchema schema)
    {
        if (generalMolecule instanceof Peptide)
        {
            return PrecursorManager.getPrecursorsForPeptide(generalMolecule.getId(), schema);
        }
        if (generalMolecule instanceof Molecule)
        {
            return MoleculePrecursorManager.getPrecursorsForMolecule(generalMolecule.getId(), schema);
        }
        throw new IllegalArgumentException();
    }

    private @NotNull Collection<? extends GeneralTransition> getTransitions(GeneralPrecursor generalPrecursor, TargetedMSSchema schema)
    {
        if (generalPrecursor instanceof Precursor)
        {
            return TransitionManager.getTransitionsForPrecursor(generalPrecursor.getId(), schema.getUser(), schema.getContainer());
        }
        if (generalPrecursor instanceof MoleculePrecursor)
        {
            return MoleculeTransitionManager.getTransitionsForPrecursor(
                    generalPrecursor.getId(), schema.getUser(), schema.getContainer());
        }
        throw new IllegalArgumentException();
    }

    private void addChromInfoRecord(ChromInfoRecord chromInfoRecord)
    {
        List<ChromInfoRecord> list = _chromInfoRecordsByReplicateId.get(chromInfoRecord.getReplicate().getId());
        if (list == null)
        {
            list = new ArrayList<>();
            _chromInfoRecordsByReplicateId.put(chromInfoRecord.getReplicate().getId(), list);
        }
        list.add(chromInfoRecord);
        _msLevels.add(chromInfoRecord.getMsLevel());
        _labels.add(chromInfoRecord.getLabel());
    }

    private List<ChromInfoRecord> getChromInfoRecords(Replicate replicate)
    {
        List<ChromInfoRecord> list = _chromInfoRecordsByReplicateId.get(replicate.getId());
        if (list == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    public void addFeatureData(Replicate replicate, ReplicateData replicateData, Integer msLevelFilter,
                               NormalizationMethodAreas.NormalizationFactors normalizationFactors, boolean allowTruncated)
    {
        for (ChromInfoRecord record : getChromInfoRecords(replicate))
        {
            if (record.getArea() == null)
            {
                continue;
            }
            if (msLevelFilter != null && msLevelFilter != record.getMsLevel())
            {
                continue;
            }
            if (!allowTruncated && record.isTruncated())
            {
                continue;
            }
            TransitionAreas transitionAreas = replicateData.ensureResultFileData().getTransitionAreas(record.getLabel());
            double normalizationFactor = normalizationFactors.getNormalizationFactor(record.getSampleFileId());
            double area = record.getArea() * normalizationFactor;
            transitionAreas = transitionAreas.setArea(record.getFeatureName(), area);
            replicateData.ensureResultFileData().setTransitionAreas(record.getLabel(), transitionAreas);
        }
    }

    public Collection<GeneralMoleculeChromInfo> getGeneralMoleculeChromInfos()
    {
        return new TableSelector(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(),
                new SimpleFilter(FieldKey.fromParts("GeneralMoleculeId"), _generalMolecule.getId()), null)
                .getCollection(GeneralMoleculeChromInfo.class);
    }

    private static class ChromInfoRecord
    {
        private final String _featureName;
        private final Replicate _replicate;
        private final String _label;
        private final int _msLevel;
        private final Double _area;
        private final long _sampleFileId;
        private final boolean _truncated;

        public ChromInfoRecord(String featureName, Replicate replicate, String isotopeLabel,
                               GeneralTransition transition, TransitionChromInfo transitionChromInfo)
        {
            this(featureName, replicate, isotopeLabel, transition.isPrecursorIon() ? 1 : 2, transitionChromInfo.getArea(), transitionChromInfo.getSampleFileId(), Boolean.TRUE.equals(transitionChromInfo.getTruncated()));
        }

        public ChromInfoRecord(String precursorName, Replicate replicate, String isotopeLabel, int msLevel, Double area, long sampleFileId)
        {
            this(precursorName + "_MsLevel" + msLevel, replicate, isotopeLabel, msLevel, area, sampleFileId, false);
        }

        private ChromInfoRecord(String featureName, Replicate replicate, String isotopeLabel,
                               int msLevel, Double area, long sampleFileId, boolean truncated)
        {
            _featureName = featureName;
            _replicate = replicate;
            _label = isotopeLabel;
            _msLevel = msLevel;
            _area = area;
            _sampleFileId = sampleFileId;
            _truncated = truncated;
        }

        public String getFeatureName()
        {
            return _featureName;
        }

        public Replicate getReplicate()
        {
            return _replicate;
        }

        public String getLabel()
        {
            return _label;
        }

        public int getMsLevel()
        {
            return _msLevel;
        }

        public Double getArea()
        {
            return _area;
        }

        public long getSampleFileId()
        {
            return _sampleFileId;
        }

        public boolean isTruncated()
        {
            return _truncated;
        }
    }
}
