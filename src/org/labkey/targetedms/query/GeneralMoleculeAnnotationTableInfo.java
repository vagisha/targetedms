/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

public class GeneralMoleculeAnnotationTableInfo extends TargetedMSTable
{
    public GeneralMoleculeAnnotationTableInfo(TableInfo table, TargetedMSSchema schema, SQLFragment joinSQL)
    {
        super(table, schema, joinSQL);

        ColumnInfo generalMoleculeId = getColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE));
        generalMoleculeId.setHidden(true);

        ColumnInfo peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE));
        addColumn(peptideId);
    }
}