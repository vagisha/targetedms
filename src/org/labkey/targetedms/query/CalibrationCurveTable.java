/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nicksh on 12/6/2016.
 */
public class CalibrationCurveTable extends TargetedMSTable
{
    public CalibrationCurveTable(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoCalibrationCurve(), schema, TargetedMSSchema.ContainerJoinType.RunFK.getSQL());
        Map<String, Object> params = new HashMap<>();
        params.put("id", FieldKey.fromParts("RunId"));
        params.put("calibrationCurveId", FieldKey.fromParts("Id"));
        DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowCalibrationCurveAction.class, getContainer()), params);
        setDetailsURL(detailsURL);
        getColumn(FieldKey.fromParts("Id")).setHidden(true);
        getColumn(FieldKey.fromParts("RunId")).setHidden(true);
        getColumn(FieldKey.fromParts("QuantificationSettingsId")).setHidden(true);
    }

    public static class PeptideCalibrationCurveTable extends CalibrationCurveTable
    {
        public PeptideCalibrationCurveTable(TargetedMSSchema schema)
        {
            super(schema);
            ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE));
            generalMoleculeId.setLabel("Peptide");
            SimpleFilter.SQLClause isPeptideFilter =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Peptide)"),
                            generalMoleculeId.getFieldKey());
            addCondition(new SimpleFilter(isPeptideFilter));
        }
    }

    public static class MoleculeCalibrationCurveTable extends CalibrationCurveTable
    {
        public MoleculeCalibrationCurveTable(TargetedMSSchema schema)
        {
            super(schema);
            ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_MOLECULE));
            generalMoleculeId.setLabel("Molecule");
            SimpleFilter.SQLClause isMoleculeFilter =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Molecule)"),
                            generalMoleculeId.getFieldKey());
            addCondition(new SimpleFilter(isMoleculeFilter));
        }
    }
}
