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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;

public class GeneralMoleculeChromInfoTableInfo extends TargetedMSTable
{
    public GeneralMoleculeChromInfoTableInfo(TableInfo table, TargetedMSSchema schema, ContainerFilter cf, TargetedMSSchema.ContainerJoinType joinType, String name)
    {
        super(table, schema, cf, joinType);
        setName(name);

        // Add a link to view the chromatogram an individual transition
        setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.GeneralMoleculeChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));

        var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE, cf));
        generalMoleculeId.setHidden(true);

        var peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE, cf));
        addColumn(peptideId);

        var moleculeId = wrapColumn("MoleculeId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        moleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE, cf));
        addColumn(moleculeId);

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("PeptideId"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId"));
        visibleColumns.add(FieldKey.fromParts("SampleFileId"));
        visibleColumns.add(FieldKey.fromParts("PeakCountRatio"));
        visibleColumns.add(FieldKey.fromParts("RetentionTime"));
        setDefaultVisibleColumns(visibleColumns);

        // PeptideModifiedAreaProportion = (sum area of precursors for this peptide in the same replicate) / (total area of all peptides for the same peptide sequence in the replicate / sample file)
        SQLFragment proportionSQL = new SQLFragment("(SELECT CAST(CASE WHEN X.AreaInReplicate = 0 THEN NULL ELSE (SELECT SUM(p.TotalArea) FROM ");
        proportionSQL.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "p");
        proportionSQL.append(" WHERE p.generalmoleculechrominfoid = ");
        proportionSQL.append(ExprColumn.STR_TABLE_ALIAS);
        proportionSQL.append(".Id) / X.AreaInReplicate END AS FLOAT) FROM ");
        proportionSQL.append(" ( ");
        proportionSQL.append(" SELECT SUM(TotalArea) AS AreaInReplicate FROM ");
        proportionSQL.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        proportionSQL.append(" INNER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        proportionSQL.append(" ON gp.Id = pci.PrecursorId INNER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        proportionSQL.append(" ON gp.GeneralMoleculeId = gm.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoMolecule(), "m");
        proportionSQL.append(" ON gm.Id = m.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoPeptide(), "p");
        proportionSQL.append(" ON p.id = gp.generalmoleculeid WHERE pci.SampleFileId = ").append(ExprColumn.STR_TABLE_ALIAS).append(".SampleFileId");
        // Group based on user-specified grouping ID if present, and fall back on peptide sequence, custom ion name,
        // and ion formula (the latter two are for small molecules only), in that order
        proportionSQL.append(" AND COALESCE(gm.AttributeGroupId, p.Sequence, m.CustomIonName, m.IonFormula) = (SELECT DISTINCT COALESCE(gm2.AttributeGroupId, p2.Sequence, m2.CustomIonName, m2.IonFormula) FROM ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp2");
        proportionSQL.append(" INNER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm2");
        proportionSQL.append(" ON gp2.GeneralMoleculeId = gm2.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoPeptide(), "p2");
        proportionSQL.append(" ON p2.Id = gm2.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoMolecule(), "m2");
        proportionSQL.append(" ON gm2.Id = m2.Id WHERE gp2.GeneralMoleculeId = ").append(ExprColumn.STR_TABLE_ALIAS).append(".GeneralMoleculeId");

        proportionSQL.append(")) X )");
        ExprColumn peptideModifiedAreaProportionCol = new ExprColumn(this, "PeptideModifiedAreaProportion", proportionSQL, JdbcType.DOUBLE);
        peptideModifiedAreaProportionCol.setFormat("##0.####%");
        addColumn(peptideModifiedAreaProportionCol);

    }
}