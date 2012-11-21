<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
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
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    boolean hasProteinConflicts = TargetedMSManager.hasConflictedProteins(getViewContext().getContainer());
    boolean hasPeptideConflicts = TargetedMSManager.hasConflictedPeptides(getViewContext().getContainer());
    ActionURL proteinConflictViewUrl = new ActionURL(TargetedMSController.ShowConflictUiAction.class, getViewContext().getContainer());
%>
<%if(hasProteinConflicts){%>
    <span style="color:red; font-weight:bold;">
        There are conflicting representative proteins in this folder.
        <a style="color:red; text-decoration:underline;" href="<%=proteinConflictViewUrl%>">Resolve conflicts.</a>
    </span>
<%}%>
<%if(hasPeptideConflicts){%>
    There are conflicting representative peptides in this folder.  Resolve conflicts.
<%}%>

