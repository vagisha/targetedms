/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

package org.labkey.targetedms.query;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.PeptideSettings;

import java.util.List;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 3:22 PM
 */
public class IsotopeLabelManager
{
    private IsotopeLabelManager() {}

    public static List<PeptideSettings.IsotopeLabel> getIsotopeLabels(int runId)
    {
        // TODO: Cache isotope label information for a run
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), runId);

        return new TableSelector(TargetedMSManager.getTableInfoIsotopeLabel(), filter, new Sort("Id")).getArrayList(PeptideSettings.IsotopeLabel.class);
    }

    public static int getLightIsotopeLabelId(int peptideId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT MIN(label.Id) ");
        sql.append("FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(", ");
        sql.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label").append(" ");
        sql.append("WHERE ");
        sql.append("label.RunId=pg.RunId ");
        sql.append("AND ");
        sql.append("pg.Id=gm.PeptideGroupId ");
        sql.append("AND ");
        sql.append("gm.Id=?");

        sql.add(peptideId);

        Integer ltIsotopeLabelId = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);

        if (ltIsotopeLabelId == null)
        {
            throw new NotFoundException("No light isotope label found for peptide: "+peptideId);
        }
        return ltIsotopeLabelId;
    }

    public static PeptideSettings.IsotopeLabel getIsotopeLabel(int isotopeLabelId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoIsotopeLabel()).getObject(isotopeLabelId, PeptideSettings.IsotopeLabel.class);
    }
}
