package org.labkey.targetedms.view;

import org.labkey.api.view.GridView;
import org.labkey.api.view.template.ClientDependency;
import org.springframework.validation.BindException;

public class ChromatogramGridView extends GridView
{
    public ChromatogramGridView(ChromatogramsDataRegion dataRegion, BindException errors)
    {
        super(dataRegion, errors);

        addClientDependency(ClientDependency.fromPath("/TargetedMS/css/svgChart.css"));
        addClientDependency(ClientDependency.fromPath("/TargetedMS/js/svgChart.js"));
    }
}
