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
    private String _description;

    private String _sequence;
    private boolean _protein;
    private Map<String, String> _alternativeProteins;

    private boolean _decoy;

    private List<Peptide> _peptideList;
    private String _note;

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

    public List<Peptide> getPeptideList()
    {
        return _peptideList;
    }

    public void setPeptideList(List<Peptide> peptideList)
    {
        _peptideList = peptideList;
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

}
