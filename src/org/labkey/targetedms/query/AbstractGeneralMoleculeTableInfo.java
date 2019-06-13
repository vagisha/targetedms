/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.RepresentativeDataState;

/**
 * User: binalpatel
 * Date: 02/25/2016
 */

public abstract class AbstractGeneralMoleculeTableInfo extends JoinedTargetedMSTable
{
    public AbstractGeneralMoleculeTableInfo(TargetedMSSchema schema, TableInfo tableInfo, ContainerFilter cf, String annotationColumnName, boolean omitAnnotations)
    {
        super(TargetedMSManager.getTableInfoGeneralMolecule(),
                tableInfo,
                schema,
                cf,
                TargetedMSSchema.ContainerJoinType.PeptideGroupFK,
                TargetedMSManager.getTableInfoGeneralMoleculeAnnotation(),
                "GeneralMoleculeId", annotationColumnName,
                "peptide", omitAnnotations); // This may change as more small molecule work is done in Skyline.

        // use the description and title column from the specialized TableInfo
        setDescription(tableInfo.getDescription());
        setTitleColumn(tableInfo.getTitleColumn());

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