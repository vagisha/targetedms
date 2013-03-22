package org.labkey.targetedms.search;

import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;

/**
 * User: cnathe
 * Date: 3/22/13
 */
public class ModificationSearchBean
{
    private final TargetedMSController.ModificationSearchForm _form;

    public ModificationSearchBean(TargetedMSController.ModificationSearchForm form)
    {
        _form = form;
    }

    public TargetedMSController.ModificationSearchForm getForm()
    {
        return _form;
    }
}
