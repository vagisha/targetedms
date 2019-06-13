/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.PrecursorChromInfoLitePlus;
import org.labkey.targetedms.model.PrecursorChromInfoPlus;
import org.labkey.targetedms.parser.MoleculePrecursor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoleculePrecursorManager
{
    private MoleculePrecursorManager() {}

    public static MoleculePrecursor getPrecursor(Container c, int id, User user)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, c);

        SQLFragment sql = new SQLFragment("SELECT pre.* FROM ");
        sql.append(new MoleculePrecursorTableInfo(schema, null, true), "pre");
        sql.append(", ");
        sql.append(new MoleculeTableInfo(schema, null, true), "mol");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE pre.GeneralMoleculeId = mol.Id AND ");
        sql.append("mol.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pre.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(MoleculePrecursor.class);
    }

    public static List<MoleculePrecursor> getPrecursorsForMolecule(int moleculeId, TargetedMSSchema targetedMSSchema)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("MoleculeId"), moleculeId);

        Sort sort = new Sort("Charge, CustomIonName");

        Set<String> colNames = new HashSet<>();
        colNames.addAll(TargetedMSManager.getTableInfoMoleculePrecursor().getColumnNameSet());
        colNames.addAll(TargetedMSManager.getTableInfoGeneralPrecursor().getColumnNameSet());

        List<MoleculePrecursor> precursors = new TableSelector(new MoleculePrecursorTableInfo(targetedMSSchema, null, true), colNames, filter,  sort).getArrayList(MoleculePrecursor.class);

        if (precursors.isEmpty())
        {
            throw new NotFoundException(String.format("No precursors found for moleculeId %d", moleculeId));
        }

        return precursors;
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(int peptideGroupId, User user, Container container)
    {
        return getChromInfosLitePlusForPeptideGroup(peptideGroupId, 0, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForPeptideGroup(int peptideGroupId, int sampleFileId, User user, Container container)
    {
        return getPrecursorChromInfosLitePlusForPeptideGroup(peptideGroupId, true, false, sampleFileId, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForMolecule(int moleculeId, User user, Container container)
    {
        return getPrecursorChromInfosLitePlusForPeptideGroup(moleculeId, false, false, 0, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getChromInfosLitePlusForMoleculePrecursor(int precursorId, User user, Container container)
    {
        return getPrecursorChromInfosLitePlusForPeptideGroup(precursorId, false, true, 0, user, container);
    }

    public static List<PrecursorChromInfoLitePlus> getPrecursorChromInfosLitePlusForPeptideGroup(int id, boolean forPeptideGroup,
                                                                     boolean forPrecursor, int sampleFileId, User user, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.id, pci.precursorId, pci.sampleFileId, pci.bestRetentionTime, pci.minStartTime, ");
        sql.append("pci.maxEndTime, pci.TotalArea, pci.maxfwhm, pci.maxHeight, pg.Label AS groupName, ");
        sql.append("mol.customIonName, mol.ionFormula ");
        sql.append(" FROM ");
        joinTablesForMoleculePrecursorChromInfo(sql, user, container);
        sql.append(" WHERE ");
        if (forPeptideGroup)
        {
            sql.append("pg.Id = ? ");
        }
        else if (forPrecursor)
        {
            sql.append("prec.Id = ? ");
        }
        else
        {
            sql.append("mol.Id = ? ");
        }
        sql.add(id);

        if (sampleFileId != 0)
        {
            sql.append(" AND pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoLitePlus.class);
    }

    public static List<PrecursorChromInfoPlus> getPrecursorChromInfosForMolecule(int moleduleId, int sampleFileId, User user, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append("pci.* , pg.Label AS groupName, mol.customIonName, prec.Charge");
        sql.append(" FROM ");
        joinTablesForMoleculePrecursorChromInfo(sql, user, container);
        sql.append(" WHERE ");
        sql.append("mol.Id=? ");
        sql.add(moleduleId);

        if(sampleFileId != 0)
        {
            sql.append("AND ");
            sql.append("pci.SampleFileId=?");
            sql.add(sampleFileId);
        }

        return  new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PrecursorChromInfoPlus.class);
    }

    private static void joinTablesForMoleculePrecursorChromInfo(SQLFragment sql, User user, Container container)
    {
        TargetedMSSchema schema = new TargetedMSSchema(user, container);

        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" INNER JOIN ");
        sql.append(new MoleculeTableInfo(schema, null, true), "mol");
        sql.append(" ON pg.Id = mol.PeptideGroupId ");
        sql.append(" INNER JOIN ");
        sql.append(new MoleculePrecursorTableInfo(schema, null, true), "prec");
        sql.append(" ON mol.Id = prec.GeneralMoleculeId ");
        sql.append(" INNER JOIN ");
        sql.append(new PrecursorChromInfoTable(schema, null), "pci");
        sql.append(" ON prec.Id = pci.PrecursorId ");
    }
}
