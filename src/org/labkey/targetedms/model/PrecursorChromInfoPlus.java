/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

package org.labkey.targetedms.model;

import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.util.Comparator;

/**
 * User: vsharma
 * Date: 5/8/12
 * Time: 4:30 PM
 */
public class PrecursorChromInfoPlus extends PrecursorChromInfo implements PrecursorComparator.Comparable
{
    private String _groupName;
    private String _sequence;
    private String _peptideModifiedSequence;
    private String _modifiedSequence;
    private int _charge;
    private String _isotopeLabel;

    private int _isotopeLabelId;

    public String getGroupName()
    {
        return _groupName;
    }

    public void setGroupName(String groupName)
    {
        _groupName = groupName;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public String getPeptideModifiedSequence()
    {
        return _peptideModifiedSequence != null ? _peptideModifiedSequence : _modifiedSequence;
    }

    public void setPeptideModifiedSequence(String peptideModifiedSequence)
    {
        _peptideModifiedSequence = peptideModifiedSequence;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public String getIsotopeLabel()
    {
        return _isotopeLabel;
    }

    public void setIsotopeLabel(String isotopeLabel)
    {
        _isotopeLabel = isotopeLabel;
    }

    public int getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(int isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }
}
