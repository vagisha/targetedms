package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.view.AnnotationUIDisplayColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeptideTableInfo extends JoinedTargetedMSTable
{
    public PeptideTableInfo(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoGeneralMolecule(),
                TargetedMSManager.getTableInfoPeptide(),
                schema,
                TargetedMSSchema.ContainerJoinType.PeptideGroupFK.getSQL(),
                TargetedMSManager.getTableInfoGeneralMoleculeAnnotation(),
                "Id", "GeneralMoleculeId");

        setName(TargetedMSSchema.TABLE_GENERAL_MOLECULE);

        ColumnInfo peptideGroupId = getColumn("PeptideGroupId");
        peptideGroupId.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
            }
        });


        final DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()),
                Collections.singletonMap("id", "Id"));
        setDetailsURL(detailsURL);

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
        defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
        defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
        defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
        setDefaultVisibleColumns(defaultCols);

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
        defaultCols.add(3, FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));

        SQLFragment currentLibPrecursorCountSQL = new SQLFragment("(SELECT COUNT(p.Id) FROM ");
        currentLibPrecursorCountSQL.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "p");
        currentLibPrecursorCountSQL.append(" WHERE p.GeneralMoleculeId = ");
        currentLibPrecursorCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        currentLibPrecursorCountSQL.append(".Id");
        currentLibPrecursorCountSQL.append(" AND p.RepresentativeDataState = ?");
        currentLibPrecursorCountSQL.add(RepresentativeDataState.Representative.ordinal());
        currentLibPrecursorCountSQL.append(")");
        ExprColumn currentLibPrecursorCountCol = new ExprColumn(this, "RepresentivePrecursorCount", currentLibPrecursorCountSQL, JdbcType.INTEGER);
        currentLibPrecursorCountCol.setLabel("Library Precursor Count");
        addColumn(currentLibPrecursorCountCol);

        setTitleColumn("Sequence");

    }
}