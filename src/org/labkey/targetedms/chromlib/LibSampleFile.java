/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import java.util.Date;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 12:06 PM
 */
public class LibSampleFile implements ObjectWithId
{
    private int _id;
    private String _filePath;
    private String _sampleName;
    private Date _acquiredTime;
    private Date _modifiedTime;
    private String _instrumentIonizationType;
    private String _instrumentAnalyzer;
    private String _instrumentDetector;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public String getFilePath()
    {
        return _filePath;
    }

    public void setFilePath(String filePath)
    {
        _filePath = filePath;
    }

    public String getSampleName()
    {
        return _sampleName;
    }

    public void setSampleName(String sampleName)
    {
        _sampleName = sampleName;
    }

    public Date getAcquiredTime()
    {
        return _acquiredTime;
    }

    public void setAcquiredTime(Date acquiredTime)
    {
        _acquiredTime = acquiredTime;
    }

    public Date getModifiedTime()
    {
        return _modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime)
    {
        _modifiedTime = modifiedTime;
    }

    public String getInstrumentIonizationType()
    {
        return _instrumentIonizationType;
    }

    public void setInstrumentIonizationType(String instrumentIonizationType)
    {
        _instrumentIonizationType = instrumentIonizationType;
    }

    public String getInstrumentAnalyzer()
    {
        return _instrumentAnalyzer;
    }

    public void setInstrumentAnalyzer(String instrumentAnalyzer)
    {
        _instrumentAnalyzer = instrumentAnalyzer;
    }

    public String getInstrumentDetector()
    {
        return _instrumentDetector;
    }

    public void setInstrumentDetector(String instrumentDetector)
    {
        _instrumentDetector = instrumentDetector;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LibSampleFile that = (LibSampleFile) o;

        if (_acquiredTime != null ? !_acquiredTime.equals(that._acquiredTime) : that._acquiredTime != null) return false;
        if (!_filePath.equals(that._filePath)) return false;
        if (_instrumentAnalyzer != null ? !_instrumentAnalyzer.equals(that._instrumentAnalyzer) : that._instrumentAnalyzer != null)
            return false;
        if (_instrumentDetector != null ? !_instrumentDetector.equals(that._instrumentDetector) : that._instrumentDetector != null)
            return false;
        if (_instrumentIonizationType != null ? !_instrumentIonizationType.equals(that._instrumentIonizationType) : that._instrumentIonizationType != null)
            return false;
        if (_modifiedTime != null ? !_modifiedTime.equals(that._modifiedTime) : that._modifiedTime != null) return false;
        if (!_sampleName.equals(that._sampleName)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _filePath.hashCode();
        result = 31 * result + _sampleName.hashCode();
        result = 31 * result + (_acquiredTime != null ? _acquiredTime.hashCode() : 0);
        result = 31 * result + (_modifiedTime != null ? _modifiedTime.hashCode() : 0);
        result = 31 * result + (_instrumentIonizationType != null ? _instrumentIonizationType.hashCode() : 0);
        result = 31 * result + (_instrumentAnalyzer != null ? _instrumentAnalyzer.hashCode() : 0);
        result = 31 * result + (_instrumentDetector != null ? _instrumentDetector.hashCode() : 0);
        return result;
    }
}
