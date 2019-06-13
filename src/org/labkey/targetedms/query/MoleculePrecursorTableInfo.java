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
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;

/**
 * User: binalpatel
 * Date: 02/23/2016
 */

public class MoleculePrecursorTableInfo extends AbstractGeneralPrecursorTableInfo
{
    public MoleculePrecursorTableInfo(final TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        this(TargetedMSManager.getTableInfoMoleculePrecursor(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, schema, cf, omitAnnotations);
    }

    public MoleculePrecursorTableInfo(final TableInfo tableInfo, String tableName, final TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(tableInfo, tableName, schema, cf, omitAnnotations);

        var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE, cf));
        generalMoleculeId.setHidden(true);

        var moleculeIdId = wrapColumn("MoleculeId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        moleculeIdId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE, cf));

        addColumn(moleculeIdId);

        var customIonName = getMutableColumn("CustomIonName");
        customIonName.setURL(getDetailsURL(null, null));
        customIonName.setLabel("Precursor");

        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "PeptideGroupId", "NoteAnnotations"));

        visibleColumns.add(FieldKey.fromParts("MoleculeId", "Molecule"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "IonFormula"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "MassAverage"));
        visibleColumns.add(FieldKey.fromParts("MoleculeId", "MassMonoisotopic"));

        visibleColumns.add(FieldKey.fromParts("CustomIonName"));
        visibleColumns.add(FieldKey.fromParts("NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("MassAverage"));
        visibleColumns.add(FieldKey.fromParts("MassMonoisotopic"));
        visibleColumns.add(FieldKey.fromParts("TransitionCount"));

        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    protected Class<? extends Controller> getDetailsActionClass()
    {
        return TargetedMSController.MoleculePrecursorAllChromatogramsChartAction.class;
    }
}
