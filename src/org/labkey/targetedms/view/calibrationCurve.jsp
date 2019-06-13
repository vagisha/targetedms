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
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<TargetedMSController.CalibrationCurveForm> me = (JspView<TargetedMSController.CalibrationCurveForm>) HttpView.currentView();
    TargetedMSController.CalibrationCurveForm bean = me.getModelBean();
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("vis/vis");
        dependencies.add("targetedms/js/CalibrationCurve.js");
        dependencies.add("targetedms/css/CalibrationCurve.css");
    }
%>
<%
    String elementId = "targetedmsCalibrationCurve";
%>
<div id=<%=q(elementId)%> class="calibration-curve">
    <div id=<%=q(elementId + "-png")%> class="export-icon" style="right: 125px;"><i class="fa fa-file-image-o"></i></div>
    <div id=<%=q(elementId + "-pdf")%> class="export-icon" style="right: 155px;"><i class="fa fa-file-pdf-o"></i></div>
</div>

<script type="text/javascript">

    Ext4.onReady(function () {

        Ext4.create('LABKEY.targetedms.CalibrationCurve', {
            renderTo: <%=q(elementId)%>,
            data: <%=bean.getJsonData()%>
        });

    });

</script>