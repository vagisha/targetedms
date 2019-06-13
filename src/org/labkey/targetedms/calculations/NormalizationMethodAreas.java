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

import org.labkey.targetedms.calculations.quantification.NormalizationMethod;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.StandardType;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the areas for a particular run which are used for either "Ratio to Global Standards" normalization,
 * or "Ratio to Surrogate XXX".
 *
 * @see StandardType
 */
public class NormalizationMethodAreas
{
    private TargetedMSRun _run;
    private User _user;
    private Container _container;
    private ReplicateDataSet _replicateDataSet;
    private Collection<StandardEntry> _standardEntries;
    private Map<NormalizationMethod, List<GeneralMoleculeResultDataSet>> _moleculeDataSets = new HashMap<>();

    public NormalizationMethodAreas(TargetedMSRun run, User user, Container container, ReplicateDataSet replicateDataSet)
    {
        _user = user;
        _container = container;
        _run = run;
        _replicateDataSet = replicateDataSet;
    }

    public NormalizationFactors getNormalizationFactors(NormalizationMethod normalizationMethod)
    {
        return new NormalizationFactors()
        {
            @Override
            public double getNormalizationFactor(int sampleFileId)
            {
                return 1.0 / getAreaForNormalization(normalizationMethod, sampleFileId);
            }
        };
    }

    public boolean hasNormalizationFactors(NormalizationMethod normalizationMethod)
    {
        return NormalizationMethod.GLOBAL_STANDARDS.equals(normalizationMethod)
                || normalizationMethod instanceof NormalizationMethod.RatioToSurrogate;
    }

    public double getAreaForNormalization(NormalizationMethod normalizationMethod, int sampleFileId)
    {
        if (!hasNormalizationFactors(normalizationMethod))
        {
            return 1;
        }
        if (_standardEntries == null)
        {
            _standardEntries = listStandardEntries();
        }
        PeptideSettings.IsotopeLabel isotopeLabel = null;
        if (normalizationMethod instanceof NormalizationMethod.RatioToSurrogate)
        {
            String isotopeLabelName = ((NormalizationMethod.RatioToSurrogate) normalizationMethod).getIsotopeLabelName();
            if (isotopeLabelName != null)
            {
                Optional<PeptideSettings.IsotopeLabel> isotopeLabelOptional
                        = _replicateDataSet.listIsotopeLabels().stream().filter(label -> isotopeLabelName.equals(label.getName())).findFirst();
                if (!isotopeLabelOptional.isPresent())
                {
                    return 0;
                }
                isotopeLabel = isotopeLabelOptional.get();
            }
        }
        double total = 0;
        for (GeneralMoleculeResultDataSet generalMoleculeResultDataSet : getMoleculeDataSets(normalizationMethod))
        {
            total += generalMoleculeResultDataSet.getTotalArea(sampleFileId, isotopeLabel);
        }
        return total;
    }

    private List<GeneralMoleculeResultDataSet> getMoleculeDataSets(NormalizationMethod normalizationMethod)
    {
        NormalizationMethod.RatioToSurrogate ratioToSurrogate = null;
        if (normalizationMethod instanceof NormalizationMethod.RatioToSurrogate)
        {
            ratioToSurrogate = (NormalizationMethod.RatioToSurrogate) normalizationMethod;
        }
        List<GeneralMoleculeResultDataSet> dataSets = _moleculeDataSets.get(normalizationMethod);
        if (dataSets != null)
        {
            return dataSets;
        }
        dataSets = new ArrayList<>();
        if (_standardEntries == null)
        {
            _standardEntries = listStandardEntries();
        }
        for (StandardEntry standardEntry : _standardEntries)
        {
            StandardType standardType = StandardType.parse(standardEntry.getStandardType());
            if (ratioToSurrogate != null)
            {
                if (standardType != StandardType.SurrogateStandard)
                {
                    continue;
                }
                if (!ratioToSurrogate.getSurrogateName().equals(standardEntry.getName()))
                {
                    continue;
                }
            }
            else if (normalizationMethod.equals(NormalizationMethod.GLOBAL_STANDARDS))
            {
                if (standardType != StandardType.Normalization)
                {
                    continue;
                }
            }
            GeneralMolecule generalMolecule;
            if (standardEntry.getPeptideId() != null)
            {
                generalMolecule = PeptideManager.getPeptide(_container, standardEntry.getPeptideId());
            }
            else
            {
                generalMolecule = MoleculeManager.getMolecule(_container, standardEntry.getMoleculeId());
            }
            dataSets.add(new GeneralMoleculeResultDataSet(_user, _container, _replicateDataSet, generalMolecule));
        }
        _moleculeDataSets.put(normalizationMethod, dataSets);
        return dataSets;
    }

    private Collection<StandardEntry> listStandardEntries()
    {
        SQLFragment sql = new SQLFragment("SELECT gm.StandardType, p.Id AS PeptideId, m.Id AS MoleculeId, " +
                "COALESCE(p.PeptideModifiedSequence, m.CustomIonName, m.IonFormula) AS Name\n" +
                "FROM targetedms.GeneralMolecule gm " +
                "LEFT JOIN targetedms.Peptide p ON gm.Id = p.Id " +
                "LEFT JOIN targetedms.Molecule m ON gm.Id = m.Id\n" +
                "WHERE gm.PeptideGroupId IN (SELECT Id FROM targetedms.peptidegroup WHERE runid = ?)", _run.getId());
        return new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(StandardEntry.class);
    }

    public static class StandardEntry
    {
        private String _standardType;
        private Integer _peptideId;
        private Integer _moleculeId;
        private String _name;

        public String getStandardType()
        {
            return _standardType;
        }

        public void setStandardType(String standardType)
        {
            _standardType = standardType;
        }

        public Integer getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(Integer peptideId)
        {
            _peptideId = peptideId;
        }

        public Integer getMoleculeId()
        {
            return _moleculeId;
        }

        public void setMoleculeId(Integer moleculeId)
        {
            _moleculeId = moleculeId;
        }

        /**
         * The name of the molecule or peptide.  For peptides, this is the PeptideModifiedSequence.
         * For custom molecules, this is the CustomIonName, or, if that is null, then the IonFormula.
         */
        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }
    }

    public interface NormalizationFactors
    {
        double getNormalizationFactor(int sampleFileId);
    }
}
