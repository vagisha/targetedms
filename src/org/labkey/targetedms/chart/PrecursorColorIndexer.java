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
package org.labkey.targetedms.chart;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PrecursorManager;

import java.util.List;

/**
 * User: vsharma
 * Date: 7/26/2014
 * Time: 10:56 PM
 */
public class PrecursorColorIndexer
{
    private int _lightLabelId = Integer.MAX_VALUE;
    private int _minCharge = Integer.MAX_VALUE;
    private int _isotopeLabelCount;

    public PrecursorColorIndexer(int runId, User user, Container container)
    {
        this(runId, 0, user, container);
    }

    public PrecursorColorIndexer(int runId, int peptideId, User user, Container container)
    {
        List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(runId);
        for(PeptideSettings.IsotopeLabel label: labels)
        {
            _lightLabelId = Math.min(_lightLabelId, label.getId());
        }
        _isotopeLabelCount = labels.size();

        if(peptideId > 0)
        {
            List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptideId, new TargetedMSSchema(user, container));
            for (Precursor precursor : precursors)
            {
                _minCharge = Math.min(_minCharge, precursor.getCharge());
            }
        }
    }

    public void setMinCharge(int charge)
    {
        _minCharge = charge;
    }

    public int getColorIndex(int precursorId, User user, Container container)
    {
        // CONSIDER caching the colors, as they will be the same for all the replicates
        Precursor precursor = PrecursorManager.getPrecursor(container, precursorId, user);
        return getColorIndex(precursor.getIsotopeLabelId(), precursor.getCharge());
    }

    public int getColorIndex(int isotopeLabelId, int charge)
    {
        return (charge - _minCharge) * _isotopeLabelCount + (isotopeLabelId - _lightLabelId);

    }
}
