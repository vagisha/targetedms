<%
/*
 * Copyright (c) 2015 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        resources.add(ClientDependency.fromPath("vis/vis"));
        resources.add(ClientDependency.fromPath("targetedms/css/ParetoPlot.css"));
        resources.add(ClientDependency.fromPath("targetedms/js/BaseQCPlotPanel.js"));
        resources.add(ClientDependency.fromPath("targetedms/js/ParetoPlotPanel.js"));
        return resources;
    }
%>
<%!
    String tiledPlotPanelId = "tiledPlotPanel-" + UniqueID.getRequestScopedUID(HttpView.currentRequest());
%>

<div id=<%=q(tiledPlotPanelId)%> class="tiledPlotPanel"></div>

<script type="text/javascript">

        function init() {
            var tiledPlotPanelId = <%=q(tiledPlotPanelId)%>;

            if (Ext4.isIE8) {
                Ext4.get(tiledPlotPanelId).update("<span class='labkey-error'>Unable to render report in Internet Explorer < 9.</span>");
                return;
            }

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: 'SELECT * FROM guideset',
                success: function(data) {
                    if (data.rows.length == 0)
                    {
                        Ext4.get(tiledPlotPanelId).update("Guide Sets not found. Please create a Guide Set using <a href=" + LABKEY.ActionURL.buildURL('project', 'begin', null, null) + ">Levey-Jennings QC Plots</a>" + ".");
                    }
                    else
                    {
                        initializeParetoPlotPanel(data, tiledPlotPanelId);
                    }
                },
                failure: function(response) {
                    Ext4.get(tiledPlotPanelId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
                }
            });
        }

        function initializeParetoPlotPanel(data, tiledPlotPanelId) {

            // initialize the panel that displays Pareto plot
            Ext4.create('LABKEY.targetedms.ParetoPlotPanel', {
                cls: 'themed-panel',
                plotPanelDiv: tiledPlotPanelId,
                guideSetData: data
            });
        }

        Ext4.onReady(init);
</script>
