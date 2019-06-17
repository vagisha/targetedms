<%
/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

<%@ page import="org.labkey.api.exp.api.ExpRun" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("targetedms/js/LinkVersionsDialog.js");
        dependencies.add("targetedms/css/LinkVersionsDialog.css");
    }
%>
<%
    JspView<ExpRun> me = (JspView<ExpRun>) HttpView.currentView();
    ExpRun run = me.getModelBean();
%>

<div id="runMethodChainDetails">Loading...</div>

<script type="text/javascript">
    Ext4.onReady(function() {
        Ext4.create('LABKEY.targetedms.LinkedVersions', {
            divId: 'runMethodChainDetails',
            selectedRowIds: [<%=run.getRowId()%>],
            highlightedRowIds: [<%=run.getRowId()%>],
            asView: true
        });
    });
</script>