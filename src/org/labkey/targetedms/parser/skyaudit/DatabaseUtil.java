package org.labkey.targetedms.parser.skyaudit;

import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtil
{

    public static Object retrieveSimpleType(SQLFragment pQuery){

        Object result = new SqlSelector(TargetedMSManager.getSchema(), pQuery)
                .getObject(Object.class );
        return result;
    }

    public static Map<String, Object> retrieveTuple(SQLFragment pQuery){
        BaseSelector.ResultSetHandler<Map<String, Object>> resultSetHandler = (rs, conn) -> {
            if(rs.next()){
                Map<String, Object> result = new HashMap<>();
                for(int i= 0; i < rs.getMetaData().getColumnCount(); i++){
                    String colName = rs.getMetaData().getColumnName(i);
                    result.put(colName, rs.getObject(i));
                    if(rs.wasNull())
                        result.put(colName, null);
                }
                return result;
            }
            else
                return new HashMap<>();
        };

        Map<String, Object> result = new SqlExecutor(TargetedMSSchema.getSchema()).executeWithResults(pQuery, resultSetHandler);
        return result;
    }
}
