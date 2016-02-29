package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;

public class GeneralMoleculeChromInfoTableInfo extends TargetedMSTable
{
    public GeneralMoleculeChromInfoTableInfo(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL, String name)
    {
        super(table, schema, joinSQL);
        setName(name);

        ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE);
            }
        });

        ColumnInfo peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE);
            }
        });
        addColumn(peptideId);

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("PeptideId"));
        visibleColumns.add(FieldKey.fromParts("SampleFileId"));
        visibleColumns.add(FieldKey.fromParts("PeakCountRatio"));
        visibleColumns.add(FieldKey.fromParts("RetentionTime"));
        setDefaultVisibleColumns(visibleColumns);
    }
}