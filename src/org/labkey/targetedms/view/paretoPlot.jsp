<%
/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

/**
* User: cnathe
* Date: Sept 19, 2011
*/

%>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("vis/vis");
        dependencies.add("targetedms/css/SVGExportIcon.css");
        dependencies.add("targetedms/css/ParetoPlot.css");
        dependencies.add("targetedms/js/BaseQCPlotPanel.js");
        dependencies.add("targetedms/js/ParetoPlotPanel.js");
    }
%>
<%
    String tiledPlotPanelId = "tiledPlotPanel-" + getRequestScopedUID();
%>

<div id=<%=q(tiledPlotPanelId)%> class="tiledPlotPanel"></div>

<script type="text/javascript">

        function init() {
            var tiledPlotPanelId = <%=q(tiledPlotPanelId)%>;

            if (Ext4.isIE8) {
                Ext4.get(tiledPlotPanelId).update("<span class='labkey-error'>Unable to render report in Internet Explorer < 9.</span>");
                return;
            }

            initializeParetoPlotPanel(tiledPlotPanelId);
        }

        function initializeParetoPlotPanel(tiledPlotPanelId) {

            // initialize the panel that displays Pareto plot
            Ext4.create('LABKEY.targetedms.ParetoPlotPanel', {
                cls: 'themed-panel',
                plotDivId: tiledPlotPanelId
            });
        }

        Ext4.onReady(init);
</script>
