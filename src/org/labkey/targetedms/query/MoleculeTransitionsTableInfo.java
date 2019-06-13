/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
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
    public MoleculeTransitionsTableInfo(final TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(schema, TargetedMSManager.getTableInfoMoleculeTransition(), cf, omitAnnotations);

        setDescription(TargetedMSManager.getTableInfoMoleculeTransition().getDescription());

        var precursorCol = getMutableColumn("GeneralPrecursorId");
        precursorCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, cf));
        precursorCol.setHidden(true);

        var precursorIdCol = wrapColumn("MoleculePrecursorId", getRealTable().getColumn(precursorCol.getFieldKey()));
        precursorIdCol.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_PRECURSOR, cf));
        addColumn(precursorIdCol);

        SQLFragment sql = new SQLFragment(" COALESCE(" + ExprColumn.STR_TABLE_ALIAS + ".CustomIonName, " +
                ExprColumn.STR_TABLE_ALIAS + ".IonFormula, "  +
                "CAST (((" + getSqlDialect().getRoundFunction( " 10000.0 * " + ExprColumn.STR_TABLE_ALIAS + ".MassAverage") + ")/10000) AS VARCHAR)) ");
        ExprColumn fragment = new ExprColumn(this, "Fragment", sql, JdbcType.VARCHAR);
        addColumn(fragment);

        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "PeptideGroupId", "Annotations"));
        // Molecule level information
        visibleColumns.add(FieldKey.fromParts("MoleculePrecursorId", "MoleculeId", "Molecule"));
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
        visibleColumns.add(FieldKey.fromParts("Fragment"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        setDefaultVisibleColumns(visibleColumns);

        if (!omitAnnotations)
        {
            // Create a WrappedColumn for Note & Annotations
            WrappedColumn noteAnnotation = new WrappedColumn(getColumn("Annotations"), "NoteAnnotations");
            noteAnnotation.setDisplayColumnFactory(colInfo -> new AnnotationUIDisplayColumn(colInfo));
            noteAnnotation.setLabel("Molecule Transition Note/Annotations");
            addColumn(noteAnnotation);
        }
    }
}
