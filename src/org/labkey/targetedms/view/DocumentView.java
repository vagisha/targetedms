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

import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.Sort;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

import java.sql.SQLException;

/**
 * User: binalpatel
 * Date: 2/27/2016
 */

public abstract class DocumentView extends NestableQueryView
{
    protected final int _runId;

    public DocumentView(ViewContext ctx, TargetedMSSchema schema, String queryName, int runId, boolean forExport,
                        QueryNestingOption nestingOption, String dataRegionName)
    {
        super(schema, schema.getSettings(ctx, dataRegionName, queryName), true, forExport, nestingOption);
        _runId = runId;
    }

    protected Sort getBaseSort()
    {
        return new Sort("Id");
    }
}
