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
<%@ page import="org.labkey.api.targetedms.TargetedMSService" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.TargetedMSManager" %>
<%@ page import="org.labkey.targetedms.chromlib.ChromatogramLibraryUtils" %>
<%@ page import="org.labkey.targetedms.query.ConflictResultsManager" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.api.security.permissions.InsertPermission" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    Container c = getContainer();
    User user = getUser();
    TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(c);

    long conflictCount = ConflictResultsManager.getConflictCount(user, c);
    boolean hasConflicts = conflictCount != 0;
    int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(c, user);
    int libRevision = hasConflicts ? ChromatogramLibraryUtils.getLastStableLibRevision(c, user) : currentRevision;

    TargetedMSController.ChromLibAnalyteCounts chromLibAnalyteCounts = TargetedMSController.getChromLibAnalyteCounts(user, c, libRevision);
    long peptideGroupCount = chromLibAnalyteCounts.getPeptideGroupCount();
    long peptideCount = chromLibAnalyteCounts.getPeptideCount();
    long transitionCount = chromLibAnalyteCounts.getTransitionCount();
    long moleculeCount = chromLibAnalyteCounts.getMoleculeCount();

    DecimalFormat format = new DecimalFormat("#,###");

    Path archiveFile = ChromatogramLibraryUtils.getChromLibFile(c, libRevision);

    ActionURL conflictViewUrl = (folderType == TargetedMSService.FolderType.LibraryProtein) ?
                                           new ActionURL(TargetedMSController.ShowProteinConflictUiAction.class, c) :
                                           new ActionURL(TargetedMSController.ShowPrecursorConflictUiAction.class, c);

    boolean readOnlyUser = !getContainer().hasPermission(getUser(), InsertPermission.class); // Importing a document and resolving conflicts require Insert permissions.

    String conflictMessage = "";
    String analyteType = folderType == TargetedMSService.FolderType.LibraryProtein ? "protein" : (moleculeCount > 0 ? "molecule" : "peptide");
    boolean libFileExists = Files.exists(archiveFile);
    if(hasConflicts)
    {
        String stableLibFileText = libFileExists ? "The download link below is for the last stable version of the library."
                : "The last stable version of the library could not be found on the server."; // In case the file has been deleted from the server.
        conflictMessage = readOnlyUser ? "The chromatogram library in this folder is in a conflicted state and is awaiting action from a folder administrator to resolve the conflicts. " +
                stableLibFileText
                : "The last Skyline document imported in this folder had " + StringUtilsLabKey.pluralize(conflictCount, analyteType) + " that were already a part of the library. " +
                "Please click the link below to resolve conflicts and choose the version of each " + analyteType + " that should be included in the library. " +
                "The library cannot be extended until the conflicts are resolved. " + stableLibFileText;
    }
%>

<div class="labkey-download">

<% if(hasConflicts) { %>
    <div style="color:red; margin-top:10px;"><%= h(conflictMessage) %></div>
    <% if(!readOnlyUser) { %>
      <div style="color:red; font-weight:bold;font-style:italic;margin-top:5px; margin-bottom:5px;">
      <a style="color:red; text-decoration:underline;" href="<%= h(conflictViewUrl) %>">RESOLVE CONFLICTS</a>
      </div>
    <% } %>
<% } %>

<%
        if (ChromatogramLibraryUtils.NO_LIB_REVISION != currentRevision)
        {
%>

<div class="banner">
<table>
<tr>
    <% if(!hasConflicts) { %>
    <! -- graph # of proteins and peptides in the library.
          Display the graph only if there are no conflicts in the folder
    -->
<td><img src="<%= h(new ActionURL(TargetedMSController.GraphLibraryStatisticsAction.class, c)) %>"></td>
    <% } %>
<td style="vert-align: top; text-align: center; padding-left: 2em">
<h3><%= h(c.getName())%> Library</h3>

<% if(libFileExists) { %>
<p>
    <%= button("Download").href(new ActionURL(TargetedMSController.DownloadChromLibraryAction.class, c).addParameter("revision", libRevision))%>
</p>
        <%= h(ChromatogramLibraryUtils.getDownloadFileName(c, libRevision)) %>
    <%= h("(" + FileUtils.byteCountToDisplaySize(Files.size(archiveFile)) + ")") %>
    <br/>
    Revision <%=libRevision%><br/>
    <br/>
<% } else if(libRevision != ChromatogramLibraryUtils.NO_LIB_REVISION) { %>
    <p>Library file <%= h(ChromatogramLibraryUtils.getDownloadFileName(c, libRevision)) %> does not exist on the server.</p>
<% } %>
<%= link("Archived Revisions", new ActionURL(TargetedMSController.ArchivedRevisionsAction.class, c))%>
</tr>
</table>
</div>

<% if(chromLibAnalyteCounts.exists()) { %>
<table width="100%">
<tr>

<td width="80%">
    <h3>Library Statistics:</h3>
    <p class="banner">
<%
            if (folderType == TargetedMSService.FolderType.Library)
            {
%>
        The library contains <%= h(peptideCount > 0 ? (format.format(peptideCount) + " peptides") : "")%>
        <%= h(peptideCount > 0 && moleculeCount > 0 ? "and" : "")%>
        <%= h(moleculeCount > 0 ? (format.format(moleculeCount) + " molecules") : "")%>
        with <%= h(format.format(transitionCount))%> ranked transitions.
<%
            }
            else if (folderType == TargetedMSService.FolderType.LibraryProtein)
            {
%>
        The library contains <%= h(format.format(peptideGroupCount))%> proteins with <%= h(format.format(peptideCount)) %> ranked peptides.<br>
        The <%=h(format.format(peptideCount))%> ranked peptides contain <%= h(format.format(transitionCount))%> ranked transitions.
<%
            }
%>
</td><td valign="top" width="20%" nowrap>
</td>

</tr>
</table>
<% } %>

<%
        }
        else
        {
%>
  This library does not currently contain any data. Import a Skyline document to proceed.
<%
        }
%>
