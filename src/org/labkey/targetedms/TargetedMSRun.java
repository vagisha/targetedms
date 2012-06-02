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

package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.MemTracker;

import java.io.Serializable;
import java.util.Date;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class TargetedMSRun implements Serializable
{
    protected int _runId;
    protected Container _container;
    protected String _description;
    protected String _path;
    protected String _fileName;
    protected String _status;
    protected Date _loaded;
    protected int _statusId;
    protected boolean _deleted;
    protected String _experimentRunLSID;

    protected int _peptideGroupCount;
    protected int _peptideCount;
    protected int _precursorCount;
    protected int _transitionCount;

    public TargetedMSRun()
    {
        assert MemTracker.put(this);
    }

    public String toString()
    {
        return getRunId() + " " + getDescription() + " " + getFileName();
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


    public String getPath()
    {
        return _path;
    }


    public void setPath(String path)
    {
        _path = path;
    }


    public String getFileName()
    {
        return _fileName;
    }

    public String getBaseName()
    {
        if(_fileName != null)
        {
            return FileUtil.getBaseName(_fileName);
        }
        return null;
    }


    public void setFileName(String fileName)
    {
        _fileName = fileName;
    }

    public Date getLoaded()
    {
        return _loaded;
    }


    public void setLoaded(Date loaded)
    {
        _loaded = loaded;
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
     * Do not use this directly to delete a run - use TargetedMSManager.markAsDeleted
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
}
