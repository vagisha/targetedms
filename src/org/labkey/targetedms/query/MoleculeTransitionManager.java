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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.MoleculeTransition;

import java.util.Collection;

public class MoleculeTransitionManager
{
    private MoleculeTransitionManager() {}

    public static MoleculeTransition get(int transitionId, User user, Container container)
    {
        TableInfo table = new MoleculeTransitionsTableInfo(new TargetedMSSchema(user, container), null, true);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("TransitionId"), transitionId);
        return new TableSelector(table, MoleculeTransition.getColumns(), filter, null).getObject(MoleculeTransition.class);
    }

    @NotNull
    public static Collection<MoleculeTransition> getTransitionsForPrecursor(int precursorId, User user, Container container)
    {
        return new TableSelector(new MoleculeTransitionsTableInfo(new TargetedMSSchema(user, container), null, true),
                MoleculeTransition.getColumns(),
                new SimpleFilter(FieldKey.fromParts("GeneralPrecursorId"), precursorId), null)
            .getCollection(MoleculeTransition.class);
    }
}
