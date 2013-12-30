<%
/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page import="org.labkey.targetedms.query.ConflictResultsManager" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%

    long conflictCount = ConflictResultsManager.getConflictCount(getUser(), getContainer());
    TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(getContainer());
    String conflictViewUrl = (folderType == TargetedMSModule.FolderType.LibraryProtein) ?
                                           new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, getContainer()).getLocalURIString() :
                                           new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, getContainer()).getLocalURIString();
%>

<%
    if(conflictCount > 0) {
%>
    <%if(folderType == TargetedMSModule.FolderType.LibraryProtein){%>
        <div style="color:red; font-weight:bold;">
            There are conflicting proteins in this folder.
            <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts.</a>
        </div>
    <%}%>
    <%if(folderType == TargetedMSModule.FolderType.Library){%>
        <div style="color:red; font-weight:bold;">
            There are conflicting peptides in this folder.
            <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts.</a>
        </div>
    <%}%>
<% } %>

