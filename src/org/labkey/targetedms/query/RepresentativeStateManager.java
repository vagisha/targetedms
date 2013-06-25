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
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMsRepresentativeStateAuditViewFactory;
import org.labkey.targetedms.chromlib.ChromatogramLibraryUtils;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.sql.SQLException;
import java.util.List;

/**
 * User: vsharma
 * Date: 11/19/12
 * Time: 9:54 PM
 */
public class RepresentativeStateManager
{
    private RepresentativeStateManager() {}

    public static int setRepresentativeState(User user, Container container,
                                             TargetedMSRun run, TargetedMSRun.RepresentativeDataState state)
                                             throws SQLException
    {
        try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction())
        {
            int conflictCount = 0;
            if(state == TargetedMSRun.RepresentativeDataState.Representative_Protein)
            {
                conflictCount = resolveRepresentativeProteinState(container, run);
                conflictCount += resolveRepresentativePeptideState(container, run);
            }
            else if(state == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
            {
                if(run.getRepresentativeDataState() == TargetedMSRun.RepresentativeDataState.Representative_Protein)
                {
                    revertProteinRepresentativeState(user, container, run);
                }
                conflictCount = resolveRepresentativePeptideState(container, run);
            }
            else if(state == TargetedMSRun.RepresentativeDataState.NotRepresentative)
            {
                TargetedMSRun.RepresentativeDataState currentState = run.getRepresentativeDataState();
                if(currentState == TargetedMSRun.RepresentativeDataState.Representative_Protein)
                {
                    revertProteinRepresentativeState(user, container, run);
                }
                else if(currentState == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
                {
                    revertPeptideRepresentativeState(user, container, run);
                }
            }
            else
                throw new IllegalArgumentException("Unrecognized representative data state: "+state);

            run.setRepresentativeDataState(state);
            run = Table.update(user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

                        // Increment the chromatogram library revision number for this container.
            ChromatogramLibraryUtils.incrementLibraryRevision(container);

            // Add event to audit log.
            TargetedMsRepresentativeStateAuditViewFactory.addAuditEntry(container, user,
                    "Updated representative state. Number of conflicts " + conflictCount);

            transaction.commit();
            return conflictCount;
        }
    }

    private static void revertProteinRepresentativeState(User user, Container container, TargetedMSRun run) throws SQLException
    {
        // Get a list of proteins in this run that are marked as representative.
        List<PeptideGroup> representativeGroups = PeptideGroupManager.getRepresentativePeptideGroups(run.getRunId());

        // Roll back representative state to the most recently deprecated proteins in other runs
        // in this container(folder).
        for(PeptideGroup pepGrp: representativeGroups)
        {
            PeptideGroup lastDeprecatedGroup = PeptideGroupManager.getLastDeprecatedPeptideGroup(pepGrp, container);
            if(lastDeprecatedGroup != null)
            {
                // Mark the last deprecated protein as representative
                lastDeprecatedGroup.setRepresentativeDataState(RepresentativeDataState.Representative);
                Table.update(user, TargetedMSManager.getTableInfoPeptideGroup(), lastDeprecatedGroup, lastDeprecatedGroup.getId());
            }
        }

        // Mark all proteins in this run as not representative
        PeptideGroupManager.setRepresentativeState(run.getId(), RepresentativeDataState.NotRepresentative);
    }

    private static void revertPeptideRepresentativeState(User user, Container container, TargetedMSRun run) throws SQLException
    {
        // Get a list of precursors in this run that are marked as representative.
        List<Precursor> representativePrecursors = PrecursorManager.getRepresentativePrecursors(run.getRunId());

        // Roll back representative state to the most recently deprecated precursors in other runs
        // in this container(folder).
        for(Precursor prec: representativePrecursors)
        {
            Precursor lastDeprecatedPrec = PrecursorManager.getLastDeprecatedPrecursor(prec, container);
            if(lastDeprecatedPrec != null)
            {
                // Mark the last deprecated precursor as representative
                lastDeprecatedPrec.setRepresentativeDataState(RepresentativeDataState.Representative);
                Table.update(user, TargetedMSManager.getTableInfoPrecursor(), lastDeprecatedPrec, lastDeprecatedPrec.getId());
            }
        }

        // Mark all precursors in this run as not representative
        PrecursorManager.setRepresentativeState(run.getId(), RepresentativeDataState.NotRepresentative);
    }

    private static int resolveRepresentativeProteinState(Container container, TargetedMSRun run) throws SQLException
    {
        // Mark everything in this run that doesn't already have representative data in this container as being active
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoPeptideGroup());
        makeActiveSQL.append(" SET RepresentativeDataState = ?");
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(" WHERE RunId=? ");
        makeActiveSQL.add(run.getId());
        makeActiveSQL.append(" AND( ");
        // If this peptide group has a SequenceId make sure we don't have another peptide group in this container
        // with the same SequenceId that has been previously marked as representative
        makeActiveSQL.append(" (SequenceId IS NOT NULL AND (SequenceId NOT IN (SELECT SequenceId FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        makeActiveSQL.append(" WHERE pg1.RunId = r1.Id AND r1.Container=? AND pg1.RepresentativeDataState=?))) ");
        makeActiveSQL.add(container);
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        // If the peptide group does not have a SequenceId or there isn't an older peptide group with the same
        // SequenceId, compare the Labels to look for conflicting proteins.
        makeActiveSQL.append(" OR (Label NOT IN (SELECT Label FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r2");
        makeActiveSQL.append(" WHERE pg2.RunID = r2.Id AND r2.Container=? AND pg2.RepresentativeDataState=?)) ");
        makeActiveSQL.add(container);
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(")");
        new SqlExecutor(TargetedMSManager.getSchema()).execute(makeActiveSQL);

        // Mark all other peptide groups as being in the conflicted state
        SQLFragment makeConflictedSQL = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        makeConflictedSQL.append(" SET RepresentativeDataState = ?");
        makeConflictedSQL.add(RepresentativeDataState.Conflicted.ordinal());
        makeConflictedSQL.append(" WHERE RunId=?");
        makeConflictedSQL.add(run.getId());
        makeConflictedSQL.append(" AND RepresentativeDataState != ?");
        makeConflictedSQL.add(RepresentativeDataState.Representative.ordinal());
        int conflictCount = new SqlExecutor(TargetedMSManager.getSchema()).execute(makeConflictedSQL);

        return conflictCount;
    }

    private static int resolveRepresentativePeptideState(Container container, TargetedMSRun run) throws SQLException
    {
        // Mark everything in this run that doesn't already have representative data in this container as being active
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoPrecursor());
        makeActiveSQL.append(" SET RepresentativeDataState = ?");
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(" FROM "+TargetedMSManager.getTableInfoPeptide());
        makeActiveSQL.append(", "+TargetedMSManager.getTableInfoPeptideGroup());
        makeActiveSQL.append(", "+TargetedMSManager.getTableInfoRuns());
        makeActiveSQL.append(" WHERE "+TargetedMSManager.getTableInfoPeptideGroup()+".RunId=? ");
        makeActiveSQL.add(run.getId());
        makeActiveSQL.append(" AND "+TargetedMSManager.getTableInfoPrecursor()+".PeptideId = "+TargetedMSManager.getTableInfoPeptide()+".Id");
        makeActiveSQL.append(" AND "+TargetedMSManager.getTableInfoPeptide()+".PeptideGroupId = "+TargetedMSManager.getTableInfoPeptideGroup()+".Id");

        makeActiveSQL.append(" AND");
        makeActiveSQL.append(" ModifiedSequence NOT IN (SELECT ModifiedSequence FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPrecursor(), "prec1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptide(), "pep1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        makeActiveSQL.append(" WHERE pg1.RunId = r1.Id ");
        makeActiveSQL.append(" AND");
        makeActiveSQL.append(" pep1.PeptideGroupId = pg1.Id");
        makeActiveSQL.append(" AND");
        makeActiveSQL.append(" prec1.PeptideId = pep1.Id");
        makeActiveSQL.append(" AND");
        makeActiveSQL.append(" r1.Container=?");
        makeActiveSQL.append(" AND");
        makeActiveSQL.append(" prec1.RepresentativeDataState=? ");
        makeActiveSQL.add(container);
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(")");
        new SqlExecutor(TargetedMSManager.getSchema()).execute(makeActiveSQL);

        // Mark all other precursors as being in the conflicted state
        SQLFragment makeConflictedSQL = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPrecursor());
        makeConflictedSQL.append(" SET RepresentativeDataState = ?");
        makeConflictedSQL.add(RepresentativeDataState.Conflicted.ordinal());
        makeConflictedSQL.append(" FROM "+TargetedMSManager.getTableInfoPeptide());
        makeConflictedSQL.append(", "+TargetedMSManager.getTableInfoPeptideGroup());
        makeConflictedSQL.append(" WHERE " + TargetedMSManager.getTableInfoPeptideGroup() + ".RunId=?");
        makeConflictedSQL.add(run.getId());
        makeConflictedSQL.append(" AND " + TargetedMSManager.getTableInfoPrecursor() + ".RepresentativeDataState != ?");
        makeConflictedSQL.add(RepresentativeDataState.Representative.ordinal());
        makeConflictedSQL.append(" AND "+TargetedMSManager.getTableInfoPrecursor()+".PeptideId = "+TargetedMSManager.getTableInfoPeptide()+".Id");
        makeConflictedSQL.append(" AND "+TargetedMSManager.getTableInfoPeptide()+".PeptideGroupId = "+TargetedMSManager.getTableInfoPeptideGroup()+".Id");
        int conflictCount = new SqlExecutor(TargetedMSManager.getSchema()).execute(makeConflictedSQL);

        return conflictCount;
    }
}
