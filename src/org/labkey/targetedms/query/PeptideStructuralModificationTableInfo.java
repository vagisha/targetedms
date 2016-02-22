package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class PeptideStructuralModificationTableInfo extends FilteredTable<TargetedMSSchema>
{
    public PeptideStructuralModificationTableInfo(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoPeptideStructuralModification(), schema);

        wrapAllColumns(true);

        ColumnInfo peptideId = getColumn("PeptideId");
        peptideId.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE);
            }
        });

    }
}