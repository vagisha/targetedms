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
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.PeptideSettings" %>
<%@ page import="org.labkey.targetedms.parser.Precursor" %>
<%@ page import="org.labkey.targetedms.view.PrecursorHtmlMaker" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.targetedms.view.ModifiedPeptideHtmlMaker" %>
<%@ page import="org.labkey.targetedms.view.IconFactory" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PeptideChromatogramsViewBean> me = (JspView<TargetedMSController.PeptideChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.PeptideChromatogramsViewBean bean = me.getModelBean();
    Map<Integer, String> labelIdMap = new HashMap<>();
    for(PeptideSettings.IsotopeLabel label: bean.getLabels())
    {
        labelIdMap.put(label.getId(), label.getName());
    }
    ActionURL precursorDetailsUrl = new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class, getContainer());
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
        <td class="labkey-form-label">Sequence</td>
        <td><%=text(new ModifiedPeptideHtmlMaker().getPeptideHtml(bean.getPeptide(), bean.getRun().getId()))%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">NeutralMass</td>
        <td><%= h(bean.getPeptide().getCalcNeutralMass() == null ? "" : Formats.f4.format(bean.getPeptide().getCalcNeutralMass()))%></td>
    </tr>
    
    <tr>
        <td class="labkey-form-label">Avg. RT</td>
        <td><%=h(bean.getPeptide().getAvgMeasuredRetentionTime() == null ? "" : Formats.f4.format(bean.getPeptide().getAvgMeasuredRetentionTime()))%></td>
    </tr>
    
    <% if(bean.getPeptide().getPredictedRetentionTime() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Predicted RT</td>
        <td><%=h(Formats.f4.format(bean.getPeptide().getPredictedRetentionTime()))%></td>
    </tr>
    <%}%>

    <% if(bean.getPeptide().getRtCalculatorScore() != null)
    {%>
    <tr>
        <td class="labkey-form-label">RT Score</td>
        <td><%=h(Formats.f4.format(bean.getPeptide().getRtCalculatorScore()))%></td>
    </tr>
    <%}%>
    
    <tr>

        <td class="labkey-form-label">Precursors</td>
        <td>
            <%for(Precursor precursor: bean.getPrecursorList())
            {%>
                 <div>
                     <%=text(PrecursorHtmlMaker.getHtml(bean.getPeptide(),
                                                    precursor,
                                                    labelIdMap.get(precursor.getIsotopeLabelId()),
                                                    bean.getRun().getId())
                     )%>

                     <% String imgUrl = IconFactory.getPrecursorIconPath(precursor.getId(), bean.getPeptide().isDecoyPeptide()); %>
                     <a href="<%=precursorDetailsUrl+"id="+precursor.getId()%>">
                        <img src="<%=imgUrl%>" alt="Click to view details"/>
                     </a>

                 </div>
            <%}%>

        </td>

    </tr>
</table>


