package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;

/**
 * User: binalpatel
 * Date: 02/25/2016
 */

public class AbstractGeneralMoleculeTableInfo extends JoinedTargetedMSTable
{
    public AbstractGeneralMoleculeTableInfo(TargetedMSSchema schema, TableInfo tableInfo)
    {
        super(TargetedMSManager.getTableInfoGeneralMolecule(),
                tableInfo,
                schema,
                TargetedMSSchema.ContainerJoinType.PeptideGroupFK.getSQL(),
                TargetedMSManager.getTableInfoGeneralMoleculeAnnotation(),
                "Id", "GeneralMoleculeId");

        ColumnInfo peptideGroupId = getColumn("PeptideGroupId");
        peptideGroupId.setFk(new LookupForeignKey("Id")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
            }
        });

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

    }
}