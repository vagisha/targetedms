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

public class MoleculeTableInfo extends AbstractGeneralMoleculeTableInfo
{
    public MoleculeTableInfo(TargetedMSSchema schema)
    {
        super(schema, TargetedMSManager.getTableInfoMolecule(), "Molecule Annotations");

//        TODO: Create and Implement ShowMoleculeAction class in TargetedMSController
//        final DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowMoleculeAction.class, getContainer()),
//                Collections.singletonMap("id", "Id"));
//        setDetailsURL(detailsURL);

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
        noteAnnotation.setLabel("Molecule Note/Annotations");
        addColumn(noteAnnotation);

        ColumnInfo customIonName = getColumn("CustomIonName");
//        customIonName.setURL(detailsURL); TODO: uncomment after ShowMoleculeAction is implemented

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "File"));
        defaultCols.add(1, FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
        defaultCols.add(FieldKey.fromParts("PeptideId", "RtCalculatorScore"));
        defaultCols.add(FieldKey.fromParts("PeptideId", "PredictedRetentionTime"));
        defaultCols.add(FieldKey.fromParts("PeptideId", "AvgMeasuredRetentionTime"));
        defaultCols.add(FieldKey.fromParts("PeptideId", "ExplicitRetentionTime"));
        defaultCols.add(FieldKey.fromParts("PeptideId", "StandardType"));
        setDefaultVisibleColumns(defaultCols);
    }
}