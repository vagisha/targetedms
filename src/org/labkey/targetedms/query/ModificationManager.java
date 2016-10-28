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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.targetedms.TargetedMSManager.getSchema;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:26 PM
 */
public class ModificationManager
{
    private static int CACHE_SIZE = 10; // Cache results for upto 10 runs.
    private static PeptideStrModIndexes _peptideStrModIndexes = new PeptideStrModIndexes();
    private static PeptideIsotopeModIndexes _peptideIsotopeModIndexes = new PeptideIsotopeModIndexes();

    private ModificationManager() {}

    /**
     * @param peptideId
     * @return Map of the modified amino acid index in the peptide sequence
     *         and the mass of the modification.
     *         ModifiedIndex -> MassDiff
     */
    public static Map<Integer, Double> getPeptideStructuralModsMap(int peptideId)
    {
        final Map<Integer, Double> strModIndexMassDiff = new HashMap<>();
        String sql = "SELECT IndexAa, MassDiff "+
                     "FROM "+ TargetedMSManager.getTableInfoPeptideStructuralModification()+" "+
                     "WHERE PeptideId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId);

        new SqlSelector(getSchema(), sf).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                int index = rs.getInt("IndexAa");
                double massDiff = rs.getDouble("MassDiff");

                Double diffAtIndex = strModIndexMassDiff.get(index);
                if (diffAtIndex != null)
                {
                    massDiff += diffAtIndex;
                }
                strModIndexMassDiff.put(index, massDiff);
            }
        });

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
        final Map<Integer, Double> isotopeModIndexMassDiff = new HashMap<>();
        String sql = "SELECT pm.IndexAa, pm.MassDiff "+
                     "FROM "+
                     TargetedMSManager.getTableInfoPeptideIsotopeModification()+" AS pm, "+
                     TargetedMSManager.getTableInfoRunIsotopeModification()+" AS m "+
                     "WHERE pm.IsotopeModId=m.IsotopeModId "+
                     "AND pm.PeptideId=? "+
                     "AND m.IsotopeLabelId=?";
        SQLFragment sf = new SQLFragment(sql, peptideId, isotopeLabelId);

        new SqlSelector(getSchema(), sf).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
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
        });

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


    /**
     * @param runId
     * @return    PeptideId -> (Set of indexes where the peptide has structural modifications)
     */
    private static Map<Integer, Set<Integer>> getPeptideStructuralModIndexMap(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append(" SELECT mod.PeptideId AS peptideId, mod.IndexAA AS indexAA ");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideStructuralModification(), "mod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.id = ? ");
        sql.add(runId);
        sql.append(" AND ");
        sql.append(" pg.runId = run.id ");
        sql.append(" AND ");
        sql.append(" gm.peptideGroupId = pg.id ");
        sql.append(" AND ");
        sql.append(" mod.peptideId = gm.id ");

        final Map<Integer, Set<Integer>> peptideModIndexMap = new HashMap<>();

        new SqlSelector(getSchema(), sql).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                int peptideId = rs.getInt("peptideId");
                int index = rs.getInt("indexAa");

                Set<Integer> modIndexes = peptideModIndexMap.get(peptideId);
                if(modIndexes == null)
                {
                    modIndexes = new HashSet<>();
                    peptideModIndexMap.put(peptideId, modIndexes);
                }
                modIndexes.add(index);
            }
        });

        return Collections.unmodifiableMap(peptideModIndexMap);
    }

    /**
     * @param runId
     * @return  PeptideId/IsotopeLabelId -> Set of indexes where the PeptideId/IsotopeLabelId has a isotope modification
     */
    private static Map<Pair<Integer,Integer>, Set<Integer>> getPeptideIsotopeModIndexMap(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append(" SELECT mod.PeptideId AS peptideId, rmod.isotopeLabelId AS isotopeLabelId, mod.IndexAA AS indexAA ");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideIsotopeModification(), "mod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoRunIsotopeModification(), "rmod");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" , ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE rmod.runId = ? ");
        sql.add(runId);
        sql.append(" AND ");
        sql.append(" pg.runId = rmod.runId ");
        sql.append(" AND ");
        sql.append(" gm.peptideGroupId = pg.id ");
        sql.append(" AND ");
        sql.append(" mod.peptideId = gm.id ");
        sql.append(" AND ");
        sql.append(" mod.isotopeModId = rmod.isotopeModId ");

        final Map<Pair<Integer, Integer>, Set<Integer>> peptideModIndexMap = new HashMap<>();

        new SqlSelector(getSchema(), sql).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                Pair<Integer, Integer> key = new Pair<>(rs.getInt("peptideId"), rs.getInt("isotopeLabelId"));
                int index = rs.getInt("indexAa");

                Set<Integer> modIndexes = peptideModIndexMap.get(key);
                if(modIndexes == null)
                {
                    modIndexes = new HashSet<>();
                    peptideModIndexMap.put(key, modIndexes);
                }
                modIndexes.add(index);
            }
        });

        return Collections.unmodifiableMap(peptideModIndexMap);
    }

    public static Set<Integer> getStructuralModIndexes(int peptideId, Integer runId)
    {
        if(runId == null)
        {
            Map<Integer, Double> modMap = getPeptideStructuralModsMap(peptideId);
            return new HashSet<>(modMap.keySet());
        }
        else
        {
            Map<Integer, Set<Integer>> modIndexesForRun = _peptideStrModIndexes.get(String.valueOf(runId), null, new CacheLoader<String, Map<Integer, Set<Integer>>>()
            {
                @Override
                public Map<Integer, Set<Integer>> load(String runId, @Nullable Object argument)
                {
                     return getPeptideStructuralModIndexMap(Integer.valueOf(runId));
                }
            });

            return modIndexesForRun.get(peptideId);
        }
    }

    public static Set<Integer> getIsotopeModIndexes(int peptideId, int isotopeLabelId, Integer runId)
    {
        if(runId == null)
        {
            Map<Integer, Double> modMap = ModificationManager.getPeptideIsotopeModsMap(peptideId, isotopeLabelId);
            return new HashSet<>(modMap.keySet());
        }
        else
        {
            Map<Pair<Integer,Integer>, Set<Integer>> modIndexesForRun = _peptideIsotopeModIndexes.get(String.valueOf(runId), null,
                    new CacheLoader<String, Map<Pair<Integer,Integer>, Set<Integer>>>(){
                        @Override
                        public Map<Pair<Integer, Integer>, Set<Integer>> load(String runId, @Nullable Object argument)
                        {
                            return getPeptideIsotopeModIndexMap(Integer.valueOf(runId));
                        }
                    });

            return modIndexesForRun.get(new Pair<>(peptideId, isotopeLabelId));
        }
    }

    public static void removeRunCachedResults(List<Integer> deletedRunIds)
    {
        if(deletedRunIds == null)
            return;

        for(Integer runId: deletedRunIds)
        {
            _peptideIsotopeModIndexes.remove(String.valueOf(runId));
            _peptideStrModIndexes.remove(String.valueOf(runId));
        }
    }

    private static class PeptideIsotopeModIndexes extends DatabaseCache<Map<Pair<Integer,Integer>, Set<Integer>>>
    {
        public PeptideIsotopeModIndexes()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Peptide isotope modification indexes");
        }
    }

    private static class PeptideStrModIndexes extends DatabaseCache<Map<Integer, Set<Integer>>>
    {
        public PeptideStrModIndexes()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Peptide structural modification indexes");
        }
    }
}
