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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.chart.ChromatogramDataset;
import org.labkey.targetedms.chart.ChromatogramDataset.RtRange;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:52 PM
 */
public class TransitionManager
{
    private TransitionManager() {}

    @Nullable
    public static Transition get(long transitionId, User user, Container container)
    {
        return new TableSelector(new DocTransitionsTableInfo(new TargetedMSSchema(user, container), null, true), Transition.getColumns()).getObject(transitionId, Transition.class);
    }

    @Nullable
    public static TransitionChromInfo getTransitionChromInfo(Container c, long id)
    {
        SQLFragment sql = new SQLFragment("SELECT tci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralTransition(), "gt");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE tci.TransitionId = gt.Id AND gt.GeneralPrecursorId = gp.Id AND gp.GeneralMoleculeId = gm.Id AND ");
        sql.append("gm.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND tci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(TransitionChromInfo.class);
    }

    @NotNull
    public static List<TransitionChromInfo> getTransitionChromInfoList(long precursorChromInfoId, long chromatogramIndex)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorChromInfoId"),precursorChromInfoId);
        filter.addCondition(FieldKey.fromParts("ChromatogramIndex"), chromatogramIndex);
        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                                 filter,
                                 null)
                                .getArrayList(TransitionChromInfo.class);
    }

    @NotNull
    public static List<TransitionChromInfo> getTransitionChromInfoList(long precursorChromInfoId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorChromInfoId"),precursorChromInfoId);
        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                filter,
                null)
                .getArrayList(TransitionChromInfo.class);
    }

    @Nullable
    public static TransitionChromInfo getTransitionChromInfoForTransition(long transitionId, long precursorChromInfoId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorChromInfoId"), precursorChromInfoId);
        filter.addCondition(FieldKey.fromParts("TransitionId"), transitionId);

        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                                 filter,
                                 null)
                                 .getObject(TransitionChromInfo.class);
    }

    public static double getMaxTransitionIntensity(long peptideId)
    {
        return getMaxTransitionIntensity(peptideId, Transition.Type.ALL);
    }

    public static double getMaxTransitionIntensity(long generalMoleculeId, Transition.Type fragmentType)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(tci.Height) FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        if(fragmentType != Transition.Type.ALL)
        {
            sql.append(", ");
            sql.append(TargetedMSManager.getTableInfoTransition(), "tran");
        }
        sql.append(" WHERE ");
        sql.append("gmci.Id = preci.GeneralMoleculeChromInfoId ");
        sql.append("AND ");
        sql.append("preci.Id = tci.PrecursorChromInfoId ");

        if(fragmentType != Transition.Type.ALL)
        {
            sql.append("AND ");
            sql.append("tran.Id = tci.TransitionId");

            String condition = fragmentType == Transition.Type.PRECURSOR ? "=" : "!=";

            sql.append(" AND ");
            sql.append("tran.FragmentType ").append(condition).append(" 'precursor' ");
        }
        sql.append(" AND ");
        sql.append("gmci.GeneralMoleculeId=?");
        sql.add(generalMoleculeId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    @NotNull
    public static Set<Integer> getTransitionChromatogramIndexes(long precursorChromInfoId)
    {
        TableInfo tinfo = TargetedMSManager.getTableInfoTransitionChromInfo();
        Collection<Integer> tranChromIndexes = new TableSelector(tinfo.getColumn("ChromatogramIndex"),
                                                        new SimpleFilter(FieldKey.fromParts("PrecursorChromInfoId"), precursorChromInfoId),
                                                        null
                                                        ).getCollection(Integer.class);
        return new HashSet<>(tranChromIndexes);
    }

    @NotNull
    public static Collection<Transition> getTransitionsForPrecursor(long precursorId, User user, Container container)
    {
        return new TableSelector(new DocTransitionsTableInfo(new TargetedMSSchema(user, container), null), Transition.getColumns(),
                                 new SimpleFilter(FieldKey.fromParts("PrecursorId"), precursorId), null).getCollection(Transition.class);
    }

    @NotNull
    public static Collection<TransitionChromInfo> getTransitionChromInfoListForTransition(long transitionId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                                 new SimpleFilter(FieldKey.fromParts("TransitionId"), transitionId), null).getCollection(TransitionChromInfo.class);
    }

    public static RtRange getPrecursorPeakRtRange(long precursorChromInfoId)
    {
        // Get the min start time from the transition peaks
        SQLFragment sql = new SQLFragment("SELECT MIN(startTime) AS minRt, MAX(endTime) AS maxRt FROM ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE precursorChromInfoId=?").add(precursorChromInfoId);

        return getRtRange(sql);
    }

    public static RtRange getGeneralMoleculeRtRange(long generalMoleculeId)
    {
        // Get the min start time and max end time across all the samples for the given general molecule id.
        // Use the the start and end time set on the transition peaks.
        return getGeneralMoleculeSampleRtSummary(generalMoleculeId, null);
    }

    public static RtRange getGeneralMoleculeSampleRtRange(long generalMoleculeId, long sampleFileId)
    {
        // Get the min start time and max end time for the given general molecule id in the given sample
        // Use the the start and end time set on the transition peaks.
        return TransitionManager.getGeneralMoleculeSampleRtSummary(generalMoleculeId, sampleFileId);
    }

    private static RtRange getGeneralMoleculeSampleRtSummary(long generalMoleculeId, Long sampleFileId)
    {
        SQLFragment sql = new SQLFragment("SELECT ").append(" MIN (tci.startTime) AS minRt, MAX(tci.endTime) AS maxRt FROM ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(" ON pci.id = tci.precursorChrominfoId ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMoleculeChromInfo(), "gmci");
        sql.append(" ON gmci.Id=pci.GeneralMoleculeChromInfoId");
        sql.append(" WHERE ");
        sql.append("gmci.GeneralMoleculeId=?").add(generalMoleculeId);
        if(sampleFileId != null)
        {
            sql.append(" AND ");
            sql.append("pci.SampleFileId = ?").add(sampleFileId);
        }

        return getRtRange(sql);
    }

    @NotNull
    private static ChromatogramDataset.RtRange getRtRange(SQLFragment sql)
    {
        Map<String, Object> row = new SqlSelector(TargetedMSManager.getSchema(), sql).getMap();
        if(row != null)
        {
            Float minRt = (Float)row.get("minRt");
            Float maxRt = (Float)row.get("maxRt");
            return new RtRange(minRt != null ? minRt : 0, maxRt != null ? maxRt : 0);
        }
        return new RtRange(0,0);
    }
}
