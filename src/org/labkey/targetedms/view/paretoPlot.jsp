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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        resources.add(ClientDependency.fromPath("vis/vis"));
        resources.add(ClientDependency.fromPath("targetedms/js/ParetoPlotPanel.js"));
        return resources;
    }
%>

<div id="reportHeaderPanel"></div>
<div id="tiledPlotPanel"></div>

<script type="text/javascript">

        function init()
        {
            if (Ext4.isIE8) {
                Ext4.get('tiledPlotPanel').update("<span class='labkey-error'>Unable to render report in Internet Explorer < 9.</span>");
                return;
            }

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: 'SELECT * FROM guideset',
                success: function(data) {
                    if (data.rows.length == 0)
                    {
                        Ext4.get('tiledPlotPanel').update("Guidesets not found. Please create a guideset using <a href=" + LABKEY.ActionURL.buildURL('project', 'begin', null, null) + ">Levey-Jennings QC Plots</a>" + ".");
                    }
                    else
                    {
                        initializeParetoPlotPanel(data);
                    }
                },
                failure: function(response) {
                    Ext4.get('tiledPlotPanel').update("<span class='labkey-error'>Error: " + response.exception + "</span>");
                }
            });
        }

        function initializeParetoPlotPanel(data) {

            // initialize the panel that displays Pareto plot
            Ext4.create('LABKEY.targetedms.ParetoPlotPanel', {
                renderTo: 'tiledPlotPanel',
                html: "<div id='paretoPlotDiv'></div>",
                height: 1000,
                cls: 'themed-panel',
                guideSetData: data
            });
        }

        Ext4.onReady(init);
</script>
