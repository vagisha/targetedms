/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
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

public class PrecursorChromInfoTable extends AnnotatedTargetedMSTable
{
    public PrecursorChromInfoTable(final TargetedMSSchema schema, ContainerFilter cf)
    {
        this(TargetedMSManager.getTableInfoPrecursorChromInfo(), schema, cf);
    }

    public PrecursorChromInfoTable(TableInfo table, TargetedMSSchema schema, ContainerFilter cf)
    {
        super(table, schema, cf, null, new SQLFragment("Container"),
                TargetedMSManager.getTableInfoPrecursorChromInfoAnnotation(), "PrecursorChromInfoId", "Precursor Result Annotations", "precursor_result", false);
        var precursorId = getMutableColumn("PrecursorId");
        precursorId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PRECURSOR, cf));

        var moleculePrecursorId = wrapColumn("MoleculePrecursorId", getRealTable().getColumn(precursorId.getFieldKey()));
        moleculePrecursorId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, cf));

        addColumn(moleculePrecursorId);

        var generalMoleculeChromInfoIdId = getMutableColumn("GeneralMoleculeChromInfoId");
        generalMoleculeChromInfoIdId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_GENERAL_MOLECULE_CHROM_INFO, cf));
        generalMoleculeChromInfoIdId.setHidden(true);

        var peptideChromInfoIdId = wrapColumn("PeptideChromInfoId", getRealTable().getColumn(generalMoleculeChromInfoIdId.getFieldKey()));
        peptideChromInfoIdId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_GENERAL_MOLECULE_CHROM_INFO, cf));
        addColumn(peptideChromInfoIdId);

        var sampleFileId = getMutableColumn("SampleFileId");
        sampleFileId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_SAMPLE_FILE, cf));

        // Add a calculated column for Full Width at Base (FWB)
        SQLFragment sql = new SQLFragment("(MaxEndTime - MinStartTime)");
        ExprColumn col = new ExprColumn(this, "MaxFWB", sql, JdbcType.DOUBLE);
        col.setDescription("Full Width at Base (FWB)");
        addColumn(col);

        // Add a calculated column for sum of area for fragment type equal 'precursor'
        sql = new SQLFragment("(SELECT SUM(PrecursorArea) FROM ");
        sql.append("(SELECT CASE WHEN FragmentType = 'precursor' THEN Area ELSE 0 END AS PrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TotalPrecursorArea", sql, JdbcType.DOUBLE);
        addColumn(col);

        // Add a calculated column for sum of area for fragment type not equal 'precursor'
        sql = new SQLFragment("(SELECT SUM(NonPrecursorArea) FROM ");
        sql.append("(SELECT CASE WHEN FragmentType != 'precursor' THEN Area ELSE 0 END AS NonPrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TotalNonPrecursorArea", sql, JdbcType.DOUBLE);
        addColumn(col);

        // Add a calculated column for the ratio of the transitions' areas (TotalNonPrecursorArea) to the precursor's area (TotalPrecursorArea)
        sql = new SQLFragment("(SELECT CASE WHEN SUM(PrecursorArea) = 0 THEN NULL ELSE SUM(NonPrecursorArea) / SUM(PrecursorArea) END FROM ");
        sql.append("(SELECT CASE WHEN FragmentType = 'precursor' THEN Area ELSE 0 END AS PrecursorArea, ");
        sql.append("CASE WHEN FragmentType != 'precursor' THEN Area ELSE 0 END AS NonPrecursorArea FROM ");
        sql.append(getTransitionJoinSQL());
        col = new ExprColumn(this, "TransitionPrecursorRatio", sql, JdbcType.DOUBLE);
        col.setDescription("Transition/Precursor Area Ratio");
        addColumn(col);

        // Add a link to view the chromatogram for all of the precursor's transitions
        setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.PrecursorChromatogramChartAction.class, getContainer()), "id", FieldKey.fromParts("Id")));

        // TotalAreaNormalized  = (area of precursor in the replicate) / (total area of all precursors in the replicate / sample file)
        SQLFragment totalAreaNormalizedSQL = new SQLFragment("(SELECT TotalArea / X.TotalPrecursorAreaInReplicate FROM ");
        totalAreaNormalizedSQL.append(" ( ");
        totalAreaNormalizedSQL.append(" SELECT SUM(TotalArea) AS TotalPrecursorAreaInReplicate FROM ");
        totalAreaNormalizedSQL.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        totalAreaNormalizedSQL.append(" WHERE pci.SampleFileId = ").append(ExprColumn.STR_TABLE_ALIAS).append(".SampleFileId");
        totalAreaNormalizedSQL.append(") X ");
        totalAreaNormalizedSQL.append(" ) ");
        ExprColumn totalAreaNormalizedCol = new ExprColumn(this, "TotalAreaNormalized", totalAreaNormalizedSQL, JdbcType.DOUBLE);
        totalAreaNormalizedCol.setFormat("##0.####%");
        addColumn(totalAreaNormalizedCol);

        // PrecursorModifiedAreaProportion = (area of precursor in the replicate) / (total area of all precursors for the same peptide sequence in the replicate / sample file)
        SQLFragment proportionSQL = new SQLFragment("(SELECT CAST(CASE WHEN X.PrecursorAreaInReplicate = 0 THEN NULL ELSE TotalArea / X.PrecursorAreaInReplicate END AS FLOAT) FROM ");
        proportionSQL.append(" ( ");
        proportionSQL.append(" SELECT SUM(TotalArea) AS PrecursorAreaInReplicate FROM ");
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
        proportionSQL.append(" AND COALESCE(gm.AttributeGroupId, p.Sequence, m.CustomIonName, m.IonFormula) = (SELECT COALESCE(gm2.AttributeGroupId, p2.Sequence, m2.CustomIonName, m2.IonFormula) FROM ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp2");
        proportionSQL.append(" INNER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm2");
        proportionSQL.append(" ON gp2.GeneralMoleculeId = gm2.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoPeptide(), "p2");
        proportionSQL.append(" ON p2.Id = gm2.Id LEFT OUTER JOIN ");
        proportionSQL.append(TargetedMSManager.getTableInfoMolecule(), "m2");
        proportionSQL.append(" ON gm2.Id = m2.Id WHERE gp2.Id = ").append(ExprColumn.STR_TABLE_ALIAS).append(".PrecursorId");
        proportionSQL.append(")) X )");
        ExprColumn peptideModifiedAreaProportionCol = new ExprColumn(this, "PrecursorModifiedAreaProportion", proportionSQL, JdbcType.DOUBLE);
        peptideModifiedAreaProportionCol.setFormat("##0.####%");
        addColumn(peptideModifiedAreaProportionCol);
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        // Backwards compatibility with original, misleading column name
        if ("PeptideModifiedAreaProportion".equalsIgnoreCase(name))
        {
            return getColumn("PrecursorModifiedAreaProportion");
        }
        return super.resolveColumn(name);
    }

    private SQLFragment getTransitionJoinSQL()
    {
        SQLFragment sql = new SQLFragment();
        sql.append(TargetedMSManager.getTableInfoGeneralTransition(), "gt");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoTransitionChromInfo(), "tci");
        sql.append(" WHERE tci.TransitionId = gt.Id AND tci.PrecursorChromInfoId = ");
        sql.append(ExprColumn.STR_TABLE_ALIAS);
        sql.append(".Id) X)");
        return sql;
    }

}
