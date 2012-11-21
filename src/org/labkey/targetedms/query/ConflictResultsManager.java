/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.labkey.targetedms.conflict.ConflictProtein;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;

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
        getConflictProteinsSql.add(PeptideGroup.RepresentativeDataState.Conflicted.ordinal());
        getConflictProteinsSql.add(PeptideGroup.RepresentativeDataState.Representative.ordinal());

        ConflictProtein[] pepGroups = new SqlSelector(TargetedMSManager.getSchema(), getConflictProteinsSql).getArray(ConflictProtein.class);
        return Arrays.asList(pepGroups);
    }

    public static List<ConflictPeptide> getConflictPeptidesForProteins(int newProteinId, int oldProteinId)
    {
        List<BestPrecursorPeptide> newProteinPeptides = getBestPrecursorsForProtein(newProteinId);
        List<BestPrecursorPeptide> oldProteinPeptides = getBestPrecursorsForProtein(oldProteinId);

        Map<String, ConflictPeptide> conflictPeptideMap = new HashMap<String, ConflictPeptide>();
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

        return new ArrayList<ConflictPeptide>(conflictPeptideMap.values());
    }

    private static List<BestPrecursorPeptide> getBestPrecursorsForProtein(int proteinId)
    {
        Collection<Peptide> peptides = PeptideManager.getPeptidesForGroup(proteinId);
        List<BestPrecursorPeptide> proteinPeptides = new ArrayList<BestPrecursorPeptide>();
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
}
