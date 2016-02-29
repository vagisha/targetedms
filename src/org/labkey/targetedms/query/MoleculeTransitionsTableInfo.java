/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * User: binalpatel
 * Date: Feb 25, 2016
 */
public class MoleculeTransitionsTableInfo extends AbstractGeneralTransitionTableInfo
{
    public MoleculeTransitionsTableInfo(final TargetedMSSchema schema)
    {
        super(schema, TargetedMSManager.getTableInfoMoleculeTransition());

        setName(TargetedMSSchema.TABLE_MOLECULE_TRANSITION);
        setDescription("Contains a row for each non-proteomic molecule transition loaded in a targeted MS run.");

        List<FieldKey> defaultCols = new ArrayList<>();
        int idx = 0;

        defaultCols.add(FieldKey.fromParts("TransitionId", "GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "Label"));
        defaultCols.add(FieldKey.fromParts("TransitionId", "GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "Description"));
        defaultCols.add(FieldKey.fromParts("TransitionId", "GeneralPrecursorId", "GeneralMoleculeId", "PeptideGroupId", "Annotations"));
        defaultCols.add(idx++, FieldKey.fromParts("Mz"));
        defaultCols.add(idx++, FieldKey.fromParts("Charge"));
        defaultCols.add(idx++, FieldKey.fromParts("FragmentType"));
        defaultCols.add(idx++, FieldKey.fromParts("IonFormula"));
        defaultCols.add(idx++, FieldKey.fromParts("MassAverage"));
        defaultCols.add(idx++, FieldKey.fromParts("MassMonoisotopic"));
        setDefaultVisibleColumns(defaultCols);

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
