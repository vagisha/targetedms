/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.query;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * Table which holds group comparison results.
 */
public class FoldChangeTable extends TargetedMSTable
{
    public FoldChangeTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableInfoFoldChange(), schema, cf, TargetedMSSchema.ContainerJoinType.RunFK);
        getMutableColumn(FieldKey.fromParts("Id")).setHidden(true);
        getMutableColumn(FieldKey.fromParts("RunId")).setHidden(true);
        getMutableColumn(FieldKey.fromParts("GroupComparisonSettingsId")).setHidden(true);
        ActionURL peptideGroupDetails = new ActionURL(TargetedMSController.ShowProteinAction.class, getContainer());
        peptideGroupDetails.addParameter("id", "${PeptideGroupId}");
        getMutableColumn(FieldKey.fromParts("PeptideGroupId")).setURL(StringExpressionFactory.createURL(peptideGroupDetails));
    }



    public static class PeptideFoldChangeTable extends FoldChangeTable
    {
        public PeptideFoldChangeTable(TargetedMSSchema schema, ContainerFilter cf)
        {
            super(schema, cf);
            var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE, cf));
            generalMoleculeId.setLabel("Peptide");

            SimpleFilter.SQLClause isPeptideClause =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Peptide)"),
                            generalMoleculeId.getFieldKey());
            SimpleFilter.FilterClause isPeptideOrBlankClause =
                    new SimpleFilter.OrClause(
                            new CompareType.CompareClause(generalMoleculeId.getFieldKey(), CompareType.ISBLANK, null),
                            isPeptideClause);
            addCondition(new SimpleFilter(isPeptideOrBlankClause));
        }
    }

    public static class MoleculeFoldChangeTable extends FoldChangeTable
    {
        public MoleculeFoldChangeTable(TargetedMSSchema schema, ContainerFilter cf)
        {
            super(schema, cf);
            var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_MOLECULE, cf));
            generalMoleculeId.setLabel("Molecule");
            SimpleFilter.SQLClause isMoleculeClause =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Molecule)"),
                            generalMoleculeId.getFieldKey());
            SimpleFilter.FilterClause isMoleculeOrBlankClause =
                    new SimpleFilter.OrClause(
                            new CompareType.CompareClause(generalMoleculeId.getFieldKey(), CompareType.ISBLANK, null),
                            isMoleculeClause);
            addCondition(new SimpleFilter(isMoleculeOrBlankClause));
        }
    }
}
