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
 * User: vsharma
 * Date: 7/24/12
 * Time: 3:00 PM
 */
public class TransitionAreaRatio extends AreaRatio
{
    private int _transitionChromInfoId;
    private int _transitionChromInfoStdId;

    public int getTransitionChromInfoId()
    {
        return _transitionChromInfoId;
    }

    public void setTransitionChromInfoId(int transitionChromInfoId)
    {
        _transitionChromInfoId = transitionChromInfoId;
    }

    public int getTransitionChromInfoStdId()
    {
        return _transitionChromInfoStdId;
    }

    public void setTransitionChromInfoStdId(int transitionChromInfoStdId)
    {
        _transitionChromInfoStdId = transitionChromInfoStdId;
    }
}
