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

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.TargetedMSManager.getSchema;

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
        return Table.selectObject(TargetedMSManager.getTableInfoPrecursor(), precursorId, Precursor.class);
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

    public static List<PrecursorChromInfo> getPrecursorChromInfo(int peptideChromInfoId)
    {
        List<PrecursorChromInfo> chromInfoList = new ArrayList<PrecursorChromInfo>();
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

        Table.TableResultSet rs = null;
        try
        {
            rs = Table.executeQuery(getSchema(), sf);
            if(rs.next())
            {
                Map<String, Object> result = new HashMap<String, Object>();

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
        finally
        {
            if(rs != null) try {rs.close();} catch(SQLException ignored){}
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
}
