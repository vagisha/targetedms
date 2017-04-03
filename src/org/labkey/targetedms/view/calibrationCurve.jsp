
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
            <%--curveId: <%=bean.getCalibrationCurveId()%>,--%>
            renderTo: <%=q(elementId)%>,
            data: <%=bean.getJsonData()%>
        });

    });

</script>