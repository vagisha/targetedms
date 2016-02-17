package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

public class PeptideIsotopeModificationTableInfo extends FilteredTable<TargetedMSSchema>
{
    public PeptideIsotopeModificationTableInfo(TargetedMSSchema schema)
    {
        super(TargetedMSManager.getTableInfoPeptideIsotopeModification(), schema);

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