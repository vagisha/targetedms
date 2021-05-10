<%
/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.MoleculePrecursorChromatogramsViewBean> me = (JspView<TargetedMSController.MoleculePrecursorChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.MoleculePrecursorChromatogramsViewBean bean = me.getModelBean();
    TargetedMSRun run = bean.getRun();
    DecimalFormat ROUND_4 = new DecimalFormat("0.0000");
%>

<table class="lk-fields-table">
    <tr>
        <td class="labkey-form-label">File</td>
        <td>
            <a href="<%= h(TargetedMSController.getShowRunURL(run.getContainer(), run.getRunId()))%>"><%= h(run.getDescription()) %></a> &nbsp;&nbsp;
            <% if (run.getFileName() != null) { TargetedMSController.createDownloadMenu(run).render(out); } %>
        </td>
    </tr>
    <tr>
        <%
            ActionURL showProteinUrl = new ActionURL(TargetedMSController.ShowProteinAction.class, getContainer());
            showProteinUrl.addParameter("id", bean.getPeptideGroup().getId());
        %>
        <td class="labkey-form-label">Molecule Group</td>
        <td><a href="<%=h(showProteinUrl)%>"><%= h(bean.getPeptideGroup().getLabel())%></a></td>
    </tr>
    <tr>
        <%
            ActionURL showMoleculeUrl = new ActionURL(TargetedMSController.ShowMoleculeAction.class, getContainer());
            showMoleculeUrl.addParameter("id", bean.getMolecule().getId());
        %>
        <td class="labkey-form-label">Molecule</td>
        <td><a href="<%=h(showMoleculeUrl)%>"><%= h(bean.getMolecule().getName())%></a></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Molecule Precursor</td>
        <td><%= h(bean.getPrecursorLabel())%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Charge</td>
        <td><%= bean.getPrecursor().getCharge()%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">m/z</td>
        <td><%= h(ROUND_4.format(bean.getPrecursor().getMz())) %></td>
    </tr>
</table>


