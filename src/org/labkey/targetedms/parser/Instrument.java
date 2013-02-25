/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

/**
 * User: vsharma
 * Date: 4/18/12
 * Time: 3:45 PM
 */
public class Instrument
{
    private int id;
    private int runId;

    private String model;
    private String ionizationType;
    private String analyzer;
    private String detector;

    public int getRunId()
    {
        return runId;
    }

    public void setRunId(int runId)
    {
        this.runId = runId;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getIonizationType()
    {
        return ionizationType;
    }

    public void setIonizationType(String ionizationType)
    {
        this.ionizationType = ionizationType;
    }

    public String getAnalyzer()
    {
        return analyzer;
    }

    public void setAnalyzer(String analyzer)
    {
        this.analyzer = analyzer;
    }

    public String getDetector()
    {
        return detector;
    }

    public void setDetector(String detector)
    {
        this.detector = detector;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Instrument)) return false;

        Instrument that = (Instrument) o;

        if (analyzer != null ? !analyzer.equals(that.analyzer) : that.analyzer != null) return false;
        if (detector != null ? !detector.equals(that.detector) : that.detector != null) return false;
        if (ionizationType != null ? !ionizationType.equals(that.ionizationType) : that.ionizationType != null)
            return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = model != null ? model.hashCode() : 0;
        result = 31 * result + (ionizationType != null ? ionizationType.hashCode() : 0);
        result = 31 * result + (analyzer != null ? analyzer.hashCode() : 0);
        result = 31 * result + (detector != null ? detector.hashCode() : 0);
        return result;
    }
}
