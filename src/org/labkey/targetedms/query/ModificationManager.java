/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
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

    /**
     * @param peptideId
     * @return Map of the modified amino acid index in the peptide sequence
     *         and the mass of the modification.
     *         ModifiedIndex -> MassDiff
     */
    public static Map<Integer, Double> getPeptideStructuralModsMap(int peptideId)
    {
        Map<Integer, Double> strModIndexMassDiff = new HashMap<>();
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

    /**
     * @param peptideId
     * @return Map of the modified amino acid index in the peptide sequence
     *         and the mass of the modification.
     *         ModifiedIndex -> MassDiff
     */
    public static Map<Integer, Double> getPeptideIsotopeModsMap(int peptideId, int isotopeLabelId)
    {
        Map<Integer, Double> isotopeModIndexMassDiff = new HashMap<>();
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

    public static List<PeptideSettings.RunIsotopeModification> getIsotopeModificationsForRun(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoIsotopeModification(), "mod");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "rmod");
        sql.append(" WHERE");
        sql.append(" mod.Id = rmod.IsotopeModId");
        sql.append(" AND rmod.RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideSettings.RunIsotopeModification.class);
    }

    public static List<PeptideSettings.RunStructuralModification> getStructuralModificationsForRun(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoStructuralModification(), "mod");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunStructuralModification(), "rmod");
        sql.append(" WHERE");
        sql.append(" mod.Id = rmod.StructuralModId");
        sql.append(" AND rmod.RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideSettings.RunStructuralModification.class);
    }

    public static List<PeptideSettings.PotentialLoss> getPotentialLossesForStructuralMod(int modId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoStructuralModLoss(),
                                 new SimpleFilter(FieldKey.fromParts("StructuralModId"), modId),
                                 null)
                                .getArrayList(PeptideSettings.PotentialLoss.class);
    }

    public static List<Peptide.StructuralModification> getPeptideStructuralModifications(int peptideId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPeptideStructuralModification(),
                                  new SimpleFilter(FieldKey.fromParts("PeptideId"), peptideId),
                                  null)
                                  .getArrayList(Peptide.StructuralModification.class);
    }

    public static List<Peptide.IsotopeModification> getPeptideIsotopelModifications(int peptideId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPeptideIsotopeModification(),
                                  new SimpleFilter(FieldKey.fromParts("PeptideId"), peptideId),
                                  null)
                                  .getArrayList(Peptide.IsotopeModification.class);
    }

    public static List<Peptide.IsotopeModification> getPeptideIsotopelModifications(int peptideId, int isotopeLabelId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT pi.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideIsotopeModification(), "pi");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "i");
        sql.append(" WHERE ");
        sql.append(" pi.peptideId = ?");
        sql.add(peptideId);
        sql.append(" AND i.isotopeLabelId = ?");
        sql.add(isotopeLabelId);
        sql.append(" AND pi.IsotopeModId=i.IsotopeModId");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Peptide.IsotopeModification.class);
    }

    public static PeptideSettings.ModificationSettings getSettings(int runId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoModificationSettings()).getObject(runId, PeptideSettings.ModificationSettings.class);
    }
}
