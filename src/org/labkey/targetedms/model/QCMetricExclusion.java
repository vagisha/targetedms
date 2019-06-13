/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

public class QCMetricExclusion
{
    private Integer _replicateId;
    private Integer _metricId;

    public QCMetricExclusion()
    {}

    public QCMetricExclusion(Integer replicateId, Integer metricId)
    {
        _replicateId = replicateId;
        _metricId = metricId;
    }

    public Integer getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(Integer replicateId)
    {
        _replicateId = replicateId;
    }

    public Integer getMetricId()
    {
        return _metricId;
    }

    public void setMetricId(Integer metricId)
    {
        _metricId = metricId;
    }
}
