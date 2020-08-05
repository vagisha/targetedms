<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.skyaudit.AuditLogEntry" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<TargetedMSController.SkylineAuditLogExtraInfoBean> me =
            (JspView<TargetedMSController.SkylineAuditLogExtraInfoBean>) HttpView.currentView();
    TargetedMSController.SkylineAuditLogExtraInfoBean bean = me.getModelBean();
    AuditLogEntry entry = bean.getEntry();
%>
<div id="targetedmsAuditLogExtraInfo" >
    <table>
        <tr><td><strong>User Name:</strong> <%= h(entry.getUserName()) %></td><td><span class="fa fa-times">&nbsp;</span></td></tr>
        <tr><td colspan="2"><strong>Entry Timestamp:</strong> <%=formatDateTime(entry.getCreateTimestamp())%></td></tr>
    </table><br/>
    <pre style="overflow: scroll; max-height: 400px"><%=h(entry.getExtraInfo()) %></pre>
</div>
