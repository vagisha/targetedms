<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.skyaudit.AuditLogEntry" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<AuditLogEntry> me =
            (JspView<AuditLogEntry>) HttpView.currentView();
    AuditLogEntry bean = me.getModelBean();
%>
    <div id="targetedmsAuditLogExtraInfo" >
    <% if (bean == null) { %>
        Unable to find requested audit log entry
    <% } else { %>
    <table>
        <tr><td><strong>User Name:</strong> <%= h(bean.getUserName()) %></td><td><span class="fa fa-times">&nbsp;</span></td></tr>
        <tr><td colspan="2"><strong>Entry Timestamp:</strong> <%=formatDateTime(bean.getCreateTimestamp())%></td></tr>
    </table><br/>
    <pre style="overflow: scroll; max-height: 400px"><%=h(bean.getExtraInfo()) %></pre>
    <% } %>
</div>
