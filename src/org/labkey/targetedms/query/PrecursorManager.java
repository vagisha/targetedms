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

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:08 PM
 */
public class PrecursorManager
{
    private PrecursorManager() {}

    public static Precursor get(int precursorId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPrecursor(),
                                 new SimpleFilter(FieldKey.fromParts("Id"), precursorId),
                                 null).getObject(Precursor.class);
    }

    public static Precursor getPrecursor(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pre.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pre.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Precursor.class);
    }

    public static PrecursorChromInfo getPrecursorChromInfo(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT pci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pci.PrecursorId = pre.Id AND pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(PrecursorChromInfo.class);
    }

    public static List<Precursor> getPrecursorsForPeptide(int peptideId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("PeptideId", peptideId);

        Sort sort = new Sort("IsotopeLabelId, Charge");

        Precursor[] precursors;
        try
        {
            precursors = Table.select(TargetedMSManager.getTableInfoPrecursor(), Table.ALL_COLUMNS, filter, sort, Precursor.class);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        if(precursors.length == 0)
        {
            throw new NotFoundException(String.format("No precursors found for peptideId %d", peptideId));
        }
        return Arrays.asList(precursors);
    }

    public static List<PrecursorChromInfo> getPrecursorChromInfosForPeptideChromInfo(int peptideChromInfoId)
    {
        List<PrecursorChromInfo> chromInfoList = new ArrayList<>();
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition("PeptideChromInfoId", peptideChromInfoId);

        PrecursorChromInfo[] chromInfos;
        try
        {
            chromInfos = Table.select(TargetedMSManager.getTableInfoPrecursorChromInfo(),
                                      Table.ALL_COLUMNS, filter, null, PrecursorChromInfo.class);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        Collections.addAll(chromInfoList, chromInfos);
        return chromInfoList;
    }

    public static List<PrecursorChromInfo> getSortedPrecursorChromInfosForPrecursor(int precursorId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorId"), precursorId);

        List<PrecursorChromInfo> precursorChromInfos = new TableSelector(TargetedMSManager.getTableInfoPrecursorChromInfo(),
                                   filter,
                                   new Sort("-TotalArea"))
                                  .getArrayList(PrecursorChromInfo.class);

        return precursorChromInfos;
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPrecursor(int precursorId)
    {
        // Get a list of chrom infos sorted by peak area (descending).
        List<PrecursorChromInfo> chromInfos = getSortedPrecursorChromInfosForPrecursor(precursorId);

        if(chromInfos == null || chromInfos.size() == 0)
        {
            return null;
        }
        else
        {
            // Look for one that has area information and the peaks are not truncated
            for (PrecursorChromInfo chromInfo : chromInfos)
            {
                if (chromInfo.getTotalArea() != null && (chromInfo.getNumTruncated() == null || chromInfo.getNumTruncated().intValue() == 0))
                {
                    return chromInfo;
                }
            }

            // If we did not find a chrom info with a non-null precursor area and non-truncated peaks,
            // return the first chrom info with non-null precursor area.
            for(PrecursorChromInfo chromInfo: chromInfos)
            {
                if(chromInfo.getTotalArea() != null)
                    return chromInfo;
            }
            return chromInfos.get(0);
        }
    }

    public static PrecursorChromInfo getBestPrecursorChromInfoForPeptide(int peptideId)
    {
        List<PrecursorChromInfoPlus> chromInfos = getPrecursorChromInfosForPeptide(peptideId);

        if(chromInfos == null || chromInfos.size() == 0)
        {
            return null;
        }
        else
        {
            Collections.sort(chromInfos, new Comparator<PrecursorChromInfoPlus>()
            {
                @Override
                public int compare(PrecursorChromInfoPlus o1, PrecursorChromInfoPlus o2)
                {
                    if( (o1 == o2 ) || (o1.getTotalArea() == o2.getTotalArea()) ) { return 0; }
                    else if(o1.getTotalArea() == null) {return 1;}
                    else if(o2.getTotalArea() == null) {return -1;}

                    return o2.getTotalArea().compareTo(o1.getTotalArea());
                }
            });

            for (PrecursorChromInfo chromInfo : chromInfos)
            {
                if (chromInfo.getTotalArea() != null && (chromInfo.getNumTruncated() == null || chromInfo.getNumTruncated().intValue() == 0))
                {
                    return chromInfo;
                }
            }
            return chromInfos.get(0);
        }
    }

    public static Map<String, Object> getPrecursorSummary(int precursorId)
    {
        SQLFragment sf = new SQLFragment("SELECT pre.Mz, pre.Charge, pep.Sequence, label.Name FROM ");
        sf.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sf.append(", ");
        sf.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label ");
        sf.append(" WHERE pre.PeptideId = pep.Id AND pre.IsotopeLabelId = label.Id AND pre.Id = ? ");
        sf.add(precursorId);

        try (Table.TableResultSet rs = new SqlSelector(TargetedMSManager.getSchema(), sf).getResultSet())
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

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptideGroup(int peptideGroupId)
    {
        return getPrecursorChromInfosForPeptideGroup(peptideGroupId, 0);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptideGroup(int peptideGroupId, int sampleFileId)
    {
        return getPrecursorChromInfoList(peptideGroupId, true, sampleFileId);
    }

     public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForPeptide(int peptideId)
    {
        return getPrecursorChromInfoList(peptideId, false, 0);
    }

    private static List<PrecursorChromInfoPlus> getPrecursorChromInfoList(int id, boolean forPeptideGroup, int sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.*, pg.Label AS groupName, pep.Sequence, prec.ModifiedSequence, prec.Charge, label.Name AS isotopeLabel ");
        sql.append("FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoIsotopeLabel(), "label");
        sql.append(" WHERE ");
        sql.append("pg.Id = pep.PeptideGroupId ");
        sql.append("AND ");
        sql.append("pep.Id = prec.PeptideId ");
        sql.append("AND ");
        sql.append("prec.Id = pci.PrecursorId ");
        sql.append("AND ");
        sql.append("prec.IsotopeLabelId = label.Id ");

        if(forPeptideGroup)
        {
            sql.append("AND ");
            sql.append("pg.Id=? ");
        }
        else
        {
            sql.append("AND ");
            sql.append("pep.Id=? ");
        }
        sql.add(id);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        PrecursorChromInfoPlus[] precChromInfos = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(PrecursorChromInfoPlus.class);

        return Arrays.asList(precChromInfos);
    }

    public static List<Precursor> getRepresentativePrecursors(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE");
        sql.append(" prec.PeptideId = pep.Id");
        sql.append(" AND");
        sql.append(" pep.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = ?");
        sql.add(runId);
        sql.append(" AND");
        sql.append(" prec.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Representative.ordinal());

        Precursor[] reprPrecursors = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(Precursor.class);
        return Arrays.asList(reprPrecursors);
    }

    public static Precursor getLastDeprecatedPrecursor(Precursor prec, Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT prec.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE");
        sql.append(" prec.PeptideId = pep.Id");
        sql.append(" AND");
        sql.append(" pep.PeptideGroupId = pg.Id");
        sql.append(" AND");
        sql.append(" pg.RunId = run.Id");
        sql.append(" AND");
        sql.append(" run.Container = ?");
        sql.add(container);
        sql.append(" AND");
        sql.append(" prec.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Deprecated.ordinal());
        sql.append(" AND");
        sql.append(" prec.ModifiedSequence = ?");
        sql.add(prec.getModifiedSequence());
        sql.append(" ORDER BY prec.Modified DESC ");

        Precursor[] deprecatedPrecursors = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(Precursor.class);
        if (deprecatedPrecursors.length == 0)
        {
            return null;
        }
        return deprecatedPrecursors[0];
    }

    public static int setRepresentativeState(int runId, RepresentativeDataState state)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ?");
        sql.add(state.ordinal());
        sql.append(" FROM "+TargetedMSManager.getTableInfoPeptide());
        sql.append(", "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" WHERE ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".Id = "+TargetedMSManager.getTableInfoPeptide()+".PeptideGroupId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoPeptide()+".Id = "+TargetedMSManager.getTableInfoPrecursor()+".PeptideId");
        sql.append(" AND ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup()+".RunId = ?");
        sql.add(runId);
        return new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static void updateRepresentativeStatus(int[] precursorIds, RepresentativeDataState representativeState)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return;

        StringBuilder precursorIdsString = new StringBuilder();
        for(int id: precursorIds)
        {
            precursorIdsString.append(",").append(id);
        }
        if(precursorIdsString.length() > 0)
            precursorIdsString.deleteCharAt(0);

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ? ");
        sql.add(representativeState.ordinal());
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPrecursor()+".Id IN (");
        sql.append(precursorIdsString.toString());
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    // Set to either NotRepresentative or Representative_Deprecated.
    // If the original status was Representative it will be updated to Representative_Deprecated.
    // If the original status was Conflicted it will be update to NotRepresentative.
    public static void updateStatusToDeprecatedOrNotRepresentative(int[] precursorIds)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return;

        StringBuilder precursorIdsString = new StringBuilder();
        for(int id: precursorIds)
        {
            precursorIdsString.append(",").append(id);
        }
        if(precursorIdsString.length() > 0)
            precursorIdsString.deleteCharAt(0);

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        sql.append(" SET RepresentativeDataState = ");
        sql.append(" CASE WHEN RepresentativeDataState = "+RepresentativeDataState.Conflicted.ordinal());
        sql.append(" THEN "+RepresentativeDataState.NotRepresentative.ordinal());
        sql.append(" ELSE "+RepresentativeDataState.Deprecated.ordinal());
        sql.append(" END");
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPrecursor()+".Id IN (");
        sql.append(precursorIdsString.toString());
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static boolean ensureContainerMembership(int[] precursorIds, Container container)
    {
        if(precursorIds == null || precursorIds.length == 0)
            return false;

        StringBuilder precIds = new StringBuilder();
        Arrays.sort(precursorIds);
        int lastPrecursorId = 0;
        int precursorIdCount = 0;
        for(int id: precursorIds)
        {
            // Ignore duplicates
            if( id != lastPrecursorId)
            {
                precIds.append(",").append(id);
                precursorIdCount++;
            }
            lastPrecursorId = id;
        }
        if(precIds.length() > 0)
            precIds.deleteCharAt(0);
        SQLFragment sql = new SQLFragment("SELECT COUNT(pg.Id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Container = ? AND pre.Id IN ("+precIds+")");
        sql.add(container.getId());

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count == precursorIdCount;
    }

    public static double getMaxPrecursorIntensity(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(precHeight) FROM (");
        sql.append("SELECT SUM(tci.Height) AS precHeight FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideChromInfo(), "pepci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE");
        sql.append(" pepci.Id = preci.PeptideChromInfoId");
        sql.append(" AND");
        sql.append(" preci.Id = tci.PrecursorChromInfoId");
        sql.append(" AND");
        sql.append(" pepci.PeptideId=?");
        sql.add(peptideId);
        sql.append(" GROUP BY tci.PrecursorChromInfoId");
        sql.append(" ) a");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }
}
