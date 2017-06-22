/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.api.data.Container;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.Peptide;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:19 PM
 */
public class PeptideManager
{
    private static final int CACHE_SIZE = 10;
    private static PeptideIdsWithSpectra _peptideIdsWithSpectra = new PeptideIdsWithSpectra();

    private PeptideManager() {}

    public static Peptide getPeptide(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pep.*, gm.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("gm.PeptideGroupId = pg.Id AND pep.Id = gm.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pep.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Peptide.class);
    }

    public static GeneralMoleculeChromInfo getGeneralMoleculeChromInfo(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT gmci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE gmci.GeneralMoleculeId = gm.Id AND  ");
        sql.append("gm.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND gmci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(GeneralMoleculeChromInfo.class);
    }

    public static Collection<Peptide> getPeptidesForGroup(int peptideGroupId, TargetedMSSchema schema)
    {
        SQLFragment sql = new SQLFragment("SELECT gm.id, gm.id, gm.peptidegroupid, gm.rtcalculatorscore, gm.predictedretentiontime, ");
        sql.append("gm.avgmeasuredretentiontime, gm.note, gm.explicitretentiontime, ");
        sql.append("gm.normalizationmethod, gm.standardtype, gm.concentrationmultiplier, gm.internalstandardconcentration, ");
        sql.append("p.id, p.sequence, p.startindex, p.endindex, p.previousaa, p.nextaa, ");
        sql.append("p.calcneutralmass, p.nummissedcleavages, p.rank, p.decoy, p.peptidemodifiedsequence, ");
        sql.append("gm.standardtype FROM targetedms.generalmolecule gm, targetedms.peptide p WHERE ");
        sql.append("p.id = gm.id AND gm.peptidegroupid=?");
        sql.add(peptideGroupId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Peptide.class);
    }

    public static Double getMinRetentionTime(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT MIN(preci.MinStartTime) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" WHERE ");
        sql.append("gmci.Id=preci.GeneralMoleculeChromInfoId");
        sql.append(" AND ");
        sql.append("gmci.GeneralMoleculeId=?");
        sql.add(peptideId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static Double getMaxRetentionTime(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(preci.MaxEndTime) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" WHERE ");
        sql.append("gmci.Id=preci.GeneralMoleculeChromInfoId");
        sql.append(" AND ");
        sql.append("gmci.GeneralMoleculeId=?");
        sql.add(peptideId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static Double getMinRetentionTime(int peptideId, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT MIN(preci.MinStartTime) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" WHERE ");
        sql.append("gmci.Id=preci.GeneralMoleculeChromInfoId");
        sql.append(" AND ");
        sql.append("gmci.GeneralMoleculeId=?");
        sql.append(" AND ");
        sql.append("preci.SampleFileId = ?");
        sql.add(peptideId);
        sql.add(sampleFileId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static Double getMaxRetentionTime(int peptideId, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(preci.MaxEndTime) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" WHERE ");
        sql.append("gmci.Id=preci.GeneralMoleculeChromInfoId");
        sql.append(" AND ");
        sql.append("gmci.GeneralMoleculeId=?");
        sql.append(" AND ");
        sql.append("preci.SampleFileId = ?");
        sql.add(peptideId);
        sql.add(sampleFileId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static boolean hasSpectrumLibraryInformation(int peptideId, Integer runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
            sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursorLibInfo(), "pcilib");
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
            sql.append(" WHERE ");
            sql.append("pre.Id=pcilib.PrecursorId");
            sql.append(" AND ");
            sql.append("gp.Id=pre.Id");
            sql.append(" AND ");
            sql.append("gp.GeneralMoleculeId=?");
            sql.add(peptideId);

            Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
            return count != null ? count > 0 : false;
        }
        else
        {
            Set<Integer> peptideIds = _peptideIdsWithSpectra.get(String.valueOf(runId), null, new CacheLoader<String, Set<Integer>>() {
                @Override
                public Set<Integer> load(String runId, @Nullable Object argument)
                {
                    SQLFragment sql = new SQLFragment("SELECT DISTINCT pep.Id FROM ");
                    sql.append(TargetedMSManager.getTableInfoPrecursorLibInfo(), "pcilib");
                    sql.append(" , ");
                    sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                    sql.append(", ");
                    sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pre");
                    sql.append(", ");
                    sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "pep");
                    sql.append(" WHERE ");
                    sql.append(" pcilib.SpectrumLibraryId = specLib.Id ");
                    sql.append(" AND ");
                    sql.append(" pre.GeneralMoleculeId = pep.Id ");
                    sql.append(" AND ");
                    sql.append(" pcilib.PrecursorId = pre.Id ");
                    sql.append(" AND ");
                    sql.append(" specLib.RunId = ? ");
                    sql.add(Integer.valueOf(runId));
                    return Collections.unmodifiableSet(new HashSet<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Integer.class)));
                }
            });

            return peptideIds.contains(peptideId);
        }
    }

    public static void removeRunCachedResults(List<Integer> deletedRunIds)
    {
        for(Integer runId: deletedRunIds)
        {
            _peptideIdsWithSpectra.remove(String.valueOf(runId));
        }
    }

    private static class PeptideIdsWithSpectra extends DatabaseCache<Set<Integer>>
    {
        public PeptideIdsWithSpectra()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Peptide IDs with library spectra");
        }
    }
}
