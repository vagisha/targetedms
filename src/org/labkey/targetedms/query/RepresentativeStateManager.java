package org.labkey.targetedms.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.PeptideGroup;

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
        TargetedMSManager.getSchema().getScope().ensureTransaction();
        try {
            int conflictCount = 0;
            if(state == TargetedMSRun.RepresentativeDataState.Representative_Protein)
            {
                if(run.getRepresentativeDataState() == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
                {
                    revertPeptideRepresentativeState(user, container, run);
                }
                conflictCount = resolveRepresentativeProteinState(container, run);
            }
            else if(state == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
            {
                // TODO
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

            TargetedMSManager.getSchema().getScope().commitTransaction();

            return conflictCount;
        }
        finally
        {
            TargetedMSManager.getSchema().getScope().closeConnection();
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
                lastDeprecatedGroup.setRepresentativeDataState(PeptideGroup.RepresentativeDataState.Representative);
                Table.update(user, TargetedMSManager.getTableInfoPeptideGroup(), lastDeprecatedGroup, lastDeprecatedGroup.getId());
            }
        }

        // Mark all proteins in this run as not representative
        PeptideGroupManager.setRepresentativeState(run.getId(), PeptideGroup.RepresentativeDataState.NotRepresentative);
    }

    private static void revertPeptideRepresentativeState(User user, Container container, TargetedMSRun run)
    {
        // TODO
    }

    private static int resolveRepresentativeProteinState(Container container, TargetedMSRun run) throws SQLException
    {
        // Mark everything in this run that doesn't already have representative data in this container as being active
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoPeptideGroup());
        // makeActiveSQL.append(" SET ActiveRepresentativeData = ? ");
        makeActiveSQL.append(" SET RepresentativeDataState = ?");
        // makeActiveSQL.add(true);
        makeActiveSQL.add(PeptideGroup.RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(" WHERE RunId=? ");
        makeActiveSQL.add(run.getId());
        makeActiveSQL.append(" AND( ");
        // If this peptide group has a SequenceId make sure we don't have another peptide group in this container
        // with the same SequenceId that has been previously marked as representative
        makeActiveSQL.append(" (SequenceId IS NOT NULL AND (SequenceId NOT IN (SELECT SequenceId FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        // makeActiveSQL.append(" WHERE pg1.RunId = r1.Id AND r1.Container=? AND pg1.ActiveRepresentativeData=?))) ");
        makeActiveSQL.append(" WHERE pg1.RunId = r1.Id AND r1.Container=? AND pg1.RepresentativeDataState=?))) ");
        makeActiveSQL.add(container);
        // makeActiveSQL.add(true);
        makeActiveSQL.add(PeptideGroup.RepresentativeDataState.Representative.ordinal());
        // If the peptide group does not have a SequenceId or there isn't an older peptide group with the same
        // SequenceId, compare the Labels to look for conflicting proteins.
        makeActiveSQL.append(" OR (Label NOT IN (SELECT Label FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        makeActiveSQL.append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r2");
        // makeActiveSQL.append(" WHERE pg2.RunID = r2.Id AND r2.Container=? AND pg2.ActiveRepresentativeData=?)) ");
        makeActiveSQL.append(" WHERE pg2.RunID = r2.Id AND r2.Container=? AND pg2.RepresentativeDataState=?)) ");
        makeActiveSQL.add(container);
        // makeActiveSQL.add(true);
        makeActiveSQL.add(PeptideGroup.RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(")");
        new SqlExecutor(TargetedMSManager.getSchema(), makeActiveSQL).execute();

        // Mark all other peptide groups as being in the conflicted state
        SQLFragment makeConflictedSQL = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        makeConflictedSQL.append(" SET RepresentativeDataState = ?");
        makeConflictedSQL.add(PeptideGroup.RepresentativeDataState.Conflicted.ordinal());
        makeConflictedSQL.append(" WHERE RunId=?");
        makeConflictedSQL.add(run.getId());
        makeConflictedSQL.append(" AND RepresentativeDataState != ?");
        makeConflictedSQL.add(PeptideGroup.RepresentativeDataState.Representative.ordinal());
        int conflictCount = new SqlExecutor(TargetedMSManager.getSchema(), makeConflictedSQL).execute();


        // See how many conflict with existing representative data
//        SQLFragment remainingConflictsSQL = new SQLFragment("SELECT COUNT(*) FROM ");
//        remainingConflictsSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
//        remainingConflictsSQL.append(" WHERE ActiveRepresentativeData = ? AND RunId = ?");
//        remainingConflictsSQL.add(false);
//        remainingConflictsSQL.add(run.getId());
//        int conflictCount = new SqlSelector(TargetedMSManager.getSchema(), remainingConflictsSQL).getObject(Integer.class);

//        if (conflictCount == 0)
//        {
//            _log.info("Run contains representative data. No conflicts with existing representative data in current container found");
//        }
//        else
//        {
//            _log.info("Run contains representative data. " + conflictCount + " conflicts with existing representative data in current container found, manual reconciliation required");
//        }

        return conflictCount;
    }
}
