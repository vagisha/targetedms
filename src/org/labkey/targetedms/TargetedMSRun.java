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

package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.util.GUID;
import org.labkey.api.util.MemTracker;

import java.io.Serializable;
import java.util.Date;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSRun implements Serializable, ITargetedMSRun
{
    /** Don't change the ordering of these enum values without updating the values in targetedms.runs.representativedatastate */
    public enum RepresentativeDataState
    {
        NotRepresentative(""),
        Representative_Protein("R - Protein"),
        Representative_Peptide("R - Peptide");

        private String _label;

        private RepresentativeDataState(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }
    }

    protected int _runId;
    protected Container _container;
    protected String _description;
    protected String _fileName;
    protected String _status;
    private Date _created;
    private int _createdBy;
    private Date _modified;
    private int _modifiedBy;
    protected int _statusId;
    protected boolean _deleted;
    protected String _experimentRunLSID;

    protected RepresentativeDataState _representativeDataState = RepresentativeDataState.NotRepresentative;

    protected int _peptideGroupCount;
    protected int _peptideCount;
    protected int _smallMoleculeCount;
    protected int _precursorCount;
    protected int _transitionCount;
    protected int _replicateCount;
    protected int _calibrationCurveCount;

    protected Integer _dataId; // FK to exp.data's RowId column for the .sky file
    protected Integer _skydDataId; // FK to exp.data's RowId column for the .skyd file
    protected Integer _iRTscaleId;

    private String _softwareVersion;
    private String _formatVersion;
    private GUID _documentGUID;

    private Long _documentSize;

    public TargetedMSRun()
    {
        MemTracker.getInstance().put(this);
    }

    public String toString()
    {
        return getRunId() + " " + getDescription() + " " + getDescription();
    }

    public void setExperimentRunLSID(String experimentRunLSID)
    {
        _experimentRunLSID = experimentRunLSID;
    }

    public String getExperimentRunLSID()
    {
        return _experimentRunLSID;
    }

    public int getId() {
        return getRunId();
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setId(int runId) {
        setRunId(runId);
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public Container getContainer()
    {
        return _container;
    }


    public void setContainer(Container container)
    {
        _container = container;
    }


    public String getDescription()
    {
        return _description;
    }


    public void setDescription(String description)
    {
        _description = description;
    }

	public String getFileName()
    {
        return _fileName;
    }

    public String getBaseName()
    {
        if(_fileName != null)
        {
            return SkylineFileUtils.getBaseName(_fileName);
        }
        return null;
    }


    public void setFileName(String fileName)
    {
        _fileName = fileName;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public int getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(int createdBy)
    {
        _createdBy = createdBy;
    }

    public Date getModified()
    {
        return _modified;
    }

    public void setModified(Date modified)
    {
        _modified = modified;
    }

    public int getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(int modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public String getStatus()
    {
        return _status;
    }


    public void setStatus(String status)
    {
        _status = status;
    }


    public int getStatusId()
    {
        return _statusId;
    }


    public void setStatusId(int statusId)
    {
        _statusId = statusId;
    }

    public boolean isDeleted()
    {
        return _deleted;
    }

    public boolean isZipFile()
    {
       return _fileName.toLowerCase().endsWith(".zip");
    }
    /**
     * Do not use this directly to delete a run - use TargetedMSManager.deleteRuns
     */
    public void setDeleted(boolean deleted)
    {
        _deleted = deleted;
    }

    public int getPeptideGroupCount()
    {
        return _peptideGroupCount;
    }

    public void setPeptideGroupCount(int peptideGroupCount)
    {
        _peptideGroupCount = peptideGroupCount;
    }

    public int getPeptideCount()
    {
        return _peptideCount;
    }

    public void setPeptideCount(int peptideCount)
    {
        _peptideCount = peptideCount;
    }

    public int getSmallMoleculeCount()
    {
        return _smallMoleculeCount;
    }

    public void setSmallMoleculeCount(int smallMoleculeCount)
    {
        _smallMoleculeCount = smallMoleculeCount;
    }

    public int getPrecursorCount()
    {
        return _precursorCount;
    }

    public void setPrecursorCount(int precursorCount)
    {
        _precursorCount = precursorCount;
    }

    public int getTransitionCount()
    {
        return _transitionCount;
    }

    public void setTransitionCount(int transitionCount)
    {
        _transitionCount = transitionCount;
    }

    public void setReplicateCount(int replicateCount)
    {
        _replicateCount = replicateCount;
    }

    public int getReplicateCount()
    {
        return _replicateCount;
    }

    public int getCalibrationCurveCount()
    {
        return _calibrationCurveCount;
    }

    public void setCalibrationCurveCount(int calibrationCurveCount)
    {
        _calibrationCurveCount = calibrationCurveCount;
    }

    public RepresentativeDataState getRepresentativeDataState()
    {
        return _representativeDataState;
    }

    public void setRepresentativeDataState(RepresentativeDataState representativeDataState)
    {
        _representativeDataState = representativeDataState;
    }

    public boolean isRepresentative()
    {
        return _representativeDataState == RepresentativeDataState.Representative_Protein ||
               _representativeDataState == RepresentativeDataState.Representative_Peptide;
    }

    public Integer getDataId()
    {
        return _dataId;
    }

    public void setDataId(Integer dataId)
    {
        _dataId = dataId;
    }

    public Integer getSkydDataId()
    {
        return _skydDataId;
    }

    public void setSkydDataId(Integer dataId)
    {
        _skydDataId = dataId;
    }

    public Integer getiRTscaleId()
    {
        return _iRTscaleId;
    }

    public void setiRTscaleId(Integer iRTscaleId)
    {
        _iRTscaleId = iRTscaleId;
    }

    public String getSoftwareVersion()
    {
        return _softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion)
    {
        _softwareVersion = softwareVersion;
    }

    public String getFormatVersion()
    {
        return _formatVersion;
    }

    public void setFormatVersion(String formatVersion)
    {
        _formatVersion = formatVersion;
    }

    public GUID getDocumentGUID()
    {
        return _documentGUID;
    }

    public void setDocumentGUID(GUID documentGUID)
    {
        _documentGUID = documentGUID;
    }

    public Long getDocumentSize()
    {
        return _documentSize;
    }

    public void setDocumentSize(Long documentSize)
    {
        _documentSize = documentSize;
    }
}
