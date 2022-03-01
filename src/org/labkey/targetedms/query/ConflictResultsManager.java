/*
 * Copyright (c) 2012-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.Formats;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.conflict.ConflictPeptide;
import org.labkey.targetedms.conflict.ConflictPrecursor;
import org.labkey.targetedms.conflict.ConflictProtein;
import org.labkey.targetedms.conflict.ConflictTransition;
import org.labkey.targetedms.parser.GeneralTransition;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.TransitionChromInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 2:25 PM
 */
public class ConflictResultsManager
{
    private ConflictResultsManager() {}

    public static List<ConflictProtein> getConflictedProteins(Container container)
    {
        // Get a list of conflicted proteins in the given container
        SQLFragment getConflictProteinsSql = new SQLFragment("SELECT ");
        getConflictProteinsSql.append("pg.Id AS newProteinId, ");
        getConflictProteinsSql.append("pg.RunId AS newProteinRunId, ");
        getConflictProteinsSql.append("r.filename AS newRunFile, ");
        getConflictProteinsSql.append("pg.Label AS newProteinLabel, ");
        getConflictProteinsSql.append("pg2.Id AS oldProteinId, ");
        getConflictProteinsSql.append("pg2.RunId AS oldProteinRunId, ");
        getConflictProteinsSql.append("r2.filename AS oldRunFile, ");
        getConflictProteinsSql.append("pg2.Label AS oldProteinLabel ");
        getConflictProteinsSql.append(" FROM ");
        getConflictProteinsSql.append(TargetedMSManager.getTableInfoRuns(), "r");
        getConflictProteinsSql.append(", ");
        getConflictProteinsSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        getConflictProteinsSql.append(" INNER JOIN ");
        getConflictProteinsSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        getConflictProteinsSql.append(" ON (pg.SequenceId = pg2.SequenceId  OR pg.Label = pg2.Label ) ");
        getConflictProteinsSql.append(" INNER JOIN ");
        getConflictProteinsSql.append(TargetedMSManager.getTableInfoRuns(), "r2");
        getConflictProteinsSql.append(" ON (r2.Id = pg2.RunId) ");
        getConflictProteinsSql.append(" WHERE r.Id = pg.RunId AND r.Container=? " );
        getConflictProteinsSql.append(" AND r2.Container=? ");
        getConflictProteinsSql.append(" AND pg.RepresentativeDataState=? ");
        getConflictProteinsSql.append(" AND pg2.RepresentativeDataState=? ");
        getConflictProteinsSql.add(container);
        getConflictProteinsSql.add(container);
        getConflictProteinsSql.add(RepresentativeDataState.Conflicted.ordinal());
        getConflictProteinsSql.add(RepresentativeDataState.Representative.ordinal());

        return new SqlSelector(TargetedMSManager.getSchema(), getConflictProteinsSql).getArrayList(ConflictProtein.class);
    }

    public static List<ConflictPrecursor> getConflictedPrecursors(Container container)
    {
        List<ConflictPrecursor> result = new ArrayList<>();
        result.addAll(getConflictedPrecursors(container, TargetedMSManager.getTableInfoPeptide(), TargetedMSManager.getTableInfoPrecursor(), "ModifiedSequence","prec.ModifiedSequence",true));
        result.addAll(getConflictedPrecursors(container, TargetedMSManager.getTableInfoMolecule(), TargetedMSManager.getTableInfoMoleculePrecursor(), "IonFormula", TargetedMSManager.getSqlDialect().concatenate("COALESCE(prec.IonFormula, '')", "' '", "COALESCE(prec.CustomIonName, '')", "' - '", "CAST(CAST(gp.mz AS DECIMAL(10, 4)) AS VARCHAR)"), false));
        return result;
    }

    private static List<ConflictPrecursor> getConflictedPrecursors(Container container, TableInfo moleculeTable, TableInfo precursorTable, String joinColumn, String displaySQL, boolean peptide)
    {
        // Get a list of conflicted precursors in the given container
        SQLFragment sql = new SQLFragment("SELECT ? AS Peptide, ");
        sql.add(peptide);
        sql.append("prec.Id AS newPrecursorId, ");
        sql.append(displaySQL);
        sql.append(" AS MoleculeName, ");
        sql.append("r.Id AS newPrecursorRunId, ");
        sql.append("r.filename AS newRunFile, ");
        sql.append("prec2.Id AS oldPrecursorId, ");
        sql.append("r2.Id AS oldPrecursorRunId, ");
        sql.append("r2.filename AS oldRunFile\n");
        sql.append("FROM ");
        sql.append("targetedms.runs r ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.peptidegroup pg ");
        sql.append("ON r.Id = pg.RunId ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.generalmolecule gm ");
        sql.append("ON pg.Id = gm.PeptideGroupId ");
        sql.append("INNER JOIN ");
        sql.append(moleculeTable, "m");
        sql.append(" ON gm.Id = m.Id ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.generalprecursor gp ");
        sql.append("ON m.Id = gp.GeneralMoleculeId ");
        sql.append("INNER JOIN ");
        sql.append(precursorTable, "prec");
        sql.append(" ON gp.Id = prec.Id ");
        sql.append("INNER JOIN ");
        sql.append(precursorTable, "prec2");
        sql.append(" ON prec." + joinColumn + " = prec2." + joinColumn);
        sql.append(" INNER JOIN targetedms.generalprecursor gp2 ");
        sql.append("ON (gp2.Id = prec2.Id AND gp.Charge = gp2.Charge AND gp.mz = gp2.mz) ");
        sql.append("INNER JOIN ");
        sql.append(moleculeTable, "m2");
        sql.append(" ON gp2.generalMoleculeId = m2.Id ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.generalmolecule gm2 ");
        sql.append("ON gm2.Id = m2.Id ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.peptidegroup pg2 ");
        sql.append("ON pg2.Id = gm2.PeptideGroupId ");
        sql.append("INNER JOIN ");
        sql.append("targetedms.runs r2 ");
        sql.append("ON r2.Id = pg2.RunId ");
        sql.append(" WHERE ");
        sql.append(" r.Container=? " );
        sql.append(" AND r2.Container=? ");
        sql.append(" AND gp.RepresentativeDataState=? ");
        sql.append(" AND gp2.RepresentativeDataState=? ");
        sql.add(container);
        sql.add(container);
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        sql.add(RepresentativeDataState.Representative.ordinal());
        sql.append(" ORDER BY gm.Id");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(ConflictPrecursor.class);
    }


    public static List<ConflictPeptide> getConflictPeptidesForProteins(long newProteinId, long oldProteinId, User user, Container container)
    {
        List<BestPrecursorPeptide> newProteinPeptides = getBestPrecursorsForProtein(newProteinId, user, container);
        List<BestPrecursorPeptide> oldProteinPeptides = getBestPrecursorsForProtein(oldProteinId, user, container);

        Map<String, ConflictPeptide> conflictPeptideMap = new HashMap<>();
        for(BestPrecursorPeptide peptide: newProteinPeptides)
        {
            ConflictPeptide cPeptide = new ConflictPeptide();
            cPeptide.setNewPeptide(peptide.getPeptide());
            cPeptide.setNewPeptidePrecursor(peptide.getPrecursor());
            cPeptide.setNewPeptideRank(peptide.getRank());

            String modifiedSequence = peptide.getPeptide().getPeptideModifiedSequence();
            if(conflictPeptideMap.containsKey(modifiedSequence))
            {
                throw new IllegalStateException("Peptide "+modifiedSequence+" has been seen already for peptide group ID "+newProteinId);
            }
            conflictPeptideMap.put(modifiedSequence, cPeptide);
        }

        for(BestPrecursorPeptide peptide: oldProteinPeptides)
        {
            String modifiedSequence = peptide.getPeptide().getPeptideModifiedSequence();
            ConflictPeptide cPeptide = conflictPeptideMap.get(modifiedSequence);
            if(cPeptide == null)
            {
                cPeptide = new ConflictPeptide();
                conflictPeptideMap.put(modifiedSequence, cPeptide);
            }
            cPeptide.setOldPeptide(peptide.getPeptide());
            cPeptide.setOldPeptidePrecursor(peptide.getPrecursor());
            cPeptide.setOldPeptideRank(peptide.getRank());
        }

        return new ArrayList<>(conflictPeptideMap.values());
    }

    private static String getPeptideModifiedSequence(Peptide peptide)
    {
        Map<Integer, Double> structuralModMap = ModificationManager.getPeptideStructuralModsMap(peptide.getId());
        StringBuilder modifiedSequence = new StringBuilder();

        String sequence = peptide.getSequence();
        for(int i = 0; i < sequence.length(); i++)
        {
            modifiedSequence.append(sequence.charAt(i));
            Double massDiff = structuralModMap.get(i);
            if(massDiff != null)
            {
                String sign = massDiff > 0 ? "+" : "";
                modifiedSequence.append("[").append(sign).append(Formats.f1.format(massDiff)).append("]");
            }
        }
        return modifiedSequence.toString();
    }

    private static List<BestPrecursorPeptide> getBestPrecursorsForProtein(long proteinId, User user, Container container)
    {
        Collection<Peptide> peptides = PeptideManager.getPeptidesForGroup(proteinId);
        List<BestPrecursorPeptide> proteinPeptides = new ArrayList<>();
        for(Peptide peptide: peptides)
        {
            if(peptide.isDecoyPeptide() || peptide.isStandardTypePeptide())
            {
                continue;
            }
            BestPrecursorPeptide bestPrecursor = getBestPrecursor(peptide, user, container);
            proteinPeptides.add(bestPrecursor);
        }
        proteinPeptides.sort(Comparator.comparingDouble(BestPrecursorPeptide::getMaxArea).reversed());
        int rank = 0;
        double lastMaxArea = Double.MAX_VALUE;
        for(BestPrecursorPeptide bestPrecursor: proteinPeptides)
        {
            if(bestPrecursor.getMaxArea() < lastMaxArea) rank++;
            bestPrecursor.setRank(rank);
            lastMaxArea = bestPrecursor.getMaxArea();
        }

        return proteinPeptides;
    }

    private static BestPrecursorPeptide getBestPrecursor(Peptide peptide, User user, Container container)
    {
        PrecursorChromInfo bestPrecursorChromInfo = PrecursorManager.getBestPrecursorChromInfo(PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId()));

        BestPrecursorPeptide bestPrecursorPeptide = new BestPrecursorPeptide();
        bestPrecursorPeptide.setPeptide(peptide);

        // If this peptide does not have a light precursor and was loaded from a Skyline document pre v1.5
        // its modified sequence was not set.
        if(peptide.getPeptideModifiedSequence() == null)
        {
            peptide.setPeptideModifiedSequence(getPeptideModifiedSequence(peptide));
        }

        if(bestPrecursorChromInfo != null)
        {
            bestPrecursorPeptide.setPrecursor(PrecursorManager.getPrecursor(container, bestPrecursorChromInfo.getPrecursorId(), user));
            bestPrecursorPeptide.setMaxArea(bestPrecursorChromInfo.getTotalArea());
        }
        else
        {
            bestPrecursorPeptide.setMaxArea(0);
        }

        return bestPrecursorPeptide;
    }

    private static class BestPrecursorPeptide
    {
        private Peptide _peptide;
        private Precursor _precursor;
        private double _maxArea;
        private int _rank;

        public Peptide getPeptide()
        {
            return _peptide;
        }

        public void setPeptide(Peptide peptide)
        {
            _peptide = peptide;
        }

        public Precursor getPrecursor()
        {
            return _precursor;
        }

        public void setPrecursor(Precursor precursor)
        {
            _precursor = precursor;
        }

        public double getMaxArea()
        {
            return _maxArea;
        }

        public void setMaxArea(double maxArea)
        {
            _maxArea = maxArea;
        }

        public int getRank()
        {
            return _rank;
        }

        public void setRank(int rank)
        {
            _rank = rank;
        }
    }

    public static List<ConflictTransition> getConflictTransitionsForPrecursors(long newPrecursorId, long oldPrecursorId, User user, Container container)
    {
        List<TransitionWithAreaAndRank> newPrecursorTransitions = getRankedTransitionsForPrecursor(newPrecursorId, user, container);
        List<TransitionWithAreaAndRank> oldPrecursorTransitions = getRankedTransitionsForPrecursor(oldPrecursorId, user, container);

        Precursor newPrecursor = PrecursorManager.getPrecursor(container, newPrecursorId, user);
        Precursor oldPrecursor = PrecursorManager.getPrecursor(container, oldPrecursorId, user);

        // Key in the conflictTransitionMap is the transition label (y7, y8, etc.)
        Map<String, ConflictTransition> conflictTransitionMap = new HashMap<>();
        for(TransitionWithAreaAndRank twr: newPrecursorTransitions)
        {
            ConflictTransition cTransition = new ConflictTransition();
            cTransition.setNewPrecursor(newPrecursor);
            cTransition.setNewTransition(twr.getTransition());
            cTransition.setNewTransitionRank(twr.getRank());

            String transitionLabel = twr.getTransition().toString();
            if(conflictTransitionMap.containsKey(transitionLabel))
            {
                throw new IllegalStateException("Transition "+transitionLabel+" has been seen already for precursor "+newPrecursor.getModifiedSequence()+" with ID "+newPrecursorId);
            }
            conflictTransitionMap.put(transitionLabel, cTransition);
        }

        for(TransitionWithAreaAndRank twr: oldPrecursorTransitions)
        {
            String transitionLabel = twr.getTransition().toString();
            ConflictTransition cTransition = conflictTransitionMap.get(transitionLabel);
            if(cTransition == null)
            {
                cTransition = new ConflictTransition();
                conflictTransitionMap.put(transitionLabel, cTransition);
            }
            cTransition.setOldPrecursor(oldPrecursor);
            cTransition.setOldTransition(twr.getTransition());
            cTransition.setOldTransitionRank(twr.getRank());
        }

        return new ArrayList<>(conflictTransitionMap.values());
    }

    public static List<TransitionWithAreaAndRank> getRankedTransitionsForPrecursor(long precursorId, User user, Container container)
    {
        Collection<? extends GeneralTransition> transitions = TransitionManager.getTransitionsForPrecursor(precursorId, user, container);
        if (transitions.isEmpty())
        {
            transitions = MoleculeTransitionManager.getTransitionsForPrecursor(precursorId, user, container);
        }

        // Each transition may have been measured in more than one replicate. We need to get the
        // the average area for each transition across all replicates
        List<TransitionWithAreaAndRank> transWithRankList = new ArrayList<>(transitions.size());
        for(GeneralTransition transition: transitions)
        {
            Collection<TransitionChromInfo> transChromInfoList = TransitionManager.getTransitionChromInfoListForTransition(transition.getId());
            double totalArea = 0.0;
            for(TransitionChromInfo tci: transChromInfoList)
            {
                if(tci.getArea() != null)
                {
                    totalArea += tci.getArea();
                }
            }
            TransitionWithAreaAndRank twr = new TransitionWithAreaAndRank();
            twr.setTransition(transition);
            twr.setAvgArea(transChromInfoList.size() == 0 ? 0 : totalArea / transChromInfoList.size());
            transWithRankList.add(twr);
        }

        // Sort by average area.
        transWithRankList.sort(Comparator.comparingDouble(TransitionWithAreaAndRank::getAvgArea).reversed());

        int rank = 1;
        for(TransitionWithAreaAndRank twr: transWithRankList)
        {
            twr.setRank(rank++);
        }

        return transWithRankList;
    }

    private static class TransitionWithAreaAndRank
    {
        private GeneralTransition _transition;
        private double _avgArea;
        private int _rank;

        public GeneralTransition getTransition()
        {
            return _transition;
        }

        public void setTransition(GeneralTransition transition)
        {
            _transition = transition;
        }

        public double getAvgArea()
        {
            return _avgArea;
        }

        public void setAvgArea(double avgArea)
        {
            _avgArea = avgArea;
        }

        public int getRank()
        {
            return _rank;
        }

        public void setRank(int rank)
        {
            _rank = rank;
        }
    }

    private static long getProteinConflictCount(User user, Container container)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo PepGrpTable = schema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
        SimpleFilter representativeStateFilter = new SimpleFilter(FieldKey.fromParts("RepresentativeDataState"), RepresentativeDataState.Conflicted.ordinal());
        return new TableSelector(PepGrpTable, representativeStateFilter, null).getRowCount();
    }

    private static long getGeneralMoleculeConflictCount(Container container)
    {
        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(gm.Id) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pc");
        sqlFragment.append(" WHERE ");
        sqlFragment.append("gm.PeptideGroupId = pg.Id AND ");
        sqlFragment.append("pg.RunId = r.Id AND ");
        sqlFragment.append("pc.GeneralMoleculeId = gm.Id  AND ");
        sqlFragment.append("r.Deleted = ? AND r.Container = ? ");
        sqlFragment.append("AND pc.RepresentativeDataState = ? ");

        sqlFragment.add(false);
        sqlFragment.add(container.getId());
        sqlFragment.add(RepresentativeDataState.Conflicted.ordinal());

        SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);
        return sqlSelector.getRowCount();
    }

    public static long getConflictCount(User user, Container container) {

        final TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(container);

        long conflictCount = 0;

        if(folderType == TargetedMSService.FolderType.LibraryProtein)
        {
            conflictCount = getProteinConflictCount(user, container);
        }
        else if(folderType == TargetedMSService.FolderType.Library)
        {
            conflictCount = getGeneralMoleculeConflictCount(container);
        }

       return conflictCount;
    }

    public static TargetedMSRun getOldestRunWithConflicts(Container container)
    {
        Integer runId = getOldestConflictRunId(container);
        return runId != null ? TargetedMSManager.getRun(runId) : null;
    }

    private static Integer getOldestConflictRunId(Container container)
    {
        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT r.id FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "p");
        sqlFragment.append(" INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sqlFragment.append(" ON p.GeneralMoleculeId = gm.Id ");
        sqlFragment.append(" INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sqlFragment.append(" ON gm.PeptideGroupId = pg.Id ");
        sqlFragment.append(" INNER JOIN ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(" ON pg.runId = r.Id ");
        sqlFragment.append(" WHERE ");
        sqlFragment.append(" r.Deleted = ? AND r.Container = ? ");
        sqlFragment.append(" AND p.RepresentativeDataState = ? ");
        sqlFragment.append(" ORDER BY r.Created ");

        sqlFragment.add(false);
        sqlFragment.add(container.getId());
        sqlFragment.add(RepresentativeDataState.Conflicted.ordinal());

        DbSchema schema = TargetedMSManager.getSchema();
        return new SqlSelector(schema, schema.getSqlDialect().limitRows(sqlFragment, 1)).getObject(Integer.class);
    }
}
