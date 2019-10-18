package org.labkey.api.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;

public interface TargetedMSFolderTypeListener
{
    /** Called after a new TargetedMS folder type has been configured **/
    void folderCreated(Container c, User user);
}
