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

package org.labkey.targetedms.query;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

/**
 * User: binalpatel
 * Date: 02/25/2016
 */

public class AbstractGeneralTransitionTableInfo extends JoinedTargetedMSTable
{
    public AbstractGeneralTransitionTableInfo(final TargetedMSSchema schema, TableInfo tableInfo, ContainerFilter cf, boolean omitAnnotations)
    {
        super(TargetedMSManager.getTableInfoGeneralTransition(), tableInfo,
                schema, cf, TargetedMSSchema.ContainerJoinType.GeneralPrecursorFK,
                TargetedMSManager.getTableInfoTransitionAnnotation(), "TransitionId", "Annotations", "transition", omitAnnotations);
    }

    public void setRunId(int runId)
    {
        checkLocked();
        super.addContainerTableFilter(new CompareType.EqualsCompareClause(FieldKey.fromParts("Id"), CompareType.EQUAL, runId));
    }
}
