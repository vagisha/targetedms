package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

public class GeneralMoleculeAnnotationTableInfo extends TargetedMSTable
{
    public GeneralMoleculeAnnotationTableInfo(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL)
    {
        super(table, schema, joinSQL);

        ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE));
        generalMoleculeId.setHidden(true);

        ColumnInfo peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE));
        addColumn(peptideId);
    }
}