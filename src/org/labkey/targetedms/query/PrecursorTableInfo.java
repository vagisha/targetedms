/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;

/**
 * User: vsharma
 * Date: 8/4/13
 * Time: 9:56 PM
 */

public class PrecursorTableInfo extends AbstractGeneralPrecursorTableInfo
{
    public PrecursorTableInfo(final TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        this(TargetedMSManager.getTableInfoPrecursor(), cf, TargetedMSSchema.TABLE_PRECURSOR, schema, omitAnnotations);
    }

    public PrecursorTableInfo(final TableInfo tableInfo, ContainerFilter cf, String tableName, final TargetedMSSchema schema, boolean omitAnnotations)
    {
        super(tableInfo, tableName, schema, cf, omitAnnotations);

        var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE, cf));
        generalMoleculeId.setHidden(true);

        var peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE, cf));
        addColumn(peptideId);

        getMutableColumn("IsotopeLabelId").setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_ISOTOPE_LABEL, cf));

        WrappedColumn modSeqCol = new WrappedColumn(getColumn("ModifiedSequence"), ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME);
        modSeqCol.setLabel("Precursor");
        modSeqCol.setDescription("Modified precursor sequence");
        modSeqCol.setDisplayColumnFactory( new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new ModifiedSequenceDisplayColumn.PrecursorCol(colInfo);
            }
        });
        modSeqCol.setURL(getDetailsURL(null, null));
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
        public ExperimentPrecursorTableInfo(final TargetedMSSchema schema, ContainerFilter cf)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), cf, TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR, schema, false);
        }

        @Override
        public String getName()
        {
            return TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR;
        }


    }

    public static class LibraryPrecursorTableInfo extends PrecursorTableInfo
    {
        public LibraryPrecursorTableInfo(final TargetedMSSchema schema, ContainerFilter cf)
        {
            super(TargetedMSManager.getTableInfoPrecursor(), cf, TargetedMSSchema.TABLE_LIBRARY_PRECURSOR, schema, false);
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

    @Override
    protected Class<? extends Controller> getDetailsActionClass()
    {
        return TargetedMSController.PrecursorAllChromatogramsChartAction.class;
    }
}
