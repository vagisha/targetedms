/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;

import java.util.ArrayList;
import java.util.Collections;

/**
 * User: vsharma
 * Date: 8/4/13
 * Time: 9:56 PM
 */

public class PrecursorTableInfo extends AbstractGeneralPrecursorTableInfo
{
    public PrecursorTableInfo(final TargetedMSSchema schema)
    {
        this(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_PRECURSOR, schema);
    }

    public PrecursorTableInfo(final TableInfo tableInfo, String tableName, final TargetedMSSchema schema)
    {
        super(tableInfo, tableName, schema);

        _detailsURL = new DetailsURL(new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class, getContainer()), Collections.singletonMap("id", "Id"));
        _detailsURL.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("GeneralMoleculeId", "PeptideGroupId", "RunId", "Folder")));
        setDetailsURL(_detailsURL);

        ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE));
        generalMoleculeId.setHidden(true);

        ColumnInfo peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE));
        addColumn(peptideId);

        getColumn("IsotopeLabelId").setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_ISOTOPE_LABEL));

        WrappedColumn modSeqCol = new WrappedColumn(getColumn("ModifiedSequence"), ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME);
        modSeqCol.setLabel("Precursor");
        modSeqCol.setDescription("Modified precursor sequence");
        modSeqCol.setDisplayColumnFactory( new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ModifiedSequenceDisplayColumn.PrecursorCol(colInfo, _detailsURL.getActionURL());
            }
        });
        addColumn(modSeqCol);

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();

        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Description"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "NoteAnnotations"));

        visibleColumns.add(FieldKey.fromParts("PeptideId", ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "NumMissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "CalcNeutralMass"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Rank"));

        visibleColumns.add(FieldKey.fromParts(ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME));
        visibleColumns.add(FieldKey.fromParts("NoteAnnotations"));
        visibleColumns.add(FieldKey.fromParts("IsotopeLabelId", "Name"));
        visibleColumns.add(FieldKey.fromParts("Charge"));
        visibleColumns.add(FieldKey.fromParts("Mz"));
        visibleColumns.add(FieldKey.fromParts("NeutralMass"));
        visibleColumns.add(FieldKey.fromParts("TransitionCount"));
        visibleColumns.add(FieldKey.fromParts("CollisionEnergy"));
        visibleColumns.add(FieldKey.fromParts("DeclusteringPotential"));

        setDefaultVisibleColumns(visibleColumns);
    }

    public static class ExperimentPrecursorTableInfo extends PrecursorTableInfo
    {
        public ExperimentPrecursorTableInfo(final TargetedMSSchema schema)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR, schema);
        }

        @Override
        public String getName()
        {
            return TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR;
        }


    }

    public static class LibraryPrecursorTableInfo extends PrecursorTableInfo
    {
        public LibraryPrecursorTableInfo(final TargetedMSSchema schema)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), TargetedMSSchema.TABLE_LIBRARY_PRECURSOR, schema);
        }

        public void selectRepresentative()
        {
            SQLFragment sql = new SQLFragment();
            sql.append("RepresentativeDataState = ? ");
            sql.add(RepresentativeDataState.Representative.ordinal());
            addCondition(sql);
        }

        @Override
        public String getName()
        {
            return TargetedMSSchema.TABLE_LIBRARY_PRECURSOR;
        }
    }
}
