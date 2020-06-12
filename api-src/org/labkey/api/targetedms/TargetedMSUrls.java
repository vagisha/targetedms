package org.labkey.api.targetedms;

import org.labkey.api.action.UrlProvider;
import org.labkey.api.data.Container;
import org.labkey.api.view.ActionURL;

public interface TargetedMSUrls extends UrlProvider
{
    ActionURL getDownloadDocumentUrl(Container container, int runId);

    ActionURL getShowRunUrl(Container container, int runId);

    ActionURL getShowProteinUrl(Container container, int proteinId);

    ActionURL getShowPeptideUrl(Container container, int peptideId);
}
