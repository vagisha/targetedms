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
package org.labkey.targetedms.conflict;

import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.Transition;

/**
 * User: vsharma
 * Date: Date: 11/25/12
 * Time: 2:34 PM
 */
public class ConflictTransition
{
    private Precursor _newPrecursor;
    private Transition _newTransition;
    private int _newTransitionRank = Integer.MAX_VALUE;
    private Precursor _oldPrecursor;
    private Transition _oldTransition;
    private int _oldTransitionRank = Integer.MAX_VALUE;

    public Precursor getNewPrecursor()
    {
        return _newPrecursor;
    }

    public void setNewPrecursor(Precursor newPrecursor)
    {
        _newPrecursor = newPrecursor;
    }

    public Transition getNewTransition()
    {
        return _newTransition;
    }

    public void setNewTransition(Transition newTransition)
    {
        _newTransition = newTransition;
    }

    public int getNewTransitionRank()
    {
        return _newTransitionRank;
    }

    public void setNewTransitionRank(int newTransitionRank)
    {
        _newTransitionRank = newTransitionRank;
    }

    public Precursor getOldPrecursor()
    {
        return _oldPrecursor;
    }

    public void setOldPrecursor(Precursor oldPrecursor)
    {
        _oldPrecursor = oldPrecursor;
    }

    public Transition getOldTransition()
    {
        return _oldTransition;
    }

    public void setOldTransition(Transition oldTransition)
    {
        _oldTransition = oldTransition;
    }

    public int getOldTransitionRank()
    {
        return _oldTransitionRank;
    }

    public void setOldTransitionRank(int oldTransitionRank)
    {
        _oldTransitionRank = oldTransitionRank;
    }
}
