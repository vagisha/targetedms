<%
/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.util.UniqueID" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("Ext4ClientApi");
        dependencies.add("vis/vis");
        dependencies.add("hopscotch/css/hopscotch.min.css");
        dependencies.add("hopscotch/js/hopscotch.min.js");
        dependencies.add("targetedms/css/SVGExportIcon.css");
        dependencies.add("targetedms/css/qcTrendPlotReport.css");
        dependencies.add("targetedms/js/QCPlotHelperBase.js");
        dependencies.add("targetedms/js/QCPlotLegendHelper.js");
        dependencies.add("targetedms/js/LeveyJenningsPlotHelper.js");
        dependencies.add("targetedms/js/CUSUMPlotHelper.js");
        dependencies.add("targetedms/js/MovingRangePlotHelper.js");
        dependencies.add("targetedms/js/QCPlotHelperWrapper.js");
        dependencies.add("targetedms/js/BaseQCPlotPanel.js");
        dependencies.add("targetedms/js/QCTrendPlotPanel.js");
        dependencies.add("targetedms/js/QCPlotHoverPanel.js");
    }
%>
<%
    int uid = UniqueID.getRequestScopedUID(HttpView.currentRequest());
    String reportPanelId = "reportHeaderPanel-" + uid;
    String plotPanelId = "tiledPlotPanel-" + uid;
    String plotPaginationPanelId = "plotPaginationPanel-" + uid;
%>

<div id=<%=q(reportPanelId)%>></div>
<div id=<%=q(plotPaginationPanelId)%> class="plotPaginationHeaderPanel"></div>
<div id=<%=q(plotPanelId)%> class="tiledPlotPanel"></div>

<script type="text/javascript">
        function init() {
            var reportPanelId = <%=q(reportPanelId)%>;
            var plotPanelId = <%=q(plotPanelId)%>;
            var plotPaginationPanelId = <%=q(plotPaginationPanelId)%>;

            if (Ext4.isIE8) {
                Ext4.get(plotPanelId).update("<span class='labkey-error'>Unable to render report in Internet Explorer < 9.</span>");
                return;
            }

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: 'SELECT MIN(SampleFileId.AcquiredTime) AS MinAcquiredTime, MAX(SampleFileId.AcquiredTime) AS MaxAcquiredTime FROM generalmoleculechrominfo',
                success: function(data) {
                    if (data.rows.length == 0 || !data.rows[0]['MinAcquiredTime']) {
                        Ext4.get(plotPanelId).update("No data found. Please upload runs using the Data Pipeline or directly from Skyline.");
                    }
                    else {
                        initializeReportPanels(data, reportPanelId, plotPanelId, plotPaginationPanelId);
                    }
                },
                failure: function(response) {
                    Ext4.get(plotPanelId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
                }
            });
        }

        function initializeReportPanels(data, reportPanelId, plotPanelId, plotPaginationPanelId)
        {
            // initialize the panel that displays the Levey-Jennings plot for trend plotting
            Ext4.create('LABKEY.targetedms.QCTrendPlotPanel', {
                renderTo: reportPanelId,
                cls: 'qc-trend-plot-panel',
                plotDivId: plotPanelId,
                plotPaginationDivId: plotPaginationPanelId,
                minAcquiredTime: Ext4.Date.parse(data.rows[0]['MinAcquiredTime'], LABKEY.Utils.getDateTimeFormatWithMS()),
                maxAcquiredTime: Ext4.Date.parse(data.rows[0]['MaxAcquiredTime'], LABKEY.Utils.getDateTimeFormatWithMS())
            });
        }

        Ext4.onReady(init);
</script>
