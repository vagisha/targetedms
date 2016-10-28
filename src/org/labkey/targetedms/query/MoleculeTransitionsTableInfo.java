/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.ArrayList;

/**
 * User: binalpatel
 * Date: Feb 25, 2016
 */
public class MoleculeTransitionsTableInfo extends AbstractGeneralTransitionTableInfo
{
    public MoleculeTransitionsTableInfo(final TargetedMSSchema schema)
    {
        super(schema, TargetedMSManager.getTableInfoMoleculeTransition());

        setDescription(TargetedMSManager.getTableInfoMoleculeTransition().getDescription());

        ColumnInfo precursorCol = getColumn("GeneralPrecursorId");
        precursorCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR));
        precursorCol.setHidden(true);

        ColumnInfo precursorIdCol = wrapColumn("MoleculePrecursorId", getRealTable().getColumn(precursorCol.getFieldKey()));
        precursorIdCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR));
        addColumn(precursorIdCol);

        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Annotations"));
        // Molecule level information
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "CustomIonName"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "IonFormula"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "MassAverage"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "MassMonoisotopic"));
        // Molecule Precursor level information
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "CustomIonName"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "Annotations"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MassAverage"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "Mz"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "Charge"));
        // Molecule Transition level information
        visibleColumns.add(FieldKey.fromParts("FragmentType"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        setDefaultVisibleColumns(visibleColumns);

        // Create a WrappedColumn for Note & Annotations
        WrappedColumn noteAnnotation = new WrappedColumn(getColumn("Annotations"), "NoteAnnotations");
        noteAnnotation.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AnnotationUIDisplayColumn(colInfo);
            }
        });
        noteAnnotation.setLabel("Molecule Transition Note/Annotations");
        addColumn(noteAnnotation);
    }
}
