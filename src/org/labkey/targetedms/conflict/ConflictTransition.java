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

import org.labkey.targetedms.parser.GeneralPrecursor;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.Transition;

/**
 * User: vsharma
 * Date: Date: 11/25/12
 * Time: 2:34 PM
 */
public class ConflictTransition
{
    private GeneralPrecursor<?> _newPrecursor;
    private GeneralTransition _newTransition;
    private int _newTransitionRank = Integer.MAX_VALUE;
    private GeneralPrecursor<?> _oldPrecursor;
    private GeneralTransition _oldTransition;
    private int _oldTransitionRank = Integer.MAX_VALUE;

    public GeneralPrecursor<?> getNewPrecursor()
    {
        return _newPrecursor;
    }

    public void setNewPrecursor(GeneralPrecursor<?> newPrecursor)
    {
        _newPrecursor = newPrecursor;
    }

    public GeneralTransition getNewTransition()
    {
        return _newTransition;
    }

    public void setNewTransition(GeneralTransition newTransition)
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

    public GeneralPrecursor getOldPrecursor()
    {
        return _oldPrecursor;
    }

    public void setOldPrecursor(GeneralPrecursor<?> oldPrecursor)
    {
        _oldPrecursor = oldPrecursor;
    }

    public GeneralTransition getOldTransition()
    {
        return _oldTransition;
    }

    public void setOldTransition(GeneralTransition oldTransition)
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
