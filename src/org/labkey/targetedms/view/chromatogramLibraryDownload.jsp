<%
/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.security.User" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.TargetedMSModule" %>
<%@ page import="org.labkey.targetedms.chromlib.ChromatogramLibraryUtils" %>
<%@ page import="org.labkey.targetedms.query.ConflictResultsManager" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    Container c = getContainer();
    User user = getUser();
    TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(c);

    long peptideGroupCount = TargetedMSController.getNumRepresentativeProteins(user, c);
    long peptideCount = TargetedMSController.getNumRepresentativePeptides(c);
    long transitionCount = TargetedMSController.getNumRankedTransitions(c);
    DecimalFormat format = new DecimalFormat("#,###");
    int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(c, user);
    Path archiveFile = ChromatogramLibraryUtils.getChromLibFile(c, currentRevision);

    long conflictCount = ConflictResultsManager.getConflictCount(user, c);
    String conflictViewUrl = (folderType == TargetedMSModule.FolderType.LibraryProtein) ?
                                           new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, c).getLocalURIString() :
                                           new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, c).getLocalURIString();
%>

<div class="labkey-download">
<%
        if (peptideCount > 0 || peptideGroupCount > 0)
        {
%>
<div class="banner">
<table>
<tr>
    <! -- graph # of proteins and peptides in the library -->
<td><img src="<%= h(new ActionURL(TargetedMSController.GraphLibraryStatisticsAction.class, c)) %>"></td>
<td style="vert-align: top; text-align: center; padding-left: 2em">
<h3><%= h(c.getName())%> Library</h3>
<p>
    <%= button("Download").href(new ActionURL(TargetedMSController.DownloadChromLibraryAction.class, c))%>
</p>
        <%= h(ChromatogramLibraryUtils.getDownloadFileName(c, currentRevision)) %>
    <%= h(Files.exists(archiveFile) ? "(" + FileUtils.byteCountToDisplaySize(Files.size(archiveFile)) + ")" : "") %>
    <br/>
    Revision <%= h(currentRevision)%><br/>
    <br/>
<%= link("Archived Revisions", new ActionURL(TargetedMSController.ArchivedRevisionsAction.class, c))%>
</tr>
</table>
</div>
<table width="100%">
<tr>
<td width="80%">
    <h3>Library Statistics:</h3>
    <p class="banner">
<%
            if (folderType == TargetedMSModule.FolderType.Library)
            {
%>
        The library contains <%= h(format.format(peptideCount))%> peptides with <%= h(format.format(transitionCount))%> ranked transitions.

<%
                if(conflictCount > 0) {
%>
                    <div style="color:red; font-weight:bold; margin-top:10px">
                        There are <%=conflictCount%> conflicting peptides in this folder.
                        <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts</a>
                    </div>
<%
                }
%>
<%
            }
            else if (folderType == TargetedMSModule.FolderType.LibraryProtein)
            {
%>
        The library contains <%= h(format.format(peptideGroupCount))%> proteins with <%= h(format.format(peptideCount)) %> ranked peptides.<br>
        The <%=h(format.format(peptideCount))%> ranked peptides contain <%= h(format.format(transitionCount))%> ranked transitions.

<%
                if(conflictCount > 0) {
%>
                    <div style="color:red; font-weight:bold; margin-top:10px">
                        There are <%=conflictCount%> conflicting proteins in this folder.
                        <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">Resolve conflicts</a>
                    </div>
<%
                }
%>
<%
            }
%>
</td><td valign="top" width="20%" nowrap>
</td>
</tr>
</table>
<%
        }
        else
        {
%>
  This library does not currently contain any data. Import a file in the Data Pipeline to proceed.
<%
        }
%>
