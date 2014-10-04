/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import java.util.Map;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 2:13 PM
 */
public class PeptideGroup extends AnnotatedEntity<PeptideGroupAnnotation>
{
    private int _runId;
    private Integer _sequenceId;

    private String _label;
    private String _name;
    private String _description;
    private String _accession;
    private String _preferredName;
    private String _gene;
    private String _species;

    private String _sequence;
    private boolean _protein;
    private Map<String, String> _alternativeProteins;

    private boolean _decoy;

    private String _note;

    private String _altDescription;

    protected RepresentativeDataState _representativeDataState = RepresentativeDataState.NotRepresentative;

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public Integer getSequenceId()
    {
        return _sequenceId;
    }

    public void setSequenceId(Integer sequenceId)
    {
        _sequenceId = sequenceId;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(String label)
    {
        _label = label;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public boolean isProtein()
    {
        return _protein;
    }

    public void setProtein(boolean protein)
    {
        _protein = protein;
    }

    public Map<String, String> getAlternativeProteins()
    {
        return _alternativeProteins;
    }

    public void setAlternativeProteins(Map<String, String> alternativeProteins)
    {
        _alternativeProteins = alternativeProteins;
    }

    public boolean isDecoy()
    {
        return _decoy;
    }

    public void setDecoy(boolean decoy)
    {
        _decoy = decoy;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public RepresentativeDataState getRepresentativeDataState()
    {
        return _representativeDataState;
    }

    public void setRepresentativeDataState(RepresentativeDataState representativeDataState)
    {
        _representativeDataState = representativeDataState;
    }

    public String getAccession()
    {
        return _accession;
    }

    public void setAccession(String accession)
    {
        _accession = accession;
    }

    public String getPreferredName()
    {
        return _preferredName;
    }

    public void setPreferredName(String preferredName)
    {
        _preferredName = preferredName;
    }

    public String getGene()
    {
        return _gene;
    }

    public void setGene(String gene)
    {
        _gene = gene;
    }

    public String getSpecies()
    {
        return _species;
    }

    public void setSpecies(String species)
    {
        _species = species;
    }

    public String getAltDescription()
    {
        return _altDescription;
    }

    public void setAltDescription(String altDescription)
    {
        _altDescription = altDescription;
    }
}
