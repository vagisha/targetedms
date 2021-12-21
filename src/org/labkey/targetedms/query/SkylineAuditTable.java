package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.view.SkylineAuditLogExtraInfoAJAXDisplayColumnFactory;

import java.util.ArrayList;
import java.util.List;

public class SkylineAuditTable extends VirtualTable<TargetedMSSchema>
{
    private final TargetedMSRun _run;

    public SkylineAuditTable(@NotNull String name, @NotNull TargetedMSSchema schema, @NotNull TargetedMSRun run)
    {
        super(TargetedMSManager.getSchema(), name, schema);
        _run = run;

        for (ColumnInfo column : TargetedMSManager.getTableInfoSkylineAuditLogEntry().getColumns())
        {
            addColumn(new BaseColumnInfo(column, this));
        }
        MutableColumnInfo extraInfoCol = new BaseColumnInfo("HasExtraInfo", this, JdbcType.VARCHAR);
        extraInfoCol.setDisplayColumnFactory(new SkylineAuditLogExtraInfoAJAXDisplayColumnFactory());
        extraInfoCol.setLabel("Extra Info");
        addColumn(extraInfoCol);
        addColumn(new BaseColumnInfo("OrderNumber", this, JdbcType.INTEGER));
        addColumn(new BaseColumnInfo("OrderNumberDescription", this, JdbcType.VARCHAR));
        addColumn(new BaseColumnInfo("MessageType", this, JdbcType.VARCHAR));
        addColumn(new BaseColumnInfo("MessageText", this, JdbcType.VARCHAR));

        getMutableColumn("VersionId").setFk(QueryForeignKey.from(schema, this.getContainerFilter()).table(TargetedMSSchema.TABLE_RUNS));
    }

    @Override
    public @NotNull SQLFragment getFromSQL()
    {
        SQLFragment cteSQL = new SQLFragment();
        cteSQL.append("SELECT ");
        String separator = "";
        for (ColumnInfo column : TargetedMSManager.getTableInfoSkylineAuditLogEntry().getColumns())
        {
            cteSQL.append(separator);
            separator = ", ";
            cteSQL.append(column.getValueSql("e"));
        }
        cteSQL.append("\n FROM ");
        cteSQL.append(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), "e");
        cteSQL.append(" WHERE e.VersionId = ? AND e.DocumentGUID = ?");
        cteSQL.add(_run.getId());
        cteSQL.add(_run.getDocumentGUID());

        cteSQL.append("\n");
        cteSQL.append("UNION ALL");
        cteSQL.append("\n");

        cteSQL.append("SELECT ");
        separator = "";
        for (ColumnInfo column : TargetedMSManager.getTableInfoSkylineAuditLogEntry().getColumns())
        {
            cteSQL.append(separator);
            separator = ", ";
            if ("VersionId".equalsIgnoreCase(column.getName()))
            {
               cteSQL.append("COALESCE(nxt.VersionId, prev.VersionId) AS VersionId");
            }
            else
            {
                cteSQL.append(column.getValueSql("nxt"));
            }
        }
        cteSQL.append("\n FROM ");
        cteSQL.append(TargetedMSManager.getTableInfoSkylineAuditLogEntry(), "nxt");
        cteSQL.append(" JOIN logTree prev ON prev.parentEntryHash = nxt.entryHash AND nxt.DocumentGUID = ?");
        cteSQL.add(_run.getDocumentGUID());

        SQLFragment result = new SQLFragment();
        result.addCommonTableExpression("TargetedMSAuditCTE", "logTree", cteSQL, getSqlDialect().isPostgreSQL());
        result.append("SELECT lt.* \n");

        result.append(", CASE WHEN msg.orderNumber = 0 AND lt.extraInfo IS NOT NULL THEN '(info)' END AS HasExtraInfo\n");
        result.append(", msg.OrderNumber\n");
        result.append(", msg.MessageType\n");
        result.append(", msg.enText AS MessageText\n");
        result.append(", CASE msg.orderNumber WHEN 0 THEN 'UndoRedo' WHEN 1 THEN 'Summary' ELSE 'All Info' END AS OrderNumberDescription\n");
        result.append(" FROM logTree lt JOIN ");
        result.append(TargetedMSManager.getTableInfoSkylineAuditLogMessage(), "msg");
        result.append(" ON lt.entryId = msg.entryId AND msg.OrderNumber = 0");
        return result;
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        List<FieldKey> result = new ArrayList<>();
        result.add(FieldKey.fromParts("CreateTimestamp"));
        result.add(FieldKey.fromParts("UserName"));
        result.add(FieldKey.fromParts("MessageText"));
        result.add(FieldKey.fromParts("HasExtraInfo"));

        return result;
    }
}
