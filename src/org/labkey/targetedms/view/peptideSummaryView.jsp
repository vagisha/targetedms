<%--
  ~ Copyright (c) 2012 LabKey Corporation
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.PeptideSettings" %>
<%@ page import="org.labkey.targetedms.parser.Precursor" %>
<%@ page import="org.labkey.targetedms.view.PrecursorHtmlMaker" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.PeptideChromatogramsViewBean> me = (JspView<TargetedMSController.PeptideChromatogramsViewBean>) HttpView.currentView();
    TargetedMSController.PeptideChromatogramsViewBean bean = me.getModelBean();
    Map<Integer, String> labelIdMap = new HashMap<Integer, String>();
    for(PeptideSettings.IsotopeLabel label: bean.getLabels())
    {
        labelIdMap.put(label.getId(), label.getName());
    }
    ActionURL precursorChromsUrl = new ActionURL(TargetedMSController.PrecursorAllChromatogramsChartAction.class, getViewContext().getContainer());
    String imgUrl = AppProps.getInstance().getContextPath() + "/TargetedMS/images/TransitionGroup.gif";
%>

<table>
    <tr>
        <td class="labkey-form-label">File</td>
        <td><%= h(bean.getRun().getFileName())%></td>
    </tr>
    <tr>
        <%
            Integer seqId = bean.getPeptideGroup().getSequenceId();
            boolean isProtein = seqId  != null;
            String fieldLabel =  isProtein ? "Protein" : "Group";
            
            ActionURL showProteinUrl = new ActionURL(TargetedMSController.ShowProteinAction.class, getViewContext().getContainer());
            showProteinUrl.addParameter("id", bean.getPeptideGroup().getId());
        %>
        <td class="labkey-form-label"><%=fieldLabel%></td>
        <td><a href="<%=h(showProteinUrl)%>"><%= h(bean.getPeptideGroup().getLabel())%></a></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Sequence</td>
        <td><%=bean.getPeptide().getSequence()%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">NeutralMass</td>
        <td><%= bean.getPeptide().getCalcNeutralMass() == null ? "" : Formats.f4.format(bean.getPeptide().getCalcNeutralMass())%></td>
    </tr>
    
    <tr>
        <td class="labkey-form-label">Avg. RT</td>
        <td><%=bean.getPeptide().getAvgMeasuredRetentionTime() == null ? "" : Formats.f4.format(bean.getPeptide().getAvgMeasuredRetentionTime())%></td>
    </tr>
    
    <% if(bean.getPeptide().getPredictedRetentionTime() != null)
    {%>
    <tr>
        <td class="labkey-form-label">Predicted RT</td>
        <td><%=Formats.f4.format(bean.getPeptide().getPredictedRetentionTime())%></td>
    </tr>
    <%}%>
    
    <tr>

        <td class="labkey-form-label">Precursors</td>
        <td>
            <%for(Precursor precursor: bean.getPrecursorList())
            {%>
                 <div>
                     <%=PrecursorHtmlMaker.getHtml(bean.getPeptide(),
                                                    precursor,
                                                    labelIdMap.get(precursor.getIsotopeLabelId()),
                                                    bean.getLightIsotopeLableId()
                     )%>

                     <a href="<%=precursorChromsUrl+"id="+precursor.getId()%>">
                        <img src="<%=imgUrl%>" alt="Precursor Chromatogram"/>
                     </a>

                 </div>
            <%}%>

        </td>

    </tr>
</table>


