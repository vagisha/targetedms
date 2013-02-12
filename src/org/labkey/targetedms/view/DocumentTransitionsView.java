/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.view;

import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.DocTransitionsTableInfo;

import java.sql.SQLException;

/**
 * User: vsharma
 * Date: 4/17/12
 * Time: 10:52 PM
 */
public class DocumentTransitionsView extends NestableQueryView
{
    public static final String DATAREGION_NAME = "transitions_view";
    public static final String TITLE = "Transition List";

    private TargetedMSSchema _targetedMsSchema = null;
    private final int _runId;

    private ViewContext _context;

    public DocumentTransitionsView(ViewContext ctx, TargetedMSSchema schema, int runId, boolean forExport) throws SQLException
    {
        super(schema, schema.getSettings(ctx, DATAREGION_NAME, TargetedMSSchema.TABLE_TRANSITION), true, !forExport,
                new QueryNestingOption(FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId"), FieldKey.fromParts("PrecursorId", "PeptideId", "PeptideGroupId", "Id"), null));
        _targetedMsSchema = schema;
        _runId = runId;
        setTitle(TITLE);
        _context = ctx;
    }

    /**
     * Overridden to add the run id filter condition.
     * @return A document transitions TableInfo filtered to the current run id
     */
    public TableInfo createTable()
    {
        assert null != _targetedMsSchema : "Targeted MS Schema was not set in DocumentTransitionsView class!";
        DocTransitionsTableInfo tinfo  = (DocTransitionsTableInfo) _targetedMsSchema.getTable(TargetedMSSchema.TABLE_TRANSITION);
        if (tinfo != null)
        {
            tinfo.setRunId(_runId);
        }
        return tinfo;
    }

    protected Sort getBaseSort()
    {
        return new Sort("Id");
    }
}
