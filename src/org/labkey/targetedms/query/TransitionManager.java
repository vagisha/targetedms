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
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:52 PM
 */
public class TransitionManager
{
    private TransitionManager() {}

    public static Transition get(int transitionId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransition()).getObject(transitionId, Transition.class);
    }

    public static TransitionChromInfo getTransitionChromInfo(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT tci.* FROM ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransition(), "t");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "pre");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE tci.TransitionId = t.Id AND t.PrecursorId = pre.Id AND pre.PeptideId = pep.Id AND ");
        sql.append("pep.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND tci.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(TransitionChromInfo.class);
    }

    // Returns null if an entry for the given precursorChromInfoId and chromatogramIndex is not found.
    public static TransitionChromInfo getTransitionChromInfo(int precursorChromInfoId, int chromatogramIndex)
    {
        String sql = "SELECT tci.* FROM "+
                     TargetedMSManager.getTableInfoTransitionChromInfo()+" AS tci, "+
                     TargetedMSManager.getTableInfoPrecursorChromInfo()+" AS pci "+
                     "WHERE tci.PrecursorChromInfoId=pci.Id "+
                     "AND pci.Id=? "+
                     "AND tci.ChromatogramIndex=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(precursorChromInfoId);
        sf.add(chromatogramIndex);

        return new SqlSelector(TargetedMSManager.getSchema(), sf).getObject(TransitionChromInfo.class);
    }

    public static TransitionChromInfo getTransitionChromInfoForTransition(int transitionId, int precursorChromInfoId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("PrecursorChromInfoId"), precursorChromInfoId);
        filter.addCondition(FieldKey.fromParts("TransitionId"), transitionId);

        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                                 filter,
                                 null)
                                 .getObject(TransitionChromInfo.class);
    }

    public static double getMaxTransitionIntensity(int peptideId)
    {
        SQLFragment sql = new SQLFragment("SELECT MAX(tci.Height) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideChromInfo(), "pepci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "preci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE ");
        sql.append("pepci.Id = preci.PeptideChromInfoId ");
        sql.append("AND ");
        sql.append("preci.Id = tci.PrecursorChromInfoId ");
        sql.append("AND ");
        sql.append("pepci.PeptideId=?");
        sql.add(peptideId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Double.class);
    }

    public static Set<Integer> getTransitionChromatogramIndexes(int precursorChromInfoId)
    {
        TableInfo tinfo = TargetedMSManager.getTableInfoTransitionChromInfo();
        Collection tranChromIndexes = new TableSelector(tinfo,
                                                        tinfo.getColumns("ChromatogramIndex"),
                                                        new SimpleFilter(FieldKey.fromParts("PrecursorChromInfoId"), precursorChromInfoId),
                                                        null
                                                        ).getCollection(Integer.class);
        return new HashSet<Integer>(tranChromIndexes);
    }

    public static Collection<Transition> getTransitionsForPrecursor(int precursorId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransition(),
                                 new SimpleFilter(FieldKey.fromParts("PrecursorId"), precursorId), null).getCollection(Transition.class);
    }

    public static Collection<TransitionChromInfo> getTransitionChromInfoListForTransition(int transitionId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoTransitionChromInfo(),
                                 new SimpleFilter(FieldKey.fromParts("TransitionId"), transitionId), null).getCollection(TransitionChromInfo.class);
    }
}
