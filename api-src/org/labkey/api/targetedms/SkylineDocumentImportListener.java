package org.labkey.api.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;

public interface SkylineDocumentImportListener
{
    void onDocumentImport(Container container, User user, ITargetedMSRun run);
}
