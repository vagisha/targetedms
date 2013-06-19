/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.conflict.ConflictPeptide;
import org.labkey.targetedms.conflict.ConflictPrecursor;
import org.labkey.targetedms.conflict.ConflictProtein;
import org.labkey.targetedms.conflict.ConflictTransition;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.Transition;
import org.labkey.targetedms.parser.TransitionChromInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

        ConflictProtein[] pepGroups = new SqlSelector(TargetedMSManager.getSchema(), getConflictProteinsSql).getArray(ConflictProtein.class);
        return Arrays.asList(pepGroups);
    }

    public static List<ConflictPrecursor> getConflictedPrecursors(Container container)
    {
        // Get a list of conflicted precursors in the given container
        SQLFragment getConflictPrecursorsSql = new SQLFragment("SELECT ");
        getConflictPrecursorsSql.append("prec.Id AS newPrecursorId, ");
        getConflictPrecursorsSql.append("r.Id AS newPrecursorRunId, ");
        getConflictPrecursorsSql.append("r.filename AS newRunFile, ");
        getConflictPrecursorsSql.append("prec2.Id AS oldPrecursorId, ");
        getConflictPrecursorsSql.append("r2.Id AS oldPrecursorRunId, ");
        getConflictPrecursorsSql.append("r2.filename AS oldRunFile");
        getConflictPrecursorsSql.append(" FROM ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoRuns(), "r");
        getConflictPrecursorsSql.append(", ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        getConflictPrecursorsSql.append(", ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        getConflictPrecursorsSql.append(", ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        getConflictPrecursorsSql.append(" INNER JOIN ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPrecursor(), "prec2");
        getConflictPrecursorsSql.append(" ON (prec.ModifiedSequence = prec2.ModifiedSequence AND prec.charge = prec2.Charge) ");
        getConflictPrecursorsSql.append(" INNER JOIN ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPeptide(), "pep2");
        getConflictPrecursorsSql.append(" ON (prec2.PeptideId = pep2.Id) ");
        getConflictPrecursorsSql.append(" INNER JOIN ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        getConflictPrecursorsSql.append(" ON (pg2.Id = pep2.PeptideGroupId) ");
        getConflictPrecursorsSql.append(" INNER JOIN ");
        getConflictPrecursorsSql.append(TargetedMSManager.getTableInfoRuns(), "r2");
        getConflictPrecursorsSql.append(" ON (r2.Id = pg2.RunId) ");
        getConflictPrecursorsSql.append(" WHERE r.Id = pg.RunId ");
        getConflictPrecursorsSql.append(" AND pg.Id = pep.PeptideGroupId");
        getConflictPrecursorsSql.append(" AND pep.Id = prec.PeptideId");
        getConflictPrecursorsSql.append(" AND r.Container=? " );
        getConflictPrecursorsSql.append(" AND r2.Container=? ");
        getConflictPrecursorsSql.append(" AND prec.RepresentativeDataState=? ");
        getConflictPrecursorsSql.append(" AND prec2.RepresentativeDataState=? ");
        getConflictPrecursorsSql.add(container);
        getConflictPrecursorsSql.add(container);
        getConflictPrecursorsSql.add(RepresentativeDataState.Conflicted.ordinal());
        getConflictPrecursorsSql.add(RepresentativeDataState.Representative.ordinal());

        ConflictPrecursor[] precursors = new SqlSelector(TargetedMSManager.getSchema(), getConflictPrecursorsSql).getArray(ConflictPrecursor.class);
        return Arrays.asList(precursors);
    }


    public static List<ConflictPeptide> getConflictPeptidesForProteins(int newProteinId, int oldProteinId)
    {
        List<BestPrecursorPeptide> newProteinPeptides = getBestPrecursorsForProtein(newProteinId);
        List<BestPrecursorPeptide> oldProteinPeptides = getBestPrecursorsForProtein(oldProteinId);

        Map<String, ConflictPeptide> conflictPeptideMap = new HashMap<>();
        for(BestPrecursorPeptide peptide: newProteinPeptides)
        {
            ConflictPeptide cPeptide = new ConflictPeptide();
            cPeptide.setNewPeptide(peptide.getPeptide());
            cPeptide.setNewPeptidePrecursor(peptide.getPrecursor());
            cPeptide.setNewPeptideRank(peptide.getRank());

            if(conflictPeptideMap.containsKey(peptide.getPeptide().getSequence()))
            {
                throw new IllegalStateException("Peptide "+peptide.getPeptide().getSequence()+" has been seen already for peptide group ID "+newProteinId);
            }
            conflictPeptideMap.put(peptide.getPeptide().getSequence(), cPeptide);
        }

        for(BestPrecursorPeptide peptide: oldProteinPeptides)
        {
            ConflictPeptide cPeptide = conflictPeptideMap.get(peptide.getPeptide().getSequence());
            if(cPeptide == null)
            {
                cPeptide = new ConflictPeptide();
                conflictPeptideMap.put(peptide.getPeptide().getSequence(), cPeptide);
            }
            cPeptide.setOldPeptide(peptide.getPeptide());
            cPeptide.setOldPeptidePrecursor(peptide.getPrecursor());
            cPeptide.setOldPeptideRank(peptide.getRank());
        }

        return new ArrayList<>(conflictPeptideMap.values());
    }

    private static List<BestPrecursorPeptide> getBestPrecursorsForProtein(int proteinId)
    {
        Collection<Peptide> peptides = PeptideManager.getPeptidesForGroup(proteinId);
        List<BestPrecursorPeptide> proteinPeptides = new ArrayList<>();
        for(Peptide peptide: peptides)
        {
            BestPrecursorPeptide bestPrecursor = getBestPrecursor(peptide);
            proteinPeptides.add(bestPrecursor);
        }
        Collections.sort(proteinPeptides, new Comparator<BestPrecursorPeptide>()
        {
            @Override
            public int compare(BestPrecursorPeptide o1, BestPrecursorPeptide o2)
            {
                return Double.valueOf(o2.getAvgArea()).compareTo(o1.getAvgArea());
            }
        });
        int rank = 1;
        for(BestPrecursorPeptide bestPrecursor: proteinPeptides)
        {
            bestPrecursor.setRank(rank);
            rank++;
        }

        return proteinPeptides;
    }

    private static BestPrecursorPeptide getBestPrecursor(Peptide peptide)
    {
        List<PrecursorChromInfoPlus> pciPlusList = PrecursorManager.getPrecursorChromInfosForPeptide(peptide.getId());
        Collections.sort(pciPlusList, new Comparator<PrecursorChromInfoPlus>()
        {
            @Override
            public int compare(PrecursorChromInfoPlus o1, PrecursorChromInfoPlus o2)
            {
                return Integer.valueOf(o1.getPrecursorId()).compareTo(o2.getPrecursorId());
            }
        });

        int bestPrecursorId = 0;
        double bestAvgArea = 0; // avg area across all replicates
        int currentPrecursorId = 0;
        double currentTotalArea = 0;
        int currentPciCount = 0;

        for(PrecursorChromInfoPlus pciPlus: pciPlusList)
        {
            if(pciPlus.getPrecursorId() != currentPrecursorId)
            {
                if(currentPrecursorId != 0 && currentPciCount > 0)
                {
                    double currentAvgArea = currentTotalArea / currentPciCount;
                    if(currentAvgArea > bestAvgArea)
                    {
                        bestAvgArea = currentAvgArea;
                        bestPrecursorId = currentPrecursorId;
                    }
                }
                currentPciCount = 0;
                currentTotalArea = 0.0;
                currentPrecursorId = 0;
            }
            if(pciPlus.getTotalArea() != null)
            {
                currentPrecursorId = pciPlus.getPrecursorId();
                currentPciCount++;
                currentTotalArea += pciPlus.getTotalArea();
            }
        }

        // last one
        if(currentPrecursorId != 0 && currentPciCount > 0)
        {
            double currentAvgArea = currentTotalArea / currentPciCount;
            if(currentAvgArea > bestAvgArea)
            {
                bestAvgArea = currentAvgArea;
                bestPrecursorId = currentPrecursorId;
            }
        }

        BestPrecursorPeptide bestPrecursorPeptide = new BestPrecursorPeptide();
        bestPrecursorPeptide.setPeptide(peptide);
        bestPrecursorPeptide.setPrecursor(PrecursorManager.get(bestPrecursorId));
        bestPrecursorPeptide.setAvgArea(bestAvgArea);
        return bestPrecursorPeptide;
    }

    private static class BestPrecursorPeptide
    {
        private Peptide _peptide;
        private Precursor _precursor;
        private double _avgArea;
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

    public static List<ConflictTransition> getConflictTransitionsForPrecursors(int newPrecursorId, int oldPrecursorId)
    {
        List<TransitionWithAreaAndRank> newPrecursorTransitions = getRankedTransitionsForPrecursor(newPrecursorId);
        List<TransitionWithAreaAndRank> oldPrecursorTransitions = getRankedTransitionsForPrecursor(oldPrecursorId);

        Precursor newPrecursor = PrecursorManager.get(newPrecursorId);
        Precursor oldPrecursor = PrecursorManager.get(oldPrecursorId);

        // Key in the conflictTransitionMap is the transition label (y7, y8, etc.)
        Map<String, ConflictTransition> conflictTransitionMap = new HashMap<>();
        for(TransitionWithAreaAndRank twr: newPrecursorTransitions)
        {
            ConflictTransition cTransition = new ConflictTransition();
            cTransition.setNewPrecursor(newPrecursor);
            cTransition.setNewTransition(twr.getTransition());
            cTransition.setNewTransitionRank(twr.getRank());

            String transitionLabel = twr.getTransition().getLabel();
            if(conflictTransitionMap.containsKey(transitionLabel))
            {
                throw new IllegalStateException("Transition "+transitionLabel+" has been seen already for precursor "+newPrecursor.getModifiedSequence()+" with ID "+newPrecursorId);
            }
            conflictTransitionMap.put(transitionLabel, cTransition);
        }

        for(TransitionWithAreaAndRank twr: oldPrecursorTransitions)
        {
            String transitionLabel = twr.getTransition().getLabel();
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

    public static List<TransitionWithAreaAndRank> getRankedTransitionsForPrecursor(int precursorId)
    {
        Collection<Transition> transitions = TransitionManager.getTransitionsForPrecursor(precursorId);

        // Each transition may have been measured in more than one replicate. We need to get the
        // the average area for each transition across all replicates
        List<TransitionWithAreaAndRank> transWithRankList = new ArrayList<>(transitions.size());
        for(Transition transition: transitions)
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
            twr.setAvgArea((transChromInfoList == null || transChromInfoList.size() == 0) ? 0 : totalArea / transChromInfoList.size());
            transWithRankList.add(twr);
        }

        // Sort by average area.
        Collections.sort(transWithRankList, new Comparator<TransitionWithAreaAndRank>()
        {
            @Override
            public int compare(TransitionWithAreaAndRank o1, TransitionWithAreaAndRank o2)
            {
                return Double.valueOf(o2.getAvgArea()).compareTo(o1.getAvgArea());
            }
        });

        int rank = 1;
        for(TransitionWithAreaAndRank twr: transWithRankList)
        {
            twr.setRank(rank++);
        }

        return transWithRankList;
    }

    private static class TransitionWithAreaAndRank
    {
        private Transition _transition;
        private double _avgArea;
        private int _rank;

        public Transition getTransition()
        {
            return _transition;
        }

        public void setTransition(Transition transition)
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
}
