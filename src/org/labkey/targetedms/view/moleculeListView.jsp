<%
/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.PeptideGroup" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.passport.PassportController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PeptideGroup> me = (JspView<PeptideGroup>) HttpView.currentView();
    PeptideGroup bean = me.getModelBean();
    TargetedMSRun run = TargetedMSManager.getRun(bean.getRunId());
%>

<table class="lk-fields-table">
    <tr>
        <td class="labkey-form-label">File</td>
        <td><a href="<%= h(TargetedMSController.getShowRunURL(run.getContainer(), run.getRunId()))%>"><%= h(run.getDescription()) %></a></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Description</td>
        <td><%= h(bean.getDescription()) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Decoy</td>
        <td><%= h(bean.isDecoy()) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Note</td>
        <td><%= h(bean.getNote()) %></td>
    </tr>
    <% if (run.getReplicateCount() > 1) { %>
    <tr>
        <td class="labkey-form-label"></td>
        <td><%= link("Reproducibility Report", new ActionURL(PassportController.ProteinAction.class, me.getViewContext().getContainer()).addParameter("proteinId", bean.getId())) %></td>
    </tr>
    <% } %>
</table>


