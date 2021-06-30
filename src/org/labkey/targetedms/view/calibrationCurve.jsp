<%
/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
%>

<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.json.JSONObject" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<JSONObject> me = (JspView<JSONObject>) HttpView.currentView();
    JSONObject bean = me.getModelBean();
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("vis/vis");
        dependencies.add("targetedms/js/CalibrationCurve.js");
        dependencies.add("targetedms/css/CalibrationCurve.css");
        dependencies.add("targetedms/js/svgChart.js");
        dependencies.add("targetedms/css/svgChart.css");
        dependencies.add("internal/jQuery");
    }
%>
<%
    String elementId = "targetedmsCalibrationCurve";
%>
<script type="text/javascript">

    var calibrationCurvePlot;

    Ext4.onReady(function () {

        calibrationCurvePlot = Ext4.create('LABKEY.targetedms.CalibrationCurve', {
            renderTo: <%=q(elementId)%>,
            data: <%=bean%>
        });

    });

</script>

<div style="width: 100%; text-align: center">
    <label for="calCurveXScale">X-axis:</label>
    <select id="calCurveXScale"onchange="calibrationCurvePlot.refreshPlot();">
        <option value="linear" selected>Linear</option>
        <option value="log">Log</option>
    </select>
    &nbsp;&nbsp;
    <label for="calCurveYScale">Y-axis:</label>
    <select id="calCurveYScale" onchange="calibrationCurvePlot.refreshPlot();">
        <option value="linear" selected>Linear</option>
        <option value="log">Log</option>
    </select>
</div><div id=<%=q(elementId)%> class="calibration-curve"></div>
