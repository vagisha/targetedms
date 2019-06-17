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

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryNestingOption;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.MoleculePrecursorTableInfo;

import java.sql.SQLException;

/**
 * User: binalpatel
 * Date: 2/26/2016
 */
public class SmallMoleculePrecursorsView extends DocumentPrecursorsView
{
    public static final String DATAREGION_NAME = "small_mol_precursors_view";
    public static final String TITLE = "Small Molecule Precursor List";

    public SmallMoleculePrecursorsView(ViewContext ctx, TargetedMSSchema schema, String queryName, int runId, boolean forExport)
    {
        super(ctx, schema, queryName, runId, !forExport,
                new QueryNestingOption(FieldKey.fromParts("MoleculeId", "PeptideGroupId"),
                        FieldKey.fromParts("MoleculeId", "PeptideGroupId", "Id"), null), DATAREGION_NAME);
        setTitle(TITLE);
    }

    /**
     * Overridden to add the run id filter condition.
     * @return A document transitions TableInfo filtered to the current run id
     */
    public TableInfo createTable()
    {
        assert null != _targetedMsSchema : "Targeted MS Schema was not set in SmallMoleculePrecursorsView class!";

        MoleculePrecursorTableInfo tinfo  = (MoleculePrecursorTableInfo) _targetedMsSchema.getTable(_tableName, null, true, true);

        if (tinfo != null)
            tinfo.setRunId(_runId);

        tinfo.setLocked(true);
        return tinfo;
    }
}
