package org.labkey.targetedms.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.MoleculeTransition;

public class MoleculeTransitionManager
{
    private MoleculeTransitionManager() {}

    public static MoleculeTransition get(int transitionId, User user, Container container)
    {
        TableInfo table = new MoleculeTransitionsTableInfo(new TargetedMSSchema(user, container));
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("TransitionId"), transitionId);
        return new TableSelector(table, MoleculeTransition.getColumns(), filter, null).getObject(MoleculeTransition.class);
    }
}
