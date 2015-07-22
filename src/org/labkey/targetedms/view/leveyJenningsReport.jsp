<%
/*
 * Copyright (c) 2011-2015 LabKey Corporation
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
        resources.add(ClientDependency.fromPath("targetedms/css/LeveyJenningsReport.css"));
        resources.add(ClientDependency.fromPath("targetedms/js/BaseQCPlotPanel.js"));
        resources.add(ClientDependency.fromPath("targetedms/js/LeveyJenningsTrendPlotPanel.js"));
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
                sql: 'SELECT MIN(SampleFileId.AcquiredTime) AS StartDate, MAX(SampleFileId.AcquiredTime) AS EndDate FROM peptidechrominfo',
                success: function(data) {
                    if (data.rows.length == 0 || !data.rows[0].StartDate)
                        Ext4.get('tiledPlotPanel').update("No data found. Please upload runs using the Data Pipeline or directly from Skyline.");
                    else
                        initializeReportPanels(data);
                },
                failure: function(response) {
                    Ext4.get('tiledPlotPanel').update("<span class='labkey-error'>Error: " + response.exception + "</span>");
                }
            });
        }

        function initializeReportPanels(data) {
            var startDate = new Date(data.rows[0].StartDate);
            var endDate = new Date(data.rows[0].EndDate);

            // initialize the panel that displays the Levey-Jennings plot for trend plotting
            Ext4.create('LABKEY.targetedms.LeveyJenningsTrendPlotPanel', {
                renderTo: 'reportHeaderPanel',
                cls: 'themed-panel2',
                startDate: startDate,
                endDate: endDate
            });
        }

        Ext4.onReady(init);
</script>
