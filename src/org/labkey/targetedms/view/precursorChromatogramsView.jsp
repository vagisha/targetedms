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
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.targetedms.parser.PeptideSettings" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PrecursorChromatogramsViewBean> me = (JspView<TargetedMSController.PrecursorChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.PrecursorChromatogramsViewBean bean = me.getModelBean();
    DecimalFormat ROUND_4 = new DecimalFormat("0.0000");
%>

<table class="lk-fields-table">
    <tr>
        <td class="labkey-form-label">File</td>
        <td><%= h(bean.getRun().getFileName())%></td>
    </tr>
    <tr>
        <%
            String fieldLabel = bean.getPeptideGroup().getSequenceId() != null ? "Protein" : "Group";
        %>
        <td class="labkey-form-label"><%=h(fieldLabel)%></td>
        <td><%= h(bean.getPeptideGroup().getLabel())%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Precursor</td>
        <td><%= text(bean.getModifiedPeptideHtml())%></td>
    </tr>

    <%
        if(!PeptideSettings.IsotopeLabel.LIGHT.equalsIgnoreCase(bean.getIsotopeLabel().getName()))
        {
    %>
    <tr>
        <td class="labkey-form-label">Label</td>
        <td><%= h(bean.getIsotopeLabel().getName()) %></td>
    </tr>
    <%}%>

    <tr>
        <td class="labkey-form-label">Charge</td>
        <td><%= bean.getPrecursor().getCharge()%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">m/z</td>
        <td><%= h(ROUND_4.format(bean.getPrecursor().getMz())) %></td>
    </tr>
</table>


