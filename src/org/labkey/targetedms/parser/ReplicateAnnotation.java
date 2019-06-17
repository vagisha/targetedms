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

/**
 * User: jeckels
 * Date: Jun 4, 2012
 */
public class ReplicateAnnotation extends AbstractAnnotation
{
    public static final String SOURCE_SKYLINE = "Skyline";
    public static final String SOURCE_USER = "User";
    public static final String SOURCE_AUTOQC = "AutoQC";

    // This is a special annotation. If set to 'true' the replicate / sample file will
    // be excluded from guide sets and not counted towards outliers in QC folders.
    public static final String IGNORE_IN_QC = "ignore_in_QC";

    private int _replicateId;

    private String _source;

    public int getReplicateId()
    {
        return _replicateId;
    }

    public void setReplicateId(int replicateId)
    {
        _replicateId = replicateId;
    }

    public String getSource()
    {
        return _source != null ? _source : SOURCE_SKYLINE;
    }

    public void setSource(String source)
    {
        _source = source;
    }

    public boolean isIgnoreInQC()
    {
        return getName() != null && getName().equalsIgnoreCase(IGNORE_IN_QC);
    }

    public static boolean isValidSource(String source)
    {
        return SOURCE_SKYLINE.equals(source) || SOURCE_AUTOQC.equals(source) || SOURCE_USER.equals(source);
    }

    public static boolean isSourceSkyline(String source)
    {
        return SOURCE_SKYLINE.equals(source);
    }
}
