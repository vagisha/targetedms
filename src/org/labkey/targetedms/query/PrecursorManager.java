/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DatabaseCache;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableResultSet;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ChromatogramDataset.RtRange;
import org.labkey.targetedms.model.PrecursorChromInfoLitePlus;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:08 PM
 */
public class PrecursorManager
{
    private static final int CACHE_SIZE = 10; // Cache results for upto 10 runs.
    private static final PrecursorIdsWithChromatograms _precursorIdsWithChromatograms = new PrecursorIdsWithChromatograms();
    private static final PrecursorIdsWithSpectra _precursorIdsWithSpectra = new PrecursorIdsWithSpectra();


    private PrecursorManager() {}

    public static Precursor getPrecursor(Container c, long id, User user)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);

        SQLFragment sql = new SQLFragment("SELECT pre.* FROM ");
        sql.append(new PrecursorTableInfo(schema, null, true), "pre");
        sql.append(", ");
        sql.append(new PeptideTableInfo(schema, null, true), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.GeneralMoleculeId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pre.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Precursor.class);
    }

    public static PrecursorChromInfo getPrecursorChromInfo(Container c, long id)
    {
        SQLFragment sql = new SQLFragment("SELECT pci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "mol");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pci.PrecursorId = gp.Id AND gp.GeneralMoleculeId = mol.Id AND ");
        sql.append("mol.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(PrecursorChromInfo.class);
    }

    public static List<Precursor> getPrecursorsForPeptide(long peptideId, TargetedMSSchema targetedMSSchema)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PeptideId"), peptideId);

        Sort sort = new Sort("Charge, IsotopeLabelId");

        Set<String> colNames = new HashSet<>();
        colNames.addAll(TargetedMSManager.getTableInfoPrecursor().getColumnNameSet());
        colNames.addAll(TargetedMSManager.getTableInfoGeneralPrecursor().getColumnNameSet());

        return new TableSelector(new PrecursorTableInfo(targetedMSSchema, null, true), colNames, filter,  sort).getArrayList(Precursor.class);
    }

    @NotNull
    public static List<PrecursorChromInfo> getSortedPrecursorChromInfosForPrecursor(long precursorId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorId"), precursorId);

        List<PrecursorChromInfo> result = new TableSelector(TargetedMSManager.getTableInfoPrecursorChromInfo(),
                                   filter,
                                   null)
                                  .getArrayList(PrecursorChromInfo.class);
        // Do the sort in Java to avoid DB-specific null sorting behaviors
        Collections.sort(result);
        return result;
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPrecursor(long precursorId)
    {
        return getBestPrecursorChromInfoForPrecursorAndReplicate(precursorId, -1);
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPrecursorAndReplicate(long precursorId, long replicateId)
    {
        // Get a list of chrom infos sorted by peak area (descending).
        List<PrecursorChromInfo> chromInfos = getSortedPrecursorChromInfosForPrecursor(precursorId);

        // Filter based on the sample file Ids for the given replicate.
        Set<Long> sampleFileIds = getSampleFileIdsForReplicate(replicateId);
        chromInfos = chromInfos.stream().filter(pci -> sampleFileIds.isEmpty() || sampleFileIds.contains(pci.getSampleFileId())).collect(Collectors.toList());

        return getBestPrecursorChromInfo(chromInfos);
    }

    public static Set<Long> getSampleFileIdsForReplicate(long replicateId)
    {
        if(replicateId <= 0)
            return Collections.emptySet();

        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("ReplicateId"), replicateId);

        Collection<Long> result =  new TableSelector(TargetedMSManager.getTableInfoSampleFile(),
                                   Collections.singleton("Id"),
                                   filter,
                                   null)
                                  .getCollection(Long.class);
        return new HashSet<>(result);
    }

    @Nullable
    public static PrecursorChromInfo getBestPrecursorChromInfo(@NotNull List<PrecursorChromInfo> chromInfos)
    {
        if(chromInfos.size() == 0)
        {
            return null;
        }

        Collections.sort(chromInfos);

        for (PrecursorChromInfo chromInfo : chromInfos)
        {
            // Look for an entry with an area that wasn't truncated
            if (chromInfo.getTotalArea() != null && (chromInfo.getNumTruncated() == null || chromInfo.getNumTruncated().intValue() == 0))
            {
                return chromInfo;
            }
        }
        return chromInfos.get(0);
    }

    @NotNull
    public static List<PrecursorChromInfo> getPrecursorChromInfosForPeptide(long peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.*");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "prec");
        sql.append(" ON ");
        sql.append("prec.Id = pci.PrecursorId ");
        sql.append(" WHERE ");
        sql.append("prec.GeneralMoleculeId=? ");
        sql.add(peptideId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfo.class);
    }

    public static Map<String, Object> getPrecursorSummary(long precursorId)
    {
        SQLFragment sf = new SQLFragment("SELECT gp.Mz, gp.Charge, pep.Sequence, label.Name FROM ");
        sf.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label ");
        sf.append(" WHERE gp.GeneralMoleculeId = gm.Id AND pep.Id = gm.Id AND pre.Id = gp.Id AND gp.IsotopeLabelId = label.Id AND pre.Id = ? ");
        sf.add(precursorId);

        try (TableResultSet rs = new SqlSelector(TargetedMSManager.getSchema(), sf).getResultSet())
        {
            if (rs.next())
            {
                Map<String, Object> result = new HashMap<>();

                result.put("charge", rs.getInt("charge"));
                result.put("mz", rs.getDouble("Mz"));
                result.put("sequence", rs.getString("Sequence"));
                result.put("label", rs.getString("Name"));

                return result;
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }

        throw new IllegalStateException("Cannot get summary for precursor "+precursorId);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(long peptideGroupId, User user, Container container)
    {
        return getChromInfosLitePlusForPeptideGroup(peptideGroupId, 0, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(long peptideGroupId, long sampleFileId, User user, Container container)
    {
        return getPrecursorChromInfoLitePlusList(peptideGroupId, true, false, sampleFileId, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptide(long peptideId, User user, Container container)
    {
        return getPrecursorChromInfoLitePlusList(peptideId, false, false, 0, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPrecursor(long precursorId, User user, Container container)
    {
        return getPrecursorChromInfoLitePlusList(precursorId, false, true, 0, user, container);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptide(long peptideId, long sampleFileId, User user, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.* , pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql, user, container);
        sql.append(" WHERE ");
        sql.append("pep.Id=? ");
        sql.add(peptideId);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoPlus.class);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForGeneralMoleculeChromInfo(long gmChromInfoId, long precursorId,
                                                                                                 long sampleFileId, User user,
                                                                                                 Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.* , pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql, user, container);
        sql.append(" WHERE ");
        sql.append("pci.GeneralMoleculeChromInfoId=? ");
        sql.add(gmChromInfoId);
        sql.append(" AND ");
        sql.append("pci.PrecursorId=? ");
        sql.add(precursorId);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoPlus.class);
    }

    private static void joinTablesForPrecursorChromInfo(SQLFragment sql, User user, Container container)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);

        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" INNER JOIN ");
        sql.append(new PeptideTableInfo(schema, null, true), "pep");
        sql.append(" ON ");
        sql.append("pg.Id = pep.PeptideGroupId ");
        sql.append(" INNER JOIN ");
        sql.append(new PrecursorTableInfo(schema, null, true), "prec");
        sql.append(" ON ");
        sql.append("pep.Id = prec.GeneralMoleculeId ");
        sql.append(" INNER JOIN ");
        sql.append(new PrecursorChromInfoTable(schema, null), "pci");
        sql.append(" ON ");
        sql.append("prec.Id = pci.PrecursorId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label");
        sql.append(" ON ");
        sql.append("prec.IsotopeLabelId = label.Id ");
    }

    private static List<PrecursorChromInfoLitePlus> getPrecursorChromInfoLitePlusList(long id, boolean forPeptideGroup,
                                                                                      boolean forPrecursor, long sampleFileId,
                                                                                      User user, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.id, pci.precursorId, pci.sampleFileId, pci.bestRetentionTime, pci.minStartTime, pci.maxEndTime, pci.TotalArea, pci.maxfwhm, pci.maxHeight");
        sql.append(", pg.Label AS groupName, pep.Sequence, pep.PeptideModifiedSequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel, label.Id AS isotopeLabelId");
        sql.append(" FROM ");
        joinTablesForPrecursorChromInfo(sql, user, container);
        sql.append(" WHERE ");
        if(forPeptideGroup)
        {
            sql.append("pg.Id=? ");
        }
        else if(forPrecursor)
        {
            sql.append("prec.Id=? ");
        }
        else
        {
            sql.append("pep.Id=? ");
        }
        sql.add(id);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoLitePlus.class);
    }

    public static List<Precursor> getRepresentativePrecursors(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT gp.*, prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE");
        sql.append(" prec.Id = gp.Id");
        sql.append(" AND");
        sql.append(" gp.GeneralMoleculeId = gm.Id");
        sql.append(" AND");
        sql.append(" gm.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = ?");
        sql.add(runId);
        sql.append(" AND");
        sql.append(" gp.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Representative.ordinal());

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Precursor.class);
    }

    public static Precursor getLastDeprecatedPrecursor(Precursor prec, Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT gp.*, prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE");
        sql.append(" prec.Id = gp.Id");
        sql.append(" AND");
        sql.append(" gp.GeneralMoleculeId = gm.Id");
        sql.append(" AND");
        sql.append(" gm.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = run.Id");
        sql.append(" AND");
        sql.append(" run.Container = ?");
        sql.add(container);
        sql.append(" AND");
        sql.append(" gp.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Deprecated.ordinal());
        sql.append(" AND");
        sql.append(" prec.ModifiedSequence = ?");
        sql.add(prec.getModifiedSequence());
        sql.append(" ORDER BY gp.Modified DESC ");

        return new SqlSelector(TargetedMSManager.getSchema(), TargetedMSManager.getSqlDialect().limitRows(sql, 1)).getObject(Precursor.class);
    }

    /** Handles both peptide and molecule variants */
    public static int setRepresentativeState(long runId, RepresentativeDataState state)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        sql.append(" SET RepresentativeDataState = ?");
        sql.add(state.ordinal());
        sql.append(" FROM "+TargetedMSManager.getTableInfoGeneralMolecule());
        sql.append(", "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" WHERE ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".Id = "+TargetedMSManager.getTableInfoGeneralMolecule()+".PeptideGroupId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule()+".Id = "+TargetedMSManager.getTableInfoGeneralPrecursor()+".GeneralMoleculeId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".RunId = ?");
        sql.add(runId);
        return new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static void updateRepresentativeStatus(List<Long> precursorIds, RepresentativeDataState representativeState)
    {
        if(precursorIds == null || precursorIds.size() == 0)
            return;

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        sql.append(" SET RepresentativeDataState = ? ");
        sql.add(representativeState.ordinal());
        sql.append(" WHERE "+TargetedMSManager.getTableInfoGeneralPrecursor()+".Id IN (");
        sql.append(StringUtils.join(precursorIds, ","));
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static void updateStatusToDeprecatedOrNotRepresentative(List<Long> precursorIds)
    {
        if(precursorIds == null || precursorIds.size() == 0)
            return;

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        sql.append(" SET RepresentativeDataState = ");
        sql.append(" CASE WHEN RepresentativeDataState = "+RepresentativeDataState.Conflicted.ordinal());
        sql.append(" THEN "+RepresentativeDataState.NotRepresentative.ordinal());
        sql.append(" ELSE "+RepresentativeDataState.Deprecated.ordinal());
        sql.append(" END");
        sql.append(" WHERE "+TargetedMSManager.getTableInfoGeneralPrecursor()+".Id IN (");
        sql.append(StringUtils.join(precursorIds, ","));
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static boolean ensureContainerMembership(List<Long> precursorIds, Container container)
    {
        if(precursorIds == null || precursorIds.isEmpty())
            return false;
        // Dedupe
        Set<Long> ids = new HashSet<>(precursorIds);

        SQLFragment sql = new SQLFragment("SELECT COUNT(pg.Id) FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE gp.GeneralMoleculeId = gm.Id AND ");
        sql.append("gm.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Container = ? AND gp.Id IN (");
        sql.append(StringUtils.join(ids, ","));
        sql.append(")");
        sql.add(container.getId());

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count.intValue() == ids.size() ;
    }

    /**
     * Sum up the value of Height for each TransitionChromInfo for a precursor peak, and return the max sum.
     * This can be used for estimating the height of the tallest precursor peak for a peptide (generalMolecule)
     * over all the replicates when we are synchronizing the intensity axis for precursor peak chromatograms.
     * Getting the actual max intensity would require summing up the intensities across the points of each peak. But this
     * method should give us a value that is at least as much as the max intensity since we are summing up the tallest
     * point of each transition.
     */
    public static Double getMaxPrecursorIntensityEstimate(long generalMoleculeId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(precHeight) FROM (");
        sql.append("SELECT SUM(tci.Height) AS precHeight FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE");
        sql.append(" gmci.Id = preci.GeneralMoleculeChromInfoId");
        sql.append(" AND");
        sql.append(" preci.Id = tci.PrecursorChromInfoId");
        sql.append(" AND");
        sql.append(" gmci.GeneralMoleculeId=?");
        sql.add(generalMoleculeId);
        sql.append(" GROUP BY tci.PrecursorChromInfoId");
        sql.append(" ) a");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    /**
     * Returns the maximum value of MaxHeight for the Precursors of a peptide (generalMolecule) over all the replicates.
     * MaxHeight of a PrecursorChromInfo is the height of the tallest transition peak for the precursor in a replicate.
     * This can be used for getting the height of the tallest transition peak for a peptide over all the replicates
     * when we are synchronizing the intensity axis for transition peak chromatograms.
     */
    public static Double getMaxPrecursorMaxHeight(long generalMoleculeId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(pci.maxHeight) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" ON pci.GeneralMoleculeChromInfoId = gmci.Id");
        sql.append(" WHERE");
        sql.append(" gmci.GeneralMoleculeId=?");
        sql.add(generalMoleculeId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static boolean hasChromatograms(long precursorId, Long runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
            sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
            sql.append(" WHERE ");
            sql.append("pci.PrecursorId=?");
            sql.add(precursorId);

            Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
            return count != null && count > 0;
        }
        else
        {
            Set<Long> precursorIds = _precursorIdsWithChromatograms.get(String.valueOf(runId), null, (runId1, argument) -> {
                SQLFragment sql = new SQLFragment("SELECT DISTINCT pci.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
                sql.append(" WHERE ");
                sql.append(" pci.PrecursorId = gp.Id ");
                sql.append(" AND ");
                sql.append(" gp.GeneralMoleculeId = gm.Id ");
                sql.append(" AND ");
                sql.append(" gm.PeptideGroupId = pg.Id ");
                sql.append(" AND ");
                sql.append("pg.RunId = ?");
                sql.add(Long.valueOf(runId1));
                return Collections.unmodifiableSet(new HashSet<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Long.class)));
            });

            return precursorIds.contains(precursorId);
        }
    }

    public static boolean hasLibrarySpectra(long precursorId, Long runId)
    {
        if(runId == null)
        {
            SQLFragment sql = new SQLFragment("SELECT bib.PrecursorId FROM ");
            sql.append(TargetedMSManager.getTableInfoBibliospec(), "bib");
            sql.append(" WHERE ");
            sql.append("bib.PrecursorId=?");
            sql.add(precursorId);
            sql.append(" UNION ");
            sql.append("SELECT hun.PrecursorId FROM ");
            sql.append(TargetedMSManager.getTableInfoHunterLib(), "hun");
            sql.append(" WHERE ");
            sql.append("hun.PrecursorId=?");
            sql.add(precursorId);
            sql.append(" UNION ");
            sql.append("SELECT nis.PrecursorId FROM ");
            sql.append(TargetedMSManager.getTableInfoNistLib(), "nis");
            sql.append(" WHERE ");
            sql.append("nis.PrecursorId=?");
            sql.add(precursorId);
            sql.append(" UNION ");
            sql.append("SELECT sp.PrecursorId FROM ");
            sql.append(TargetedMSManager.getTableInfoSpectrastLib(), "sp");
            sql.append(" WHERE ");
            sql.append("sp.PrecursorId=?");
            sql.add(precursorId);
            sql.append(" UNION ");
            sql.append("SELECT ch.PrecursorId FROM ");
            sql.append(TargetedMSManager.getTableInfoChromatogramLib(), "ch");
            sql.append(" WHERE ");
            sql.append("ch.PrecursorId=?");
            sql.add(precursorId);

            return new SqlSelector(TargetedMSManager.getSchema(), sql).exists();
        }
        else
        {
            Set<Long> precursorIds = _precursorIdsWithSpectra.get(String.valueOf(runId), null, (runId1, argument) -> {
                SQLFragment sql = new SQLFragment("SELECT DISTINCT bib.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoBibliospec(), "bib");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(" WHERE ");
                sql.append("bib.SpectrumLibraryId = specLib.Id");
                sql.append(" AND ");
                sql.append("specLib.RunId = ?");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT hun.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoHunterLib(), "hun");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(" WHERE ");
                sql.append("hun.SpectrumLibraryId = specLib.Id");
                sql.append(" AND ");
                sql.append("specLib.RunId = ?");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT nis.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoNistLib(), "nis");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(" WHERE ");
                sql.append("nis.SpectrumLibraryId = specLib.Id");
                sql.append(" AND ");
                sql.append("specLib.RunId = ?");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT sp.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoSpectrastLib(), "sp");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(" WHERE ");
                sql.append("sp.SpectrumLibraryId = specLib.Id");
                sql.append(" AND ");
                sql.append("specLib.RunId = ?");
                sql.add(Long.valueOf(runId1));
                sql.append(" UNION ");
                sql.append("SELECT DISTINCT ch.PrecursorId FROM ");
                sql.append(TargetedMSManager.getTableInfoChromatogramLib(), "ch");
                sql.append(" , ");
                sql.append(TargetedMSManager.getTableInfoSpectrumLibrary(), "specLib");
                sql.append(" WHERE ");
                sql.append("ch.SpectrumLibraryId = specLib.Id");
                sql.append(" AND ");
                sql.append("specLib.RunId = ?");
                sql.add(Long.valueOf(runId1));
                return Set.copyOf(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Long.class));
            });

            return precursorIds.contains(precursorId);
        }
    }

    public static void removeRunCachedResults(List<Long> deletedRunIds)
    {
        for(Long runId: deletedRunIds)
        {
            _precursorIdsWithChromatograms.remove(String.valueOf(runId));
            _precursorIdsWithSpectra.remove(String.valueOf(runId));
        }
    }

    public static boolean canBeSplitView(long peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT DISTINCT gt.fragmenttype FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralTransition(), "gt");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(" WHERE gt.GeneralPrecursorId = gp.Id AND  ");
        sql.append("gp.GeneralMoleculeId = "+peptideId);
        String[] sqls = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(String.class);

        if(sqls.length > 1)
        {
            for(String result: sqls)
            {
                if(result.equals("precursor"))
                    return true;
            }
        }
        return false;
    }

    public static boolean hasOptimizationPeaks(long peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT pci.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");

        sql.append(" WHERE gm.id = gp.GeneralMoleculeId ");
        sql.append(" AND ");
        sql.append(" gp.Id = pci.PrecursorId ");
        sql.append(" AND ");
        sql.append(" pci.OptimizationStep IS NOT NULL ");
        sql.append(" AND ");
        sql.append(" gm.Id = " + peptideId);
        return new SqlSelector(TargetedMSManager.getSchema(), sql).exists();
    }

    public static RtRange getPrecursorPeakRtRange(@NotNull PrecursorChromInfo pci)
    {
        if(pci.getMinStartTime() != null && pci.getMaxEndTime() != null)
        {
            return new RtRange(pci.getMinStartTime(), pci.getMaxEndTime());
        }
        else
        {
            // Issue 41617: Chromatograms are not displayed for precursors that do not have any quantitative transitions.
            // Min peak start and max peak end times for a precursor chrom info can be null if none of the
            // transitions for the precursor are quantitative. We will try to get the min start and max end
            // retention times from the transition chrom infos instead.
            return TransitionManager.getTransitionPeakRtRange(pci.getId());
        }
    }

    private static class PrecursorIdsWithSpectra extends DatabaseCache<String, Set<Long>>
    {
       public PrecursorIdsWithSpectra()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Precursors having library spectra");
        }
    }

    private static class PrecursorIdsWithChromatograms extends DatabaseCache<String, Set<Long>>
    {
        public PrecursorIdsWithChromatograms()
        {
            super(TargetedMSManager.getSchema().getScope(), CACHE_SIZE, CacheManager.DAY, "Precursors having chromatograms");
        }
    }

    public static class TestCase
    {
        @Test
        public void testSort()
        {
            List<PrecursorChromInfo> areaInfos = new ArrayList<>();
            areaInfos.add(create(null, 10.0));
            areaInfos.add(create(null, 20.0));
            areaInfos.add(create(null, null));

            Collections.sort(areaInfos);
            Assert.assertEquals(Double.valueOf(20.0), areaInfos.get(0).getTotalArea());
            Assert.assertEquals(Double.valueOf(10.0), areaInfos.get(1).getTotalArea());
            Assert.assertNull(areaInfos.get(2).getTotalArea());

            // Add some entries with q-values
            areaInfos.add(create(0.5, 11.0));
            areaInfos.add(create(0.1, 5.0));
            areaInfos.add(create(0.1, 8.0));

            Collections.sort(areaInfos);
            Assert.assertEquals(Double.valueOf(0.1), areaInfos.get(0).getQvalue());
            Assert.assertEquals(Double.valueOf(8.0), areaInfos.get(0).getTotalArea());
            Assert.assertEquals(Double.valueOf(0.1), areaInfos.get(1).getQvalue());
            Assert.assertEquals(Double.valueOf(5.0), areaInfos.get(1).getTotalArea());
            Assert.assertEquals(Double.valueOf(0.5), areaInfos.get(2).getQvalue());
            Assert.assertEquals(Double.valueOf(11.0), areaInfos.get(2).getTotalArea());
            Assert.assertEquals(Double.valueOf(20.0), areaInfos.get(3).getTotalArea());
            Assert.assertEquals(Double.valueOf(10.0), areaInfos.get(4).getTotalArea());
        }

        private PrecursorChromInfo create(Double qvalue, Double totalArea)
        {
            PrecursorChromInfo result = new PrecursorChromInfo();
            result.setQvalue(qvalue);
            result.setTotalArea(totalArea);
            return result;
        }
    }
}
