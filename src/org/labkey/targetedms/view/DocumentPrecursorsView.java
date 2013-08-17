/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.PrecursorTableInfo;

import java.sql.SQLException;

/**
 * User: vsharma
 * Date: 4/17/12
 * Time: 10:52 PM
 */
public class DocumentPrecursorsView extends NestableQueryView
{
    public static final String DATAREGION_NAME = "precursors_view";
    public static final String TITLE = "Precursor List";

    private TargetedMSSchema _targetedMsSchema = null;
    private final int _runId;
    private final String _tableName;

    public DocumentPrecursorsView(ViewContext ctx, TargetedMSSchema schema, String queryName, int runId, boolean forExport) throws SQLException
    {
        super(schema, schema.getSettings(ctx, DATAREGION_NAME, queryName), true, !forExport,
                new QueryNestingOption(FieldKey.fromParts("PeptideId", "PeptideGroupId"),
                        FieldKey.fromParts("PeptideId", "PeptideGroupId", "Id"), null));
        _targetedMsSchema = schema;
        _runId = runId;
        _tableName = queryName;
        setTitle(TITLE);
    }

    /**
     * Overridden to add the run id filter condition.
     * @return A document transitions TableInfo filtered to the current run id
     */
    public TableInfo createTable()
    {
        assert null != _targetedMsSchema : "Targeted MS Schema was not set in DocumentPrecursorsView class!";
        PrecursorTableInfo tinfo  = (PrecursorTableInfo) _targetedMsSchema.getTable(_tableName);
        if (tinfo != null)
        {
            tinfo.setRunId(_runId);
        }
        String viewName = getSettings().getViewName();
        if (_tableName.equalsIgnoreCase(TargetedMSSchema.TABLE_LIBRARY_DOC_PRECURSOR) &&
            (StringUtils.isBlank(viewName)))
        {
            // If we are looking at the default view for the precursor list of a document in a library
            // folder, show only the current representative precursors.
            PrecursorTableInfo.LibraryPrecursorTableInfo tableInfo = (PrecursorTableInfo.LibraryPrecursorTableInfo) tinfo;
            tableInfo.selectRepresentative();
        }
        return tinfo;
    }

    protected Sort getBaseSort()
    {
        return new Sort("Id");
    }
}
