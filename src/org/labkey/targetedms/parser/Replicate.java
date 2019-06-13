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
package org.labkey.targetedms.parser;

import java.util.List;

/**
 * User: jeckels
 * Date: Apr 18, 2012
 */
public class Replicate extends AnnotatedEntity<ReplicateAnnotation>
{
    private int _runId;
    private String _name;

    private Integer _cePredictorId;
    private Integer _dpPredictorId;
    private String _sampleType;
    private Double _analyteConcentration;
    private Double _sampleDilutionFactor;

    private List<SampleFile> _sampleFileList;


    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
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

    public List<SampleFile> getSampleFileList()
    {
        return _sampleFileList;
    }

    public void setSampleFileList(List<SampleFile> sampleFileList)
    {
        _sampleFileList = sampleFileList;
    }

    public String getSampleType()
    {
        return _sampleType;
    }

    public void setSampleType(String sampleType)
    {
        _sampleType = sampleType;
    }

    public Double getAnalyteConcentration()
    {
        return _analyteConcentration;
    }

    public void setAnalyteConcentration(Double analyteConcentration)
    {
        _analyteConcentration = analyteConcentration;
    }

    public Double getSampleDilutionFactor()
    {
        return _sampleDilutionFactor;
    }

    public void setSampleDilutionFactor(Double sampleDilutionFactor)
    {
        _sampleDilutionFactor = sampleDilutionFactor;
    }
}
