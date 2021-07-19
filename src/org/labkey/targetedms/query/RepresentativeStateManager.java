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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.RunRepresentativeDataState;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMsRepresentativeStateAuditProvider;
import org.labkey.targetedms.chromlib.ChromatogramLibraryUtils;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Precursor;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * User: vsharma
 * Date: 11/19/12
 * Time: 9:54 PM
 */
public class RepresentativeStateManager
{
    private RepresentativeStateManager() {}

    public static void setRepresentativeState(User user, Container container, LocalDirectory localDirectory,
                                              TargetedMSRun run, RunRepresentativeDataState state)
    {
        try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction())
        {
            int conflictCount = 0;
            if(state == RunRepresentativeDataState.Representative_Protein)
            {
                conflictCount = resolveRepresentativeProteinState(container, run);
            }
            else if(state == RunRepresentativeDataState.Representative_Peptide)
            {
                conflictCount = resolveRepresentativeState(container, run, TargetedMSManager.getTableInfoPeptide(),
                        TargetedMSManager.getTableInfoPrecursor(),
                        new SQLFragment(" AND (m.Decoy IS NULL OR m.Decoy = ?)", false), (p, gp) -> p + ".ModifiedSequence");
                conflictCount += resolveRepresentativeState(container, run, TargetedMSManager.getTableInfoMolecule(),
                        TargetedMSManager.getTableInfoMoleculePrecursor(),
                        new SQLFragment(), (p, gp) ->
                                TargetedMSManager.getSqlDialect().concatenate(
                                        "COALESCE(" + p + ".IonFormula, '')",
                                        "' '",
                                        "COALESCE(" + p + ".CustomIonName, '')",
                                        "' - '",
                                        "CAST(" + gp + ".mz AS VARCHAR)"));
            }
            else if(state == RunRepresentativeDataState.NotRepresentative)
            {
                RunRepresentativeDataState currentState = run.getRepresentativeDataState();
                if(currentState == RunRepresentativeDataState.Representative_Protein)
                {
                    revertProteinRepresentativeState(user, container, run);
                }
                else if(currentState == RunRepresentativeDataState.Representative_Peptide)
                {
                    revertPeptideRepresentativeState(user, container, run);
                    revertMoleculeRepresentativeState(user, container, run);

                    // Mark all precursors in this run as not representative
                    PrecursorManager.setRepresentativeState(run.getId(), RepresentativeDataState.NotRepresentative);

                }
            }
            else
                throw new IllegalArgumentException("Unrecognized representative data state: "+state);

            run.setRepresentativeDataState(state);

            // If there are runs in the container that no longer have any representative data mark
            // them as being not representative.
            TargetedMSManager.markRunsNotRepresentative(container, RunRepresentativeDataState.Representative_Protein);

            // Increment the chromatogram library revision number for this container.
            ChromatogramLibraryUtils.incrementLibraryRevision(container, user, localDirectory);

            // Add event to audit log.
            TargetedMsRepresentativeStateAuditProvider.addAuditEntry(container, user,
                    "Updated representative state. Number of conflicts " + conflictCount);

            Table.update(user, TargetedMSManager.getTableInfoRuns(), run, run.getId());

            transaction.commit();
        }
    }

    private static void revertProteinRepresentativeState(User user, Container container, TargetedMSRun run)
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

                // Set the representative state of all the precursors in this peptide group to be the same
                // as the representative state of the peptide group
                updatePrecursorRepresentativeState(lastDeprecatedGroup);
            }
        }

        // Mark all proteins in this run as not representative
        PeptideGroupManager.setRepresentativeState(run.getId(), RepresentativeDataState.NotRepresentative);

        // Set the representative state of all the precursors in this run to not-representative
        updatePrecursorRepresentativeState(run);
    }

    private static void revertPeptideRepresentativeState(User user, Container container, TargetedMSRun run)
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
                Table.update(user, TargetedMSManager.getTableInfoGeneralPrecursor(), lastDeprecatedPrec, lastDeprecatedPrec.getId());
            }
        }
    }

    private static void revertMoleculeRepresentativeState(User user, Container container, TargetedMSRun run)
    {
        // Get a list of precursors in this run that are marked as representative.
        List<MoleculePrecursor> representativePrecursors = MoleculePrecursorManager.getRepresentativeMoleculePrecursors(run.getRunId());

        // Roll back representative state to the most recently deprecated precursors in other runs
        // in this container(folder).
        for(MoleculePrecursor prec: representativePrecursors)
        {
            MoleculePrecursor lastDeprecatedPrec = MoleculePrecursorManager.getLastDeprecatedMoleculePrecursor(prec, container);
            if(lastDeprecatedPrec != null)
            {
                // Mark the last deprecated precursor as representative
                lastDeprecatedPrec.setRepresentativeDataState(RepresentativeDataState.Representative);
                Table.update(user, TargetedMSManager.getTableInfoGeneralPrecursor(), lastDeprecatedPrec, lastDeprecatedPrec.getId());
            }
        }
    }

    private static int resolveRepresentativeProteinState(Container container, TargetedMSRun run)
    {
        // Get a list of peptide group ids that only have either decoy or standard peptides
        List<Long> peptideGroupIdsToExclude = getAllDecoyOrStandardPeptideGroups(run);

        // Mark everything in this run that doesn't already have representative data in this container as being active
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoPeptideGroup());
        makeActiveSQL.append(" SET RepresentativeDataState = ?");
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(" WHERE RunId=? ");
        makeActiveSQL.add(run.getId());
        if(peptideGroupIdsToExclude.size() > 0)
        {
            makeActiveSQL.append(" AND Id NOT IN (" + StringUtils.join(peptideGroupIdsToExclude, ',') + ")");
        }
        makeActiveSQL.append(" AND Id NOT IN ( ");
        // Issue 40407: Chromatogram library folder issues
        // We will ignore (not mark as representative) all proteins that are already in the library - i.e. the protein
        // either has a sequenceId that is the same as a library protein OR it has a label that is the same as a
        // library protein. Library proteins are the ones that have their RepresentativeDataState set to
        // RepresentativeDataState.Representative.ordinal()

        // Get protein Ids that have the same sequenceId as a library protein
        /*
        SELECT id from targetedms.peptidegroup where runId = <runId> AND sequenceId IN
   	    (SELECT sequenceId from targetedms.peptidegroup pg1 INNER JOIN targetedms.runs r1 ON pg1.RunId = r1.Id
        WHERE r1.Container=<container> AND pg1.RepresentativeDataState=1 AND pg1.SequenceId IS NOT NULL
         */
        makeActiveSQL.append(" (SELECT Id FROM ").append(TargetedMSManager.getTableInfoPeptideGroup(), "pg_seq");
        makeActiveSQL.append(" WHERE RunId = ?").add(run.getId());
        makeActiveSQL.append(" AND SequenceId IN ( ");
        makeActiveSQL.append(" SELECT SequenceId FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1");
        makeActiveSQL.append(" INNER JOIN ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        makeActiveSQL.append(" ON pg1.RunID = r1.Id ");
        makeActiveSQL.append(" WHERE r1.Container=? AND pg1.RepresentativeDataState=? ");
        makeActiveSQL.append(" AND  pg1.SequenceId IS NOT NULL)) ");
        makeActiveSQL.add(container);
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());

        makeActiveSQL.append(" UNION ");

        // Get protein Ids that have the same label as a library protein
        /*
        SELECT id from targetedms.peptidegroup where runId = <runId> AND Label in
        (SELECT Label from targetedms.peptidegroup pg2 INNER JOIN targetedms.runs r2 ON pg2.RunId = r2.Id
        WHERE r2.Container=<container> AND pg2.RepresentativeDataState=1
         */
        makeActiveSQL.append(" (SELECT Id FROM ").append(TargetedMSManager.getTableInfoPeptideGroup(), "pg_label");
        makeActiveSQL.append(" WHERE RunId = ?").add(run.getId());
        makeActiveSQL.append(" AND Label IN ( ");
        makeActiveSQL.append(" SELECT Label FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg2");
        makeActiveSQL.append(" INNER JOIN ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r2");
        makeActiveSQL.append(" ON pg2.RunID = r2.Id ");
        makeActiveSQL.append(" WHERE r2.Container=? AND pg2.RepresentativeDataState=?)) ");
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
        if(peptideGroupIdsToExclude.size() > 0)
        {
            makeConflictedSQL.append(" AND Id NOT IN (" + StringUtils.join(peptideGroupIdsToExclude, ',') + ")");
        }
        makeConflictedSQL.append(" AND RepresentativeDataState != ?");
        makeConflictedSQL.add(RepresentativeDataState.Representative.ordinal());
        int conflictCount = new SqlExecutor(TargetedMSManager.getSchema()).execute(makeConflictedSQL);

        // Issue 23843: Include iRT peptides in chromatogram libraries
        // We do not want users to have to resolve conflicts for standard peptides/proteins.  The latest version of the
        // "standard" protein (containing only standard peptides) will be marked as representative.
        // If this run has "standard" proteins, mark them as being representative.
        // Older versions of the proteins, from previous runs, will be marked as deprecated.
        List<Long> standardPeptideGroupIds = getAllStandardPeptideGroups(run);
        if(standardPeptideGroupIds != null && standardPeptideGroupIds.size() > 0)
        {
            updateStandardPeptideGroups(container, standardPeptideGroupIds);
        }

        // Set the representative state of all the precursors in this run to be the same
        // as the representative state of the proteins
        updatePrecursorRepresentativeState(run);


        return conflictCount;
    }

    private static void updateStandardPeptideGroups(Container container, List<Long> stdPepGrpIdsInRun)
    {
        // Get a list of the current standard peptide group ids in the folder that have the same label
        // as the standard peptide groups in the new run.
        List<Long> currentStdPepGrpIds = getCurrentStandardPeptideGroupIds(container, stdPepGrpIdsInRun);

        // Set RepresentativeDataState of the new ones to Representative.
        PeptideGroupManager.updateRepresentativeStatus(stdPepGrpIdsInRun, RepresentativeDataState.Representative);

        // Set old ones to Representative_Deprecated.
        PeptideGroupManager.updateStatusToDeprecatedOrNotRepresentative(currentStdPepGrpIds);
    }

    private static List<Long> getCurrentStandardPeptideGroupIds(Container container, List<Long> stdPepGrpIdsInRun)
    {
        SQLFragment sql = new SQLFragment("SELECT pg.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" ON (run.Id = pg.RunId AND run.Container=?) ");
        sql.add(container);
        sql.append(" WHERE pg.Label IN (");
        sql.append(" SELECT Label FROM ").append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(" WHERE Id IN (");
        sql.append(StringUtils.join(stdPepGrpIdsInRun, ","));
        sql.append(")");
        sql.append(")");
        sql.append(" AND pg.RepresentativeDataState=").append(RepresentativeDataState.Representative.ordinal());

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Long.class);
    }

    private static List<Long> getAllDecoyOrStandardPeptideGroups(TargetedMSRun run)
    {
        // Get a list of peptide group ids that only have either decoy or standard peptides
        return getFilteredPeptideGroupIds(run, true, true);
    }

    private static List<Long> getAllStandardPeptideGroups(TargetedMSRun run)
    {
        // Get a list of peptide group ids that only have either decoy or standard peptides
        return getFilteredPeptideGroupIds(run, true, false);
    }

    private static List<Long> getFilteredPeptideGroupIds(TargetedMSRun run, boolean includeStandard, boolean includeDecoy)
    {
        // Get a list of peptide group ids that only have either decoy or standard peptides
        SQLFragment sql = new SQLFragment("SELECT DISTINCT Id FROM " + TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" WHERE RunId = ? ");
        sql.add(run.getId());
        sql.append(" AND Id NOT IN (");
        sql.append(" SELECT pg.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" WHERE pep.Id = gm.Id");
        sql.append(" AND ");
        sql.append("gm.PeptideGroupId = pg.Id");
        sql.append(" AND pg.RunId = ?");
        sql.add(run.getId());
        if(includeDecoy)
        {
            sql.append(" AND (pep.Decoy IS NULL OR pep.Decoy = " +
                    TargetedMSManager.getSqlDialect().getBooleanFALSE() + ")");
        }
        if(includeStandard)
        {
            sql.append(" AND gm.StandardType IS NULL ");
        }
        sql.append(")");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Long.class);
    }

    private static List<Long> getStdPrecursorIdsInRun(TargetedMSRun run)
    {
        // Get a list of peptide group ids that only have either decoy or standard peptides
        SQLFragment sql = new SQLFragment("SELECT gp.Id FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(" ON pep.Id = gp.GeneralMoleculeId");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" ON (gm.Id = gp.GeneralMoleculeId AND pep.Id = gm.Id AND gm.StandardType IS NOT NULL) ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (pg.Id = gm.PeptideGroupId AND pg.RunId = ?) ");
        sql.add(run.getId());

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Long.class);
    }

    private static List<Long> getCurrentStandardPrecursorIds(Container container, List<Long> stdPrecursorIdsInRun, TableInfo precursorTable, BiFunction<String, String, String> joinValue)
    {
        if(stdPrecursorIdsInRun == null || stdPrecursorIdsInRun.size() == 0)
        {
            return Collections.emptyList();
        }
        SQLFragment sql = new SQLFragment("SELECT pre2.Id FROM (");
        sql.append(precursorTable, "pre1");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp1");
        sql.append(" ON pre1.Id = gp1.Id");
        sql.append(") INNER JOIN (");
        sql.append(precursorTable, "pre2");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp2");
        sql.append(" ON pre2.Id = gp2.Id)");
        sql.append(" ON ((");
        sql.append(TargetedMSManager.getSqlDialect().concatenate(joinValue.apply("pre1", "gp1"), "CAST(gp1.Charge AS varchar)"));
        sql.append(") = (");
        sql.append(TargetedMSManager.getSqlDialect().concatenate(joinValue.apply("pre2", "gp2"), "CAST(gp2.Charge AS varchar)"));
        sql.append("))");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(" ON gp.Id = pre2.Id AND gp.RepresentativeDataState=?");
        sql.add(RepresentativeDataState.Representative.ordinal());
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(" ON (gm.Id = gp.GeneralMoleculeId) ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" ON (pg.Id = gm.PeptideGroupId) ");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" ON (run.Id = pg.RunId AND run.Container=?) ");
        sql.add(container);
        sql.append(" WHERE pre1.Id in (").append(StringUtils.join(stdPrecursorIdsInRun, ",")).append(")");

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Long.class);
    }

    private static void updatePrecursorRepresentativeState(TargetedMSRun run)
    {
       updatePrecursorRepresentativeState("pg.RunId", run.getId());
    }

    private static void updatePrecursorRepresentativeState(PeptideGroup peptideGroup)
    {
        updatePrecursorRepresentativeState("pg.Id", peptideGroup.getId());
    }

    private static void updatePrecursorRepresentativeState(String refCol, long refId)
    {
        SQLFragment updatePrecursorStateSQL = new SQLFragment();
        updatePrecursorStateSQL.append("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        updatePrecursorStateSQL.append(" SET RepresentativeDataState = pg.RepresentativeDataState ");
        updatePrecursorStateSQL.append(" FROM ");
        updatePrecursorStateSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        updatePrecursorStateSQL.append(", ");
        updatePrecursorStateSQL.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        updatePrecursorStateSQL.append(", ");
        updatePrecursorStateSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        updatePrecursorStateSQL.append(" WHERE pep.Id = gm.Id");
        updatePrecursorStateSQL.append(" AND pg.Id = gm.peptideGroupId");
        updatePrecursorStateSQL.append(" AND pep.Id = "+TargetedMSManager.getTableInfoGeneralPrecursor()+".GeneralMoleculeId");
        updatePrecursorStateSQL.append(" AND " + refCol + " = ?");
        // Ignore decoy peptides
        updatePrecursorStateSQL.append(" AND ( pep.Decoy IS NULL ");
        updatePrecursorStateSQL.append(" OR ");
        updatePrecursorStateSQL.append( "pep.Decoy = " + TargetedMSManager.getSqlDialect().getBooleanFALSE());
        updatePrecursorStateSQL.append(") ");
        updatePrecursorStateSQL.add(refId);

        new SqlExecutor(TargetedMSManager.getSchema()).execute(updatePrecursorStateSQL);
    }

    private static int resolveRepresentativeState(Container container, TargetedMSRun run, TableInfo moleculeTable,
                                                  TableInfo precursorTable,
                                                  SQLFragment moleculeFilterSQL, BiFunction<String, String, String> joinValue)
    {
        // Mark everything in this run that doesn't already have representative data in this container as being representative
        SQLFragment makeActiveSQL = new SQLFragment("UPDATE " + TargetedMSManager.getTableInfoGeneralPrecursor());
        makeActiveSQL.append(" SET RepresentativeDataState = ? FROM ");
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(moleculeTable, "m").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(", ");
        makeActiveSQL.append(precursorTable, "pre").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r");
        makeActiveSQL.append(" WHERE pg.RunId = ? ");
        makeActiveSQL.add(run.getId());
        makeActiveSQL.append(" AND pre.Id = " + TargetedMSManager.getTableInfoGeneralPrecursor() + ".Id");
        makeActiveSQL.append(" AND " + TargetedMSManager.getTableInfoGeneralPrecursor()+".GeneralMoleculeId = m.Id");
        makeActiveSQL.append(" AND m.Id = gm.Id");
        makeActiveSQL.append(" AND gm.PeptideGroupId = pg.Id");
        // Ignore standard (e.g. iRT) peptides
        makeActiveSQL.append(" AND gm.StandardType IS NULL");
        makeActiveSQL.append(moleculeFilterSQL);
        makeActiveSQL.append(" AND ");
        makeActiveSQL.append(TargetedMSManager.getSqlDialect().concatenate(joinValue.apply("pre", TargetedMSManager.getTableInfoGeneralPrecursor().toString()), "CAST(Charge AS varchar)", "CAST(mz AS VARCHAR)"));
        makeActiveSQL.append(" NOT IN (SELECT ");
        makeActiveSQL.append(TargetedMSManager.getSqlDialect().concatenate(joinValue.apply("precur1", "prec1"), "CAST(Charge AS varchar)", "CAST(mz AS VARCHAR)"));
        makeActiveSQL.append(" FROM ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "prec1").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm1").append(", ");
        makeActiveSQL.append(precursorTable, "precur1").append(", ");
        makeActiveSQL.append(moleculeTable, "m1").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg1").append(", ");
        makeActiveSQL.append(TargetedMSManager.getTableInfoRuns(), "r1");
        makeActiveSQL.append(" WHERE pg1.RunId = r1.Id ");
        makeActiveSQL.append(" AND m1.Id = gm1.Id");
        makeActiveSQL.append(" AND gm1.PeptideGroupId = pg1.Id");
        makeActiveSQL.append(" AND prec1.GeneralMoleculeId = m1.Id");
        makeActiveSQL.append(" AND precur1.Id = prec1.Id");
        makeActiveSQL.append(" AND r1.Container = ?");
        makeActiveSQL.append(" AND prec1.RepresentativeDataState = ?");
        makeActiveSQL.add(container);
        makeActiveSQL.add(RepresentativeDataState.Representative.ordinal());
        makeActiveSQL.append(")");
        int result = new SqlExecutor(TargetedMSManager.getSchema()).execute(makeActiveSQL);

        // Mark all other precursors as being in the conflicted state
        SQLFragment makeConflictedSQL = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        makeConflictedSQL.append(" SET RepresentativeDataState = ?");
        makeConflictedSQL.add(RepresentativeDataState.Conflicted.ordinal());
        makeConflictedSQL.append(" FROM ");
        makeConflictedSQL.append(moleculeTable, "m").append(", ");
        makeConflictedSQL.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(", ");
        makeConflictedSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        makeConflictedSQL.append(" WHERE pg.RunId=?");
        makeConflictedSQL.add(run.getId());
        makeConflictedSQL.append(" AND " + TargetedMSManager.getTableInfoGeneralPrecursor() + ".RepresentativeDataState != ?");
        makeConflictedSQL.add(RepresentativeDataState.Representative.ordinal());
        makeConflictedSQL.append(moleculeFilterSQL);
        // Ignore standard (e.g. iRT) peptides
        makeConflictedSQL.append(" AND gm.StandardType IS NULL");
        makeConflictedSQL.append(" AND "+TargetedMSManager.getTableInfoGeneralPrecursor()+".GeneralMoleculeId = m.Id");
        makeConflictedSQL.append(" AND m.Id = gm.Id");
        makeConflictedSQL.append(" AND gm.PeptideGroupId = pg.Id");

        int conflictCount = new SqlExecutor(TargetedMSManager.getSchema()).execute(makeConflictedSQL);

        // Get a list of standard precursors in this run
        // Issue 23843: Include iRT peptides in chromatogram libraries
        // We do not want users to have to resolve conflicts for standard peptides.  The latest version of the
        // standard peptides will be marked as representative.
        // If there are standard peptides in this run, we will mark them as representative. Older versions of these
        // peptides, from previous runs, will be marked as deprecated.
        List<Long> stdPrecursorIdsInRun = getStdPrecursorIdsInRun(run);
        if(stdPrecursorIdsInRun.size() > 0)
        {
            // Get a list of current representative precursors in the folder that have the same identifier and charge
            // as the standard precursors in the run.
            List<Long> currentRepStdPrecursorIds = getCurrentStandardPrecursorIds(container, stdPrecursorIdsInRun, precursorTable, joinValue);

            PrecursorManager.updateRepresentativeStatus(stdPrecursorIdsInRun, RepresentativeDataState.Representative);
            PrecursorManager.updateStatusToDeprecatedOrNotRepresentative(currentRepStdPrecursorIds);
        }

        return conflictCount;
    }
}
