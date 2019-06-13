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

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", _id);
        jsonObject.put("name", _name);
        jsonObject.put("series1Label",      _series1Label);
        jsonObject.put("series1SchemaName", _series1SchemaName);
        jsonObject.put("series1QueryName",  _series1QueryName);
        if(_series2Label != null){
            jsonObject.put("series2Label",      _series2Label);
        }
        if(_series2SchemaName != null){
            jsonObject.put("series2SchemaName", _series2SchemaName);
        }
        if(_series2QueryName != null){
            jsonObject.put("series2QueryName",  _series2QueryName);
        }
        return jsonObject;
    }
}
