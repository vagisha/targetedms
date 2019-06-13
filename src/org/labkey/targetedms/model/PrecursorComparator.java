/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import java.util.Comparator;

/**
 * User: vsharma
 * Date: 9/5/2014
 * Time: 4:00 PM
 */
public class PrecursorComparator implements Comparator<PrecursorComparator.Comparable>
{
    @Override
    public int compare(Comparable o1, Comparable o2)
    {
        int result = Integer.valueOf(o1.getCharge()).compareTo(o2.getCharge());
        if(result != 0)
        {
            return result;
        }

        return Integer.valueOf(o1.getIsotopeLabelId()).compareTo(o2.getIsotopeLabelId());
    }

    public interface Comparable
    {
        int getCharge();
        int getIsotopeLabelId();
    }
}
