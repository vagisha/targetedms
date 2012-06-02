/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.labkey.api.data.Table;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.labkey.targetedms.TargetedMSManager.getSchema;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:26 PM
 */
public class ModificationManager
{
    private ModificationManager() {}

    public static Map<Integer, Double> getPeptideStructuralMods(int peptideId)
    {
        Map<Integer, Double> strModIndexMassDiff = new HashMap<Integer, Double>();
        String sql = "SELECT IndexAa, MassDiff "+
                     "FROM "+ TargetedMSManager.getTableInfoPeptideStructuralModification()+" "+
                     "WHERE PeptideId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId);
        Table.TableResultSet rs = null;
        try
        {
            rs = Table.executeQuery(getSchema(), sf);
            while(rs.next())
            {
                int index = rs.getInt("IndexAa");
                double massDiff = rs.getDouble("MassDiff");

                Double diffAtIndex = strModIndexMassDiff.get(index);
                if(diffAtIndex != null)
                {
                    massDiff += diffAtIndex;
                }
                strModIndexMassDiff.put(index, massDiff);
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
        }
        return strModIndexMassDiff;
    }

    public static Map<Integer, Double> getPeptideIsotopeMods(int peptideId, int isotopeLabelId)
    {
        Map<Integer, Double> isotopeModIndexMassDiff = new HashMap<Integer, Double>();
        String sql = "SELECT pm.IndexAa, pm.MassDiff "+
                     "FROM "+
                     TargetedMSManager.getTableInfoPeptideIsotopeModification()+" AS pm, "+
                     TargetedMSManager.getTableInfoRunIsotopeModification()+" AS m "+
                     "WHERE pm.IsotopeModId=m.IsotopeModId "+
                     "AND pm.PeptideId=? "+
                     "AND m.IsotopeLabelId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId, isotopeLabelId);
        Table.TableResultSet rs = null;
        try
        {
            rs = Table.executeQuery(getSchema(), sf);
            while(rs.next())
            {
                int index = rs.getInt("IndexAa");
                double massDiff = rs.getDouble("MassDiff");

                Double diffAtIndex = isotopeModIndexMassDiff.get(index);
                if(diffAtIndex != null)
                {
                    massDiff += diffAtIndex;
                }
                isotopeModIndexMassDiff.put(index, massDiff);
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
        }
        return isotopeModIndexMassDiff;
    }
}
