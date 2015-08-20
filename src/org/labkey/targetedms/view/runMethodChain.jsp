
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.exp.api.ExpRun" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        resources.add(ClientDependency.fromPath("targetedms/js/LinkVersionsDialog.js"));
        resources.add(ClientDependency.fromPath("targetedms/css/LinkVersionsDialog.css"));
        return resources;
    }
%>
<%
    JspView<ExpRun> me = (JspView<ExpRun>) HttpView.currentView();
    ExpRun run = me.getModelBean();
%>

<div id="runMethodChainDetails"></div>

<script type="text/javascript">
    Ext4.onReady(function() {
        Ext4.create('LABKEY.targetedms.LinkedVersions', {
            divId: 'runMethodChainDetails',
            selectedRowIds: [<%=run.getRowId()%>],
            asView: true
        });
    });
</script>