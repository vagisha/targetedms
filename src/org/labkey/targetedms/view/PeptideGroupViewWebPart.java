/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.view;

import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;

public class PeptideGroupViewWebPart extends QueryView
{
    public PeptideGroupViewWebPart(ViewContext viewContext)
    {
        super(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()));
        setSettings(createQuerySettings(viewContext, TargetedMSSchema.TABLE_PEPTIDE_GROUP));
        setTitle("Proteins");
        setShowDetailsColumn(false);

        setShowBorders(true);
        setShadeAlternatingRows(true);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        UserSchema schema = getSchema();
        QuerySettings settings = schema.getSettings(portalCtx, dataRegionName, TargetedMSSchema.TABLE_PEPTIDE_GROUP);

        if (!portalCtx.getRequest().getParameterMap().containsKey(settings.param(QueryParam.viewName)))
            settings.setViewName("LibraryProteins");

        return settings;
    }
}
