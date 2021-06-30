<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    JspView<String> me = (JspView<String>) HttpView.currentView();
    String divId = me.getModelBean();
%>

<div id="<%= h(divId )%>"></div>
