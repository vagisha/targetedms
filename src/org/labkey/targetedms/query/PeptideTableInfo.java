/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeptideTableInfo extends AbstractGeneralMoleculeTableInfo
{
    public PeptideTableInfo(TargetedMSSchema schema)
    {
        super(schema, TargetedMSManager.getTableInfoPeptide(), "Peptide Annotations");

        final DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()),
                Collections.singletonMap("id", "Id"));
        setDetailsURL(detailsURL);

        // Add a WrappedColumn for Note & Annotations
        WrappedColumn noteAnnotation = new WrappedColumn(getColumn("Annotations"), "NoteAnnotations");
        noteAnnotation.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AnnotationUIDisplayColumn(colInfo);
            }
        });
        noteAnnotation.setLabel("Peptide Note/Annotations");
        addColumn(noteAnnotation);

        ColumnInfo peptideGroupId = getColumn("PeptideGroupId");
        peptideGroupId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE_GROUP));

        ColumnInfo sequenceColumn = getColumn("Sequence");
        sequenceColumn.setURL(detailsURL);
        WrappedColumn modSeqCol = new WrappedColumn(getColumn("PeptideModifiedSequence"), ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME);
        modSeqCol.setLabel("Peptide");
        modSeqCol.setDescription("Modified peptide sequence");
        modSeqCol.setDisplayColumnFactory( new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ModifiedSequenceDisplayColumn.PeptideCol(colInfo, detailsURL.getActionURL());
            }
        });
        addColumn(modSeqCol);

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
        defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
        defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
        defaultCols.add(3, FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
        setDefaultVisibleColumns(defaultCols);
    }
}