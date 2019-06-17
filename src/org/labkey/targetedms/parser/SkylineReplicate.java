/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
 * User: vsharma
 * Date: 2/3/2016
 * Time: 9:41 AM
 */
public class SkylineReplicate extends AnnotatedEntity<ReplicateAnnotation>
{
    private String _name;
    private TransitionSettings.Predictor _cePredictor;
    private TransitionSettings.Predictor _dpPredictor;
    private List<SampleFile> _sampleFileList;
    private String sampleType;
    private Double analyteConcentration;
    private Double sampleDilutionFactor;

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public TransitionSettings.Predictor getCePredictor()
    {
        return _cePredictor;
    }

    public void setCePredictor(TransitionSettings.Predictor cePredictor)
    {
        _cePredictor = cePredictor;
    }

    public TransitionSettings.Predictor getDpPredictor()
    {
        return _dpPredictor;
    }

    public void setDpPredictor(TransitionSettings.Predictor dpPredictor)
    {
        _dpPredictor = dpPredictor;
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
        return sampleType;
    }

    public void setSampleType(String sampleType)
    {
        this.sampleType = sampleType;
    }

    public Double getAnalyteConcentration()
    {
        return analyteConcentration;
    }

    public void setAnalyteConcentration(Double analyteConcentration)
    {
        this.analyteConcentration = analyteConcentration;
    }

    public Double getSampleDilutionFactor()
    {
        return sampleDilutionFactor;
    }

    public void setSampleDilutionFactor(Double sampleDilutionFactor)
    {
        this.sampleDilutionFactor = sampleDilutionFactor;
    }
}

