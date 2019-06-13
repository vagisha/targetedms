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
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;
import java.util.List;

public class PeptideTableInfo extends AbstractGeneralMoleculeTableInfo
{
    public PeptideTableInfo(TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(schema, TargetedMSManager.getTableInfoPeptide(), cf, "Peptide Annotations", omitAnnotations);

        if (!omitAnnotations)
        {
            // Add a WrappedColumn for Note & Annotations
            WrappedColumn noteAnnotation = new WrappedColumn(getColumn("Annotations"), "NoteAnnotations");
            noteAnnotation.setDisplayColumnFactory(colInfo -> new AnnotationUIDisplayColumn(colInfo));
            noteAnnotation.setLabel("Peptide Note/Annotations");
            addColumn(noteAnnotation);
        }

        var peptideGroupId = getMutableColumn("PeptideGroupId");
        peptideGroupId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE_GROUP, cf));

        var sequenceColumn = getMutableColumn("Sequence");
        sequenceColumn.setURL(getDetailsURL(null, null));
        WrappedColumn modSeqCol = new WrappedColumn(getColumn("PeptideModifiedSequence"), ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME);
        modSeqCol.setLabel("Peptide");
        modSeqCol.setDescription("Modified peptide sequence");
        modSeqCol.setDisplayColumnFactory(colInfo -> new ModifiedSequenceDisplayColumn.PeptideCol(colInfo));
        modSeqCol.setURL(getDetailsURL(null, null));
        addColumn(modSeqCol);

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
        defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
        defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
        defaultCols.add(3, FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected Class<? extends Controller> getDetailsActionClass()
    {
        return TargetedMSController.ShowPeptideAction.class;
    }
}