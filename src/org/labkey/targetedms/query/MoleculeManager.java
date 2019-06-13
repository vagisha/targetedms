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
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Molecule;

import java.util.Collection;

public class MoleculeManager
{
    private MoleculeManager() {}

    public static Molecule getMolecule(Container c, int id)
    {
        SQLFragment sql = new SQLFragment("SELECT mol.*, gm.* FROM ");
        sql.append(TargetedMSManager.getTableInfoMolecule(), "mol");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("gm.PeptideGroupId = pg.Id AND mol.Id = gm.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND mol.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Molecule.class);
    }

    public static Collection<Molecule> getMoleculesForGroup(int peptideGroupId)
    {
        SQLFragment sql = new SQLFragment("SELECT gm.id, gm.peptidegroupid, gm.rtcalculatorscore, gm.predictedretentiontime, ");
        sql.append("gm.avgmeasuredretentiontime, gm.note, gm.explicitretentiontime, ");
        sql.append("gm.normalizationmethod, gm.standardtype, gm.concentrationmultiplier, gm.internalstandardconcentration, ");
        sql.append("m.id, m.ionformula, m.customionname, m.massaverage, m.massmonoisotopic ");
        sql.append("FROM targetedms.generalmolecule gm, targetedms.molecule m WHERE ");
        sql.append("m.id = gm.id AND gm.peptidegroupid=?");
        sql.add(peptideGroupId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(Molecule.class);
    }
}
