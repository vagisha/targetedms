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
import java.util.Objects;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 12:06 PM
 */
public class LibSampleFile extends AbstractLibEntity
{
    private String _filePath;
    private String _sampleName;
    private Date _acquiredTime;
    private Date _modifiedTime;
    private String _instrumentIonizationType;
    private String _instrumentAnalyzer;
    private String _instrumentDetector;
    private Integer _cePredictorId;
    private Integer _dpPredictorId;

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

    public Integer getCePredictorId()
    {
        return _cePredictorId;
    }

    public void setCePredictorId(Integer cePredictorId)
    {
        _cePredictorId = cePredictorId;
    }

    public Integer getDpPredictorId()
    {
        return _dpPredictorId;
    }

    public void setDpPredictorId(Integer dpPredictorId)
    {
        _dpPredictorId = dpPredictorId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibSampleFile that = (LibSampleFile) o;
        return Objects.equals(_filePath, that._filePath) &&
                Objects.equals(_sampleName, that._sampleName) &&
                Objects.equals(_acquiredTime, that._acquiredTime) &&
                Objects.equals(_modifiedTime, that._modifiedTime) &&
                Objects.equals(_instrumentIonizationType, that._instrumentIonizationType) &&
                Objects.equals(_instrumentAnalyzer, that._instrumentAnalyzer) &&
                Objects.equals(_instrumentDetector, that._instrumentDetector) &&
                Objects.equals(_cePredictorId, that._cePredictorId) &&
                Objects.equals(_dpPredictorId, that._dpPredictorId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_filePath, _sampleName, _acquiredTime, _modifiedTime, _instrumentIonizationType, _instrumentAnalyzer, _instrumentDetector, _cePredictorId, _dpPredictorId);
    }
}
