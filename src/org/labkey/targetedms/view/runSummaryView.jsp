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

<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSRun" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<TargetedMSController.RunDetailsBean> me = (JspView<TargetedMSController.RunDetailsBean>) HttpView.currentView();
    TargetedMSController.RunDetailsBean bean = me.getModelBean();
    boolean conflictedRun = TargetedMSManager.isRunConflicted(bean.getRun());
    TargetedMSRun.RepresentativeDataState representativeState = bean.getRun().getRepresentativeDataState();

    ActionURL changeStateUrl = new ActionURL(TargetedMSController.ChangeRepresentativeStateAction.class, getViewContext().getContainer());

    ActionURL downloadAction = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getViewContext().getContainer());
    downloadAction.addParameter("runId", bean.getRun().getId());
%>

<table>
    <tr>
        <td class="labkey-form-label">File</td>
        <td>
            <%= h(bean.getRun().getFileName())%>
            <%= textLink("Download", downloadAction)%>
        </td>
    </tr>
    <tr>
        <td class="labkey-form-label">Representative State</td>
        <%String color = conflictedRun ?
                         "#ff0000" :
                         (bean.getRun().isRepresentative() ?
                         "#008000" : "#000000");
        %>

        <td>
            <span style="color:<%=text(color)%>;">
                <%= representativeState%>
            </span>
        </td>
        <td>
            <%if(conflictedRun){
                ActionURL url = (representativeState == TargetedMSRun.RepresentativeDataState.Representative_Protein) ?
                                            new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, getViewContext().getContainer()) :
                                            new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, getViewContext().getContainer());

                url.addParameter("conflictedRunId", String.valueOf(bean.getRun().getId()));
                String conflictViewUrl = url.getLocalURIString();
            %>
                <a style="color:red; text-decoration:underline;" href="<%=conflictViewUrl%>">Resolve conflicts</a>
                <%
                    changeStateUrl.addParameter("state", TargetedMSRun.RepresentativeDataState.NotRepresentative.toString());
                    changeStateUrl.addParameter("runId", String.valueOf(bean.getRun().getId()));
                %>
                <a style="text-decoration:underline;" href="<%=changeStateUrl.getLocalURIString()%>">Set Not Representative</a>
            <%} else {%>
                <form action="<%=changeStateUrl%>">
                    <input type="hidden" name="runId" value="<%=bean.getRun().getId()%>"/>
                    <select name="state">
                        <%if(representativeState != TargetedMSRun.RepresentativeDataState.NotRepresentative){%>
                            <option value="<%=TargetedMSRun.RepresentativeDataState.NotRepresentative%>">Not Representative</option>
                        <%}%>
                        <%if(representativeState != TargetedMSRun.RepresentativeDataState.Representative_Protein){%>
                            <option value="<%=TargetedMSRun.RepresentativeDataState.Representative_Protein%>">Representative Protein</option>
                        <%}%>
                        <%if(representativeState != TargetedMSRun.RepresentativeDataState.Representative_Peptide){%>
                            <option value="<%=TargetedMSRun.RepresentativeDataState.Representative_Peptide%>">Representative Peptide</option>
                        <%}%>
                    </select>
                    <%=generateSubmitButton("Change State")%>
                </form>
            <%}%>
        </td>
    </tr>
    <tr>
        <td class="labkey-form-label">Peptide Group Count</td>
        <td><%= bean.getRun().getPeptideGroupCount()%></td>

        <td class="labkey-form-label">Peptide Count</td>
        <td><%= bean.getRun().getPeptideCount()%></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Precursor Count</td>
        <td><%= bean.getRun().getPrecursorCount()%></td>

        <td class="labkey-form-label">Transition Count</td>
        <td><%= bean.getRun().getTransitionCount()%></td>
    </tr>
</table>


