
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
    }
%>
<%
    String elementId = "targetedmsCalibrationCurve";
%>
<div id=<%=q(elementId)%>> </div>

<script type="text/javascript">

    Ext4.onReady(function () {

        Ext4.create('LABKEY.targetedms.CalibrationCurve', {
            <%--curveId: <%=bean.getCalibrationCurveId()%>,--%>
            renderTo: <%=q(elementId)%>,
            data: <%=bean.getJsonData()%>
        });

    });

</script>