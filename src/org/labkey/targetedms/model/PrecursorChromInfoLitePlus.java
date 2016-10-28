/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

package org.labkey.targetedms.model;

import org.labkey.targetedms.parser.SkylineEntity;

/**
 * User: vsharma
 * Date: 5/8/12
 * Time: 4:30 PM
 */
public class PrecursorChromInfoLitePlus extends SkylineEntity implements PrecursorComparator.Comparable
{
    // Columns from the PrecursorChromInfo table
    private int _sampleFileId;
    private int _precursorId;
    private int _generalMoleculeChromInfoId;
    private Double _bestRetentionTime;
    private Double _minStartTime;
    private Double _maxEndTime;
    private Double _totalArea;
    private Double _maxFwhm;
    private Double _maxHeight;

    // Column from the PeptideGroup table
    private String _groupName;

    // Columns from the Peptide table
    private String _sequence;
    private String _peptideModifiedSequence;

    // Columns from the PrecursorTable
    private String _modifiedSequence;
    private int _charge;

    // Columns from the IsotopeLabel table
    private String _isotopeLabel;
    private int _isotopeLabelId;

    // Columns from the Molecule table
    private String _customIonName;

    public int getPrecursorId()
    {
        return _precursorId;
    }

    public void setPrecursorId(int precursorId)
    {
        _precursorId = precursorId;
    }

    public int getGeneralMoleculeChromInfoId()
    {
        return _generalMoleculeChromInfoId;
    }

    public void setGeneralMoleculeChromInfoId(int generalMoleculeChromInfoId)
    {
        _generalMoleculeChromInfoId = generalMoleculeChromInfoId;
    }

    public int getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(int sampleFileId)
    {
        this._sampleFileId = sampleFileId;
    }

    public Double getBestRetentionTime()
    {
        return _bestRetentionTime;
    }

    public void setBestRetentionTime(Double bestRetentionTime)
    {
        _bestRetentionTime = bestRetentionTime;
    }

    public Double getMinStartTime()
    {
        return _minStartTime;
    }

    public void setMinStartTime(Double minStartTime)
    {
        _minStartTime = minStartTime;
    }

    public Double getMaxEndTime()
    {
        return _maxEndTime;
    }

    public void setMaxEndTime(Double maxEndTime)
    {
        _maxEndTime = maxEndTime;
    }

    public Double getTotalArea()
    {
        return _totalArea;
    }

    public void setTotalArea(Double totalArea)
    {
        _totalArea = totalArea;
    }

    public Double getMaxFwhm()
    {
        return _maxFwhm;
    }

    public void setMaxFwhm(Double maxFwhm)
    {
        _maxFwhm = maxFwhm;
    }

    public Double getMaxHeight()
    {
        return _maxHeight;
    }

    public void setMaxHeight(Double maxHeight)
    {
        _maxHeight = maxHeight;
    }

    public String getGroupName()
    {
        return _groupName;
    }

    public void setGroupName(String groupName)
    {
        _groupName = groupName;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public String getPeptideModifiedSequence()
    {
        return _peptideModifiedSequence != null ? _peptideModifiedSequence : _modifiedSequence;
    }

    public void setPeptideModifiedSequence(String peptideModifiedSequence)
    {
        _peptideModifiedSequence = peptideModifiedSequence;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }

    public String getCustomIonName()
    {
        return _customIonName;
    }

    public void setCustomIonName(String customIonName)
    {
        _customIonName = customIonName;
    }
}
