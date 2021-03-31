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
package org.labkey.targetedms.model;

import org.json.JSONObject;

public class QCMetricConfiguration
{
    private int _id;
    private String _name;
    private String _series1Label;
    private String _series1SchemaName;
    private String _series1QueryName;
    private String _series2Label;
    private String _series2SchemaName;
    private String _series2QueryName;
    private boolean _precursorScoped;
    private String _enabledQueryName;
    private String _enabledSchemaName;
    private Boolean _enabled;
    private String _traceName;
    private Double _timeValue;
    private Double _traceValue;
    private String _yAxisLabel1;
    private String _yAxisLabel2;

    public int getId()
    {
        return _id;
    }

    public void setId(int rowId)
    {
        _id = rowId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getSeries1Label()
    {
        return _series1Label;
    }

    public void setSeries1Label(String series1Label)
    {
        _series1Label = series1Label;
    }

    public String getSeries1SchemaName()
    {
        return _series1SchemaName;
    }

    public void setSeries1SchemaName(String series1SchemaName)
    {
        _series1SchemaName = series1SchemaName;
    }
    public String getSeries1QueryName()
    {
        return _series1QueryName;
    }

    public void setSeries1QueryName(String series1QueryName)
    {
        _series1QueryName = series1QueryName;
    }

    public String getSeries2Label()
    {
        return _series2Label;
    }

    public void setSeries2Label(String series2Label)
    {
        _series2Label = series2Label;
    }

    public String getSeries2SchemaName()
    {
        return _series2SchemaName;
    }

    public void setSeries2SchemaName(String series2SchemaName)
    {
        _series2SchemaName = series2SchemaName;
    }

    public String getSeries2QueryName()
    {
        return _series2QueryName;
    }

    public void setSeries2QueryName(String series2QueryName)
    {
        _series2QueryName = series2QueryName;
    }

    public boolean isPrecursorScoped()
    {
        return _precursorScoped;
    }

    public void setPrecursorScoped(boolean precursorScoped)
    {
        _precursorScoped = precursorScoped;
    }

    public String getEnabledQueryName()
    {
        return _enabledQueryName;
    }

    public void setEnabledQueryName(String enabledQueryName)
    {
        _enabledQueryName = enabledQueryName;
    }

    public Boolean getEnabled()
    {
        return _enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        _enabled = enabled;
    }

    public String getEnabledSchemaName()
    {
        return _enabledSchemaName;
    }

    public void setEnabledSchemaName(String enabledSchemaName)
    {
        _enabledSchemaName = enabledSchemaName;
    }

    public String getTraceName()
    {
        return _traceName;
    }

    public void setTraceName(String traceName)
    {
        _traceName = traceName;
    }

    public Double getTimeValue()
    {
        return _timeValue;
    }

    public void setTimeValue(Double timeValue)
    {
        _timeValue = timeValue;
    }

    public Double getTraceValue()
    {
        return _traceValue;
    }

    public void setTraceValue(Double traceValue)
    {
        _traceValue = traceValue;
    }

    public String getyAxisLabel1()
    {
        return _yAxisLabel1;
    }

    public void setyAxisLabel1(String yAxisLabel1)
    {
        _yAxisLabel1 = yAxisLabel1;
    }

    public String getyAxisLabel2()
    {
        return _yAxisLabel2;
    }

    public void setyAxisLabel2(String yAxisLabel2)
    {
        _yAxisLabel2 = yAxisLabel2;
    }

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", _id);
        jsonObject.put("name", _name);
        jsonObject.put("series1Label",      _series1Label);
        jsonObject.put("series1SchemaName", _series1SchemaName);
        jsonObject.put("series1QueryName",  _series1QueryName);
        jsonObject.put("precursorScoped",  _precursorScoped);
        if (_series2Label != null) {
            jsonObject.put("series2Label",      _series2Label);
        }
        if (_series2SchemaName != null) {
            jsonObject.put("series2SchemaName", _series2SchemaName);
        }
        if (_series2QueryName != null) {
            jsonObject.put("series2QueryName",  _series2QueryName);
        }
        if (_enabledQueryName != null) {
            jsonObject.put("enabledQueryName", _enabledQueryName);
        }
        if (_enabledQueryName != null) {
            jsonObject.put("enabledSchemaName", _enabledSchemaName);
        }
        if (_traceName != null) {
            jsonObject.put("traceName", _traceName);
        }
        if (_traceValue != null) {
            jsonObject.put("traceValue", _traceValue);
        }
        if (_timeValue != null) {
            jsonObject.put("timeValue", _timeValue);
        }
        if (_yAxisLabel1 != null) {
            jsonObject.put("yAxisLabel1", _yAxisLabel1);
        }
        if (_yAxisLabel2 != null) {
            jsonObject.put("yAxisLabel2", _yAxisLabel2);
        }

        return jsonObject;
    }
}
