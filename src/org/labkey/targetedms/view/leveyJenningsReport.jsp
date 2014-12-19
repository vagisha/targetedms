<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
        resources.add(ClientDependency.fromPath("clientapi/ext3"));
        resources.add(ClientDependency.fromPath("targetedms/css/LeveyJenningsReport.css"));
        resources.add(ClientDependency.fromPath("targetedms/js/LeveyJenningsTrendPlotPanel.js"));
        return resources;
    }
%>

<div id="rPlotPanel"></div>
<div id="hiddenPlotPanel" style="display: none"></div>
<div id="tiledPlotPanel"></div>

<script type="text/javascript">

        var $h = Ext.util.Format.htmlEncode;

        function init()
        {
            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: 'SELECT MIN(SampleFileId.AcquiredTime) AS StartDate, MAX(SampleFileId.AcquiredTime) AS EndDate FROM peptidechrominfo',
                success: function(data) {
                    if (data.rows.length == 0 || !data.rows[0].StartDate)
                        Ext.get('tiledPlotPanel').update("No data found. Please upload runs using the Data Pipeline or directly from Skyline.");
                    else
                        initializeReportPanels(data);
                },
                failure: function(response) {
                    Ext.get('tiledPlotPanel').update("Error: " + response.exception);
                }
            });
        }

        function initializeReportPanels(data) {
            var startDate = new Date(data.rows[0].StartDate);
            var endDate = new Date(data.rows[0].EndDate);

            // initialize the panel that displays the R plot for the trend plotting of EC50, AUC, and High MFI
            var trendPlotPanel = new LABKEY.LeveyJenningsTrendPlotPanel({
                renderTo: 'rPlotPanel',
                cls: 'extContainer',
                startDate: startDate,
                endDate: endDate
            });
        }

        Ext.onReady(init);
</script>
