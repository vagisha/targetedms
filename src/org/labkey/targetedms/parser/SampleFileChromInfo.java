/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.targetedms.TargetedMSRun;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:39 PM
 */
public class SampleFileChromInfo extends AbstractChromInfo
{
    private Float _startTime;
    private Float _endTime;
    private String _textId;

    public SampleFileChromInfo()
    {
    }

    public SampleFileChromInfo(Container c)
    {
        super(c);
    }



    /** Always load from disk */
    @Nullable
    @Override
    public Chromatogram createChromatogram(TargetedMSRun run)
    {
        return createChromatogram(run, true);
    }

    @Nullable
    @Override
    protected byte[] getChromatogram()
    {
        // We've never stored this type of chromatogram in the DB
        return null;
    }

    @Override
    public int getNumTransitions()
    {
        // We don't have transitions, and all chromatograms of this type should be of a format that doesn't need this
        return -1;
    }

    public Float getStartTime()
    {
        return _startTime;
    }

    public void setStartTime(Float startTime)
    {
        _startTime = startTime;
    }

    public Float getEndTime()
    {
        return _endTime;
    }

    public void setEndTime(Float endTime)
    {
        _endTime = endTime;
    }

    public String getTextId()
    {
        return _textId;
    }

    public void setTextId(String textId)
    {
        _textId = textId;
    }
}
