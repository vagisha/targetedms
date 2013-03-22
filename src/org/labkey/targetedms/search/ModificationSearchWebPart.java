package org.labkey.targetedms.search;

import org.labkey.api.view.JspView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSModule;

/**
 * User: cnathe
 * Date: 3/22/13
 */
public class ModificationSearchWebPart extends JspView<ModificationSearchBean>
{
    public static final String NAME = "Targeted MS Modification Search";

    public ModificationSearchWebPart(TargetedMSController.ModificationSearchForm form)
    {
        super("/org/labkey/targetedms/search/modificationSearch.jsp");
        setTitle(NAME);
        setModelBean(new ModificationSearchBean(form));
    }
}
