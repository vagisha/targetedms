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

<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.MoleculePrecursor" %>
<%@ page import="org.labkey.targetedms.view.IconFactory" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.MoleculeChromatogramsViewBean> me = (JspView<TargetedMSController.MoleculeChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.MoleculeChromatogramsViewBean bean = me.getModelBean();

    ActionURL precursorDetailsUrl = new ActionURL(TargetedMSController.MoleculePrecursorAllChromatogramsChartAction.class, getContainer());
%>

<table class="lk-fields-table">
    <tr>
        <td class="labkey-form-label">Name</td>
        <td><%= h(bean.getRun().getDescription())%></td>
    </tr>
    <tr>
        <%
            Integer seqId = bean.getPeptideGroup().getSequenceId();
            boolean isProtein = seqId  != null;
            String fieldLabel =  isProtein ? "Protein" : "Group";

            ActionURL showProteinUrl = new ActionURL(TargetedMSController.ShowProteinAction.class, getContainer());
            showProteinUrl.addParameter("id", bean.getPeptideGroup().getId());
        %>
        <td class="labkey-form-label"><%=h(fieldLabel)%></td>
        <td><a href="<%=h(showProteinUrl)%>"><%= h(bean.getPeptideGroup().getLabel())%></a></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Custom Ion Name</td>
        <td><%= h(bean.getMolecule().getCustomIonName() == null ? "" : bean.getMolecule().getCustomIonName())%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Ion Formula</td>
        <td><%= h(bean.getMolecule().getIonFormula() == null ? "" : bean.getMolecule().getIonFormula())%></td>
    </tr>

    <% if(bean.getMolecule().getMassAverage() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Mass Average</td>
        <td><%=h(Formats.f4.format(bean.getMolecule().getMassAverage()))%></td>
    </tr>
    <%}%>

    <% if(bean.getMolecule().getMassMonoisotopic() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Mass Monoisotopic</td>
        <td><%=h(Formats.f4.format(bean.getMolecule().getMassMonoisotopic()))%></td>
    </tr>
    <%}%>

    <% if(bean.getMolecule().getAvgMeasuredRetentionTime() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Avg. RT</td>
        <td><%=h(Formats.f4.format(bean.getMolecule().getAvgMeasuredRetentionTime()))%></td>
    </tr>
    <%}%>

    <% if(bean.getMolecule().getPredictedRetentionTime() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Predicted RT</td>
        <td><%=h(Formats.f4.format(bean.getMolecule().getPredictedRetentionTime()))%></td>
    </tr>
    <%}%>

    <% if(bean.getMolecule().getRtCalculatorScore() != null)
    {%>
    <tr>
        <td class="labkey-form-label">RT Score</td>
        <td><%=h(Formats.f4.format(bean.getMolecule().getRtCalculatorScore()))%></td>
    </tr>
    <%}%>

    <tr>
        <td class="labkey-form-label">Molecule Precursors</td>
        <td>
    <%
            for (MoleculePrecursor precursor: bean.getPrecursorList())
            {
    %>
            <div>
                <%=text(precursor.getHtml())%>
                <a href="<%=precursorDetailsUrl+"id="+precursor.getId()%>">
                    <img src="<%=IconFactory.getTransitionGroupIconPath()%>" alt="Click to view details"/>
                </a>
            </div>
    <%
            }
    %>
        </td>

    </tr>
</table>


