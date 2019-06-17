/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.PrecursorTableInfo;

import java.sql.SQLException;

/**
 * User: binalpatel
 * Date: 2/26/2016
 */

public class PeptidePrecursorsView extends DocumentPrecursorsView
{
    public static final String DATAREGION_NAME = "precursors_view";
    public static final String TITLE = "Precursor List";

    public PeptidePrecursorsView(ViewContext ctx, TargetedMSSchema schema, String queryName, int runId, boolean forExport)
    {
        super(ctx, schema, queryName, runId, !forExport,
                new QueryNestingOption(FieldKey.fromParts("PeptideId", "PeptideGroupId"),
                        FieldKey.fromParts("PeptideId", "PeptideGroupId", "Id"), null), DATAREGION_NAME);
        setTitle(TITLE);

    }

    /**
     * Overridden to add the run id filter condition.
     * @return A document transitions TableInfo filtered to the current run id
     */
    public TableInfo createTable()
    {
        assert null != _targetedMsSchema : "Targeted MS Schema was not set in PeptidePrecursorsView class!";
        String viewName = getSettings().getViewName();

        PrecursorTableInfo tinfo = (PrecursorTableInfo) _targetedMsSchema.getTable(_tableName, null, true, true);

        if (_tableName.equalsIgnoreCase(TargetedMSSchema.TABLE_LIBRARY_DOC_PRECURSOR) &&
                (StringUtils.isBlank(viewName)))
        {
            // If we are looking at the default view for the precursor list of a document in a library
            // folder, show only the current representative precursors.
            PrecursorTableInfo.LibraryPrecursorTableInfo tableInfo = (PrecursorTableInfo.LibraryPrecursorTableInfo) tinfo;
            tableInfo.selectRepresentative();
        }

        if (tinfo != null)
        {
            tinfo.setRunId(_runId);
        }

        tinfo.setLocked(true);
        return tinfo;
    }
}
